-- Structured request/client/user context for API, error, and audit logs.

CREATE TABLE IF NOT EXISTS log_api_access_log (
    id bigserial PRIMARY KEY,
    method varchar(255),
    uri varchar(255),
    status integer NOT NULL,
    username varchar(255),
    request_body text,
    response_body text,
    created_at timestamp
);

CREATE TABLE IF NOT EXISTS log_error_log (
    id bigserial PRIMARY KEY,
    method varchar(255),
    uri varchar(255),
    status integer NOT NULL,
    username varchar(255),
    error_type varchar(255),
    message text,
    request_body text,
    response_body text,
    created_at timestamp
);

CREATE TABLE IF NOT EXISTS log_audit_log (
    id bigserial PRIMARY KEY,
    username varchar(255),
    action varchar(255),
    entity_name varchar(255),
    entity_id varchar(255),
    details text,
    created_at timestamp
);

ALTER TABLE log_api_access_log
    ADD COLUMN IF NOT EXISTS trace_id varchar(64),
    ADD COLUMN IF NOT EXISTS client_code varchar(50),
    ADD COLUMN IF NOT EXISTS client_type varchar(50),
    ADD COLUMN IF NOT EXISTS user_id bigint,
    ADD COLUMN IF NOT EXISTS api_code varchar(100),
    ADD COLUMN IF NOT EXISTS module_code varchar(2),
    ADD COLUMN IF NOT EXISTS module_name varchar(255),
    ADD COLUMN IF NOT EXISTS submodule_code varchar(2),
    ADD COLUMN IF NOT EXISTS submodule_name varchar(255),
    ADD COLUMN IF NOT EXISTS feature_code varchar(3),
    ADD COLUMN IF NOT EXISTS feature_name varchar(255),
    ADD COLUMN IF NOT EXISTS action_code varchar(2),
    ADD COLUMN IF NOT EXISTS action_name varchar(255),
    ADD COLUMN IF NOT EXISTS access_mode varchar(50),
    ADD COLUMN IF NOT EXISTS decision varchar(50),
    ADD COLUMN IF NOT EXISTS deny_reason varchar(255),
    ADD COLUMN IF NOT EXISTS business_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;

ALTER TABLE log_error_log
    ADD COLUMN IF NOT EXISTS trace_id varchar(64),
    ADD COLUMN IF NOT EXISTS client_code varchar(50),
    ADD COLUMN IF NOT EXISTS client_type varchar(50),
    ADD COLUMN IF NOT EXISTS user_id bigint,
    ADD COLUMN IF NOT EXISTS api_code varchar(100),
    ADD COLUMN IF NOT EXISTS module_code varchar(2),
    ADD COLUMN IF NOT EXISTS module_name varchar(255),
    ADD COLUMN IF NOT EXISTS submodule_code varchar(2),
    ADD COLUMN IF NOT EXISTS submodule_name varchar(255),
    ADD COLUMN IF NOT EXISTS feature_code varchar(3),
    ADD COLUMN IF NOT EXISTS feature_name varchar(255),
    ADD COLUMN IF NOT EXISTS action_code varchar(2),
    ADD COLUMN IF NOT EXISTS action_name varchar(255),
    ADD COLUMN IF NOT EXISTS access_mode varchar(50),
    ADD COLUMN IF NOT EXISTS business_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;

ALTER TABLE log_audit_log
    ADD COLUMN IF NOT EXISTS trace_id varchar(64),
    ADD COLUMN IF NOT EXISTS client_code varchar(50),
    ADD COLUMN IF NOT EXISTS client_type varchar(50),
    ADD COLUMN IF NOT EXISTS user_id bigint,
    ADD COLUMN IF NOT EXISTS module_code varchar(2),
    ADD COLUMN IF NOT EXISTS module_name varchar(255),
    ADD COLUMN IF NOT EXISTS submodule_code varchar(2),
    ADD COLUMN IF NOT EXISTS submodule_name varchar(255),
    ADD COLUMN IF NOT EXISTS feature_code varchar(3),
    ADD COLUMN IF NOT EXISTS feature_name varchar(255),
    ADD COLUMN IF NOT EXISTS action_code varchar(2),
    ADD COLUMN IF NOT EXISTS action_name varchar(255),
    ADD COLUMN IF NOT EXISTS access_mode varchar(50),
    ADD COLUMN IF NOT EXISTS business_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;
