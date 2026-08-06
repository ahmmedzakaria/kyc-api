-- Seed the privileges needed to protect security-administration APIs.
--
-- ROLE_ADMIN lives in auth_db, while this catalog lives in system_db. Flyway
-- must not assume that a role has the same numeric ID in every environment.
-- DataSeeder resolves ROLE_ADMIN in auth_db and assigns these catalog entries
-- through SystemPrivilegeRegistryService after all datasources are available.
-- System actor 0 owns rows created or reconciled by this migration.

ALTER TABLE sys_priv_privileges
    ADD COLUMN IF NOT EXISTS created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by bigint NOT NULL DEFAULT 0;

ALTER TABLE sys_priv_role_privileges
    ADD COLUMN IF NOT EXISTS created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

INSERT INTO sys_priv_modules (
    code, name, active, created_by, updated_by, created_at, updated_at
)
VALUES ('11', 'System', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_submodules (
    module_id, code, name, active, created_by, updated_by, created_at, updated_at
)
SELECT module.id, seed.code, seed.name, true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_priv_modules module
CROSS JOIN (VALUES
    ('01', 'Privilege'),
    ('02', 'Access Control'),
    ('03', 'License'),
    ('04', 'Layout'),
    ('05', 'Workflow')
) AS seed(code, name)
WHERE module.code = '11'
ON CONFLICT (module_id, code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_features (
    submodule_id, feature_type_id, feature_code, feature_name,
    active, created_by, updated_by, created_at, updated_at
)
SELECT submodule.id, feature_type.id, seed.feature_code, seed.feature_name,
       true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('01', '001', 'Privilege Catalog'),
    ('02', '001', 'Client Application'),
    ('02', '002', 'Client Credential'),
    ('02', '003', 'Client API Permission'),
    ('02', '004', 'Client Feature Permission'),
    ('02', '005', 'Client Tenant Assignment'),
    ('02', '006', 'API Registry'),
    ('03', '999', 'License Administration'),
    ('04', '001', 'Layout Administration'),
    ('05', '001', 'Workflow Administration')
) AS seed(submodule_code, feature_code, feature_name)
JOIN sys_priv_modules module ON module.code = '11'
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id
 AND submodule.code = seed.submodule_code
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL
 AND feature_type.feature_type_code = '01'
ON CONFLICT (submodule_id, feature_type_id, feature_code) DO UPDATE
SET feature_name = EXCLUDED.feature_name,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_priv_privileges (
    privilege_code, feature_id, action_id, sub_menu_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT seed.privilege_code, feature.id, action.id, NULL, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('11020100101', '02', '001', '01'),
    ('11020100187', '02', '001', '87'),
    ('11020100283', '02', '002', '83'),
    ('11020100381', '02', '003', '81'),
    ('11020100481', '02', '004', '81'),
    ('11020100581', '02', '005', '81'),
    ('11020100601', '02', '006', '01'),
    ('11020100687', '02', '006', '87'),
    ('11020100680', '02', '006', '80'),
    ('11010100101', '01', '001', '01'),
    ('11010100180', '01', '001', '80'),
    ('11010100181', '01', '001', '81'),
    ('11040100101', '04', '001', '01'),
    ('11040100187', '04', '001', '87'),
    ('11050100101', '05', '001', '01'),
    ('11050100187', '05', '001', '87'),
    ('11030199901', '03', '999', '01'),
    ('11030199987', '03', '999', '87')
) AS seed(privilege_code, submodule_code, feature_code, action_code)
JOIN sys_priv_modules module ON module.code = '11'
JOIN sys_priv_submodules submodule
  ON submodule.module_id = module.id
 AND submodule.code = seed.submodule_code
JOIN sys_priv_feature_types feature_type
  ON feature_type.tenant_id IS NULL
 AND feature_type.feature_type_code = '01'
JOIN sys_priv_features feature
  ON feature.submodule_id = submodule.id
 AND feature.feature_type_id = feature_type.id
 AND feature.feature_code = seed.feature_code
JOIN sys_priv_actions action ON action.action_code = seed.action_code
ON CONFLICT (privilege_code) DO UPDATE
SET feature_id = EXCLUDED.feature_id,
    action_id = EXCLUDED.action_id,
    active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;
