-- Authorize the Client Feature Permission page for SYSTEM_ADMIN_WEB.
-- The Angular route is /client-feature-permission.
-- Access requires CLIENT_FEATURE_PERMISSION_ASSIGN (11020100481).

INSERT INTO sys_layout_route_policies (
    client_application_id,
    route_url,
    match_mode,
    active,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    client.id,
    '/client-feature-permission',
    'ANY',
    true,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, route_url)
    WHERE client_application_id IS NOT NULL
DO UPDATE SET
    match_mode = EXCLUDED.match_mode,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_route_policy_privileges (
    route_policy_id,
    privilege_id,
    active,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    route_policy.id,
    privilege.id,
    true,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM sys_layout_route_policies route_policy
JOIN sys_acc_client_applications client
  ON client.id = route_policy.client_application_id
JOIN sys_priv_privileges privilege
  ON privilege.privilege_code = '11020100481'
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
  AND route_policy.route_url = '/client-feature-permission'
ON CONFLICT (route_policy_id, privilege_id)
DO UPDATE SET
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;
