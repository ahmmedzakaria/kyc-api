ALTER TABLE auth_users
    ADD COLUMN IF NOT EXISTS person_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_users_person_id
    ON auth_users (person_id)
    WHERE person_id IS NOT NULL;
