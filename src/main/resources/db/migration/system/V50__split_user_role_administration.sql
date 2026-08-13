INSERT INTO sys_layout_route_policies (client_application_id, route_url, match_mode, active, created_by, updated_by, created_at, updated_at)
SELECT c.id, route, 'ANY', true, 0, 0, now(), now() FROM sys_acc_client_applications c
CROSS JOIN (VALUES ('/access-control/roles'), ('/access-control/roles/:id/privileges')) seed(route)
WHERE c.client_code='SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, route_url) WHERE client_application_id IS NOT NULL
DO UPDATE SET active=true, updated_by=0, updated_at=now();

INSERT INTO sys_layout_route_policy_privileges (route_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at)
SELECT rp.id, p.id, true, 0, 0, now(), now() FROM sys_layout_route_policies rp
JOIN sys_acc_client_applications c ON c.id=rp.client_application_id AND c.client_code='SYSTEM_ADMIN_WEB'
JOIN sys_priv_privileges p ON p.privilege_code='11020100801'
WHERE rp.route_url IN ('/access-control/roles','/access-control/roles/:id/privileges')
ON CONFLICT (route_policy_id, privilege_id) DO UPDATE SET active=true, updated_by=0, updated_at=now();

INSERT INTO sys_layout_ui_policies (client_application_id, action_code, match_mode, active, created_by, updated_by, created_at, updated_at)
SELECT c.id, 'roles.manage', 'ANY', true, 0, 0, now(), now() FROM sys_acc_client_applications c WHERE c.client_code='SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, action_code) WHERE client_application_id IS NOT NULL
DO UPDATE SET active=true, updated_by=0, updated_at=now();

INSERT INTO sys_layout_ui_policy_privileges (ui_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at)
SELECT up.id, p.id, true, 0, 0, now(), now() FROM sys_layout_ui_policies up
JOIN sys_acc_client_applications c ON c.id=up.client_application_id AND c.client_code='SYSTEM_ADMIN_WEB'
JOIN sys_priv_privileges p ON p.privilege_code='11020100887' WHERE up.action_code='roles.manage'
ON CONFLICT (ui_policy_id, privilege_id) DO UPDATE SET active=true, updated_by=0, updated_at=now();

INSERT INTO sys_layout_features (feature_group_id, feature_code, t_code, feature_name, route, icon, display_order, active, created_by, updated_by, created_at, updated_at)
SELECT fg.id, 'ROLE_ADMINISTRATION', 'SYS_ROLE_ADMIN', 'Roles', '/access-control/roles', 'shield-check', 71, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg WHERE fg.feature_group_code='ACCESS_CONTROL' ON CONFLICT DO NOTHING;

INSERT INTO sys_layout_feature_privileges (layout_feature_id, privilege_id, match_mode, active, created_by, updated_by, created_at, updated_at)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now() FROM sys_layout_features f
JOIN sys_priv_privileges p ON p.privilege_code='11020100801' WHERE f.feature_code='ROLE_ADMINISTRATION'
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE SET active=true, updated_by=0, updated_at=now();

INSERT INTO sys_acc_client_api_permissions (client_application_id, api_registry_id, active, created_by, updated_by, created_at, updated_at)
SELECT c.id, a.id, true, 0, 0, now(), now() FROM sys_acc_client_applications c
JOIN sys_acc_api_registry a ON a.path_pattern IN ('/api/v1/system/user/detail','/api/v1/system/user/role-assignments',
 '/api/v1/system/role/detail','/api/v1/system/role/privilege-assignments') AND a.active=true
WHERE c.client_code='SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id) DO UPDATE SET active=true, updated_by=0, updated_at=now();
