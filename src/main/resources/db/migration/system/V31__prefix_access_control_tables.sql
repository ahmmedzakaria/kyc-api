-- Access-control tables were originally grouped under the privilege prefix.
-- Rename them in place so existing identifiers, data, and foreign keys are preserved.
ALTER TABLE sys_priv_client_applications RENAME TO sys_acc_client_applications;
ALTER TABLE sys_priv_client_credentials RENAME TO sys_acc_client_credentials;
ALTER TABLE sys_priv_api_registry RENAME TO sys_acc_api_registry;
ALTER TABLE sys_priv_client_api_permissions RENAME TO sys_acc_client_api_permissions;
ALTER TABLE sys_priv_client_feature_permissions RENAME TO sys_acc_client_feature_permissions;
ALTER TABLE sys_priv_client_application_tenants RENAME TO sys_acc_client_application_tenants;

ALTER INDEX IF EXISTS idx_sys_client_credentials_app
    RENAME TO idx_sys_acc_client_credentials_app;
ALTER INDEX IF EXISTS idx_sys_api_registry_method_active
    RENAME TO idx_sys_acc_api_registry_method_active;
ALTER INDEX IF EXISTS idx_sys_priv_api_registry_source_active
    RENAME TO idx_sys_acc_api_registry_source_active;
ALTER INDEX IF EXISTS uk_sys_priv_api_registry_method_path_source
    RENAME TO uk_sys_acc_api_registry_method_path_source;
ALTER INDEX IF EXISTS uk_sys_priv_api_registry_active_method_path
    RENAME TO uk_sys_acc_api_registry_active_method_path;

ALTER TABLE sys_acc_client_api_permissions
    RENAME CONSTRAINT uk_sys_client_api_permissions TO uk_sys_acc_client_api_permissions;
ALTER TABLE sys_acc_client_feature_permissions
    RENAME CONSTRAINT uk_sys_client_feature_permissions TO uk_sys_acc_client_feature_permissions;
ALTER TABLE sys_acc_client_application_tenants
    RENAME CONSTRAINT uk_sys_client_application_tenants TO uk_sys_acc_client_application_tenants;
