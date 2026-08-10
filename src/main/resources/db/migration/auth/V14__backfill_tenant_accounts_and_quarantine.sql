-- Phase 3: deterministic Auth-local account backfill.
-- Tenant ownership is derived only from active server-owned scope assignments.

CREATE TABLE auth_user_backfill_quarantine (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL UNIQUE REFERENCES auth_users(id) ON DELETE CASCADE,
    reason varchar(50) NOT NULL,
    candidate_tenant_ids bigint[] NOT NULL DEFAULT '{}',
    details jsonb NOT NULL DEFAULT '{}'::jsonb,
    resolved boolean NOT NULL DEFAULT false,
    resolved_by bigint,
    resolved_at timestamp,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_auth_user_backfill_quarantine_reason CHECK (
        reason IN (
            'NO_ACTIVE_SCOPE',
            'AMBIGUOUS_ACTIVE_TENANT',
            'INVALID_NORMALIZED_USERNAME',
            'NORMALIZED_USERNAME_CONFLICT'
        )
    ),
    CONSTRAINT ck_auth_user_backfill_quarantine_resolution CHECK (
        (resolved = false AND resolved_by IS NULL AND resolved_at IS NULL)
        OR (resolved = true AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
    )
);

CREATE INDEX idx_auth_user_backfill_quarantine_open
    ON auth_user_backfill_quarantine(reason, user_id)
    WHERE resolved = false;

-- PostgreSQL normalize(..., NFKC) matches the application normalizer's Unicode form.
UPDATE auth_users
SET normalized_username = lower(normalize(btrim(username), NFKC)),
    updated_by = 0
WHERE normalized_username IS NULL
  AND username IS NOT NULL
  AND btrim(username) <> ''
  AND char_length(lower(normalize(btrim(username), NFKC))) <= 150;

WITH tenant_candidates AS (
    SELECT u.id AS user_id,
           array_agg(DISTINCT s.tenant_id ORDER BY s.tenant_id)
               FILTER (WHERE s.active) AS tenant_ids,
           count(DISTINCT s.tenant_id) FILTER (WHERE s.active) AS tenant_count
    FROM auth_users u
    LEFT JOIN auth_user_scope_assignments s ON s.user_id = u.id
    WHERE u.tenant_id IS NULL
    GROUP BY u.id
), eligible AS (
    SELECT u.id AS user_id, candidates.tenant_ids[1] AS tenant_id
    FROM auth_users u
    JOIN tenant_candidates candidates ON candidates.user_id = u.id
    WHERE candidates.tenant_count = 1
      AND u.normalized_username IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM auth_users other
          WHERE other.id <> u.id
            AND other.tenant_id = candidates.tenant_ids[1]
            AND other.normalized_username = u.normalized_username
      )
)
UPDATE auth_users u
SET tenant_id = eligible.tenant_id,
    updated_by = 0
FROM eligible
WHERE u.id = eligible.user_id;

WITH tenant_candidates AS (
    SELECT u.id AS user_id,
           COALESCE(
               array_agg(DISTINCT s.tenant_id ORDER BY s.tenant_id)
                   FILTER (WHERE s.active),
               '{}'::bigint[]
           ) AS tenant_ids,
           count(DISTINCT s.tenant_id) FILTER (WHERE s.active) AS tenant_count
    FROM auth_users u
    LEFT JOIN auth_user_scope_assignments s ON s.user_id = u.id
    WHERE u.tenant_id IS NULL
    GROUP BY u.id
), classified AS (
    SELECT u.id AS user_id,
           candidates.tenant_ids,
           CASE
               WHEN u.normalized_username IS NULL THEN 'INVALID_NORMALIZED_USERNAME'
               WHEN candidates.tenant_count = 0 THEN 'NO_ACTIVE_SCOPE'
               WHEN candidates.tenant_count > 1 THEN 'AMBIGUOUS_ACTIVE_TENANT'
               ELSE 'NORMALIZED_USERNAME_CONFLICT'
           END AS reason,
           jsonb_build_object(
               'username', u.username,
               'normalizedUsername', u.normalized_username,
               'activeTenantCount', candidates.tenant_count
           ) AS details
    FROM auth_users u
    JOIN tenant_candidates candidates ON candidates.user_id = u.id
)
INSERT INTO auth_user_backfill_quarantine (
    user_id, reason, candidate_tenant_ids, details,
    resolved, created_by, updated_by
)
SELECT user_id, reason, tenant_ids, details, false, 0, 0
FROM classified
ON CONFLICT (user_id) DO UPDATE
SET reason = EXCLUDED.reason,
    candidate_tenant_ids = EXCLUDED.candidate_tenant_ids,
    details = EXCLUDED.details,
    resolved = false,
    resolved_by = NULL,
    resolved_at = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

-- All pre-Phase-3 roles are existing platform role templates.
UPDATE auth_roles
SET role_code = name,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE tenant_id IS NULL
  AND role_code IS NULL;
