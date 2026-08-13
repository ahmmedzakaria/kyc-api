-- The system frontend addresses UI authorization by stable semantic action code.
-- The backend owns the current action-to-privilege mapping.
INSERT INTO sys_layout_ui_policies (
    client_application_id, action_code, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, 'privileges.manage', 'ANY', true, 0, 0, now(), now()
FROM sys_acc_client_applications client
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (client_application_id, action_code) WHERE client_application_id IS NOT NULL
DO UPDATE SET active = true, updated_by = 0, updated_at = now();

INSERT INTO sys_layout_ui_policy_privileges (
    ui_policy_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT policy.id, privilege.id, true, 0, 0, now(), now()
FROM sys_acc_client_applications client
JOIN sys_layout_ui_policies policy
  ON policy.client_application_id = client.id
 AND policy.action_code = 'privileges.manage'
JOIN sys_priv_privileges privilege
  ON privilege.privilege_code = '11010100180'
WHERE client.client_code = 'SYSTEM_ADMIN_WEB'
ON CONFLICT (ui_policy_id, privilege_id)
DO UPDATE SET active = true, updated_by = 0, updated_at = now();
