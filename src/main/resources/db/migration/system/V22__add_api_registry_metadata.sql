ALTER TABLE sys_priv_api_registry
    ADD COLUMN client_authentication_requirement varchar(30) NOT NULL DEFAULT 'REQUIRED',
    ADD COLUMN user_authorization_requirement varchar(30) NOT NULL DEFAULT 'PRIVILEGE',
    ADD COLUMN data_scope varchar(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN source varchar(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN priority integer NOT NULL DEFAULT 0,
    ADD COLUMN last_synchronized_at timestamp;

CREATE INDEX idx_sys_priv_api_registry_source_active
    ON sys_priv_api_registry(source, active);

CREATE UNIQUE INDEX uk_sys_priv_api_registry_method_path_source
    ON sys_priv_api_registry(http_method, path_pattern)
    WHERE source = 'ANNOTATION';
