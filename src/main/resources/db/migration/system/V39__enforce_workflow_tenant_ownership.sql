-- Workflow definitions and runtime records are tenant-owned. Child tables derive
-- ownership through their parent; composite foreign keys prevent cross-tenant links.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sys_workflow_definitions WHERE tenant_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_workflow_instances WHERE tenant_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_workflow_tasks WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Phase 4 blocked: workflow rows exist without trusted tenant ownership';
    END IF;
END $$;

ALTER TABLE sys_workflow_definitions
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_workflow_definitions_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT ck_sys_workflow_definitions_business_tenant CHECK (business_id IS NULL OR tenant_id IS NOT NULL),
    ADD CONSTRAINT uk_sys_workflow_definitions_id_tenant UNIQUE (id, tenant_id);

ALTER TABLE sys_workflow_instances
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_workflow_instances_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT ck_sys_workflow_instances_scope CHECK (branch_id IS NULL OR business_id IS NOT NULL),
    ADD CONSTRAINT uk_sys_workflow_instances_id_tenant UNIQUE (id, tenant_id),
    ADD CONSTRAINT fk_sys_workflow_instances_definition_tenant
        FOREIGN KEY (workflow_definition_id, tenant_id)
        REFERENCES sys_workflow_definitions(id, tenant_id);

ALTER TABLE sys_workflow_tasks
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_workflow_tasks_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT ck_sys_workflow_tasks_scope CHECK (branch_id IS NULL OR business_id IS NOT NULL),
    ADD CONSTRAINT fk_sys_workflow_tasks_instance_tenant
        FOREIGN KEY (workflow_instance_id, tenant_id)
        REFERENCES sys_workflow_instances(id, tenant_id);

CREATE INDEX idx_sys_workflow_definitions_tenant_active
    ON sys_workflow_definitions(tenant_id, active, workflow_code);
CREATE INDEX idx_sys_workflow_instances_tenant_status
    ON sys_workflow_instances(tenant_id, status, id);
CREATE INDEX idx_sys_workflow_tasks_tenant_status
    ON sys_workflow_tasks(tenant_id, status, id);

