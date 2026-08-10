-- Phase 2 database backstop for tenant-role assignment compatibility.

CREATE OR REPLACE FUNCTION auth_validate_user_role_tenant_compatibility()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    account_tenant_id bigint;
    assigned_role_tenant_id bigint;
    assigned_role_active boolean;
BEGIN
    SELECT tenant_id
      INTO account_tenant_id
      FROM auth_users
     WHERE id = NEW.user_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Auth user % does not exist', NEW.user_id;
    END IF;

    SELECT tenant_id, active
      INTO assigned_role_tenant_id, assigned_role_active
      FROM auth_roles
     WHERE id = NEW.role_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Auth role % does not exist', NEW.role_id;
    END IF;

    IF NOT assigned_role_active THEN
        RAISE EXCEPTION 'Inactive role % cannot be assigned', NEW.role_id;
    END IF;

    IF assigned_role_tenant_id IS NOT NULL
       AND assigned_role_tenant_id IS DISTINCT FROM account_tenant_id THEN
        RAISE EXCEPTION 'Tenant role cannot be assigned to an account in another tenant';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auth_user_role_tenant_compatibility ON auth_user_roles;
CREATE CONSTRAINT TRIGGER trg_auth_user_role_tenant_compatibility
AFTER INSERT OR UPDATE OF user_id, role_id ON auth_user_roles
DEFERRABLE INITIALLY IMMEDIATE
FOR EACH ROW EXECUTE FUNCTION auth_validate_user_role_tenant_compatibility();

CREATE OR REPLACE FUNCTION auth_prevent_assigned_role_tenant_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.tenant_id IS DISTINCT FROM NEW.tenant_id
       AND EXISTS (SELECT 1 FROM auth_user_roles WHERE role_id = OLD.id) THEN
        RAISE EXCEPTION 'Assigned role tenant ownership cannot be changed; create a replacement role';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auth_prevent_assigned_role_tenant_change ON auth_roles;
CREATE TRIGGER trg_auth_prevent_assigned_role_tenant_change
BEFORE UPDATE OF tenant_id ON auth_roles
FOR EACH ROW EXECUTE FUNCTION auth_prevent_assigned_role_tenant_change();
