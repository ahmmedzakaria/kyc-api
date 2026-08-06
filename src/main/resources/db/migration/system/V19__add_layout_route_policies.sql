CREATE TABLE sys_layout_route_policies (
    id bigserial PRIMARY KEY,
    client_application_id bigint REFERENCES sys_priv_client_applications(id) ON DELETE CASCADE,
    route_url varchar(255) NOT NULL,
    match_mode varchar(20) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_layout_route_policies_match_mode CHECK (match_mode IN ('ANY', 'ALL'))
);

CREATE UNIQUE INDEX uk_sys_layout_route_policies_global_url
    ON sys_layout_route_policies(route_url)
    WHERE client_application_id IS NULL;

CREATE UNIQUE INDEX uk_sys_layout_route_policies_client_url
    ON sys_layout_route_policies(client_application_id, route_url)
    WHERE client_application_id IS NOT NULL;

CREATE INDEX idx_sys_layout_route_policies_client_active
    ON sys_layout_route_policies(client_application_id, active);

CREATE TABLE sys_layout_route_policy_privileges (
    route_policy_id bigint NOT NULL REFERENCES sys_layout_route_policies(id) ON DELETE CASCADE,
    privilege_id bigint NOT NULL REFERENCES sys_priv_privileges(id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (route_policy_id, privilege_id)
);

CREATE INDEX idx_sys_layout_route_policy_privileges_policy_active
    ON sys_layout_route_policy_privileges(route_policy_id, active);
