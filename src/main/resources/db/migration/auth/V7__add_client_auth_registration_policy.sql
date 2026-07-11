CREATE TABLE IF NOT EXISTS auth_client_auth_policy (
    id bigserial PRIMARY KEY,
    client_code varchar(100) NOT NULL,
    login_method varchar(50) NOT NULL,
    login_identifier_type varchar(50),
    enabled boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_client_auth_policy
    ON auth_client_auth_policy (
        LOWER(client_code),
        login_method,
        COALESCE(login_identifier_type, '')
    );

CREATE INDEX IF NOT EXISTS idx_auth_client_auth_policy_client_enabled
    ON auth_client_auth_policy (LOWER(client_code), enabled);

CREATE TABLE IF NOT EXISTS auth_client_registration_policy (
    id bigserial PRIMARY KEY,
    client_code varchar(100) NOT NULL,
    registration_credential_model varchar(80) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_client_registration_policy
    ON auth_client_registration_policy (
        LOWER(client_code),
        registration_credential_model
    );

CREATE INDEX IF NOT EXISTS idx_auth_client_registration_policy_client_enabled
    ON auth_client_registration_policy (LOWER(client_code), enabled);
