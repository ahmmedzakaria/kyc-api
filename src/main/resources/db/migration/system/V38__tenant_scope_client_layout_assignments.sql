-- Phase 3 pilot: client layout assignments are tenant-owned. Layout profiles remain
-- global reusable templates. Existing assignments are expanded across trusted,
-- active client/tenant mappings; unresolved rows are quarantined before cutover.

CREATE TABLE sys_layout_assignment_tenant_quarantine (
    id bigserial PRIMARY KEY,
    source_assignment_id bigint NOT NULL UNIQUE,
    client_application_id bigint NOT NULL,
    layout_profile_id bigint NOT NULL,
    reason varchar(80) NOT NULL,
    snapshot jsonb NOT NULL,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_client_layout_profiles ADD COLUMN tenant_id bigint;

CREATE TEMP TABLE tmp_layout_assignment_tenants ON COMMIT DROP AS
SELECT assignment.id AS assignment_id,
       client_tenant.tenant_id,
       row_number() OVER (PARTITION BY assignment.id ORDER BY client_tenant.tenant_id) AS tenant_rank
FROM sys_client_layout_profiles assignment
JOIN sys_acc_client_application_tenants client_tenant
  ON client_tenant.client_application_id = assignment.client_application_id
 AND client_tenant.active
 AND client_tenant.tenant_id IS NOT NULL
JOIN sys_tenants tenant
  ON tenant.id = client_tenant.tenant_id;

INSERT INTO sys_layout_assignment_tenant_quarantine (
    source_assignment_id, client_application_id, layout_profile_id, reason, snapshot
)
SELECT assignment.id,
       assignment.client_application_id,
       assignment.layout_profile_id,
       'NO_TRUSTED_CLIENT_TENANT',
       to_jsonb(assignment)
FROM sys_client_layout_profiles assignment
WHERE NOT EXISTS (
    SELECT 1 FROM tmp_layout_assignment_tenants mapping
    WHERE mapping.assignment_id = assignment.id
)
ON CONFLICT (source_assignment_id) DO NOTHING;

UPDATE sys_client_layout_profiles assignment
SET tenant_id = mapping.tenant_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM tmp_layout_assignment_tenants mapping
WHERE mapping.assignment_id = assignment.id
  AND mapping.tenant_rank = 1;

INSERT INTO sys_client_layout_profiles (
    client_application_id, layout_profile_id, assignment_scope, role_code,
    privilege_code, device_target, module_code, default_profile, selectable,
    display_order, active, created_by, updated_by, created_at, updated_at, tenant_id
)
SELECT assignment.client_application_id, assignment.layout_profile_id,
       assignment.assignment_scope, assignment.role_code, assignment.privilege_code,
       assignment.device_target, assignment.module_code, assignment.default_profile,
       assignment.selectable, assignment.display_order, assignment.active,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, mapping.tenant_id
FROM tmp_layout_assignment_tenants mapping
JOIN sys_client_layout_profiles assignment ON assignment.id = mapping.assignment_id
WHERE mapping.tenant_rank > 1;

DELETE FROM sys_client_layout_profiles WHERE tenant_id IS NULL;

DROP INDEX IF EXISTS uk_sys_client_layout_profiles_one_default;

ALTER TABLE sys_client_layout_profiles
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_client_layout_profiles_tenant
        FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT uk_sys_client_layout_profiles_tenant_assignment
        UNIQUE (tenant_id, client_application_id, layout_profile_id, assignment_scope,
                role_code, privilege_code, device_target, module_code);

CREATE UNIQUE INDEX uk_sys_client_layout_profiles_one_default
    ON sys_client_layout_profiles(tenant_id, client_application_id)
    WHERE default_profile = true AND active = true;

CREATE INDEX idx_sys_client_layout_profiles_tenant_client_active
    ON sys_client_layout_profiles(tenant_id, client_application_id, active);

