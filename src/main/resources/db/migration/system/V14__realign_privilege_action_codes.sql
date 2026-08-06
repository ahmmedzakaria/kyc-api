-- Realign persisted privilege actions with the categorized PrivilegeAction ranges.
-- Existing privilege IDs are preserved, so role/user assignments remain intact.
-- System actor 0 owns this automated migration update.

CREATE TEMP TABLE privilege_action_code_migration (
    old_code varchar(2) PRIMARY KEY,
    temporary_code varchar(2) NOT NULL UNIQUE,
    new_code varchar(2) NOT NULL UNIQUE,
    action_name varchar(255) NOT NULL
) ON COMMIT DROP;

INSERT INTO privilege_action_code_migration (old_code, temporary_code, new_code, action_name)
VALUES
    ('01', '60', '10', 'Create'),
    ('02', '61', '12', 'Update'),
    ('03', '62', '40', 'Delete'),
    ('04', '63', '22', 'Reject'),
    ('05', '64', '20', 'Send Back'),
    ('06', '65', '01', 'View'),
    ('07', '66', '21', 'Approve'),
    ('08', '67', '02', 'Search');

CREATE TEMP TABLE duplicate_privilege_migration AS
SELECT legacy.id AS legacy_id, canonical.id AS canonical_id
FROM sys_priv_privileges legacy
JOIN privilege_action_code_migration mapping
  ON mapping.old_code = legacy.action_code
JOIN sys_priv_privileges canonical
  ON canonical.privilege_code = left(legacy.privilege_code, 9) || mapping.new_code
 AND canonical.id <> legacy.id;

-- Preserve role and user grants while consolidating duplicate catalog rows.
INSERT INTO sys_priv_role_privileges (role_id, privilege_id)
SELECT assignment.role_id, duplicate.canonical_id
FROM sys_priv_role_privileges assignment
JOIN duplicate_privilege_migration duplicate ON duplicate.legacy_id = assignment.privilege_id
ON CONFLICT (role_id, privilege_id) DO NOTHING;

DELETE FROM sys_priv_role_privileges assignment
USING duplicate_privilege_migration duplicate
WHERE assignment.privilege_id = duplicate.legacy_id;

INSERT INTO sys_priv_user_privileges (user_id, privilege_id)
SELECT assignment.user_id, duplicate.canonical_id
FROM sys_priv_user_privileges assignment
JOIN duplicate_privilege_migration duplicate ON duplicate.legacy_id = assignment.privilege_id
ON CONFLICT (user_id, privilege_id) DO NOTHING;

DELETE FROM sys_priv_user_privileges assignment
USING duplicate_privilege_migration duplicate
WHERE assignment.privilege_id = duplicate.legacy_id;

-- Consolidate client and layout assignments that have uniqueness constraints.
INSERT INTO sys_priv_client_feature_permissions (
    client_application_id, privilege_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT permission.client_application_id, duplicate.canonical_id, permission.active,
       permission.created_by, 0, permission.created_at, CURRENT_TIMESTAMP
FROM sys_priv_client_feature_permissions permission
JOIN duplicate_privilege_migration duplicate ON duplicate.legacy_id = permission.privilege_id
ON CONFLICT (client_application_id, privilege_id) DO UPDATE
SET active = sys_priv_client_feature_permissions.active OR EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM sys_priv_client_feature_permissions permission
USING duplicate_privilege_migration duplicate
WHERE permission.privilege_id = duplicate.legacy_id;

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT assignment.layout_feature_id, duplicate.canonical_id, assignment.match_mode, assignment.active,
       assignment.created_by, 0, assignment.created_at, CURRENT_TIMESTAMP
FROM sys_layout_feature_privileges assignment
JOIN duplicate_privilege_migration duplicate ON duplicate.legacy_id = assignment.privilege_id
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE
SET active = sys_layout_feature_privileges.active OR EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM sys_layout_feature_privileges assignment
USING duplicate_privilege_migration duplicate
WHERE assignment.privilege_id = duplicate.legacy_id;

-- These entitlement references are not unique and can be reassigned directly.
UPDATE sys_license_plan_entitlements entitlement
SET privilege_id = duplicate.canonical_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM duplicate_privilege_migration duplicate
WHERE entitlement.privilege_id = duplicate.legacy_id;

UPDATE sys_license_entitlement_overrides entitlement
SET privilege_id = duplicate.canonical_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM duplicate_privilege_migration duplicate
WHERE entitlement.privilege_id = duplicate.legacy_id;

-- All ID-based references now point to the canonical new-code row.
DELETE FROM sys_priv_privileges privilege
USING duplicate_privilege_migration duplicate
WHERE privilege.id = duplicate.legacy_id;

-- Update denormalized privilege-code references before changing the catalog code.
UPDATE sys_priv_api_registry registry
SET required_privilege_code = left(registry.required_privilege_code, 9) || mapping.new_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE length(registry.required_privilege_code) = 11
  AND right(registry.required_privilege_code, 2) = mapping.old_code;

UPDATE sys_priv_api_registry registry
SET action_code = mapping.new_code,
    action_name = mapping.action_name,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE registry.action_code = mapping.old_code;

UPDATE sys_client_layout_profiles profile
SET privilege_code = left(profile.privilege_code, 9) || mapping.new_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE length(profile.privilege_code) = 11
  AND right(profile.privilege_code, 2) = mapping.old_code;

UPDATE sys_workflow_transitions transition
SET required_privilege_code = left(transition.required_privilege_code, 9) || mapping.new_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE length(transition.required_privilege_code) = 11
  AND right(transition.required_privilege_code, 2) = mapping.old_code;

UPDATE sys_workflow_assignment_policies policy
SET privilege_code = left(policy.privilege_code, 9) || mapping.new_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE length(policy.privilege_code) = 11
  AND right(policy.privilege_code, 2) = mapping.old_code;

UPDATE sys_workflow_tasks task
SET assigned_privilege_code = left(task.assigned_privilege_code, 9) || mapping.new_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE length(task.assigned_privilege_code) = 11
  AND right(task.assigned_privilege_code, 2) = mapping.old_code;

-- Use temporary suffixes so VIEW 06 -> 01 and SEARCH 08 -> 02 cannot collide
-- with CREATE 01 and UPDATE 02 while the unique index is being updated.
UPDATE sys_priv_privileges privilege
SET privilege_code = left(privilege.privilege_code, 9) || mapping.temporary_code,
    action_code = mapping.temporary_code,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE privilege.action_code = mapping.old_code;

UPDATE sys_priv_privileges privilege
SET privilege_code = left(privilege.privilege_code, 9) || mapping.new_code,
    action_code = mapping.new_code,
    action_name = mapping.action_name,
    updated_at = CURRENT_TIMESTAMP
FROM privilege_action_code_migration mapping
WHERE privilege.action_code = mapping.temporary_code;
