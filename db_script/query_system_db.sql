-- User and Role wise features
--target_user_id : 1
--target_role_ids : ARRAY[1]
--target_tenant_id : 1
--target_client_code : WEB
WITH effective_privileges AS (
    -- Privileges assigned directly to the user
    SELECT up.privilege_id, 'DIRECT' AS assignment_source
    FROM sys_priv_user_privileges up
    WHERE up.user_id = :'target_user_id'::bigint

UNION

-- Privileges inherited through roles
SELECT rp.privilege_id, 'ROLE' AS assignment_source
FROM sys_priv_role_privileges rp
WHERE rp.role_id = ANY (:'target_role_ids'::bigint[])
    )
SELECT
    ft.feature_type_code,
    ft.category_name     AS main_menu,
    ft.icon             AS main_menu_icon,
    lf.id               AS navigation_feature_id,
    lf.feature_name     AS navigation_feature_name,
    lf.route            AS navigation_route,
    lf.icon             AS navigation_icon,
    lfg.display_order   AS feature_group_order,
    lf.display_order    AS feature_order,
    m.module_priv_code   AS module_code,
    m.navigation_module_name AS module_name,
    s.submodule_priv_code AS submodule_code,
    s.name               AS submodule_name,
    f.feature_code,
    f.feature_name,
    array_agg(DISTINCT p.privilege_code ORDER BY p.privilege_code)
                         AS privilege_codes,
    string_agg(DISTINCT ep.assignment_source, ', ' ORDER BY ep.assignment_source)
                         AS assignment_sources
FROM effective_privileges ep
         JOIN sys_priv_privileges p
              ON p.id = ep.privilege_id
                  AND p.active = true
         JOIN sys_acc_client_feature_permissions cfp
              ON cfp.privilege_id = p.id
                  AND cfp.tenant_id = :'target_tenant_id'::bigint
                  AND cfp.active = true
         JOIN sys_acc_client_applications ca
              ON ca.id = cfp.client_application_id
                  AND ca.client_code = :'target_client_code'
                  AND ca.status = 'ACTIVE'
         JOIN sys_priv_features f
              ON f.id = p.feature_id
                  AND f.active = true
         JOIN sys_layout_feature_privileges lfp
              ON lfp.privilege_id = p.id
                  AND lfp.active = true
         JOIN sys_layout_features lf
              ON lf.id = lfp.layout_feature_id
                  AND lf.active = true
         JOIN sys_layout_feature_groups lfg
              ON lfg.id = lf.feature_group_id
                  AND lfg.active = true
         JOIN sys_layout_navigation_categories ft
              ON ft.id = f.navigation_category_id
                  AND ft.active = true
         JOIN sys_priv_submodules s
              ON s.id = f.submodule_id
                  AND s.active = true
         JOIN sys_priv_modules m
              ON m.id = s.module_id
                  AND m.active = true
GROUP BY
    ft.feature_type_code,
    ft.category_name,
    ft.icon,
    lf.id,
    lf.feature_name,
    lf.route,
    lf.icon,
    lfg.display_order,
    lf.display_order,
    m.module_priv_code,
    m.navigation_module_name,
    s.submodule_priv_code,
    s.name,
    f.feature_code,
    f.feature_name
ORDER BY
    lfg.display_order,
    ft.feature_type_code,
    lf.display_order,
    lf.feature_name;
