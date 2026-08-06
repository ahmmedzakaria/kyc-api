-- Normalize feature types and actions while preserving feature/privilege IDs and codes.

CREATE TABLE sys_priv_feature_types (
    id bigserial PRIMARY KEY,
    tenant_id bigint,
    feature_type_code varchar(2) NOT NULL,
    feature_type_name varchar(255) NOT NULL,
    description varchar(255),
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sys_priv_feature_types_global_code
    ON sys_priv_feature_types(feature_type_code)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_sys_priv_feature_types_tenant_code
    ON sys_priv_feature_types(tenant_id, feature_type_code)
    WHERE tenant_id IS NOT NULL;

INSERT INTO sys_priv_feature_types (
    tenant_id, feature_type_code, feature_type_name, description,
    active, created_by, updated_by, created_at, updated_at
)
SELECT NULL, feature_type_code, MAX(feature_type_name),
       'Global feature type migrated from sys_priv_features',
       true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_features
GROUP BY feature_type_code;

-- Ensure the three standard types exist even when no feature currently uses one.
INSERT INTO sys_priv_feature_types (
    tenant_id, feature_type_code, feature_type_name, description,
    active, created_by, updated_by, created_at, updated_at
)
VALUES
    (NULL, '01', 'Setup', 'Configuration and setup features', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (NULL, '02', 'Operations', 'Operational and transaction features', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (NULL, '03', 'Report', 'Reporting and analytical features', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (feature_type_code) WHERE tenant_id IS NULL DO UPDATE
SET feature_type_name = EXCLUDED.feature_type_name,
    description = EXCLUDED.description,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE sys_priv_features
    RENAME COLUMN code TO feature_code;

ALTER TABLE sys_priv_features
    RENAME COLUMN name TO feature_name;

ALTER TABLE sys_priv_features
    ADD COLUMN feature_type_id bigint;

UPDATE sys_priv_features feature
SET feature_type_id = feature_type.id
FROM sys_priv_feature_types feature_type
WHERE feature_type.tenant_id IS NULL
  AND feature_type.feature_type_code = feature.feature_type_code;

ALTER TABLE sys_priv_features
    ALTER COLUMN feature_type_id SET NOT NULL;

ALTER TABLE sys_priv_features
    ADD CONSTRAINT fk_sys_priv_features_feature_type
        FOREIGN KEY (feature_type_id) REFERENCES sys_priv_feature_types(id);

ALTER TABLE sys_priv_features
    DROP CONSTRAINT IF EXISTS uk_sys_features_submodule_type_code;

ALTER TABLE sys_priv_features
    ADD CONSTRAINT uk_sys_features_submodule_type_code
        UNIQUE (submodule_id, feature_type_id, feature_code);

ALTER TABLE sys_priv_features
    DROP COLUMN feature_type_code,
    DROP COLUMN feature_type_name;

CREATE TABLE sys_priv_actions (
    id bigserial PRIMARY KEY,
    action_code varchar(2) NOT NULL UNIQUE,
    action_name varchar(255) NOT NULL,
    description varchar(255),
    category varchar(40) NOT NULL,
    display_order integer NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_priv_actions (
    action_code, action_name, description, category, display_order,
    active, created_by, updated_by, created_at, updated_at
)
VALUES
    ('01', 'View', 'Read a specific record or detailed resource', 'READ_ONLY', 1, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('02', 'Search', 'List, filter, or query multiple resources', 'READ_ONLY', 2, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('03', 'Validate', 'Check validity or eligibility without changing state', 'READ_ONLY', 3, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('04', 'Export', 'Download or extract data from the system', 'READ_ONLY', 4, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10', 'Create', 'Add a new business or configuration record', 'DATA_CHANGE', 10, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('11', 'Import', 'Create or update records from an external source', 'DATA_CHANGE', 11, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('12', 'Update', 'Modify an existing record without changing lifecycle state', 'DATA_CHANGE', 12, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('13', 'Execute', 'Run a business operation, command, job, or process', 'DATA_CHANGE', 13, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20', 'Send Back', 'Return workflow work to an earlier step', 'WORKFLOW', 20, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('21', 'Approve', 'Accept a pending workflow decision or request', 'WORKFLOW', 21, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22', 'Reject', 'Decline a pending workflow decision or request', 'WORKFLOW', 22, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('23', 'Publish', 'Make an approved draft or version available', 'WORKFLOW', 23, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30', 'Activate', 'Enable an inactive resource for operational use', 'LIFECYCLE', 30, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('31', 'Suspend', 'Temporarily prevent use while preserving the resource', 'LIFECYCLE', 31, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('32', 'Cancel', 'Stop an in-progress operation or agreement', 'LIFECYCLE', 32, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33', 'Retire', 'End future use while retaining historical data', 'LIFECYCLE', 33, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('34', 'Archive', 'Move an inactive resource out of normal views', 'LIFECYCLE', 34, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40', 'Delete', 'Remove a resource while recovery remains possible', 'DESTRUCTIVE', 40, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('41', 'Purge', 'Irreversibly erase or anonymize a resource', 'DESTRUCTIVE', 41, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('80', 'Synchronize', 'Reconcile generated or external metadata', 'SYSTEM', 80, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('81', 'Assign', 'Grant or replace a role, privilege, scope, or owner', 'SYSTEM', 81, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('82', 'Generate', 'Create a system-issued key, secret, token, or artifact', 'SYSTEM', 82, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('83', 'Rotate', 'Replace a credential and retire its previous value', 'SYSTEM', 83, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('84', 'Revoke', 'Invalidate a credential, token, grant, or permission', 'SYSTEM', 84, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('85', 'Override', 'Replace a standard policy decision with authority', 'SYSTEM', 85, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('86', 'Impersonate', 'Act temporarily as another user under control', 'SYSTEM', 86, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('87', 'Manage', 'Perform broad administration when narrower actions do not apply', 'SYSTEM', 87, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Preserve any custom actions that predate the normalized catalog.
INSERT INTO sys_priv_actions (
    action_code, action_name, description, category, display_order,
    active, created_by, updated_by, created_at, updated_at
)
SELECT DISTINCT privilege.action_code, privilege.action_name,
       'Custom action migrated from sys_priv_privileges', 'CUSTOM',
       privilege.action_code::integer, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_privileges privilege
WHERE NOT EXISTS (
    SELECT 1 FROM sys_priv_actions action WHERE action.action_code = privilege.action_code
);

ALTER TABLE sys_priv_privileges
    ADD COLUMN action_id bigint;

UPDATE sys_priv_privileges privilege
SET action_id = action.id
FROM sys_priv_actions action
WHERE action.action_code = privilege.action_code;

ALTER TABLE sys_priv_privileges
    ALTER COLUMN action_id SET NOT NULL;

ALTER TABLE sys_priv_privileges
    ADD CONSTRAINT fk_sys_priv_privileges_action
        FOREIGN KEY (action_id) REFERENCES sys_priv_actions(id);

ALTER TABLE sys_priv_privileges
    DROP COLUMN action_code,
    DROP COLUMN action_name;
