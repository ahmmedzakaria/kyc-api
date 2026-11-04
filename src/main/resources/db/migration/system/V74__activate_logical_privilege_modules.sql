-- V73 promoted layout navigation modules into the logical privilege catalog.
-- Its non-navigation fallback rows were intentionally hidden, but inherited
-- active=false from their layout seed. Logical privilege activation must
-- instead follow the archived privilege catalog independently of navigation.
UPDATE sys_priv_modules logical_module
SET active = archived_module.active,
    navigation_enabled = archived_module.active
        AND left(logical_module.navigation_module_code, 5) <> 'PRIV_',
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_priv_modules_tobe_deleted archived_module
WHERE logical_module.module_priv_code = archived_module.module_priv_code
  AND (logical_module.active IS DISTINCT FROM archived_module.active
       OR logical_module.navigation_enabled IS DISTINCT FROM (
           archived_module.active
           AND left(logical_module.navigation_module_code, 5) <> 'PRIV_'
       ));

