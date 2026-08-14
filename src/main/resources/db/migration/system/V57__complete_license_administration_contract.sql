ALTER TABLE sys_license_subscriptions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_license_plan_entitlements ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_license_entitlement_overrides ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_sys_license_usage_subscription_measured
    ON sys_license_usage_snapshots (license_subscription_id, measured_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_license_audit_subscription_created
    ON sys_license_audit_events (license_subscription_id, created_at DESC);

INSERT INTO sys_acc_client_api_permissions (client_application_id, api_registry_id, active, created_by, updated_by, created_at, updated_at)
SELECT client.id, api.id, TRUE, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api ON api.path_pattern IN (
    '/api/v1/system/license/key/revoke', '/api/v1/system/license/usage/list',
    '/api/v1/system/license/audit/list', '/api/v1/system/license/renewal/summary')
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, api_registry_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;
