-- Phase 4 KYC expansion. Global people remain Auth-owned; profiles and all KYC
-- evidence/legacy records are tenant-owned and must fail closed.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM kyc_person_details WHERE profile_id IS NULL)
       OR EXISTS (SELECT 1 FROM kyc_person_document WHERE profile_id IS NULL) THEN
        RAISE EXCEPTION 'Phase 4 blocked: KYC evidence exists without an owned profile';
    END IF;
END $$;

ALTER TABLE kyc_person_details ALTER COLUMN profile_id SET NOT NULL;
ALTER TABLE kyc_person_document ALTER COLUMN profile_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_kyc_person_details_profile_person
    ON kyc_person_details(profile_id, person_id);
CREATE INDEX IF NOT EXISTS idx_kyc_person_document_profile_id
    ON kyc_person_document(profile_id, id);

CREATE TABLE kyc_record_tenant_quarantine (
    id bigserial PRIMARY KEY,
    source_record_id bigint NOT NULL UNIQUE,
    reason varchar(80) NOT NULL,
    snapshot jsonb NOT NULL,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO kyc_record_tenant_quarantine(source_record_id, reason, snapshot)
SELECT id, 'NO_TRUSTED_TENANT', to_jsonb(record)
FROM kyc_record record
ON CONFLICT (source_record_id) DO NOTHING;

DELETE FROM kyc_record;

ALTER TABLE kyc_record
    ADD COLUMN tenant_id bigint NOT NULL,
    ADD COLUMN created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_by bigint NOT NULL DEFAULT 0;

CREATE INDEX idx_kyc_record_tenant_id ON kyc_record(tenant_id, id);

