-- Prepare client authentication policy for tenant ownership without inventing
-- cross-database ownership. The authoritative client-to-tenant assignment is
-- stored in system_db, so existing rows remain unresolved until an explicit
-- reconciliation process supplies a trusted tenant_id.

ALTER TABLE auth_client_auth_policy
    ADD COLUMN IF NOT EXISTS tenant_id bigint;

UPDATE auth_client_auth_policy
SET client_code = TRIM(client_code),
    created_by = COALESCE(created_by, 0),
    updated_by = COALESCE(updated_by, 0),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);

ALTER TABLE auth_client_auth_policy
    ALTER COLUMN created_by SET DEFAULT 0,
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_by SET DEFAULT 0,
    ALTER COLUMN updated_by SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE auth_client_auth_policy
    DROP CONSTRAINT IF EXISTS ck_auth_client_auth_policy_tenant_positive;

ALTER TABLE auth_client_auth_policy
    ADD CONSTRAINT ck_auth_client_auth_policy_tenant_positive
        CHECK (tenant_id IS NULL OR tenant_id > 0);

-- The legacy index encodes a multi-method client-only model. Remove it before
-- adding tenant-aware invariants. Unresolved rows deliberately have no active
-- runtime uniqueness guarantee until an operator supplies trusted ownership.
DROP INDEX IF EXISTS ux_auth_client_auth_policy;
DROP INDEX IF EXISTS idx_auth_client_auth_policy_client_enabled;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_client_auth_policy_tenant_client
    ON auth_client_auth_policy (tenant_id, LOWER(client_code))
    WHERE tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_client_auth_policy_tenant_client_enabled
    ON auth_client_auth_policy (tenant_id, LOWER(client_code), enabled)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_client_auth_policy_unresolved
    ON auth_client_auth_policy (LOWER(client_code), id)
    WHERE tenant_id IS NULL;

CREATE TABLE IF NOT EXISTS auth_client_auth_policy_reconciliation (
    policy_id bigint PRIMARY KEY,
    client_code varchar(100) NOT NULL,
    reason varchar(80) NOT NULL,
    candidate_tenant_ids bigint[] NOT NULL DEFAULT '{}',
    details varchar(500),
    resolved boolean NOT NULL DEFAULT false,
    resolved_tenant_id bigint,
    resolved_by bigint,
    resolved_at timestamp,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_client_policy_reconciliation_policy
        FOREIGN KEY (policy_id) REFERENCES auth_client_auth_policy(id) ON DELETE CASCADE,
    CONSTRAINT ck_auth_client_policy_reconciliation_tenant_positive
        CHECK (resolved_tenant_id IS NULL OR resolved_tenant_id > 0),
    CONSTRAINT ck_auth_client_policy_reconciliation_resolution
        CHECK (
            (NOT resolved AND resolved_tenant_id IS NULL AND resolved_by IS NULL AND resolved_at IS NULL)
            OR
            (resolved AND resolved_tenant_id IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
        )
);

INSERT INTO auth_client_auth_policy_reconciliation (
    policy_id,
    client_code,
    reason,
    candidate_tenant_ids,
    details,
    resolved,
    created_by,
    updated_by
)
SELECT policy.id,
       policy.client_code,
       'TRUSTED_TENANT_REQUIRED',
       '{}',
       'Resolve from the authoritative system client-to-tenant assignment; do not infer from request headers or usernames.',
       false,
       0,
       0
FROM auth_client_auth_policy policy
WHERE policy.tenant_id IS NULL
ON CONFLICT (policy_id) DO UPDATE
SET client_code = EXCLUDED.client_code,
    reason = EXCLUDED.reason,
    details = EXCLUDED.details,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE NOT auth_client_auth_policy_reconciliation.resolved;

CREATE INDEX IF NOT EXISTS idx_auth_client_policy_reconciliation_unresolved
    ON auth_client_auth_policy_reconciliation (resolved, client_code, policy_id)
    WHERE NOT resolved;

COMMENT ON COLUMN auth_client_auth_policy.tenant_id IS
    'Application-level tenant reference. Validate against authoritative system_db client assignments; no cross-database FK.';

COMMENT ON TABLE auth_client_auth_policy_reconciliation IS
    'Operator/reconciliation queue for legacy auth policies whose tenant ownership cannot be proven inside auth_db.';
