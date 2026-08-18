-- Add the Logs page nav entry (Error/Access/Audit Log tabs, log-frontend-21
-- Phase 2) under the LOG_ADMINISTRATION module V48 seeded. Ships after V48
-- since it references privilege codes that only exist once the backend has
-- booted once with BootstrapAdministrationPrivilegeProvider's LOG_ADMINISTRATION
-- entries registered — same ordering constraint V29/V32's nav migrations follow.
--
-- Linked ANY-match to all three LOG_*_VIEW privileges so the nav item is
-- visible to a user holding any one of them; the page itself only renders
-- the tabs the signed-in user is actually authorized for, and each tab's
-- own list endpoint independently enforces its own privilege server-side
-- regardless of nav visibility.

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'LOGS', 'LOG_ADMIN_LOGS', 'Logs', '/logs', 'document', 20, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
JOIN sys_layout_navigation_categories c ON c.id = fg.navigation_category_id
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'LOG'
  AND c.category_code = 'OPERATION'
  AND fg.feature_group_code = 'OVERVIEW'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id AND f.feature_code = 'LOGS'
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT f.id, p.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features f
JOIN sys_layout_feature_groups fg ON fg.id = f.feature_group_id
JOIN sys_priv_privileges p ON p.privilege_code IN ('09010100101', '09010100201', '09010100301')
WHERE fg.feature_group_code = 'OVERVIEW'
  AND f.feature_code = 'LOGS'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges fp
      WHERE fp.layout_feature_id = f.id AND fp.privilege_id = p.id
  );
