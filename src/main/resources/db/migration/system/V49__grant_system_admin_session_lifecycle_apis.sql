-- SYSTEM_ADMIN_WEB uses the shared authentication lifecycle from
-- @nexacore/platform. These authenticated endpoints are required for initial
-- context loading, active-session monitoring, and server-side logout.
INSERT INTO sys_acc_client_api_permissions (
    client_application_id, api_registry_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, api.id, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api
  ON api.path_pattern IN (
      '/api/v1/auth/application-context',
      '/api/v1/auth/session-status',
      '/api/v1/auth/logout'
  )
 AND api.active = true
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id) DO UPDATE
SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;
