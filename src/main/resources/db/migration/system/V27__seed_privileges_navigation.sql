-- Add a nav-tree entry for the Privileges page built in system-frontend-21
-- Phase 2, under the same ACCESS_CONTROL feature group V25 created for
-- Client Applications/API Registry. Linked to PRIVILEGE_CATALOG_VIEW so the
-- sidebar only shows it to users who can actually load the page (same
-- pattern as V25).

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'PRIVILEGES', 'SYS_PRIVILEGES', 'Privileges', '/access-control/privileges', 'lock',
       30, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id AND f.feature_code = 'PRIVILEGES'
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_priv_privileges p ON p.privilege_code = '11010100101'
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND f.feature_code = 'PRIVILEGES'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id AND fp.privilege_id = p.id
  );
