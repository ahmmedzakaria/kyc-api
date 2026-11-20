ALTER TABLE sys_acc_client_feature_permissions
    ADD COLUMN tenant_id bigint;

CREATE TEMP TABLE legacy_client_feature_permissions ON COMMIT DROP AS
SELECT client_application_id, privilege_id, active,
       created_by, updated_by, created_at, updated_at
FROM sys_acc_client_feature_permissions;

ALTER TABLE sys_acc_client_feature_permissions
    DROP CONSTRAINT uk_sys_acc_client_feature_permissions;

DELETE FROM sys_acc_client_feature_permissions;

INSERT INTO sys_acc_client_feature_permissions (
    client_application_id, tenant_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT DISTINCT legacy.client_application_id,
       assignment.tenant_id,
       legacy.privilege_id,
       legacy.active,
       legacy.created_by,
       legacy.updated_by,
       legacy.created_at,
       legacy.updated_at
FROM legacy_client_feature_permissions legacy
JOIN sys_acc_client_application_tenants assignment
  ON assignment.client_application_id = legacy.client_application_id
 AND assignment.active = true;

ALTER TABLE sys_acc_client_feature_permissions
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_acc_client_feature_permissions_tenant
        FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT uk_sys_acc_client_feature_permissions_tenant
        UNIQUE (tenant_id, client_application_id, privilege_id);

CREATE INDEX idx_sys_acc_client_feature_permissions_tenant_client_active
    ON sys_acc_client_feature_permissions (tenant_id, client_application_id, active);
