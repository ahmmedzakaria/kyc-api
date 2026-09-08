-- User and Role wise features
--target_user_id : 1
--target_role_ids : ARRAY[1]
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
    ft.feature_type_name AS main_menu,
    ft.icon             AS main_menu_icon,
    sm.id               AS submenu_id,
    sm.name             AS submenu_name,
    sm.url               AS submenu_url,
    sm.icon              AS submenu_icon,
    sm.menu_order,
    sm.sub_menu_order,
    m.code               AS module_code,
    m.name               AS module_name,
    s.code               AS submodule_code,
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
         JOIN sys_priv_sub_menus sm
              ON sm.id = p.sub_menu_id
                  AND sm.active = true
         JOIN sys_priv_features f
              ON f.id = p.feature_id
                  AND f.active = true
         JOIN sys_priv_feature_types ft
              ON ft.id = f.feature_type_id
                  AND ft.active = true
         JOIN sys_priv_submodules s
              ON s.id = f.submodule_id
                  AND s.active = true
         JOIN sys_priv_modules m
              ON m.id = s.module_id
                  AND m.active = true
GROUP BY
    ft.feature_type_code,
    ft.feature_type_name,
    ft.icon,
    sm.id,
    sm.name,
    sm.url,
    sm.icon,
    sm.menu_order,
    sm.sub_menu_order,
    m.code,
    m.name,
    s.code,
    s.name,
    f.feature_code,
    f.feature_name
ORDER BY
    sm.menu_order,
    ft.feature_type_code,
    sm.sub_menu_order,
    sm.name;