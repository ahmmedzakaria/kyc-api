-- Repair databases where the original person-navigation seed was skipped by
-- Flyway baselining. All statements are idempotent so this is also safe where
-- V8 already populated the hierarchy.

INSERT INTO sys_layout_feature_groups (
    navigation_category_id, feature_group_code, feature_group_name,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, 'PERSON_MANAGEMENT', 'Person Management',
       10, true, 0, 0, now(), now()
FROM sys_layout_navigation_categories c
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'KYC'
  AND c.category_code = 'OPERATION'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_layout_feature_groups fg
      WHERE fg.navigation_category_id = c.id
        AND fg.feature_group_code = 'PERSON_MANAGEMENT'
  );

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    physical_module_code, physical_submodule_code,
    physical_feature_type_code, physical_feature_code,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, seed.feature_code, seed.t_code, seed.feature_name, seed.route, seed.icon,
       '01', '01', '02', '001',
       seed.display_order, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
JOIN sys_layout_navigation_categories c ON c.id = fg.navigation_category_id
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
CROSS JOIN (
    VALUES
        ('PERSON_LIST', 'PERSON_LIST', 'Person List', '/person', 'users', 10),
        ('PERSON_CREATE', 'PERSON_CREATE', 'Add Person', '/person/create', 'user', 20)
) AS seed(feature_code, t_code, feature_name, route, icon, display_order)
WHERE m.navigation_module_code = 'KYC'
  AND c.category_code = 'OPERATION'
  AND fg.feature_group_code = 'PERSON_MANAGEMENT'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id
        AND f.feature_code = seed.feature_code
  );

-- Attach the menu entries to the same privileges used by the person API.
-- If the privilege catalog is not populated yet, the features remain unlinked
-- and therefore visible; API authorization is still enforced independently.
INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_privileges p ON p.privilege_code IN ('01010200106', '01010200108')
WHERE fg.feature_group_code = 'PERSON_MANAGEMENT'
  AND f.feature_code = 'PERSON_LIST'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id
        AND fp.privilege_id = p.id
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_privileges p ON p.privilege_code = '01010200101'
WHERE fg.feature_group_code = 'PERSON_MANAGEMENT'
  AND f.feature_code = 'PERSON_CREATE'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id
        AND fp.privilege_id = p.id
  );
