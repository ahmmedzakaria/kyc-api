CREATE TABLE IF NOT EXISTS sys_tenant_businesses (
 id bigserial PRIMARY KEY, tenant_id bigint NOT NULL REFERENCES sys_tenants(id), business_code varchar(100) NOT NULL,
 display_name varchar(200) NOT NULL, active boolean NOT NULL DEFAULT true,
 created_by bigint NOT NULL, updated_by bigint NOT NULL, created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(),
 CONSTRAINT uk_sys_tenant_business UNIQUE (tenant_id, business_code), CONSTRAINT uk_sys_tenant_business_id UNIQUE (tenant_id, id)
);
CREATE INDEX IF NOT EXISTS idx_sys_tenant_business_active ON sys_tenant_businesses(tenant_id, active, display_name);

CREATE TABLE IF NOT EXISTS sys_tenant_branches (
 id bigserial PRIMARY KEY, tenant_id bigint NOT NULL, business_id bigint NOT NULL, branch_code varchar(100) NOT NULL,
 display_name varchar(200) NOT NULL, active boolean NOT NULL DEFAULT true,
 created_by bigint NOT NULL, updated_by bigint NOT NULL, created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(),
 CONSTRAINT fk_sys_tenant_branch_business FOREIGN KEY (tenant_id,business_id) REFERENCES sys_tenant_businesses(tenant_id,id),
 CONSTRAINT uk_sys_tenant_branch UNIQUE (business_id,branch_code), CONSTRAINT uk_sys_tenant_branch_id UNIQUE (tenant_id,business_id,id)
);
CREATE INDEX IF NOT EXISTS idx_sys_tenant_branch_active ON sys_tenant_branches(tenant_id,business_id,active,display_name);

INSERT INTO sys_acc_client_api_permissions (client_application_id,api_registry_id,active,created_by,updated_by,created_at,updated_at)
SELECT c.id,a.id,true,0,0,now(),now() FROM sys_acc_client_applications c
JOIN sys_acc_api_registry a ON a.path_pattern IN ('/api/v1/system/tenants/authorized','/api/v1/system/tenants/businesses','/api/v1/system/tenants/branches') AND a.active
WHERE c.client_code='SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id,api_registry_id) DO UPDATE SET active=true,updated_by=0,updated_at=now();
