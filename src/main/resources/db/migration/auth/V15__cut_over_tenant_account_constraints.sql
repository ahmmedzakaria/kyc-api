-- Phase 5: make tenant account constraints authoritative.
-- This migration intentionally fails closed until every quarantined legacy account
-- has received an explicit, trusted tenant disposition.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM auth_users WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Phase 5 blocked: auth_users still contains accounts without a trusted tenant';
    END IF;
    IF EXISTS (SELECT 1 FROM auth_users WHERE normalized_username IS NULL) THEN
        RAISE EXCEPTION 'Phase 5 blocked: auth_users still contains accounts without a normalized username';
    END IF;
    IF EXISTS (
        SELECT 1 FROM auth_user_scope_assignments scope
        JOIN auth_users account ON account.id = scope.user_id
        WHERE scope.tenant_id IS DISTINCT FROM account.tenant_id
    ) THEN
        RAISE EXCEPTION 'Phase 5 blocked: an account scope belongs to a different tenant';
    END IF;
    IF EXISTS (
        SELECT 1 FROM auth_user_roles assignment
        JOIN auth_users account ON account.id = assignment.user_id
        JOIN auth_roles role ON role.id = assignment.role_id
        WHERE role.tenant_id IS NOT NULL
          AND role.tenant_id IS DISTINCT FROM account.tenant_id
    ) THEN
        RAISE EXCEPTION 'Phase 5 blocked: a tenant role is assigned across tenants';
    END IF;
END $$;

ALTER TABLE auth_users
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN normalized_username SET NOT NULL;

-- Remove legacy global uniqueness, including the JPA-created username constraint
-- found on older installations.
ALTER TABLE auth_users DROP CONSTRAINT IF EXISTS auth_users_username_key;
DROP INDEX IF EXISTS ux_auth_users_username;
DROP INDEX IF EXISTS ux_auth_users_person_id;

DROP INDEX IF EXISTS ux_auth_users_tenant_person_phase1;
DROP INDEX IF EXISTS ux_auth_users_tenant_username_phase1;
DROP INDEX IF EXISTS ux_auth_users_tenant_external_identity_phase1;

CREATE UNIQUE INDEX ux_auth_users_tenant_person
    ON auth_users(tenant_id, person_id);
CREATE UNIQUE INDEX ux_auth_users_tenant_username
    ON auth_users(tenant_id, normalized_username);
CREATE UNIQUE INDEX ux_auth_users_tenant_external_identity
    ON auth_users(tenant_id, external_provider, external_subject)
    WHERE external_provider IS NOT NULL AND external_subject IS NOT NULL;

-- Replace the legacy single-column scope FK with a tenant-preserving composite FK.
ALTER TABLE auth_user_scope_assignments
    DROP CONSTRAINT IF EXISTS auth_user_scope_assignments_user_id_fkey;
ALTER TABLE auth_user_scope_assignments
    ADD CONSTRAINT fk_auth_scope_user_tenant
    FOREIGN KEY (user_id, tenant_id)
    REFERENCES auth_users(id, tenant_id)
    ON DELETE CASCADE;

-- Role name/code uniqueness becomes global-template or tenant-local rather than
-- globally unique across both namespaces.
ALTER TABLE auth_roles DROP CONSTRAINT IF EXISTS auth_roles_name_key;
DROP INDEX IF EXISTS ux_auth_roles_global_name_phase1;
DROP INDEX IF EXISTS ux_auth_roles_tenant_name_phase1;
DROP INDEX IF EXISTS ux_auth_roles_global_code_phase1;
DROP INDEX IF EXISTS ux_auth_roles_tenant_code_phase1;

CREATE UNIQUE INDEX ux_auth_roles_global_name
    ON auth_roles(LOWER(name)) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX ux_auth_roles_tenant_name
    ON auth_roles(tenant_id, LOWER(name)) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX ux_auth_roles_global_code
    ON auth_roles(LOWER(role_code)) WHERE tenant_id IS NULL AND role_code IS NOT NULL;
CREATE UNIQUE INDEX ux_auth_roles_tenant_code
    ON auth_roles(tenant_id, LOWER(role_code)) WHERE tenant_id IS NOT NULL AND role_code IS NOT NULL;

-- V13 already creates these triggers. Re-enable them defensively in case an
-- operator disabled triggers during a controlled data repair.
ALTER TABLE auth_user_roles ENABLE TRIGGER trg_auth_user_role_tenant_compatibility;
ALTER TABLE auth_roles ENABLE TRIGGER trg_auth_prevent_assigned_role_tenant_change;
