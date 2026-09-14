-- Make layout navigation categories the single source of truth for privilege
-- feature types. The physical module code joins the privilege and layout
-- catalogs; infer it from existing layout features before creating fallback
-- catalog-only navigation nodes.

UPDATE sys_layout_navigation_modules navigation_module
SET physical_module_code = inferred.module_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT category.navigation_module_id, MIN(feature.physical_module_code) AS module_code
    FROM sys_layout_navigation_categories category
    JOIN sys_layout_feature_groups feature_group
      ON feature_group.navigation_category_id = category.id
    JOIN sys_layout_features feature
      ON feature.feature_group_id = feature_group.id
    WHERE feature.physical_module_code IS NOT NULL
    GROUP BY category.navigation_module_id
    HAVING COUNT(DISTINCT feature.physical_module_code) = 1
) inferred
WHERE navigation_module.id = inferred.navigation_module_id;

INSERT INTO sys_layout_module_groups (
    group_code, group_name, icon, display_order, active,
    created_by, updated_by, created_at, updated_at
)
SELECT 'PRIVILEGE_CATALOG', 'Privilege Catalog', 'lock', 999, false,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_layout_module_groups WHERE group_code = 'PRIVILEGE_CATALOG'
);

INSERT INTO sys_layout_navigation_modules (
    module_group_id, navigation_module_code, navigation_module_name,
    physical_module_code, icon, display_order, active,
    created_by, updated_by, created_at, updated_at
)
SELECT module_group.id, 'PRIV_' || privilege_module.code,
       privilege_module.name, privilege_module.code, 'lock', 999, false,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules privilege_module
JOIN sys_layout_module_groups module_group
  ON module_group.group_code = 'PRIVILEGE_CATALOG'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_layout_navigation_modules navigation_module
    WHERE navigation_module.physical_module_code = privilege_module.code
);

INSERT INTO sys_layout_navigation_categories (
    navigation_module_id, category_code, category_name, category_kind,
    feature_type_code, icon, display_order, active,
    created_by, updated_by, created_at, updated_at
)
SELECT navigation_module.id,
       'FEATURE_TYPE_' || feature_type.feature_type_code,
       feature_type.feature_type_name,
       'PRIVILEGE_CATALOG', feature_type.feature_type_code,
       feature_type.icon, feature_type.feature_type_code::integer, false,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_navigation_modules navigation_module
JOIN sys_priv_modules privilege_module
  ON privilege_module.code = navigation_module.physical_module_code
CROSS JOIN sys_priv_feature_types feature_type
WHERE feature_type.tenant_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM sys_priv_features feature
      JOIN sys_priv_submodules submodule ON submodule.id = feature.submodule_id
      WHERE submodule.module_id = privilege_module.id
        AND feature.feature_type_id = feature_type.id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_layout_navigation_categories category
      WHERE category.navigation_module_id = navigation_module.id
        AND category.feature_type_code = feature_type.feature_type_code
  );

ALTER TABLE sys_priv_features
    ADD COLUMN navigation_category_id bigint;

UPDATE sys_priv_features feature
SET navigation_category_id = category_match.category_id
FROM (
    SELECT feature_to_map.id AS feature_id, MIN(category.id) AS category_id
    FROM sys_priv_features feature_to_map
    JOIN sys_priv_submodules submodule ON submodule.id = feature_to_map.submodule_id
    JOIN sys_priv_modules privilege_module ON privilege_module.id = submodule.module_id
    JOIN sys_priv_feature_types feature_type ON feature_type.id = feature_to_map.feature_type_id
    JOIN sys_layout_navigation_modules navigation_module
      ON navigation_module.physical_module_code = privilege_module.code
    JOIN sys_layout_navigation_categories category
      ON category.navigation_module_id = navigation_module.id
     AND category.feature_type_code = feature_type.feature_type_code
    GROUP BY feature_to_map.id
) category_match
WHERE feature.id = category_match.feature_id;

ALTER TABLE sys_priv_features
    ALTER COLUMN navigation_category_id SET NOT NULL;

ALTER TABLE sys_priv_features
    DROP CONSTRAINT IF EXISTS fk_sys_priv_features_feature_type,
    DROP CONSTRAINT IF EXISTS uk_sys_features_submodule_type_code;

ALTER TABLE sys_priv_features
    ADD CONSTRAINT fk_sys_priv_features_navigation_category
        FOREIGN KEY (navigation_category_id) REFERENCES sys_layout_navigation_categories(id),
    ADD CONSTRAINT uk_sys_features_submodule_type_code
        UNIQUE (submodule_id, navigation_category_id, feature_code);

ALTER TABLE sys_priv_features DROP COLUMN feature_type_id;
DROP TABLE sys_priv_feature_types;
