-- Normalize module, submodule, and feature metadata out of privilege and menu rows.

CREATE TABLE IF NOT EXISTS sys_modules (
    id bigserial PRIMARY KEY,
    code varchar(2) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_submodules (
    id bigserial PRIMARY KEY,
    module_id bigint NOT NULL REFERENCES sys_modules(id) ON DELETE CASCADE,
    code varchar(2) NOT NULL,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_submodules_module_code UNIQUE (module_id, code)
);

CREATE TABLE IF NOT EXISTS sys_features (
    id bigserial PRIMARY KEY,
    submodule_id bigint NOT NULL REFERENCES sys_submodules(id) ON DELETE CASCADE,
    feature_type_code varchar(2) NOT NULL,
    feature_type_name varchar(255) NOT NULL,
    code varchar(3) NOT NULL,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT uk_sys_features_submodule_type_code UNIQUE (submodule_id, feature_type_code, code)
);

INSERT INTO sys_modules (code, name, active, created_at, updated_at)
SELECT module_code, MAX(module_name), true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT module_code, module_name FROM sys_privileges
    UNION
    SELECT module_code, module_name FROM sys_sub_menus
) module_source
WHERE module_code IS NOT NULL
GROUP BY module_code
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_submodules (module_id, code, name, active, created_at, updated_at)
SELECT m.id, source.submodule_code, MAX(source.submodule_name), true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT module_code, submodule_code, submodule_name FROM sys_privileges
    UNION
    SELECT module_code, submodule_code, submodule_name FROM sys_sub_menus
) source
JOIN sys_modules m ON m.code = source.module_code
WHERE source.submodule_code IS NOT NULL
GROUP BY m.id, source.submodule_code
ON CONFLICT (module_id, code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_features (
    submodule_id,
    feature_type_code,
    feature_type_name,
    code,
    name,
    active,
    created_at,
    updated_at
)
SELECT DISTINCT
    sm.id,
    source.feature_type_code,
    MAX(source.feature_type_name),
    source.feature_code,
    MAX(source.feature_name),
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT module_code, submodule_code, feature_type_code, feature_type_name, feature_code, feature_name FROM sys_privileges
    UNION
    SELECT module_code, submodule_code, feature_type_code, feature_type_name, feature_code, feature_name FROM sys_sub_menus
) source
JOIN sys_modules m ON m.code = source.module_code
JOIN sys_submodules sm ON sm.module_id = m.id AND sm.code = source.submodule_code
WHERE source.feature_code IS NOT NULL
GROUP BY sm.id, source.feature_type_code, source.feature_code
ON CONFLICT (submodule_id, feature_type_code, code) DO UPDATE
SET feature_type_name = EXCLUDED.feature_type_name,
    name = EXCLUDED.name,
    active = true,
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE sys_privileges
    ADD COLUMN IF NOT EXISTS feature_id bigint;

UPDATE sys_privileges p
SET feature_id = f.id
FROM sys_features f
JOIN sys_submodules sm ON sm.id = f.submodule_id
JOIN sys_modules m ON m.id = sm.module_id
WHERE p.feature_id IS NULL
  AND m.code = p.module_code
  AND sm.code = p.submodule_code
  AND f.feature_type_code = p.feature_type_code
  AND f.code = p.feature_code;

ALTER TABLE sys_sub_menus
    ADD COLUMN IF NOT EXISTS feature_id bigint;

UPDATE sys_sub_menus menu
SET feature_id = f.id
FROM sys_features f
JOIN sys_submodules sm ON sm.id = f.submodule_id
JOIN sys_modules m ON m.id = sm.module_id
WHERE menu.feature_id IS NULL
  AND m.code = menu.module_code
  AND sm.code = menu.submodule_code
  AND f.feature_type_code = menu.feature_type_code
  AND f.code = menu.feature_code;

ALTER TABLE sys_privileges
    ALTER COLUMN feature_id SET NOT NULL;

ALTER TABLE sys_sub_menus
    ALTER COLUMN feature_id SET NOT NULL;

ALTER TABLE sys_privileges
    DROP CONSTRAINT IF EXISTS fk_sys_privileges_feature;

ALTER TABLE sys_privileges
    ADD CONSTRAINT fk_sys_privileges_feature
        FOREIGN KEY (feature_id) REFERENCES sys_features(id);

ALTER TABLE sys_sub_menus
    DROP CONSTRAINT IF EXISTS fk_sys_sub_menus_feature;

ALTER TABLE sys_sub_menus
    ADD CONSTRAINT fk_sys_sub_menus_feature
        FOREIGN KEY (feature_id) REFERENCES sys_features(id);

DROP INDEX IF EXISTS idx_sys_privileges_module_feature;
DROP INDEX IF EXISTS uk_sys_sub_menus_feature_url;

CREATE INDEX IF NOT EXISTS idx_sys_privileges_feature
    ON sys_privileges (feature_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_sub_menus_feature_url
    ON sys_sub_menus (feature_id, url);

CREATE INDEX IF NOT EXISTS idx_sys_submodules_module
    ON sys_submodules (module_id);

CREATE INDEX IF NOT EXISTS idx_sys_features_submodule
    ON sys_features (submodule_id);

ALTER TABLE sys_privileges
    DROP COLUMN IF EXISTS module_code,
    DROP COLUMN IF EXISTS module_name,
    DROP COLUMN IF EXISTS submodule_code,
    DROP COLUMN IF EXISTS submodule_name,
    DROP COLUMN IF EXISTS feature_type_code,
    DROP COLUMN IF EXISTS feature_type_name,
    DROP COLUMN IF EXISTS feature_code,
    DROP COLUMN IF EXISTS feature_name;

ALTER TABLE sys_sub_menus
    DROP COLUMN IF EXISTS module_code,
    DROP COLUMN IF EXISTS module_name,
    DROP COLUMN IF EXISTS submodule_code,
    DROP COLUMN IF EXISTS submodule_name,
    DROP COLUMN IF EXISTS feature_type_code,
    DROP COLUMN IF EXISTS feature_type_name,
    DROP COLUMN IF EXISTS feature_code,
    DROP COLUMN IF EXISTS feature_name;
