-- Explicit destructive reset requested after the tenant-account cutover.
-- Client authentication policy is configuration, not user/login identity data,
-- and is intentionally retained.

TRUNCATE TABLE
    auth_user_backfill_quarantine,
    auth_user_scope_assignments,
    auth_user_roles,
    auth_users,
    auth_roles,
    auth_persons
RESTART IDENTITY CASCADE;
