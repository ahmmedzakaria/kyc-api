-- Add nav-tree entries for the Layout Administration pages built in
-- system-frontend-21 Phase 3 (Layout Profiles/UI Policies/Client Assignment,
-- and the Navigation Tree admin), under the same ACCESS_CONTROL feature group
-- V25 created. Linked to LAYOUT_ADMINISTRATION_VIEW so the sidebar only shows
-- them to users who can actually load the pages (same pattern as V25/V27).

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, seed.feature_code, seed.t_code, seed.feature_name, seed.route, seed.icon,
       seed.display_order, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
CROSS JOIN (
    VALUES
        ('LAYOUT_PROFILES', 'SYS_LAYOUT_PROFILES', 'Layout Profiles', '/layout/profiles', 'grid', 40),
        ('LAYOUT_NAVIGATION_TREE', 'SYS_LAYOUT_NAV_TREE', 'Navigation Tree', '/layout/navigation-tree', 'bar-chart', 50)
) AS seed(feature_code, t_code, feature_name, route, icon, display_order)
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
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
JOIN sys_priv_privileges p ON p.privilege_code = '11040100101'
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND f.feature_code IN ('LAYOUT_PROFILES', 'LAYOUT_NAVIGATION_TREE')
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id AND fp.privilege_id = p.id
  );
