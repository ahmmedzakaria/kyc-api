-- Populate route policies (routePolicies) and UI policies (uiPolicies) for the
-- KYC Person module — both tables have been empty since V19/V20 created them,
-- which is why:
--   (a) routePrivilegeGuard fail-closes on every /person route regardless of
--       the caller's actual privileges (routePolicies drives route access,
--       separately from the nav-tree's own privilegeCodes which only control
--       link *visibility*), and
--   (b) every *appAuthorizedUi button on person-list/person-preview has been
--       silently hidden for all users, since AuthorizedUiDirective fails
--       closed when no uiPolicy row matches its actionCode.
-- All rows are seeded GLOBAL (client_application_id NULL) — Person is a
-- KYC-domain concern, not scoped to a specific client app; a client-specific
-- override can be added later via client_application_id if ever needed.
-- Privilege codes below are exactly what KycPrivilegeProvider.java derives
-- (module 01 + submodule 01 + featureType 02 + feature 001 + action code).

INSERT INTO sys_layout_route_policies (
    client_application_id, route_url, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT NULL, seed.route_url, 'ANY', true, 0, 0, now(), now()
FROM (VALUES
    ('/person'),
    ('/person/create'),
    ('/person/:id/edit'),
    ('/person/:id/preview')
) AS seed(route_url)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_layout_route_policies p
    WHERE p.client_application_id IS NULL AND p.route_url = seed.route_url
);

INSERT INTO sys_layout_route_policy_privileges (
    route_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM sys_layout_route_policies policy
JOIN (VALUES
    ('/person', '01010200101'),         -- Person / View
    ('/person/create', '01010200110'),  -- Person / Create
    ('/person/:id/edit', '01010200112'),-- Person / Update
    ('/person/:id/preview', '01010200101')
) AS seed(route_url, privilege_code) ON seed.route_url = policy.route_url
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
WHERE policy.client_application_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_route_policy_privileges rpp
      WHERE rpp.route_policy_id = policy.id AND rpp.privilege_id = privilege.id
  );

INSERT INTO sys_layout_ui_policies (
    client_application_id, action_code, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT NULL, seed.action_code, 'ANY', true, 0, 0, now(), now()
FROM (VALUES
    ('person.list.add-button'),
    ('person.list.preview-button'),
    ('person.list.edit-button'),
    ('person.list.delete-button'),
    ('person.preview.edit-button')
) AS seed(action_code)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_layout_ui_policies p
    WHERE p.client_application_id IS NULL AND p.action_code = seed.action_code
);

INSERT INTO sys_layout_ui_policy_privileges (
    ui_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM sys_layout_ui_policies policy
JOIN (VALUES
    ('person.list.add-button', '01010200110'),      -- Person / Create
    ('person.list.preview-button', '01010200101'),  -- Person / View
    ('person.list.edit-button', '01010200112'),     -- Person / Update
    ('person.list.delete-button', '01010200140'),   -- Person / Delete
    ('person.preview.edit-button', '01010200112')
) AS seed(action_code, privilege_code) ON seed.action_code = policy.action_code
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
WHERE policy.client_application_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_ui_policy_privileges upp
      WHERE upp.ui_policy_id = policy.id AND upp.privilege_id = privilege.id
  );
