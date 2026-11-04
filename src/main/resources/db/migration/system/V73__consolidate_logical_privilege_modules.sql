-- Promote the logical navigation catalog to the privilege-module catalog while
-- retaining the former privilege modules as migration evidence.

ALTER TABLE sys_priv_modules RENAME TO sys_priv_modules_tobe_deleted;
ALTER TABLE sys_layout_navigation_modules RENAME TO sys_priv_modules;

-- Free the schema-level backing-index name for the promoted table.
ALTER TABLE sys_priv_modules_tobe_deleted
    RENAME CONSTRAINT uk_sys_priv_modules_module_priv_code
    TO uk_sys_priv_modules_tobe_deleted_module_priv_code;

ALTER TABLE sys_priv_modules
    ADD COLUMN module_priv_code varchar(2),
    ADD COLUMN physical_module_id bigint REFERENCES sys_physical_modules(id),
    ADD COLUMN navigation_enabled boolean NOT NULL DEFAULT true;

-- Resolve both legacy presentation codes (LOG) and old numeric privilege codes (09).
UPDATE sys_priv_modules logical_module
SET physical_module_id = physical_module.id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_physical_modules physical_module
JOIN sys_priv_modules_tobe_deleted old_module
  ON old_module.name = physical_module.module_name
WHERE physical_module.module_code = logical_module.physical_module_code
   OR old_module.module_priv_code = logical_module.physical_module_code;

CREATE TEMP TABLE logical_module_selection ON COMMIT DROP AS
SELECT old_module.id AS old_module_id,
       candidate.id AS logical_module_id,
       row_number() OVER (
           PARTITION BY old_module.id
           ORDER BY candidate.active DESC,
                    (left(candidate.navigation_module_code, 5) <> 'PRIV_') DESC,
                    candidate.id
       ) AS preference
FROM sys_priv_modules_tobe_deleted old_module
JOIN sys_physical_modules physical_module
  ON physical_module.module_name = old_module.name
JOIN sys_priv_modules candidate
  ON candidate.physical_module_id = physical_module.id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_priv_modules_tobe_deleted old_module
        WHERE old_module.active
          AND NOT EXISTS (
              SELECT 1 FROM logical_module_selection selection
              WHERE selection.old_module_id = old_module.id
          )
    ) THEN
        RAISE EXCEPTION 'Cannot consolidate privilege modules: an active physical module has no logical module';
    END IF;
END $$;

-- Move categories from duplicate fallback modules to the selected logical module.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_layout_navigation_categories source_category
        JOIN logical_module_selection source_selection
          ON source_selection.logical_module_id = source_category.navigation_module_id
         AND source_selection.preference > 1
        JOIN logical_module_selection target_selection
          ON target_selection.old_module_id = source_selection.old_module_id
         AND target_selection.preference = 1
        JOIN sys_layout_navigation_categories target_category
          ON target_category.navigation_module_id = target_selection.logical_module_id
         AND target_category.feature_type_code IS NOT DISTINCT FROM source_category.feature_type_code
         AND target_category.category_code = source_category.category_code
        JOIN sys_layout_feature_groups source_group
          ON source_group.navigation_category_id = source_category.id
        JOIN sys_layout_feature_groups target_group
          ON target_group.navigation_category_id = target_category.id
         AND target_group.feature_group_code = source_group.feature_group_code
    ) THEN
        RAISE EXCEPTION 'Cannot consolidate logical modules: duplicate category contains conflicting feature groups';
    END IF;
END $$;

UPDATE sys_layout_feature_groups feature_group
SET navigation_category_id = target_category.id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_layout_navigation_categories source_category
JOIN logical_module_selection source_selection
  ON source_selection.logical_module_id = source_category.navigation_module_id
 AND source_selection.preference > 1
JOIN logical_module_selection target_selection
  ON target_selection.old_module_id = source_selection.old_module_id
 AND target_selection.preference = 1
JOIN sys_layout_navigation_categories target_category
  ON target_category.navigation_module_id = target_selection.logical_module_id
 AND target_category.feature_type_code IS NOT DISTINCT FROM source_category.feature_type_code
 AND target_category.category_code = source_category.category_code
WHERE feature_group.navigation_category_id = source_category.id;

UPDATE sys_priv_features feature
SET navigation_category_id = target_category.id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_layout_navigation_categories source_category
JOIN logical_module_selection source_selection
  ON source_selection.logical_module_id = source_category.navigation_module_id
 AND source_selection.preference > 1
JOIN logical_module_selection target_selection
  ON target_selection.old_module_id = source_selection.old_module_id
 AND target_selection.preference = 1
JOIN sys_layout_navigation_categories target_category
  ON target_category.navigation_module_id = target_selection.logical_module_id
 AND target_category.feature_type_code IS NOT DISTINCT FROM source_category.feature_type_code
 AND target_category.category_code = source_category.category_code
WHERE feature.navigation_category_id = source_category.id;

DELETE FROM sys_layout_navigation_categories source_category
USING logical_module_selection source_selection,
      logical_module_selection target_selection,
      sys_layout_navigation_categories target_category
WHERE source_selection.logical_module_id = source_category.navigation_module_id
  AND source_selection.preference > 1
  AND target_selection.old_module_id = source_selection.old_module_id
  AND target_selection.preference = 1
  AND target_category.navigation_module_id = target_selection.logical_module_id
  AND target_category.feature_type_code IS NOT DISTINCT FROM source_category.feature_type_code
  AND target_category.category_code = source_category.category_code;

UPDATE sys_layout_navigation_categories category
SET navigation_module_id = target_selection.logical_module_id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM logical_module_selection source_selection
JOIN logical_module_selection target_selection
  ON target_selection.old_module_id = source_selection.old_module_id
 AND target_selection.preference = 1
WHERE category.navigation_module_id = source_selection.logical_module_id
  AND source_selection.preference > 1;

DELETE FROM sys_priv_modules duplicate_module
USING logical_module_selection selection
WHERE duplicate_module.id = selection.logical_module_id
  AND selection.preference > 1;

UPDATE sys_priv_modules logical_module
SET module_priv_code = old_module.module_priv_code,
    physical_module_id = physical_module.id,
    navigation_enabled = logical_module.active
        AND left(logical_module.navigation_module_code, 5) <> 'PRIV_',
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM logical_module_selection selection
JOIN sys_priv_modules_tobe_deleted old_module
  ON old_module.id = selection.old_module_id
JOIN sys_physical_modules physical_module
  ON physical_module.module_name = old_module.name
WHERE selection.preference = 1
  AND logical_module.id = selection.logical_module_id;

ALTER TABLE sys_priv_modules
    ADD CONSTRAINT uk_sys_priv_modules_module_priv_code UNIQUE (module_priv_code);

-- Reparent existing submodule IDs to logical modules, preserving feature and
-- license references, and add their physical presentation code.
ALTER TABLE sys_priv_submodules
    ADD COLUMN physical_submodule_id bigint REFERENCES sys_physical_submodules(id),
    ADD COLUMN submodule_code varchar(80);

UPDATE sys_priv_submodules logical_submodule
SET physical_submodule_id = physical_submodule.id,
    submodule_code = physical_submodule.submodule_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_priv_modules_tobe_deleted old_module
JOIN sys_physical_modules physical_module
  ON physical_module.module_name = old_module.name
JOIN sys_physical_submodules physical_submodule
  ON physical_submodule.physical_module_id = physical_module.id
WHERE logical_submodule.module_id = old_module.id
  AND physical_submodule.submodule_name = logical_submodule.name;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys_priv_submodules
        WHERE physical_submodule_id IS NULL OR submodule_code IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot consolidate privilege submodules: physical submodule mapping is missing';
    END IF;
END $$;

ALTER TABLE sys_priv_submodules DROP CONSTRAINT sys_submodules_module_id_fkey;

UPDATE sys_priv_submodules logical_submodule
SET module_id = logical_module.id,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_priv_modules_tobe_deleted old_module
JOIN sys_priv_modules logical_module
  ON logical_module.module_priv_code = old_module.module_priv_code
WHERE logical_submodule.module_id = old_module.id;

ALTER TABLE sys_priv_submodules
    ALTER COLUMN submodule_code SET NOT NULL,
    ADD CONSTRAINT fk_sys_priv_submodules_logical_module
        FOREIGN KEY (module_id) REFERENCES sys_priv_modules(id) ON DELETE RESTRICT,
    ADD CONSTRAINT uk_sys_priv_submodules_module_logical_code
        UNIQUE (module_id, submodule_code);

-- Repoint module-level license references before the archived table is retired.
DO $$
DECLARE
    module_fk record;
BEGIN
    FOR module_fk IN
        SELECT constraint_record.conname
        FROM pg_constraint constraint_record
        JOIN pg_class table_record
          ON table_record.oid = constraint_record.conrelid
        JOIN pg_namespace namespace_record
          ON namespace_record.oid = table_record.relnamespace
        WHERE constraint_record.contype = 'f'
          AND namespace_record.nspname = current_schema()
          AND table_record.relname = 'sys_license_plan_entitlements'
          AND constraint_record.conkey = ARRAY[
              (SELECT attribute_record.attnum
               FROM pg_attribute attribute_record
               WHERE attribute_record.attrelid = table_record.oid
                 AND attribute_record.attname = 'module_id'
                 AND NOT attribute_record.attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format(
            'ALTER TABLE sys_license_plan_entitlements DROP CONSTRAINT %I',
            module_fk.conname
        );
    END LOOP;
END $$;
UPDATE sys_license_plan_entitlements entitlement
SET module_id = logical_module.id
FROM sys_priv_modules_tobe_deleted old_module
JOIN sys_priv_modules logical_module
  ON logical_module.module_priv_code = old_module.module_priv_code
WHERE entitlement.module_id = old_module.id;
ALTER TABLE sys_license_plan_entitlements
    ADD CONSTRAINT fk_sys_license_plan_entitlements_logical_module
        FOREIGN KEY (module_id) REFERENCES sys_priv_modules(id);

DO $$
DECLARE
    module_fk record;
BEGIN
    FOR module_fk IN
        SELECT constraint_record.conname
        FROM pg_constraint constraint_record
        JOIN pg_class table_record
          ON table_record.oid = constraint_record.conrelid
        JOIN pg_namespace namespace_record
          ON namespace_record.oid = table_record.relnamespace
        WHERE constraint_record.contype = 'f'
          AND namespace_record.nspname = current_schema()
          AND table_record.relname = 'sys_license_entitlement_overrides'
          AND constraint_record.conkey = ARRAY[
              (SELECT attribute_record.attnum
               FROM pg_attribute attribute_record
               WHERE attribute_record.attrelid = table_record.oid
                 AND attribute_record.attname = 'module_id'
                 AND NOT attribute_record.attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format(
            'ALTER TABLE sys_license_entitlement_overrides DROP CONSTRAINT %I',
            module_fk.conname
        );
    END LOOP;
END $$;
UPDATE sys_license_entitlement_overrides entitlement
SET module_id = logical_module.id
FROM sys_priv_modules_tobe_deleted old_module
JOIN sys_priv_modules logical_module
  ON logical_module.module_priv_code = old_module.module_priv_code
WHERE entitlement.module_id = old_module.id;
ALTER TABLE sys_license_entitlement_overrides
    ADD CONSTRAINT fk_sys_license_entitlement_overrides_logical_module
        FOREIGN KEY (module_id) REFERENCES sys_priv_modules(id);
