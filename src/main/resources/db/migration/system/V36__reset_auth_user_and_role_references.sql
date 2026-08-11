-- Auth user and role IDs are reset in auth/V16. Remove cross-database mappings
-- before the replacement bootstrap role receives its new assignments.

TRUNCATE TABLE
    sys_priv_user_privileges,
    sys_priv_role_privileges;

UPDATE sys_workflow_assignment_policies
SET user_id = NULL,
    role_id = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id IS NOT NULL OR role_id IS NOT NULL;

UPDATE sys_workflow_tasks
SET assigned_user_id = NULL,
    assigned_role_id = NULL,
    claimed_by_user_id = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE assigned_user_id IS NOT NULL
   OR assigned_role_id IS NOT NULL
   OR claimed_by_user_id IS NOT NULL;

UPDATE sys_workflow_instances
SET requester_user_id = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE requester_user_id IS NOT NULL;

UPDATE sys_workflow_history
SET actor_user_id = NULL,
    actor_username = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE actor_user_id IS NOT NULL OR actor_username IS NOT NULL;

DO $$
DECLARE table_record record;
BEGIN
    FOR table_record IN
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name LIKE 'sys\_%' ESCAPE '\'
          AND column_name IN ('created_by', 'updated_by')
        GROUP BY table_name
        HAVING count(DISTINCT column_name) = 2
    LOOP
        EXECUTE format(
            'UPDATE %I SET created_by = 0, updated_by = 0 WHERE created_by <> 0 OR updated_by <> 0',
            table_record.table_name
        );
    END LOOP;
END $$;
