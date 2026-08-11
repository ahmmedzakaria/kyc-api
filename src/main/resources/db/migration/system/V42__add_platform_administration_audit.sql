CREATE TABLE sys_platform_admin_audit_events (
    id bigserial PRIMARY KEY,
    actor_user_id bigint NOT NULL,
    actor_tenant_id bigint,
    target_tenant_id bigint,
    action_code varchar(100) NOT NULL,
    outcome varchar(30) NOT NULL,
    reason varchar(500),
    trace_id varchar(100),
    created_by bigint NOT NULL,
    updated_by bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_platform_admin_audit_actor_tenant
        FOREIGN KEY (actor_tenant_id) REFERENCES sys_tenants(id),
    CONSTRAINT fk_sys_platform_admin_audit_target_tenant
        FOREIGN KEY (target_tenant_id) REFERENCES sys_tenants(id)
);

CREATE INDEX idx_sys_platform_admin_audit_target_time
    ON sys_platform_admin_audit_events(target_tenant_id, created_at DESC);
CREATE INDEX idx_sys_platform_admin_audit_actor_time
    ON sys_platform_admin_audit_events(actor_user_id, created_at DESC);
