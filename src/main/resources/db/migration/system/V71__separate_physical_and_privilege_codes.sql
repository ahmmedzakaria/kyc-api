ALTER TABLE sys_physical_modules
    ALTER COLUMN module_code TYPE varchar(80);

ALTER TABLE sys_physical_submodules
    ALTER COLUMN submodule_code TYPE varchar(80);

ALTER TABLE sys_priv_modules
    RENAME COLUMN code TO module_priv_code;

ALTER TABLE sys_priv_submodules
    RENAME COLUMN code TO submodule_priv_code;

ALTER TABLE sys_priv_modules
    RENAME CONSTRAINT sys_modules_code_key TO uk_sys_priv_modules_module_priv_code;

ALTER TABLE sys_priv_submodules
    RENAME CONSTRAINT uk_sys_submodules_module_code TO uk_sys_priv_submodules_module_priv_code;
