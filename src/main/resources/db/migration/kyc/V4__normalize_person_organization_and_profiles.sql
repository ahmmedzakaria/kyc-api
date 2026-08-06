CREATE TABLE IF NOT EXISTS kyc_person_profiles (
    id bigserial PRIMARY KEY,
    person_id bigint NOT NULL REFERENCES kyc_person(id),
    tenant_id bigint NOT NULL,
    business_id bigint,
    branch_id bigint,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_kyc_person_profile_branch_business CHECK (branch_id IS NULL OR business_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_person_profile_scope
    ON kyc_person_profiles(person_id, tenant_id, COALESCE(business_id, -1), COALESCE(branch_id, -1));
CREATE INDEX IF NOT EXISTS idx_kyc_person_profile_scope
    ON kyc_person_profiles(active, tenant_id, business_id, branch_id, person_id);

CREATE TABLE IF NOT EXISTS kyc_person_organization_memberships (
    id bigserial PRIMARY KEY,
    person_id bigint NOT NULL REFERENCES kyc_person(id),
    tenant_id bigint NOT NULL,
    business_id bigint,
    branch_id bigint,
    membership_type varchar(40) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    primary_membership boolean NOT NULL DEFAULT false,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_kyc_person_membership_branch_business CHECK (branch_id IS NULL OR business_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_person_membership_scope_type
    ON kyc_person_organization_memberships(
        person_id, tenant_id, COALESCE(business_id, -1), COALESCE(branch_id, -1), membership_type
    );
CREATE INDEX IF NOT EXISTS idx_kyc_person_membership_scope
    ON kyc_person_organization_memberships(active, tenant_id, business_id, branch_id, person_id);

INSERT INTO kyc_person_profiles (
    person_id, tenant_id, business_id, branch_id, active, created_by, updated_by
)
SELECT id, tenant_id, business_id, branch_id, true, 0, 0
FROM kyc_person
WHERE tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO kyc_person_organization_memberships (
    person_id, tenant_id, business_id, branch_id, membership_type,
    active, primary_membership, created_by, updated_by
)
SELECT id, tenant_id, business_id, branch_id, 'CUSTOMER', true, true, 0, 0
FROM kyc_person
WHERE tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE kyc_person_document ADD COLUMN IF NOT EXISTS profile_id bigint;
ALTER TABLE kyc_person_details ADD COLUMN IF NOT EXISTS profile_id bigint;

UPDATE kyc_person_document document
SET profile_id = (SELECT profile.id FROM kyc_person_profiles profile
                  WHERE profile.person_id = document.person_id
                  ORDER BY profile.id LIMIT 1)
WHERE document.profile_id IS NULL;

UPDATE kyc_person_details details
SET profile_id = (SELECT profile.id FROM kyc_person_profiles profile
                  WHERE profile.person_id = details.person_id
                  ORDER BY profile.id LIMIT 1)
WHERE details.profile_id IS NULL;

ALTER TABLE kyc_person_document
    ADD CONSTRAINT fk_kyc_person_document_profile
        FOREIGN KEY (profile_id) REFERENCES kyc_person_profiles(id);
ALTER TABLE kyc_person_details
    ADD CONSTRAINT fk_kyc_person_details_profile
        FOREIGN KEY (profile_id) REFERENCES kyc_person_profiles(id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_person_details_profile
    ON kyc_person_details(profile_id) WHERE profile_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kyc_person_document_profile
    ON kyc_person_document(profile_id, document_type, created_at DESC);

DO $$
DECLARE constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'kyc_person_details'::regclass
      AND contype = 'u'
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                          WHERE attrelid = 'kyc_person_details'::regclass AND attname = 'person_id')]::smallint[]
    LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE kyc_person_details DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

DROP INDEX IF EXISTS idx_kyc_person_scope;
ALTER TABLE kyc_person
    DROP COLUMN IF EXISTS tenant_id,
    DROP COLUMN IF EXISTS business_id,
    DROP COLUMN IF EXISTS branch_id;

-- Rows that had no trusted legacy owner deliberately retain a NULL profile_id.
-- They remain inaccessible and must be assigned through a reviewed backfill before
-- the profile foreign keys can be made NOT NULL in a later migration.
