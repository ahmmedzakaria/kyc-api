-- Shorten system-module table prefix from system_ to sys_.
-- This follows V3, which moved privilege tables from auth_ to system_.

DO $$
BEGIN
    IF to_regclass('sys_sub_menus') IS NULL THEN
        IF to_regclass('system_sub_menus') IS NOT NULL THEN
            ALTER TABLE system_sub_menus RENAME TO sys_sub_menus;
        ELSIF to_regclass('auth_sub_menus') IS NOT NULL THEN
            ALTER TABLE auth_sub_menus RENAME TO sys_sub_menus;
        END IF;
    END IF;

    IF to_regclass('sys_privileges') IS NULL THEN
        IF to_regclass('system_privileges') IS NOT NULL THEN
            ALTER TABLE system_privileges RENAME TO sys_privileges;
        ELSIF to_regclass('auth_privileges') IS NOT NULL THEN
            ALTER TABLE auth_privileges RENAME TO sys_privileges;
        END IF;
    END IF;

    IF to_regclass('sys_role_privileges') IS NULL THEN
        IF to_regclass('system_role_privileges') IS NOT NULL THEN
            ALTER TABLE system_role_privileges RENAME TO sys_role_privileges;
        ELSIF to_regclass('auth_role_privileges') IS NOT NULL THEN
            ALTER TABLE auth_role_privileges RENAME TO sys_role_privileges;
        END IF;
    END IF;

    IF to_regclass('sys_user_privileges') IS NULL THEN
        IF to_regclass('system_user_privileges') IS NOT NULL THEN
            ALTER TABLE system_user_privileges RENAME TO sys_user_privileges;
        ELSIF to_regclass('auth_user_privileges') IS NOT NULL THEN
            ALTER TABLE auth_user_privileges RENAME TO sys_user_privileges;
        END IF;
    END IF;
END $$;
