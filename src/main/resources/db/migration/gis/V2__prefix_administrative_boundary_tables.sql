DO $$
DECLARE
    boundary_level integer;
    old_table text;
    new_table text;
BEGIN
    FOR boundary_level IN 0..6 LOOP
        old_table := format('administrative_boundaries_level_%s', boundary_level);
        new_table := format('gis_administrative_boundaries_level_%s', boundary_level);

        IF to_regclass(format('public.%I', old_table)) IS NOT NULL
           AND to_regclass(format('public.%I', new_table)) IS NOT NULL THEN
            RAISE EXCEPTION
                'Cannot rename %.%: target %.% already exists',
                'public', old_table, 'public', new_table;
        END IF;

        IF to_regclass(format('public.%I', old_table)) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE public.%I RENAME TO %I',
                old_table,
                new_table
            );
        END IF;
    END LOOP;
END $$;
