CREATE TABLE sys_physical_modules (
    id bigserial PRIMARY KEY,
    module_code varchar(2) NOT NULL,
    module_name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_physical_modules_module_code UNIQUE (module_code)
);

CREATE TABLE sys_physical_submodules (
    id bigserial PRIMARY KEY,
    physical_module_id bigint NOT NULL,
    submodule_code varchar(2) NOT NULL,
    submodule_name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_physical_submodules_module
        FOREIGN KEY (physical_module_id)
        REFERENCES sys_physical_modules(id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_sys_physical_submodules_module_code
        UNIQUE (physical_module_id, submodule_code)
);

INSERT INTO sys_physical_modules (
    module_code, module_name, active,
    created_by, updated_by, created_at, updated_at
)
SELECT module.code,
       module.name,
       module.active,
       COALESCE(module.created_by, 0),
       COALESCE(module.updated_by, 0),
       COALESCE(module.created_at, CURRENT_TIMESTAMP),
       COALESCE(module.updated_at, CURRENT_TIMESTAMP)
FROM sys_priv_modules module;

INSERT INTO sys_physical_submodules (
    physical_module_id, submodule_code, submodule_name, active,
    created_by, updated_by, created_at, updated_at
)
SELECT physical_module.id,
       submodule.code,
       submodule.name,
       submodule.active,
       COALESCE(submodule.created_by, 0),
       COALESCE(submodule.updated_by, 0),
       COALESCE(submodule.created_at, CURRENT_TIMESTAMP),
       COALESCE(submodule.updated_at, CURRENT_TIMESTAMP)
FROM sys_priv_submodules submodule
JOIN sys_priv_modules module
  ON module.id = submodule.module_id
JOIN sys_physical_modules physical_module
  ON physical_module.module_code = module.code;
