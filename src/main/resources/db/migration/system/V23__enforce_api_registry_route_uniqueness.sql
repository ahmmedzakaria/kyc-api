DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_priv_api_registry
        WHERE active = true
        GROUP BY upper(http_method), path_pattern
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce API registry route uniqueness: duplicate active method/path entries must be resolved';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_sys_priv_api_registry_active_method_path
    ON sys_priv_api_registry(upper(http_method), path_pattern)
    WHERE active = true;
