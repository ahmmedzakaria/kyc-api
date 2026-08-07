-- Register the System Administration frontend (frontendApplications/system-frontend-21)
-- as its own backend client, reuse the existing WEB_DEFAULT layout profile
-- (same theme/sizes/fonts as the KYC web client — no visual reason to
-- duplicate it yet), and seed a minimal nav tree covering only the
-- Dashboard page that exists today. Access Control/Privilege/Layout/License
-- nav nodes are added by later migrations as each phase's pages are
-- actually built (see frontendApplications/system-frontend-21/AGENTS.md and
-- the implementation plan referenced there).

INSERT INTO sys_priv_client_applications (
    client_code, client_name, client_type, status, description,
    created_by, updated_by, created_at, updated_at
)
VALUES (
    'SYSTEM_ADMIN_WEB',
    'System Administration Console',
    'WEB',
    'ACTIVE',
    'Angular admin frontend (system-frontend-21) for the system module: access control, privilege, layout, and license administration.',
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (client_code) DO NOTHING;

INSERT INTO sys_client_layout_profiles (
    client_application_id, layout_profile_id, assignment_scope, device_target,
    default_profile, selectable, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, p.id, 'CLIENT', 'ANY', true, true, 100, true, 0, 0, now(), now()
FROM sys_priv_client_applications c
JOIN sys_layout_profiles p ON p.profile_code = 'WEB_DEFAULT'
WHERE c.client_code = 'SYSTEM_ADMIN_WEB'
  AND NOT EXISTS (
      SELECT 1 FROM sys_client_layout_profiles clp
      WHERE clp.client_application_id = c.id
        AND clp.default_profile = true
        AND clp.active = true
  );

INSERT INTO sys_layout_module_groups (
    group_code, group_name, icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT 'SYSTEM_ADMINISTRATION', 'System Administration', 'gear', 90, true, 0, 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sys_layout_module_groups WHERE group_code = 'SYSTEM_ADMINISTRATION');

INSERT INTO sys_layout_navigation_modules (
    module_group_id, navigation_module_code, navigation_module_name, physical_module_code,
    icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT g.id, 'SYSTEM', 'System', 'SYSTEM', 'gear', 10, true, 0, 0, now(), now()
FROM sys_layout_module_groups g
WHERE g.group_code = 'SYSTEM_ADMINISTRATION'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_navigation_modules WHERE navigation_module_code = 'SYSTEM');

INSERT INTO sys_layout_navigation_categories (
    navigation_module_id, category_code, category_name, category_kind, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT m.id, seed.code, seed.name, seed.kind, seed.icon, seed.sort, true, 0, 0, now(), now()
FROM sys_layout_navigation_modules m
CROSS JOIN (
    VALUES
      ('OPERATION', 'Operation', 'WORK', 'bolt', 10),
      ('SETUP', 'Setup', 'CONFIGURATION', 'gear', 20)
) AS seed(code, name, kind, icon, sort)
WHERE m.navigation_module_code = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_navigation_categories c
      WHERE c.navigation_module_id = m.id AND c.category_code = seed.code
  );

INSERT INTO sys_layout_feature_groups (
    navigation_category_id, feature_group_code, feature_group_name,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, 'OVERVIEW', 'Overview', 10, true, 0, 0, now(), now()
FROM sys_layout_navigation_categories c
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'SYSTEM'
  AND c.category_code = 'OPERATION'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_groups fg
      WHERE fg.navigation_category_id = c.id AND fg.feature_group_code = 'OVERVIEW'
  );

-- Left unlinked to any privilege (matches V8's PERSON_MANAGEMENT precedent
-- for a route the frontend already marks `data: { public: true }`) —
-- visible to any authenticated SYSTEM_ADMIN_WEB user; API authorization is
-- enforced independently server-side regardless of nav visibility.
INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'DASHBOARD', 'SYS_ADMIN_DASHBOARD', 'Dashboard', '/dashboard', 'home', 10, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
JOIN sys_layout_navigation_categories c ON c.id = fg.navigation_category_id
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'SYSTEM'
  AND c.category_code = 'OPERATION'
  AND fg.feature_group_code = 'OVERVIEW'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id AND f.feature_code = 'DASHBOARD'
  );
