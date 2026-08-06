ALTER TABLE auth_users
    ADD COLUMN IF NOT EXISTS tenant_id bigint,
    ADD COLUMN IF NOT EXISTS business_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;

CREATE INDEX IF NOT EXISTS idx_auth_users_scope
    ON auth_users(tenant_id, business_id, branch_id);
