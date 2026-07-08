-- Add submodule ownership to auth privilege catalog data.
-- This project currently lets JPA create auth tables during the Flyway transition.
-- On a fresh database the tables may not exist yet, so this migration no-ops
-- until it is run against an existing auth schema.

DO $$
BEGIN
    IF to_regclass('auth_sub_menus') IS NULL
        OR to_regclass('auth_privileges') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE auth_sub_menus
        ADD COLUMN IF NOT EXISTS submodule_code varchar(2),
        ADD COLUMN IF NOT EXISTS submodule_name varchar(255);

    ALTER TABLE auth_privileges
        ADD COLUMN IF NOT EXISTS submodule_code varchar(2),
        ADD COLUMN IF NOT EXISTS submodule_name varchar(255);

    UPDATE auth_sub_menus
    SET submodule_code = CASE module_code
            WHEN '01' THEN '01'
            WHEN '02' THEN '01'
            ELSE '00'
        END,
        submodule_name = CASE module_code
            WHEN '01' THEN 'Person'
            WHEN '02' THEN 'Privilege'
            ELSE 'Default'
        END
    WHERE submodule_code IS NULL
       OR submodule_name IS NULL;

    UPDATE auth_privileges
    SET submodule_code = CASE module_code
            WHEN '01' THEN '01'
            WHEN '02' THEN '01'
            ELSE '00'
        END,
        submodule_name = CASE module_code
            WHEN '01' THEN 'Person'
            WHEN '02' THEN 'Privilege'
            ELSE 'Default'
        END
    WHERE submodule_code IS NULL
       OR submodule_name IS NULL;

    -- If a partial seed already created the new 11-digit row, move joins from
    -- the old duplicate row to the new row before deleting the old duplicate.
    IF to_regclass('auth_role_privileges') IS NOT NULL THEN
        WITH duplicate_privileges AS (
            SELECT old_privilege.id AS old_id,
                   new_privilege.id AS new_id
            FROM auth_privileges old_privilege
            JOIN auth_privileges new_privilege
              ON new_privilege.privilege_code =
                 substring(old_privilege.privilege_code FROM 1 FOR 2)
                 || old_privilege.submodule_code
                 || substring(old_privilege.privilege_code FROM 3)
            WHERE length(old_privilege.privilege_code) = 9
              AND length(new_privilege.privilege_code) = 11
        )
        INSERT INTO auth_role_privileges (role_id, privilege_id)
        SELECT role_privilege.role_id, duplicate_privileges.new_id
        FROM auth_role_privileges role_privilege
        JOIN duplicate_privileges
          ON duplicate_privileges.old_id = role_privilege.privilege_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM auth_role_privileges existing_role_privilege
            WHERE existing_role_privilege.role_id = role_privilege.role_id
              AND existing_role_privilege.privilege_id = duplicate_privileges.new_id
        );
    END IF;

    IF to_regclass('auth_user_privileges') IS NOT NULL THEN
        WITH duplicate_privileges AS (
            SELECT old_privilege.id AS old_id,
                   new_privilege.id AS new_id
            FROM auth_privileges old_privilege
            JOIN auth_privileges new_privilege
              ON new_privilege.privilege_code =
                 substring(old_privilege.privilege_code FROM 1 FOR 2)
                 || old_privilege.submodule_code
                 || substring(old_privilege.privilege_code FROM 3)
            WHERE length(old_privilege.privilege_code) = 9
              AND length(new_privilege.privilege_code) = 11
        )
        INSERT INTO auth_user_privileges (user_id, privilege_id)
        SELECT user_privilege.user_id, duplicate_privileges.new_id
        FROM auth_user_privileges user_privilege
        JOIN duplicate_privileges
          ON duplicate_privileges.old_id = user_privilege.privilege_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM auth_user_privileges existing_user_privilege
            WHERE existing_user_privilege.user_id = user_privilege.user_id
              AND existing_user_privilege.privilege_id = duplicate_privileges.new_id
        );
    END IF;

    IF to_regclass('auth_role_privileges') IS NOT NULL THEN
        WITH duplicate_privileges AS (
            SELECT old_privilege.id AS old_id
            FROM auth_privileges old_privilege
            JOIN auth_privileges new_privilege
              ON new_privilege.privilege_code =
                 substring(old_privilege.privilege_code FROM 1 FOR 2)
                 || old_privilege.submodule_code
                 || substring(old_privilege.privilege_code FROM 3)
            WHERE length(old_privilege.privilege_code) = 9
              AND length(new_privilege.privilege_code) = 11
        )
        DELETE FROM auth_role_privileges role_privilege
        USING duplicate_privileges
        WHERE role_privilege.privilege_id = duplicate_privileges.old_id;
    END IF;

    IF to_regclass('auth_user_privileges') IS NOT NULL THEN
        WITH duplicate_privileges AS (
            SELECT old_privilege.id AS old_id
            FROM auth_privileges old_privilege
            JOIN auth_privileges new_privilege
              ON new_privilege.privilege_code =
                 substring(old_privilege.privilege_code FROM 1 FOR 2)
                 || old_privilege.submodule_code
                 || substring(old_privilege.privilege_code FROM 3)
            WHERE length(old_privilege.privilege_code) = 9
              AND length(new_privilege.privilege_code) = 11
        )
        DELETE FROM auth_user_privileges user_privilege
        USING duplicate_privileges
        WHERE user_privilege.privilege_id = duplicate_privileges.old_id;
    END IF;

    WITH duplicate_privileges AS (
        SELECT old_privilege.id AS old_id
        FROM auth_privileges old_privilege
        JOIN auth_privileges new_privilege
          ON new_privilege.privilege_code =
             substring(old_privilege.privilege_code FROM 1 FOR 2)
             || old_privilege.submodule_code
             || substring(old_privilege.privilege_code FROM 3)
        WHERE length(old_privilege.privilege_code) = 9
          AND length(new_privilege.privilege_code) = 11
    )
    DELETE FROM auth_privileges privilege
    USING duplicate_privileges
    WHERE privilege.id = duplicate_privileges.old_id;

    UPDATE auth_privileges
    SET privilege_code = substring(privilege_code FROM 1 FOR 2)
                         || submodule_code
                         || substring(privilege_code FROM 3)
    WHERE length(privilege_code) = 9;

    ALTER TABLE auth_sub_menus
        ALTER COLUMN submodule_code SET NOT NULL,
        ALTER COLUMN submodule_name SET NOT NULL;

    ALTER TABLE auth_privileges
        ALTER COLUMN privilege_code TYPE varchar(11),
        ALTER COLUMN submodule_code SET NOT NULL,
        ALTER COLUMN submodule_name SET NOT NULL;
END $$;
