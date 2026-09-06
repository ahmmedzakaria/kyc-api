-- Dashboard navigation entries were intentionally created without privilege
-- links in V24 and V48. LayoutNavigationServiceImpl treats an unlinked feature
-- as globally visible, so SYSTEM_ADMIN_WEB, LOG_ADMIN_WEB, and WEB could see
-- one another's dashboard/module branch. Give each dashboard a dedicated VIEW
-- privilege and grant it only to the frontend client that owns the route.

INSERT INTO sys_priv_modules (
    code, name, active, created_by, updated_by, created_at, updated_at
)
VALUES
    ('11', 'System', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('09', 'Log', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_submodules (
    module_id, code, name, active,
    created_by, updated_by, created_at, updated_at
)
SELECT module.id, seed.submodule_code, seed.submodule_name, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('11', '02', 'Access Control'),
    ('09', '01', 'Log Administration')
) AS seed(module_code, submodule_code, submodule_name)
JOIN sys_priv_modules module ON module.code = seed.module_code
ON CONFLICT (module_id, code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_features (
    submodule_id, feature_type_id, feature_code, feature_name,
    active, created_by, updated_by, created_at, updated_at
)
SELECT submodule.id, feature_type.id, seed.feature_code, seed.feature_name,
       true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('11', '02', '010', 'System Dashboard'),
    ('09', '01', '004', 'Log Dashboard')
) AS seed(module_code, submodule_code, feature_code, feature_name)
JOIN sys_priv_modules module ON module.code = seed.module_code
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id
 AND submodule.code = seed.submodule_code
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL
 AND feature_type.feature_type_code = '01'
ON CONFLICT (submodule_id, feature_type_id, feature_code) DO UPDATE
SET feature_name = EXCLUDED.feature_name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_privileges (
    privilege_code, feature_id, action_id, sub_menu_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT seed.privilege_code, feature.id, action.id, NULL, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('11020101001', '11', '02', '010'),
    ('09010100401', '09', '01', '004')
) AS seed(privilege_code, module_code, submodule_code, feature_code)
JOIN sys_priv_modules module ON module.code = seed.module_code
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id
 AND submodule.code = seed.submodule_code
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL
 AND feature_type.feature_type_code = '01'
JOIN sys_priv_features feature
  ON feature.submodule_id = submodule.id
 AND feature.feature_type_id = feature_type.id
 AND feature.feature_code = seed.feature_code
JOIN sys_priv_actions action ON action.action_code = '01'
ON CONFLICT (privilege_code) DO UPDATE
SET feature_id = EXCLUDED.feature_id,
    action_id = EXCLUDED.action_id,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT feature.id, privilege.id, 'ANY', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('SYS_ADMIN_DASHBOARD', '11020101001'),
    ('LOG_ADMIN_DASHBOARD', '09010100401')
) AS seed(t_code, privilege_code)
JOIN sys_layout_features feature ON feature.t_code = seed.t_code
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE
SET match_mode = 'ANY',
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_acc_client_feature_permissions (
    client_application_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, privilege.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('SYSTEM_ADMIN_WEB', '11020101001'),
    ('LOG_ADMIN_WEB', '09010100401')
) AS seed(client_code, privilege_code)
JOIN sys_acc_client_applications client ON client.client_code = seed.client_code
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
ON CONFLICT (client_application_id, privilege_id) DO UPDATE
SET active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;
