UPDATE sys_physical_modules physical_module
SET module_code = mapping.presentation_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('01', 'KYC'),
    ('02', 'AUTH'),
    ('03', 'GIS'),
    ('04', 'SERVICES'),
    ('05', 'APP_CONFIG'),
    ('06', 'COMMON'),
    ('07', 'ESB'),
    ('08', 'GATEWAY'),
    ('09', 'LOG'),
    ('10', 'POS'),
    ('11', 'SYSTEM')
) AS mapping(module_priv_code, presentation_code)
JOIN sys_priv_modules privilege_module
  ON privilege_module.module_priv_code = mapping.module_priv_code
WHERE physical_module.module_name = privilege_module.name
  AND physical_module.module_code = privilege_module.module_priv_code;

UPDATE sys_physical_modules physical_module
SET module_code = 'MODULE_' || privilege_module.module_priv_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_priv_modules privilege_module
WHERE physical_module.module_name = privilege_module.name
  AND physical_module.module_code = privilege_module.module_priv_code;

UPDATE sys_physical_submodules physical_submodule
SET submodule_code = mapping.presentation_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('01', '01', 'KYC_PERSON'),
    ('11', '01', 'SYS_PRIVILEGE'),
    ('11', '02', 'SYSTEM_ACCESS_CONTROL'),
    ('11', '03', 'SYSTEM_LICENSE'),
    ('11', '04', 'SYSTEM_LAYOUT'),
    ('11', '05', 'SYSTEM_WORKFLOW'),
    ('11', '06', 'SYSTEM_BACKUP'),
    ('09', '01', 'LOG_ADMINISTRATION'),
    ('04', '01', 'SERVICE_CACHE'),
    ('04', '02', 'SERVICE_EMAIL'),
    ('04', '03', 'SERVICE_FILE'),
    ('04', '04', 'SERVICE_FIREBASE_AUTH'),
    ('04', '05', 'SERVICE_MESSAGING'),
    ('04', '06', 'SERVICE_QUEUEING'),
    ('04', '07', 'SERVICE_REPORT')
) AS mapping(module_priv_code, submodule_priv_code, presentation_code)
JOIN sys_priv_modules privilege_module
  ON privilege_module.module_priv_code = mapping.module_priv_code
JOIN sys_priv_submodules privilege_submodule
  ON privilege_submodule.module_id = privilege_module.id
 AND privilege_submodule.submodule_priv_code = mapping.submodule_priv_code
JOIN sys_physical_modules physical_module
  ON physical_module.module_code = CASE mapping.module_priv_code
      WHEN '01' THEN 'KYC'
      WHEN '02' THEN 'AUTH'
      WHEN '03' THEN 'GIS'
      WHEN '04' THEN 'SERVICES'
      WHEN '05' THEN 'APP_CONFIG'
      WHEN '06' THEN 'COMMON'
      WHEN '07' THEN 'ESB'
      WHEN '08' THEN 'GATEWAY'
      WHEN '09' THEN 'LOG'
      WHEN '10' THEN 'POS'
      WHEN '11' THEN 'SYSTEM'
  END
WHERE physical_submodule.physical_module_id = physical_module.id
  AND physical_submodule.submodule_name = privilege_submodule.name;

UPDATE sys_physical_submodules physical_submodule
SET submodule_code = 'SUBMODULE_' || privilege_submodule.submodule_priv_code,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_priv_submodules privilege_submodule
JOIN sys_priv_modules privilege_module
  ON privilege_module.id = privilege_submodule.module_id
JOIN sys_physical_modules physical_module
  ON physical_module.module_name = privilege_module.name
WHERE physical_submodule.physical_module_id = physical_module.id
  AND physical_submodule.submodule_name = privilege_submodule.name
  AND physical_submodule.submodule_code = privilege_submodule.submodule_priv_code;
