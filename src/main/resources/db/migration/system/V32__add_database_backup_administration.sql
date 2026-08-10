-- Database backup job metadata, privileges, and system-admin navigation.

CREATE TABLE sys_backup_jobs (
    id uuid PRIMARY KEY,
    trigger_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    requested_by varchar(150) NOT NULL,
    requested_at timestamp NOT NULL,
    started_at timestamp,
    completed_at timestamp,
    artifact_name varchar(255),
    artifact_size_bytes bigint,
    artifact_sha256 varchar(64),
    encryption_key_id varchar(120),
    failure_code varchar(80),
    sanitized_failure_message varchar(1000),
    expires_at timestamp,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sys_backup_job_trigger CHECK (trigger_type IN ('SCHEDULED', 'MANUAL')),
    CONSTRAINT ck_sys_backup_job_status CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'PARTIAL', 'FAILED', 'DELETED'))
);

CREATE INDEX idx_sys_backup_jobs_requested_at ON sys_backup_jobs(requested_at DESC);
CREATE INDEX idx_sys_backup_jobs_status ON sys_backup_jobs(status);

CREATE TABLE sys_backup_database_results (
    id bigserial PRIMARY KEY,
    backup_job_id uuid NOT NULL REFERENCES sys_backup_jobs(id) ON DELETE CASCADE,
    logical_database varchar(40) NOT NULL,
    database_name varchar(120) NOT NULL,
    status varchar(20) NOT NULL,
    started_at timestamp,
    completed_at timestamp,
    size_bytes bigint,
    sha256 varchar(64),
    failure_code varchar(80),
    sanitized_failure_message varchar(1000),
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_backup_database_result UNIQUE (backup_job_id, logical_database)
);

INSERT INTO sys_priv_submodules (
    module_id, code, name, active, created_by, updated_by, created_at, updated_at
)
SELECT module.id, '06', 'Backup', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules module
WHERE module.code = '11'
ON CONFLICT (module_id, code) DO UPDATE
SET name = EXCLUDED.name, active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_features (
    submodule_id, feature_type_id, feature_code, feature_name,
    active, created_by, updated_by, created_at, updated_at
)
SELECT submodule.id, feature_type.id, '001', 'Database Backup Administration',
       true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules module
JOIN sys_priv_submodules submodule ON submodule.module_id = module.id AND submodule.code = '06'
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL AND feature_type.feature_type_code = '01'
WHERE module.code = '11'
ON CONFLICT (submodule_id, feature_type_id, feature_code) DO UPDATE
SET feature_name = EXCLUDED.feature_name, active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_privileges (
    privilege_code, feature_id, action_id, sub_menu_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT seed.privilege_code, feature.id, action.id, NULL, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('11060100101', '01'),
    ('11060100113', '13'),
    ('11060100104', '04'),
    ('11060100180', '80'),
    ('11060100187', '87')
) AS seed(privilege_code, action_code)
JOIN sys_priv_modules module ON module.code = '11'
JOIN sys_priv_submodules submodule ON submodule.module_id = module.id AND submodule.code = '06'
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL AND feature_type.feature_type_code = '01'
JOIN sys_priv_features feature
  ON feature.submodule_id = submodule.id
 AND feature.feature_type_id = feature_type.id
 AND feature.feature_code = '001'
JOIN sys_priv_actions action ON action.action_code = seed.action_code
ON CONFLICT (privilege_code) DO UPDATE
SET feature_id = EXCLUDED.feature_id, action_id = EXCLUDED.action_id,
    active = true, updated_by = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'DATABASE_BACKUP', 'SYS_DATABASE_BACKUP', 'Database Backup', '/backup', 'archive',
       70, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_feature_groups fg
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features feature
      WHERE feature.feature_group_id = fg.id AND feature.feature_code = 'DATABASE_BACKUP'
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT feature.id, privilege.id, 'ANY', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_features feature
JOIN sys_layout_feature_groups feature_group ON feature_group.id = feature.feature_group_id
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11060100101'
WHERE feature_group.feature_group_code = 'ACCESS_CONTROL'
  AND feature.feature_code = 'DATABASE_BACKUP'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges link
      WHERE link.layout_feature_id = feature.id AND link.privilege_id = privilege.id
  );
