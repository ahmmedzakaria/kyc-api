-- Privilege catalog and privilege assignment tables are system-module tables.
-- Keep auth user/role tables auth-prefixed, but move privilege-owned tables to
-- the system_ prefix without dropping existing data.

DO $$
BEGIN
    IF to_regclass('system_sub_menus') IS NULL
        AND to_regclass('auth_sub_menus') IS NOT NULL THEN
        ALTER TABLE auth_sub_menus RENAME TO system_sub_menus;
    END IF;

    IF to_regclass('system_privileges') IS NULL
        AND to_regclass('auth_privileges') IS NOT NULL THEN
        ALTER TABLE auth_privileges RENAME TO system_privileges;
    END IF;

    IF to_regclass('system_role_privileges') IS NULL
        AND to_regclass('auth_role_privileges') IS NOT NULL THEN
        ALTER TABLE auth_role_privileges RENAME TO system_role_privileges;
    END IF;

    IF to_regclass('system_user_privileges') IS NULL
        AND to_regclass('auth_user_privileges') IS NOT NULL THEN
        ALTER TABLE auth_user_privileges RENAME TO system_user_privileges;
    END IF;
END $$;
