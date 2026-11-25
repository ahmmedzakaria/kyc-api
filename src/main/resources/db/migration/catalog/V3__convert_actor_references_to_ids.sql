-- Actor audit fields are application-level references to auth_db user IDs.
-- Actor 0 is the reserved system actor used by trusted migrations and jobs.
-- Cross-database foreign keys are intentionally not created.

DO $$
DECLARE
    actor_column RECORD;
    invalid_value TEXT;
BEGIN
    FOR actor_column IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name LIKE 'catalog\_%' ESCAPE '\'
          AND column_name IN ('created_by', 'updated_by', 'published_by')
          AND data_type IN ('character varying', 'text')
        ORDER BY table_name, ordinal_position
    LOOP
        -- V2 is the only trusted legacy descriptive marker. Do not infer IDs
        -- from usernames, display names, email addresses, or other strings.
        EXECUTE format(
            'UPDATE %I SET %I = ''0'' WHERE %I = ''migration:v2''',
            actor_column.table_name,
            actor_column.column_name,
            actor_column.column_name
        );

        EXECUTE format(
            'SELECT %I FROM %I WHERE %I IS NOT NULL AND btrim(%I) !~ ''^[0-9]+$'' LIMIT 1',
            actor_column.column_name,
            actor_column.table_name,
            actor_column.column_name,
            actor_column.column_name
        ) INTO invalid_value;

        IF invalid_value IS NOT NULL THEN
            RAISE EXCEPTION
                'Cannot convert %.% to an actor ID: unresolved value %',
                actor_column.table_name,
                actor_column.column_name,
                quote_literal(invalid_value);
        END IF;

        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE BIGINT USING btrim(%I)::BIGINT',
            actor_column.table_name,
            actor_column.column_name,
            actor_column.column_name
        );

        EXECUTE format(
            'COMMENT ON COLUMN %I.%I IS ''Numeric Auth user ID; 0 is the reserved system actor''',
            actor_column.table_name,
            actor_column.column_name
        );
    END LOOP;
END
$$;
