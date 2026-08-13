-- Replace the legacy independent tenant/business lists with hierarchical tuples.
ALTER TABLE sys_acc_client_application_tenants
    ADD COLUMN IF NOT EXISTS branch_id bigint;

CREATE TABLE IF NOT EXISTS sys_acc_client_scope_quarantine (
    id bigserial PRIMARY KEY,
    source_assignment_id bigint NOT NULL,
    client_application_id bigint NOT NULL,
    tenant_id bigint,
    business_id bigint,
    branch_id bigint,
    quarantine_reason varchar(120) NOT NULL,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_acc_client_scope_quarantine (
    source_assignment_id, client_application_id, tenant_id, business_id, branch_id,
    quarantine_reason, created_by, updated_by, created_at, updated_at
)
SELECT id, client_application_id, tenant_id, business_id, branch_id,
       'TENANT_OWNERSHIP_UNRESOLVED', 0, 0, now(), now()
FROM sys_acc_client_application_tenants
WHERE tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- Legacy business-only rows cannot be assigned a tenant from trusted system data.
-- Quarantine and fail closed rather than guessing ownership.
DELETE FROM sys_acc_client_application_tenants WHERE tenant_id IS NULL;

ALTER TABLE sys_acc_client_application_tenants
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE sys_acc_client_application_tenants
    DROP CONSTRAINT IF EXISTS ck_sys_acc_client_scope_hierarchy;
ALTER TABLE sys_acc_client_application_tenants
    ADD CONSTRAINT ck_sys_acc_client_scope_hierarchy
        CHECK (branch_id IS NULL OR business_id IS NOT NULL);

ALTER TABLE sys_acc_client_application_tenants
    DROP CONSTRAINT IF EXISTS fk_sys_acc_client_scope_tenant;
ALTER TABLE sys_acc_client_application_tenants
    ADD CONSTRAINT fk_sys_acc_client_scope_tenant
        FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id);

DROP INDEX IF EXISTS uk_sys_client_application_tenants;
ALTER TABLE sys_acc_client_application_tenants
    DROP CONSTRAINT IF EXISTS uk_sys_acc_client_application_tenants;
CREATE UNIQUE INDEX uk_sys_acc_client_scope_tuple
    ON sys_acc_client_application_tenants (
        client_application_id,
        tenant_id,
        COALESCE(business_id, -1),
        COALESCE(branch_id, -1)
    );

CREATE INDEX idx_sys_acc_client_scope_lookup
    ON sys_acc_client_application_tenants (client_application_id, active, tenant_id, business_id, branch_id);
