ALTER TABLE kyc_person
    ADD COLUMN IF NOT EXISTS tenant_id bigint,
    ADD COLUMN IF NOT EXISTS business_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;

CREATE INDEX IF NOT EXISTS idx_kyc_person_scope
    ON kyc_person(tenant_id, business_id, branch_id);

-- Existing rows remain unassigned and therefore invisible to scoped API queries
-- until an administrator explicitly assigns their trusted organizational owner.
