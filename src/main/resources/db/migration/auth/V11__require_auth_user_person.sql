DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM auth_users WHERE person_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot require auth_users.person_id: orphan users must be linked to KYC persons first';
    END IF;
END $$;

ALTER TABLE auth_users ALTER COLUMN person_id SET NOT NULL;
