-- Baseline migration for system_db.
-- System module owns privilege catalog, privilege menu metadata, and privilege assignments.

CREATE TABLE IF NOT EXISTS sys_sub_menus (
    id bigserial PRIMARY KEY,
    name varchar(255) NOT NULL,
    url varchar(255) NOT NULL,
    icon varchar(255),
    module_code varchar(2) NOT NULL,
    module_name varchar(255) NOT NULL,
    submodule_code varchar(2) NOT NULL,
    submodule_name varchar(255) NOT NULL,
    feature_type_code varchar(2) NOT NULL,
    feature_type_name varchar(255) NOT NULL,
    feature_code varchar(3) NOT NULL,
    feature_name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_sub_menus_feature_url
    ON sys_sub_menus (module_code, submodule_code, feature_type_code, feature_code, url);

CREATE TABLE IF NOT EXISTS sys_privileges (
    id bigserial PRIMARY KEY,
    privilege_code varchar(11) NOT NULL UNIQUE,
    module_code varchar(2) NOT NULL,
    module_name varchar(255) NOT NULL,
    submodule_code varchar(2) NOT NULL,
    submodule_name varchar(255) NOT NULL,
    feature_type_code varchar(2) NOT NULL,
    feature_type_name varchar(255) NOT NULL,
    feature_code varchar(3) NOT NULL,
    feature_name varchar(255) NOT NULL,
    action_code varchar(2) NOT NULL,
    action_name varchar(255) NOT NULL,
    sub_menu_id bigint REFERENCES sys_sub_menus(id),
    active boolean NOT NULL DEFAULT true,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_sys_privileges_module_feature
    ON sys_privileges (module_code, submodule_code, feature_type_code, feature_code);

CREATE TABLE IF NOT EXISTS sys_role_privileges (
    role_id bigint NOT NULL,
    privilege_id bigint NOT NULL REFERENCES sys_privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, privilege_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_role_privileges_role_id
    ON sys_role_privileges (role_id);

CREATE TABLE IF NOT EXISTS sys_user_privileges (
    user_id bigint NOT NULL,
    privilege_id bigint NOT NULL REFERENCES sys_privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, privilege_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_privileges_user_id
    ON sys_user_privileges (user_id);
