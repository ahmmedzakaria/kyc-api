INSERT INTO sys_acc_client_api_permissions(client_application_id, api_registry_id, active, created_by, updated_by, created_at, updated_at)
SELECT client.id, api.id, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api ON api.path_pattern LIKE '/api/v1/system/auth-policy/%' AND api.active
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT(client_application_id, api_registry_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_route_policies(client_application_id, route_url, match_mode, active, created_by, updated_by, created_at, updated_at)
SELECT client.id, '/access-control/auth-policies', 'ANY', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT(client_application_id, route_url) WHERE client_application_id IS NOT NULL DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_route_policy_privileges(route_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at)
SELECT policy.id, privilege.id, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_route_policies policy
JOIN sys_acc_client_applications client ON client.id = policy.client_application_id
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11020100101'
WHERE client.client_code = 'SYSTEM_ADMIN_WEB' AND policy.route_url = '/access-control/auth-policies'
ON CONFLICT(route_policy_id, privilege_id) DO UPDATE SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_ui_policies(client_application_id, action_code, match_mode, active, created_by, updated_by, created_at, updated_at)
SELECT client.id, 'auth-policies.manage', 'ANY', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT(client_application_id, action_code) WHERE client_application_id IS NOT NULL DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_ui_policy_privileges(ui_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at)
SELECT policy.id, privilege.id, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_ui_policies policy
JOIN sys_acc_client_applications client ON client.id = policy.client_application_id
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11020100187'
WHERE client.client_code = 'SYSTEM_ADMIN_WEB' AND policy.action_code = 'auth-policies.manage'
ON CONFLICT(ui_policy_id, privilege_id) DO UPDATE SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;
