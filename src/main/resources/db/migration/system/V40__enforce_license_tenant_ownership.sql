-- License plans/entitlements remain global catalogs. Subscriptions and their
-- usage/audit records are tenant-owned. A client mapping is a narrowing dimension,
-- never an alternative to tenant ownership.

UPDATE sys_license_subscriptions subscription
SET tenant_id = resolved.tenant_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT subscription.id,
           min(mapping.tenant_id) AS tenant_id,
           count(DISTINCT mapping.tenant_id) AS tenant_count
    FROM sys_license_subscriptions subscription
    JOIN sys_acc_client_application_tenants mapping
      ON mapping.client_application_id = subscription.client_application_id
     AND mapping.active
     AND mapping.tenant_id IS NOT NULL
    WHERE subscription.tenant_id IS NULL
    GROUP BY subscription.id
) resolved
WHERE subscription.id = resolved.id
  AND resolved.tenant_count = 1;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sys_license_subscriptions WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Phase 4 blocked: license subscriptions exist without a trusted tenant';
    END IF;
END $$;

UPDATE sys_license_usage_snapshots usage
SET tenant_id = subscription.tenant_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_license_subscriptions subscription
WHERE subscription.id = usage.license_subscription_id
  AND usage.tenant_id IS NULL;

UPDATE sys_license_audit_events audit
SET tenant_id = subscription.tenant_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_license_subscriptions subscription
WHERE subscription.id = audit.license_subscription_id
  AND audit.tenant_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sys_license_usage_snapshots WHERE tenant_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_license_audit_events WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Phase 4 blocked: license usage or audit rows exist without a trusted tenant';
    END IF;
END $$;

ALTER TABLE sys_license_subscriptions
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_license_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT uk_sys_license_subscriptions_id_tenant UNIQUE (id, tenant_id);

DO $$
DECLARE constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'sys_license_subscriptions'::regclass
      AND contype = 'u'
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                          WHERE attrelid = 'sys_license_subscriptions'::regclass
                            AND attname = 'subscription_code')]::smallint[]
    LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE sys_license_subscriptions DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE sys_license_subscriptions
    ADD CONSTRAINT uk_sys_license_subscriptions_tenant_code UNIQUE (tenant_id, subscription_code);

ALTER TABLE sys_license_usage_snapshots
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_license_usage_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT fk_sys_license_usage_subscription_tenant
        FOREIGN KEY (license_subscription_id, tenant_id)
        REFERENCES sys_license_subscriptions(id, tenant_id);

ALTER TABLE sys_license_audit_events
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_sys_license_audit_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenants(id),
    ADD CONSTRAINT fk_sys_license_audit_subscription_tenant
        FOREIGN KEY (license_subscription_id, tenant_id)
        REFERENCES sys_license_subscriptions(id, tenant_id);

CREATE INDEX idx_sys_license_subscriptions_tenant_status
    ON sys_license_subscriptions(tenant_id, status, id);
CREATE INDEX idx_sys_license_usage_tenant_period
    ON sys_license_usage_snapshots(tenant_id, usage_period, usage_code);
