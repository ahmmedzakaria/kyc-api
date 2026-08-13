-- Client-specific route policies consumed by WEB and SYSTEM_ADMIN_WEB.
INSERT INTO sys_layout_route_policies (
    client_application_id, route_url, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, seed.route_url, 'ANY', true, 0, 0, now(), now()
FROM (VALUES
    ('WEB', '/person'), ('WEB', '/person/create'), ('WEB', '/person/:id/edit'), ('WEB', '/person/:id/preview'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/create'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/:id/edit'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/:id'),
    ('SYSTEM_ADMIN_WEB', '/access-control/api-registry'),
    ('SYSTEM_ADMIN_WEB', '/access-control/privileges'),
    ('SYSTEM_ADMIN_WEB', '/access-control/tenants'),
    ('SYSTEM_ADMIN_WEB', '/access-control/users'),
    ('SYSTEM_ADMIN_WEB', '/layout/profiles'), ('SYSTEM_ADMIN_WEB', '/layout/navigation-tree'),
    ('SYSTEM_ADMIN_WEB', '/license'), ('SYSTEM_ADMIN_WEB', '/backup')
) seed(client_code, route_url)
JOIN sys_acc_client_applications client ON client.client_code = seed.client_code
ON CONFLICT (client_application_id, route_url) WHERE client_application_id IS NOT NULL
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

INSERT INTO sys_layout_route_policy_privileges (
    route_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM (VALUES
    ('WEB', '/person', '01010200102'), ('WEB', '/person/create', '01010200110'),
    ('WEB', '/person/:id/edit', '01010200112'), ('WEB', '/person/:id/preview', '01010200101'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications', '11020100101'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/create', '11020100187'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/:id/edit', '11020100187'),
    ('SYSTEM_ADMIN_WEB', '/access-control/client-applications/:id', '11020100101'),
    ('SYSTEM_ADMIN_WEB', '/access-control/api-registry', '11020100601'),
    ('SYSTEM_ADMIN_WEB', '/access-control/privileges', '11010100101'),
    ('SYSTEM_ADMIN_WEB', '/access-control/tenants', '11020100901'),
    ('SYSTEM_ADMIN_WEB', '/access-control/users', '11020100701'),
    ('SYSTEM_ADMIN_WEB', '/layout/profiles', '11040100101'),
    ('SYSTEM_ADMIN_WEB', '/layout/navigation-tree', '11040100101'),
    ('SYSTEM_ADMIN_WEB', '/license', '11030199901'),
    ('SYSTEM_ADMIN_WEB', '/backup', '11060100101')
) seed(client_code, route_url, privilege_code)
JOIN sys_acc_client_applications client ON client.client_code = seed.client_code
JOIN sys_layout_route_policies policy ON policy.client_application_id = client.id AND policy.route_url = seed.route_url
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
ON CONFLICT (route_policy_id, privilege_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

INSERT INTO sys_layout_ui_policies (
    client_application_id, action_code, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, seed.action_code, 'ANY', true, 0, 0, now(), now()
FROM (VALUES
    ('WEB', 'person.list.add-button'), ('WEB', 'person.list.preview-button'),
    ('WEB', 'person.list.edit-button'), ('WEB', 'person.list.delete-button'), ('WEB', 'person.preview.edit-button'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.create'), ('SYSTEM_ADMIN_WEB', 'client-applications.edit'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.rotate'), ('SYSTEM_ADMIN_WEB', 'client-applications.assign-api'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.assign-feature'), ('SYSTEM_ADMIN_WEB', 'client-applications.assign-scope'),
    ('SYSTEM_ADMIN_WEB', 'api-registry.manage'), ('SYSTEM_ADMIN_WEB', 'api-registry.synchronize'),
    ('SYSTEM_ADMIN_WEB', 'privileges.assign'), ('SYSTEM_ADMIN_WEB', 'users.manage'),
    ('SYSTEM_ADMIN_WEB', 'users.assign'), ('SYSTEM_ADMIN_WEB', 'tenants.register'),
    ('SYSTEM_ADMIN_WEB', 'tenants.verify-domain'), ('SYSTEM_ADMIN_WEB', 'tenants.manage-lifecycle'),
    ('SYSTEM_ADMIN_WEB', 'layout.manage'), ('SYSTEM_ADMIN_WEB', 'license.manage'),
    ('SYSTEM_ADMIN_WEB', 'backup.execute'), ('SYSTEM_ADMIN_WEB', 'backup.download'),
    ('SYSTEM_ADMIN_WEB', 'backup.deliver'), ('SYSTEM_ADMIN_WEB', 'backup.manage')
) seed(client_code, action_code)
JOIN sys_acc_client_applications client ON client.client_code = seed.client_code
ON CONFLICT (client_application_id, action_code) WHERE client_application_id IS NOT NULL
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

INSERT INTO sys_layout_ui_policy_privileges (
    ui_policy_id, privilege_id, active, created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM (VALUES
    ('WEB', 'person.list.add-button', '01010200110'), ('WEB', 'person.list.preview-button', '01010200101'),
    ('WEB', 'person.list.edit-button', '01010200112'), ('WEB', 'person.list.delete-button', '01010200140'),
    ('WEB', 'person.preview.edit-button', '01010200112'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.create', '11020100187'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.edit', '11020100187'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.rotate', '11020100283'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.assign-api', '11020100381'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.assign-feature', '11020100481'),
    ('SYSTEM_ADMIN_WEB', 'client-applications.assign-scope', '11020100581'),
    ('SYSTEM_ADMIN_WEB', 'api-registry.manage', '11020100687'),
    ('SYSTEM_ADMIN_WEB', 'api-registry.synchronize', '11020100680'),
    ('SYSTEM_ADMIN_WEB', 'privileges.assign', '11010100181'),
    ('SYSTEM_ADMIN_WEB', 'users.manage', '11020100787'), ('SYSTEM_ADMIN_WEB', 'users.assign', '11020100781'),
    ('SYSTEM_ADMIN_WEB', 'tenants.register', '11020100910'),
    ('SYSTEM_ADMIN_WEB', 'tenants.verify-domain', '11020100980'),
    ('SYSTEM_ADMIN_WEB', 'tenants.manage-lifecycle', '11020100987'),
    ('SYSTEM_ADMIN_WEB', 'layout.manage', '11040100187'),
    ('SYSTEM_ADMIN_WEB', 'license.manage', '11030199987'),
    ('SYSTEM_ADMIN_WEB', 'backup.execute', '11060100113'),
    ('SYSTEM_ADMIN_WEB', 'backup.download', '11060100104'),
    ('SYSTEM_ADMIN_WEB', 'backup.deliver', '11060100180'),
    ('SYSTEM_ADMIN_WEB', 'backup.manage', '11060100187')
) seed(client_code, action_code, privilege_code)
JOIN sys_acc_client_applications client ON client.client_code = seed.client_code
JOIN sys_layout_ui_policies policy ON policy.client_application_id = client.id AND policy.action_code = seed.action_code
JOIN sys_priv_privileges privilege ON privilege.privilege_code = seed.privilege_code
ON CONFLICT (ui_policy_id, privilege_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = now();
