CREATE TABLE IF NOT EXISTS sys_workflow_definitions (
    id bigserial PRIMARY KEY,
    workflow_code varchar(100) NOT NULL,
    workflow_name varchar(180) NOT NULL,
    module_id bigint,
    submodule_id bigint,
    feature_id bigint,
    subject_type varchar(100) NOT NULL,
    tenant_id bigint,
    business_id bigint,
    engine_type varchar(30) NOT NULL DEFAULT 'LOCAL',
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_workflow_definitions_scope
    ON sys_workflow_definitions(workflow_code, subject_type, COALESCE(tenant_id, -1), COALESCE(business_id, -1));

CREATE TABLE IF NOT EXISTS sys_workflow_versions (
    id bigserial PRIMARY KEY,
    workflow_definition_id bigint NOT NULL REFERENCES sys_workflow_definitions(id),
    version_number integer NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    effective_from timestamp,
    effective_to timestamp,
    published_at timestamp,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_workflow_versions_definition_number
    ON sys_workflow_versions(workflow_definition_id, version_number);

CREATE TABLE IF NOT EXISTS sys_workflow_steps (
    id bigserial PRIMARY KEY,
    workflow_version_id bigint NOT NULL REFERENCES sys_workflow_versions(id),
    step_code varchar(100) NOT NULL,
    step_name varchar(180) NOT NULL,
    display_name varchar(180),
    step_type varchar(30) NOT NULL DEFAULT 'USER_TASK',
    terminal boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_workflow_steps_version_code
    ON sys_workflow_steps(workflow_version_id, step_code);

CREATE TABLE IF NOT EXISTS sys_workflow_transitions (
    id bigserial PRIMARY KEY,
    workflow_version_id bigint NOT NULL REFERENCES sys_workflow_versions(id),
    from_step_id bigint NOT NULL REFERENCES sys_workflow_steps(id),
    to_step_id bigint NOT NULL REFERENCES sys_workflow_steps(id),
    action_code varchar(100) NOT NULL,
    action_name varchar(180) NOT NULL,
    required_privilege_code varchar(120),
    requires_comment boolean NOT NULL DEFAULT false,
    requires_attachment boolean NOT NULL DEFAULT false,
    auto_assign_next_task boolean NOT NULL DEFAULT true,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_workflow_transitions_version_from_action
    ON sys_workflow_transitions(workflow_version_id, from_step_id, action_code);

CREATE TABLE IF NOT EXISTS sys_workflow_assignment_policies (
    id bigserial PRIMARY KEY,
    workflow_version_id bigint NOT NULL REFERENCES sys_workflow_versions(id),
    step_id bigint NOT NULL REFERENCES sys_workflow_steps(id),
    policy_type varchar(40) NOT NULL,
    role_id bigint,
    user_id bigint,
    privilege_code varchar(120),
    branch_scoped boolean NOT NULL DEFAULT false,
    business_scoped boolean NOT NULL DEFAULT false,
    expression_key varchar(120),
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_workflow_instances (
    id bigserial PRIMARY KEY,
    workflow_definition_id bigint NOT NULL REFERENCES sys_workflow_definitions(id),
    workflow_version_id bigint NOT NULL REFERENCES sys_workflow_versions(id),
    workflow_code varchar(100) NOT NULL,
    subject_type varchar(100) NOT NULL,
    subject_id varchar(120) NOT NULL,
    tenant_id bigint,
    business_id bigint,
    branch_id bigint,
    requester_user_id bigint,
    current_step_id bigint REFERENCES sys_workflow_steps(id),
    status varchar(30) NOT NULL DEFAULT 'RUNNING',
    started_at timestamp,
    completed_at timestamp,
    cancelled_at timestamp,
    engine_instance_id varchar(120),
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS ix_sys_workflow_instances_subject
    ON sys_workflow_instances(subject_type, subject_id);

CREATE TABLE IF NOT EXISTS sys_workflow_tasks (
    id bigserial PRIMARY KEY,
    workflow_instance_id bigint NOT NULL REFERENCES sys_workflow_instances(id),
    step_id bigint NOT NULL REFERENCES sys_workflow_steps(id),
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    assigned_user_id bigint,
    assigned_role_id bigint,
    assigned_privilege_code varchar(120),
    tenant_id bigint,
    business_id bigint,
    branch_id bigint,
    claimed_by_user_id bigint,
    claimed_at timestamp,
    due_at timestamp,
    completed_at timestamp,
    engine_task_id varchar(120),
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS ix_sys_workflow_tasks_instance_status
    ON sys_workflow_tasks(workflow_instance_id, status);

CREATE INDEX IF NOT EXISTS ix_sys_workflow_tasks_assignment
    ON sys_workflow_tasks(status, assigned_user_id, assigned_role_id, assigned_privilege_code);

CREATE TABLE IF NOT EXISTS sys_workflow_history (
    id bigserial PRIMARY KEY,
    workflow_instance_id bigint NOT NULL REFERENCES sys_workflow_instances(id),
    workflow_task_id bigint REFERENCES sys_workflow_tasks(id),
    event_type varchar(40) NOT NULL,
    action_code varchar(100),
    from_step_code varchar(100),
    to_step_code varchar(100),
    actor_user_id bigint,
    actor_username varchar(120),
    comment_text text,
    message_code varchar(120),
    safe_context_json text,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS ix_sys_workflow_history_instance
    ON sys_workflow_history(workflow_instance_id, created_at);

CREATE TABLE IF NOT EXISTS sys_workflow_engine_mappings (
    id bigserial PRIMARY KEY,
    workflow_definition_id bigint NOT NULL REFERENCES sys_workflow_definitions(id),
    workflow_version_id bigint REFERENCES sys_workflow_versions(id),
    engine_type varchar(30) NOT NULL,
    engine_deployment_id varchar(120),
    engine_process_key varchar(120),
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);
