CREATE TABLE auth_user_scope_assignments (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    tenant_id bigint NOT NULL,
    business_id bigint,
    branch_id bigint,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_auth_user_scope_branch_business
        CHECK (branch_id IS NULL OR business_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_auth_user_scope_assignment
    ON auth_user_scope_assignments (
        user_id,
        tenant_id,
        COALESCE(business_id, -1),
        COALESCE(branch_id, -1)
    );

CREATE INDEX idx_auth_user_scope_lookup
    ON auth_user_scope_assignments(user_id, active, tenant_id, business_id, branch_id);

INSERT INTO auth_user_scope_assignments (
    user_id, tenant_id, business_id, branch_id,
    active, created_by, updated_by
)
SELECT id, tenant_id, business_id, branch_id, true, 0, 0
FROM auth_users
WHERE tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;

DROP INDEX IF EXISTS idx_auth_users_scope;

ALTER TABLE auth_users
    DROP COLUMN IF EXISTS tenant_id,
    DROP COLUMN IF EXISTS business_id,
    DROP COLUMN IF EXISTS branch_id;
