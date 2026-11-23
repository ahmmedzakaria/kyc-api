-- Register Catalog in both the physical application catalog and the logical
-- privilege/navigation catalog. Privilege synchronization deliberately fails
-- closed when these trusted rows do not exist.

INSERT INTO sys_layout_module_groups (
    id, group_code, group_name, icon, display_order, active,
    created_by, updated_by, created_at, updated_at
)
VALUES (
    nextval('sys_layout_module_groups_id_seq'),
    'CATALOG', 'Catalog', 'store', 20, true,
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (group_code) DO UPDATE
SET group_name = EXCLUDED.group_name,
    icon = EXCLUDED.icon,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_physical_modules (
    module_code, module_name, active,
    created_by, updated_by, created_at, updated_at
)
VALUES (
    'CATALOG', 'Catalog', true,
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (module_code) DO UPDATE
SET module_name = EXCLUDED.module_name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_physical_submodules (
    physical_module_id, submodule_code, submodule_name, active,
    created_by, updated_by, created_at, updated_at
)
SELECT physical_module.id, 'CONTENT', 'Catalog Content', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_physical_modules physical_module
WHERE physical_module.module_code = 'CATALOG'
ON CONFLICT (physical_module_id, submodule_code) DO UPDATE
SET submodule_name = EXCLUDED.submodule_name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_modules (
    id, module_group_id, physical_module_id,
    navigation_module_code, navigation_module_name,
    module_priv_code, physical_module_code,
    icon, description, display_order, navigation_enabled, active,
    created_by, updated_by, created_at, updated_at
)
SELECT nextval('sys_layout_navigation_modules_id_seq'),
       module_group.id,
       physical_module.id,
       'CATALOG', 'Catalog',
       '12', '12',
       'store', 'Public catalog and corporate website administration',
       20, true, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_module_groups module_group
JOIN sys_physical_modules physical_module
  ON physical_module.module_code = 'CATALOG'
WHERE module_group.group_code = 'CATALOG'
  AND NOT EXISTS (
      SELECT 1 FROM sys_priv_modules existing
      WHERE existing.module_priv_code = '12'
         OR existing.navigation_module_code = 'CATALOG'
  );

UPDATE sys_priv_modules logical_module
SET module_group_id = module_group.id,
    physical_module_id = physical_module.id,
    navigation_module_code = 'CATALOG',
    navigation_module_name = 'Catalog',
    module_priv_code = '12',
    physical_module_code = '12',
    icon = 'store',
    description = 'Public catalog and corporate website administration',
    display_order = 20,
    navigation_enabled = true,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_layout_module_groups module_group
JOIN sys_physical_modules physical_module
  ON physical_module.module_code = 'CATALOG'
WHERE module_group.group_code = 'CATALOG'
  AND logical_module.module_priv_code = '12';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys_priv_modules
        WHERE module_priv_code = '12'
          AND navigation_module_code = 'CATALOG'
          AND active
    ) THEN
        RAISE EXCEPTION 'Catalog logical privilege module could not be registered consistently';
    END IF;
END $$;

INSERT INTO sys_priv_submodules (
    module_id, physical_submodule_id,
    submodule_priv_code, submodule_code, name, active,
    created_by, updated_by, created_at, updated_at
)
SELECT logical_module.id,
       physical_submodule.id,
       '01', 'CONTENT', 'Catalog Content', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules logical_module
JOIN sys_physical_modules physical_module
  ON physical_module.module_code = 'CATALOG'
JOIN sys_physical_submodules physical_submodule
  ON physical_submodule.physical_module_id = physical_module.id
 AND physical_submodule.submodule_code = 'CONTENT'
WHERE logical_module.module_priv_code = '12'
ON CONFLICT (module_id, submodule_priv_code) DO UPDATE
SET physical_submodule_id = EXCLUDED.physical_submodule_id,
    submodule_code = EXCLUDED.submodule_code,
    name = EXCLUDED.name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_navigation_categories (
    id, navigation_module_id,
    category_code, category_name, category_kind, feature_type_code,
    icon, display_order, active,
    created_by, updated_by, created_at, updated_at
)
SELECT nextval('sys_layout_navigation_categories_id_seq'),
       logical_module.id,
       seed.category_code, seed.category_name, seed.category_kind, seed.feature_type_code,
       seed.icon, seed.display_order, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules logical_module
CROSS JOIN (VALUES
    ('SETUP', 'Setup', 'CONFIGURATION', '01', 'sliders', 10),
    ('OPERATION', 'Operations', 'WORK', '02', 'briefcase', 20),
    ('REPORT', 'Reports', 'REPORTING', '03', 'chart-line', 30)
) AS seed(category_code, category_name, category_kind, feature_type_code, icon, display_order)
WHERE logical_module.module_priv_code = '12'
ON CONFLICT (navigation_module_id, category_code) DO UPDATE
SET category_name = EXCLUDED.category_name,
    category_kind = EXCLUDED.category_kind,
    feature_type_code = EXCLUDED.feature_type_code,
    icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;
