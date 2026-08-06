-- Only the Person feature is currently implemented in the KYC module.
-- Retire legacy KYC Record (feature 002) and KYC Report (feature 003)
-- entries without deleting historical privilege assignments.

UPDATE sys_priv_privileges privilege
SET active = false,
    updated_at = now()
FROM sys_priv_features feature
JOIN sys_priv_submodules submodule ON submodule.id = feature.submodule_id
JOIN sys_priv_modules module ON module.id = submodule.module_id
WHERE privilege.feature_id = feature.id
  AND module.code = '01'
  AND submodule.code = '01'
  AND feature.feature_code IN ('002', '003');

UPDATE sys_priv_sub_menus menu
SET active = false,
    updated_by = 0,
    updated_at = now()
FROM sys_priv_features feature
JOIN sys_priv_submodules submodule ON submodule.id = feature.submodule_id
JOIN sys_priv_modules module ON module.id = submodule.module_id
WHERE menu.feature_id = feature.id
  AND module.code = '01'
  AND submodule.code = '01'
  AND feature.feature_code IN ('002', '003');

UPDATE sys_priv_features feature
SET active = false,
    updated_by = 0,
    updated_at = now()
FROM sys_priv_submodules submodule
JOIN sys_priv_modules module ON module.id = submodule.module_id
WHERE feature.submodule_id = submodule.id
  AND module.code = '01'
  AND submodule.code = '01'
  AND feature.feature_code IN ('002', '003');

UPDATE sys_layout_features
SET active = false,
    updated_by = 0,
    updated_at = now()
WHERE physical_module_code = '01'
  AND physical_submodule_code = '01'
  AND (
      physical_feature_code IN ('002', '003')
      OR lower(feature_name) IN ('kyc record', 'kyc records', 'kyc report', 'kyc reports')
      OR route = '/kyc'
  );
