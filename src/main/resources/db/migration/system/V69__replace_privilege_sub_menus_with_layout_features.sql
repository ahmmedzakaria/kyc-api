-- Move the remaining legacy privilege-menu presentation into the layout tree.
-- Authorization remains in sys_priv_privileges; navigation visibility is
-- expressed only through sys_layout_feature_privileges.

CREATE TEMP TABLE legacy_sub_menu_layout_feature (
    sub_menu_id bigint PRIMARY KEY,
    layout_feature_id bigint NOT NULL
) ON COMMIT DROP;

-- Reuse an already configured layout feature when it represents the same
-- physical feature and route.
INSERT INTO legacy_sub_menu_layout_feature (sub_menu_id, layout_feature_id)
SELECT menu.id, MIN(layout_feature.id)
FROM sys_priv_sub_menus menu
JOIN sys_priv_features privilege_feature ON privilege_feature.id = menu.feature_id
JOIN sys_priv_submodules submodule ON submodule.id = privilege_feature.submodule_id
JOIN sys_priv_modules module ON module.id = submodule.module_id
JOIN sys_layout_navigation_categories category
  ON category.id = privilege_feature.navigation_category_id
JOIN sys_layout_feature_groups feature_group
  ON feature_group.navigation_category_id = category.id
JOIN sys_layout_features layout_feature
  ON layout_feature.feature_group_id = feature_group.id
 AND layout_feature.physical_module_code = module.code
 AND layout_feature.physical_submodule_code = submodule.code
 AND layout_feature.physical_feature_type_code = category.feature_type_code
 AND layout_feature.physical_feature_code = privilege_feature.feature_code
 AND layout_feature.route = menu.url
GROUP BY menu.id;

-- A feature group is presentation structure. Create one per privilege
-- submodule only where a legacy menu has no existing layout representation.
INSERT INTO sys_layout_feature_groups (
    navigation_category_id, feature_group_code, feature_group_name,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT DISTINCT privilege_feature.navigation_category_id,
       'PRIV_SUBMODULE_' || submodule.code,
       submodule.name,
       COALESCE(menu.menu_order, 100), true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_sub_menus menu
JOIN sys_priv_features privilege_feature ON privilege_feature.id = menu.feature_id
JOIN sys_priv_submodules submodule ON submodule.id = privilege_feature.submodule_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_layout_feature_groups feature_group
    WHERE feature_group.navigation_category_id = privilege_feature.navigation_category_id
      AND feature_group.feature_group_code = 'PRIV_SUBMODULE_' || submodule.code
)
ON CONFLICT (navigation_category_id, feature_group_code) DO NOTHING;

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    physical_module_code, physical_submodule_code,
    physical_feature_type_code, physical_feature_code,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT feature_group.id, 'PRIV_MENU_' || menu.id, NULL,
       menu.name, menu.url, menu.icon,
       module.code, submodule.code, category.feature_type_code,
       privilege_feature.feature_code,
       COALESCE(menu.sub_menu_order, 100), menu.active,
       COALESCE(menu.created_by, 0), COALESCE(menu.updated_by, 0),
       COALESCE(menu.created_at, CURRENT_TIMESTAMP),
       COALESCE(menu.updated_at, CURRENT_TIMESTAMP)
FROM sys_priv_sub_menus menu
JOIN sys_priv_features privilege_feature ON privilege_feature.id = menu.feature_id
JOIN sys_priv_submodules submodule ON submodule.id = privilege_feature.submodule_id
JOIN sys_priv_modules module ON module.id = submodule.module_id
JOIN sys_layout_navigation_categories category
  ON category.id = privilege_feature.navigation_category_id
JOIN sys_layout_feature_groups feature_group
  ON feature_group.navigation_category_id = category.id
 AND feature_group.feature_group_code = 'PRIV_SUBMODULE_' || submodule.code
WHERE NOT EXISTS (
    SELECT 1 FROM legacy_sub_menu_layout_feature mapped
    WHERE mapped.sub_menu_id = menu.id
)
ON CONFLICT (feature_group_id, feature_code) DO NOTHING;

INSERT INTO legacy_sub_menu_layout_feature (sub_menu_id, layout_feature_id)
SELECT menu.id, layout_feature.id
FROM sys_priv_sub_menus menu
JOIN sys_priv_features privilege_feature ON privilege_feature.id = menu.feature_id
JOIN sys_layout_feature_groups feature_group
  ON feature_group.navigation_category_id = privilege_feature.navigation_category_id
JOIN sys_layout_features layout_feature
  ON layout_feature.feature_group_id = feature_group.id
 AND layout_feature.feature_code = 'PRIV_MENU_' || menu.id
ON CONFLICT (sub_menu_id) DO NOTHING;

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT mapped.layout_feature_id, privilege.id, 'ANY', privilege.active,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_privileges privilege
JOIN legacy_sub_menu_layout_feature mapped
  ON mapped.sub_menu_id = privilege.sub_menu_id
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE
SET active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE sys_priv_privileges DROP COLUMN sub_menu_id;
DROP TABLE sys_priv_sub_menus;
