-- Add nav-tree entries for the Access Control pages built in
-- system-frontend-21 Phase 1 (Client Applications, API Registry), under the
-- SYSTEM_ADMINISTRATION > SYSTEM > SETUP branch seeded by V24. Each feature
-- is linked to its matching *_VIEW privilege so the sidebar only shows it to
-- users who can actually load the page (same pattern as V8's PERSON_MANAGEMENT
-- linking PERSON_LIST to the person view privileges) — an unlinked feature
-- would stay visible to every authenticated user regardless of privilege.

INSERT INTO sys_layout_feature_groups (
    navigation_category_id, feature_group_code, feature_group_name,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, 'ACCESS_CONTROL', 'Access Control', 10, true, 0, 0, now(), now()
FROM sys_layout_navigation_categories c
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'SYSTEM'
  AND c.category_code = 'SETUP'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_groups fg
      WHERE fg.navigation_category_id = c.id AND fg.feature_group_code = 'ACCESS_CONTROL'
  );

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, seed.feature_code, seed.t_code, seed.feature_name, seed.route, seed.icon,
       seed.display_order, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
JOIN sys_layout_navigation_categories c ON c.id = fg.navigation_category_id
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
CROSS JOIN (
    VALUES
        ('CLIENT_APPLICATIONS', 'SYS_CLIENT_APPS', 'Client Applications', '/access-control/client-applications', 'building', 10),
        ('API_REGISTRY', 'SYS_API_REGISTRY', 'API Registry', '/access-control/api-registry', 'table', 20)
) AS seed(feature_code, t_code, feature_name, route, icon, display_order)
WHERE m.navigation_module_code = 'SYSTEM'
  AND c.category_code = 'SETUP'
  AND fg.feature_group_code = 'ACCESS_CONTROL'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id AND f.feature_code = seed.feature_code
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_priv_privileges p ON p.privilege_code = '11020100101'
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND f.feature_code = 'CLIENT_APPLICATIONS'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id AND fp.privilege_id = p.id
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_priv_privileges p ON p.privilege_code = '11020100601'
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND f.feature_code = 'API_REGISTRY'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id AND fp.privilege_id = p.id
  );
