-- Effective authorization intersects role privileges with client permissions.
-- Seed the tenant catalog entries here as well as through the runtime provider,
-- then grant them to the dedicated administration frontend.

INSERT INTO sys_priv_features (
    submodule_id, feature_type_id, feature_code, feature_name,
    active, created_by, updated_by, created_at, updated_at
)
SELECT submodule.id, feature_type.id, '009', 'Tenant Administration',
       true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules module
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id AND submodule.code = '02'
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL AND feature_type.feature_type_code = '01'
WHERE module.code = '11'
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
    ('11020100901', '01'),
    ('11020100910', '10'),
    ('11020100980', '80'),
    ('11020100987', '87')
) AS seed(privilege_code, action_code)
JOIN sys_priv_modules module ON module.code = '11'
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id AND submodule.code = '02'
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL AND feature_type.feature_type_code = '01'
JOIN sys_priv_features feature
  ON feature.submodule_id = submodule.id
 AND feature.feature_type_id = feature_type.id
 AND feature.feature_code = '009'
JOIN sys_priv_actions action ON action.action_code = seed.action_code
ON CONFLICT (privilege_code) DO UPDATE
SET feature_id = EXCLUDED.feature_id,
    action_id = EXCLUDED.action_id,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

-- V43 may have run before the runtime catalog synchronizer created TENANT_VIEW.
INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT feature.id, privilege.id, 'ANY', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_features feature
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11020100901'
WHERE feature.feature_code = 'TENANT_ADMINISTRATION'
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_acc_client_feature_permissions (
    client_application_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, privilege.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_priv_privileges privilege
  ON privilege.privilege_code IN ('11020100901', '11020100910', '11020100980', '11020100987')
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, privilege_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

-- Existing installations already have annotation-discovered API rows.
-- DataSeeder repeats this grant after synchronization for fresh installations.
INSERT INTO sys_acc_client_api_permissions (
    client_application_id, api_registry_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, api.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api
  ON api.path_pattern LIKE '/api/v1/system/tenants%'
 AND api.active = true
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;
