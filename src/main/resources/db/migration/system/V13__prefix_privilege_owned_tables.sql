DO $$
DECLARE
    rename_pair record;
BEGIN
    FOR rename_pair IN
        SELECT old_name, new_name
        FROM (VALUES
            ('sys_modules', 'sys_priv_modules'),
            ('sys_submodules', 'sys_priv_submodules'),
            ('sys_features', 'sys_priv_features'),
            ('sys_privileges', 'sys_priv_privileges'),
            ('sys_sub_menus', 'sys_priv_sub_menus'),
            ('sys_role_privileges', 'sys_priv_role_privileges'),
            ('sys_user_privileges', 'sys_priv_user_privileges'),
            ('sys_client_applications', 'sys_priv_client_applications'),
            ('sys_client_credentials', 'sys_priv_client_credentials'),
            ('sys_api_registry', 'sys_priv_api_registry'),
            ('sys_client_api_permissions', 'sys_priv_client_api_permissions'),
            ('sys_client_feature_permissions', 'sys_priv_client_feature_permissions'),
            ('sys_client_application_tenants', 'sys_priv_client_application_tenants')
        ) AS table_names(old_name, new_name)
    LOOP
        IF to_regclass(format('public.%I', rename_pair.old_name)) IS NOT NULL
           AND to_regclass(format('public.%I', rename_pair.new_name)) IS NOT NULL THEN
            RAISE EXCEPTION
                'Cannot rename %.%: target %.% already exists',
                'public', rename_pair.old_name, 'public', rename_pair.new_name;
        END IF;

        IF to_regclass(format('public.%I', rename_pair.old_name)) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE public.%I RENAME TO %I',
                rename_pair.old_name,
                rename_pair.new_name
            );
        END IF;
    END LOOP;
END $$;
