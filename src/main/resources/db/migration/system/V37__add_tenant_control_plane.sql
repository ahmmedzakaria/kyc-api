-- Tenant control plane. Tenant identity is owned by system_db; other databases
-- store immutable application-level references to sys_tenants.id.

CREATE TABLE sys_tenants (
    id bigserial PRIMARY KEY,
    tenant_code varchar(63) NOT NULL,
    display_name varchar(255) NOT NULL,
    legal_name varchar(255),
    registration_number varchar(100),
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    default_locale varchar(20) NOT NULL DEFAULT 'en',
    default_time_zone varchar(64) NOT NULL DEFAULT 'UTC',
    billing_email varchar(320),
    version bigint NOT NULL DEFAULT 0,
    activated_at timestamp,
    suspended_at timestamp,
    suspension_reason varchar(500),
    cancelled_at timestamp,
    cancellation_reason varchar(500),
    created_by bigint NOT NULL,
    updated_by bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_tenants_code_normalized
        CHECK (tenant_code = lower(tenant_code) AND tenant_code ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$'),
    CONSTRAINT ck_sys_tenants_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uk_sys_tenants_code_lower ON sys_tenants (lower(tenant_code));

CREATE TABLE sys_tenant_domains (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL REFERENCES sys_tenants(id),
    hostname varchar(253) NOT NULL,
    domain_type varchar(30) NOT NULL,
    verification_status varchar(20) NOT NULL DEFAULT 'PENDING',
    verification_token_hash varchar(255),
    verification_expires_at timestamp,
    verified_at timestamp,
    last_checked_at timestamp,
    primary_domain boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL,
    updated_by bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_tenant_domains_hostname_normalized CHECK (hostname = lower(hostname)),
    CONSTRAINT ck_sys_tenant_domains_type CHECK (domain_type IN ('PLATFORM_SUBDOMAIN', 'CUSTOM')),
    CONSTRAINT ck_sys_tenant_domains_verification
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED', 'REVOKED'))
);

CREATE UNIQUE INDEX uk_sys_tenant_domains_hostname_lower ON sys_tenant_domains (lower(hostname));
CREATE UNIQUE INDEX uk_sys_tenant_domains_primary_active
    ON sys_tenant_domains (tenant_id) WHERE primary_domain AND active;
CREATE INDEX idx_sys_tenant_domains_resolution
    ON sys_tenant_domains (hostname, active, verification_status);

-- Preserve the tenant already used by the Auth cutover and local client mappings.
INSERT INTO sys_tenants (
    id, tenant_code, display_name, status, activated_at,
    created_by, updated_by, created_at, updated_at
) VALUES (
    1, 'system', 'System Tenant', 'ACTIVE', CURRENT_TIMESTAMP,
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('sys_tenants', 'id'),
              GREATEST((SELECT max(id) FROM sys_tenants), 1), true);

INSERT INTO sys_tenant_domains (
    tenant_id, hostname, domain_type, verification_status, verified_at,
    primary_domain, active, created_by, updated_by, created_at, updated_at
) VALUES
    (1, 'localhost', 'PLATFORM_SUBDOMAIN', 'VERIFIED', CURRENT_TIMESTAMP,
     true, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '127.0.0.1', 'PLATFORM_SUBDOMAIN', 'VERIFIED', CURRENT_TIMESTAMP,
     false, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

ALTER TABLE sys_acc_client_application_tenants
    ADD CONSTRAINT fk_sys_acc_client_tenant_registry
    FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id) NOT VALID;

