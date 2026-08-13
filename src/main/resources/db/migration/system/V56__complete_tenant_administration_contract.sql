-- Detail routes are backend-owned authorization policy, independent of the
-- frontend's semantic inventory.
INSERT INTO sys_layout_route_policies (
    client_application_id, route_url, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, '/access-control/tenants/:id', 'ANY', true, 0, 0, now(), now()
FROM sys_acc_client_applications client
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, route_url) WHERE client_application_id IS NOT NULL
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

INSERT INTO sys_layout_route_policy_privileges (
    route_policy_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM sys_layout_route_policies policy
JOIN sys_acc_client_applications client
  ON client.id = policy.client_application_id AND client.client_code = 'SYSTEM_ADMIN_WEB'
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11020100901'
WHERE policy.route_url = '/access-control/tenants/:id'
ON CONFLICT (route_policy_id, privilege_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

-- Conditional upgrade grant. Fresh installs and registries synchronized after
-- Flyway are reconciled by DataSeeder's /system/tenants path grant.
INSERT INTO sys_acc_client_api_permissions (
    client_application_id, api_registry_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, api.id, true, 0, 0, now(), now()
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api ON api.path_pattern IN (
    '/api/v1/system/tenants/detail',
    '/api/v1/system/tenants/domain/add',
    '/api/v1/system/tenants/domain/set-primary',
    '/api/v1/system/tenants/domain/deactivate'
) AND api.active
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

CREATE INDEX IF NOT EXISTS idx_sys_platform_admin_audit_target_created
    ON sys_platform_admin_audit_events (target_tenant_id, created_at DESC);
