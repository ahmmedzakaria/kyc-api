CREATE TABLE IF NOT EXISTS sys_license_plans (
    id bigserial PRIMARY KEY,
    plan_code varchar(80) NOT NULL UNIQUE,
    plan_name varchar(255) NOT NULL,
    plan_type varchar(30) NOT NULL,
    billing_cycle varchar(30) NOT NULL,
    trial_days integer,
    description text,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_license_plan_entitlements (
    id bigserial PRIMARY KEY,
    license_plan_id bigint NOT NULL REFERENCES sys_license_plans(id) ON DELETE CASCADE,
    entitlement_type varchar(30) NOT NULL,
    module_id bigint REFERENCES sys_modules(id),
    submodule_id bigint REFERENCES sys_submodules(id),
    feature_id bigint REFERENCES sys_features(id),
    privilege_id bigint REFERENCES sys_privileges(id),
    api_registry_id bigint REFERENCES sys_api_registry(id),
    limit_code varchar(80),
    limit_value bigint,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_license_subscriptions (
    id bigserial PRIMARY KEY,
    subscription_code varchar(80) NOT NULL UNIQUE,
    license_plan_id bigint NOT NULL REFERENCES sys_license_plans(id),
    tenant_id bigint,
    business_id bigint,
    client_application_id bigint REFERENCES sys_client_applications(id),
    status varchar(30) NOT NULL,
    starts_at timestamp,
    expires_at timestamp,
    grace_period_ends_at timestamp,
    auto_renew boolean NOT NULL DEFAULT false,
    cancelled_at timestamp,
    suspended_at timestamp,
    suspension_reason text,
    metadata_json text,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT chk_sys_license_subscription_owner
        CHECK (tenant_id IS NOT NULL OR business_id IS NOT NULL OR client_application_id IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS sys_license_keys (
    id bigserial PRIMARY KEY,
    license_subscription_id bigint NOT NULL REFERENCES sys_license_subscriptions(id) ON DELETE CASCADE,
    license_key_hash varchar(128) NOT NULL UNIQUE,
    key_prefix varchar(16) NOT NULL,
    activation_fingerprint_hash varchar(128),
    issued_at timestamp,
    activated_at timestamp,
    last_validated_at timestamp,
    expires_at timestamp,
    revoked_at timestamp,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_license_entitlement_overrides (
    id bigserial PRIMARY KEY,
    license_subscription_id bigint NOT NULL REFERENCES sys_license_subscriptions(id) ON DELETE CASCADE,
    entitlement_type varchar(30) NOT NULL,
    module_id bigint REFERENCES sys_modules(id),
    submodule_id bigint REFERENCES sys_submodules(id),
    feature_id bigint REFERENCES sys_features(id),
    privilege_id bigint REFERENCES sys_privileges(id),
    api_registry_id bigint REFERENCES sys_api_registry(id),
    limit_code varchar(80),
    limit_value bigint,
    override_mode varchar(30) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_license_usage_snapshots (
    id bigserial PRIMARY KEY,
    license_subscription_id bigint NOT NULL REFERENCES sys_license_subscriptions(id) ON DELETE CASCADE,
    tenant_id bigint,
    business_id bigint,
    usage_period varchar(20) NOT NULL,
    usage_code varchar(80) NOT NULL,
    usage_value bigint NOT NULL DEFAULT 0,
    measured_at timestamp,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_license_usage_subscription_period_code
        UNIQUE (license_subscription_id, usage_period, usage_code)
);

CREATE TABLE IF NOT EXISTS sys_license_audit_events (
    id bigserial PRIMARY KEY,
    license_subscription_id bigint REFERENCES sys_license_subscriptions(id) ON DELETE SET NULL,
    event_type varchar(80) NOT NULL,
    event_message_code varchar(120) NOT NULL,
    actor_user_id bigint,
    tenant_id bigint,
    business_id bigint,
    client_application_id bigint,
    safe_context_json text,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_sys_license_plan_entitlements_plan
    ON sys_license_plan_entitlements (license_plan_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_plan_entitlements_module
    ON sys_license_plan_entitlements (module_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_plan_entitlements_feature
    ON sys_license_plan_entitlements (feature_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_plan_entitlements_api
    ON sys_license_plan_entitlements (api_registry_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_subscriptions_owner
    ON sys_license_subscriptions (tenant_id, business_id, client_application_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_subscriptions_status
    ON sys_license_subscriptions (status);

CREATE INDEX IF NOT EXISTS idx_sys_license_keys_subscription
    ON sys_license_keys (license_subscription_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_overrides_subscription
    ON sys_license_entitlement_overrides (license_subscription_id);

CREATE INDEX IF NOT EXISTS idx_sys_license_usage_subscription
    ON sys_license_usage_snapshots (license_subscription_id);
