-- Register the Log Administration frontend (frontendApplications/log-frontend-21)
-- as its own backend client, reuse the existing WEB_DEFAULT layout profile,
-- and seed a minimal nav tree covering only the Dashboard page that exists
-- today. The Logs page itself (Error/Access/Audit tabs) is added by a later
-- migration once its privileges have been synced (see
-- BootstrapAdministrationPrivilegeProvider's LOG_ADMINISTRATION entries) —
-- same ordering constraint V32/V33's user/role navigation had.

INSERT INTO sys_acc_client_applications (
    client_code, client_name, client_type, status, description,
    created_by, updated_by, created_at, updated_at
)
VALUES (
    'LOG_ADMIN_WEB',
    'Log Administration Console',
    'WEB',
    'ACTIVE',
    'Angular admin frontend (log-frontend-21) for viewing Error Log, Access Log, and Audit Log.',
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (client_code) DO NOTHING;

-- Client layout assignments are tenant-owned (see V38) — link LOG_ADMIN_WEB
-- to the platform "system" tenant (id 1, seeded by V37), same as every other
-- platform-administration client's own scope.
INSERT INTO sys_acc_client_application_tenants (
    client_application_id, tenant_id, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, t.id, true, 0, 0, now(), now()
FROM sys_acc_client_applications c
JOIN sys_tenants t ON t.tenant_code = 'system'
WHERE c.client_code = 'LOG_ADMIN_WEB'
  AND NOT EXISTS (
      SELECT 1 FROM sys_acc_client_application_tenants cat
      WHERE cat.client_application_id = c.id AND cat.tenant_id = t.id
  );

INSERT INTO sys_client_layout_profiles (
    client_application_id, layout_profile_id, tenant_id, assignment_scope, device_target,
    default_profile, selectable, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, p.id, t.id, 'CLIENT', 'ANY', true, true, 100, true, 0, 0, now(), now()
FROM sys_acc_client_applications c
JOIN sys_layout_profiles p ON p.profile_code = 'WEB_DEFAULT'
JOIN sys_tenants t ON t.tenant_code = 'system'
WHERE c.client_code = 'LOG_ADMIN_WEB'
  AND NOT EXISTS (
      SELECT 1 FROM sys_client_layout_profiles clp
      WHERE clp.client_application_id = c.id
        AND clp.tenant_id = t.id
        AND clp.default_profile = true
        AND clp.active = true
  );

INSERT INTO sys_layout_module_groups (
    group_code, group_name, icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT 'LOG_ADMINISTRATION', 'Log Administration', 'document', 91, true, 0, 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sys_layout_module_groups WHERE group_code = 'LOG_ADMINISTRATION');

INSERT INTO sys_layout_navigation_modules (
    module_group_id, navigation_module_code, navigation_module_name, physical_module_code,
    icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT g.id, 'LOG', 'Log', 'LOG', 'document', 10, true, 0, 0, now(), now()
FROM sys_layout_module_groups g
WHERE g.group_code = 'LOG_ADMINISTRATION'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_navigation_modules WHERE navigation_module_code = 'LOG');

INSERT INTO sys_layout_navigation_categories (
    navigation_module_id, category_code, category_name, category_kind, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT m.id, seed.code, seed.name, seed.kind, seed.icon, seed.sort, true, 0, 0, now(), now()
FROM sys_layout_navigation_modules m
CROSS JOIN (
    VALUES
      ('OPERATION', 'Operation', 'WORK', 'bolt', 10)
) AS seed(code, name, kind, icon, sort)
WHERE m.navigation_module_code = 'LOG'
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
WHERE m.navigation_module_code = 'LOG'
  AND c.category_code = 'OPERATION'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_groups fg
      WHERE fg.navigation_category_id = c.id AND fg.feature_group_code = 'OVERVIEW'
  );

-- Left unlinked to any privilege (matches V24's SYS_ADMIN_DASHBOARD precedent)
-- — visible to any authenticated LOG_ADMIN_WEB user; API authorization is
-- enforced independently server-side regardless of nav visibility.
INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'DASHBOARD', 'LOG_ADMIN_DASHBOARD', 'Dashboard', '/dashboard', 'home', 10, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
JOIN sys_layout_navigation_categories c ON c.id = fg.navigation_category_id
JOIN sys_layout_navigation_modules m ON m.id = c.navigation_module_id
WHERE m.navigation_module_code = 'LOG'
  AND c.category_code = 'OPERATION'
  AND fg.feature_group_code = 'OVERVIEW'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features f
      WHERE f.feature_group_id = fg.id AND f.feature_code = 'DASHBOARD'
  );
