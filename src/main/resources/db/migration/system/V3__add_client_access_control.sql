-- Client application access control.

CREATE TABLE IF NOT EXISTS sys_client_applications (
    id bigserial PRIMARY KEY,
    client_code varchar(50) NOT NULL UNIQUE,
    client_name varchar(255) NOT NULL,
    client_type varchar(50) NOT NULL,
    status varchar(50) NOT NULL DEFAULT 'ACTIVE',
    allowed_origins text,
    allowed_ips text,
    rate_limit_per_minute integer,
    description text,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_client_credentials (
    id bigserial PRIMARY KEY,
    client_application_id bigint NOT NULL REFERENCES sys_client_applications(id) ON DELETE CASCADE,
    client_id varchar(100) UNIQUE,
    api_key_hash varchar(255),
    client_secret_hash varchar(255),
    expires_at timestamp,
    last_used_at timestamp,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_sys_client_credentials_app
    ON sys_client_credentials (client_application_id);

CREATE TABLE IF NOT EXISTS sys_api_registry (
    id bigserial PRIMARY KEY,
    api_code varchar(100) NOT NULL UNIQUE,
    http_method varchar(20) NOT NULL,
    path_pattern varchar(255) NOT NULL,
    module_code varchar(2),
    module_name varchar(255),
    submodule_code varchar(2),
    submodule_name varchar(255),
    feature_type_code varchar(2),
    feature_type_name varchar(255),
    feature_code varchar(3),
    feature_name varchar(255),
    action_code varchar(2),
    action_name varchar(255),
    required_privilege_code varchar(11),
    public_api boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_sys_api_registry_method_active
    ON sys_api_registry (http_method, active);

CREATE TABLE IF NOT EXISTS sys_client_api_permissions (
    id bigserial PRIMARY KEY,
    client_application_id bigint NOT NULL REFERENCES sys_client_applications(id) ON DELETE CASCADE,
    api_registry_id bigint NOT NULL REFERENCES sys_api_registry(id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_client_api_permissions UNIQUE (client_application_id, api_registry_id)
);

CREATE TABLE IF NOT EXISTS sys_client_feature_permissions (
    id bigserial PRIMARY KEY,
    client_application_id bigint NOT NULL REFERENCES sys_client_applications(id) ON DELETE CASCADE,
    privilege_id bigint NOT NULL REFERENCES sys_privileges(id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_client_feature_permissions UNIQUE (client_application_id, privilege_id)
);

CREATE TABLE IF NOT EXISTS sys_client_application_tenants (
    id bigserial PRIMARY KEY,
    client_application_id bigint NOT NULL REFERENCES sys_client_applications(id) ON DELETE CASCADE,
    tenant_id bigint,
    business_id bigint,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_client_application_tenants UNIQUE (client_application_id, tenant_id, business_id)
);

INSERT INTO sys_client_applications (
    client_code,
    client_name,
    client_type,
    status,
    description,
    created_by,
    updated_by,
    created_at,
    updated_at
)
VALUES (
    'WEB',
    'Default Web Application',
    'WEB',
    'ACTIVE',
    'Default first-party web frontend client. Generate a real API key before enforcing registered APIs in production.',
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (client_code) DO NOTHING;
