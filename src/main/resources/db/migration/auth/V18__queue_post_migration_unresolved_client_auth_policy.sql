-- V17 queues policies that exist during migration. Keep the reconciliation
-- queue complete for unresolved policies inserted later by bootstrap/seeding.

CREATE OR REPLACE FUNCTION auth_queue_unresolved_client_auth_policy()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.tenant_id IS NULL THEN
        INSERT INTO auth_client_auth_policy_reconciliation (
            policy_id,
            client_code,
            reason,
            candidate_tenant_ids,
            details,
            resolved,
            created_by,
            updated_by
        ) VALUES (
            NEW.id,
            NEW.client_code,
            'TRUSTED_TENANT_REQUIRED',
            '{}',
            'Resolve from the authoritative system client-to-tenant assignment; do not infer from request headers or usernames.',
            false,
            0,
            0
        )
        ON CONFLICT (policy_id) DO UPDATE
        SET client_code = EXCLUDED.client_code,
            reason = EXCLUDED.reason,
            candidate_tenant_ids = EXCLUDED.candidate_tenant_ids,
            details = EXCLUDED.details,
            resolved = false,
            resolved_tenant_id = NULL,
            resolved_by = NULL,
            resolved_at = NULL,
            updated_by = 0,
            updated_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auth_queue_unresolved_client_auth_policy
    ON auth_client_auth_policy;

CREATE TRIGGER trg_auth_queue_unresolved_client_auth_policy
AFTER INSERT OR UPDATE OF tenant_id, client_code
ON auth_client_auth_policy
FOR EACH ROW
EXECUTE FUNCTION auth_queue_unresolved_client_auth_policy();
