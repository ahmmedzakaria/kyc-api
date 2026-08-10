-- SYSTEM_ADMIN_WEB must permit the same User/Role administration privileges
-- that are assigned to ROLE_SYSTEM_ADMIN. Effective authorization is the
-- intersection of user privileges and client permissions, so both sides are
-- required (same gap V34 closed for Database Backup access).

INSERT INTO sys_acc_client_feature_permissions (
    client_application_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, privilege.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_priv_privileges privilege
  ON privilege.privilege_code IN (
      '11020100701', '11020100787', '11020100781', '11020100801', '11020100887'
  )
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, privilege_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

-- Existing installations already have annotation-discovered API rows when
-- this migration runs. DataSeeder performs the same additive grant after API
-- synchronization to cover a completely fresh installation.
INSERT INTO sys_acc_client_api_permissions (
    client_application_id, api_registry_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, api.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api
  ON (api.path_pattern LIKE '/api/v1/system/user%' OR api.path_pattern LIKE '/api/v1/system/role%')
 AND api.active = true
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;
