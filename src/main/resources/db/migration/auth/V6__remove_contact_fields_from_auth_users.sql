CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_users_username
    ON auth_users (LOWER(username))
    WHERE username IS NOT NULL;

ALTER TABLE auth_users
    DROP COLUMN IF EXISTS email,
    DROP COLUMN IF EXISTS email_verified,
    DROP COLUMN IF EXISTS mobile_number,
    DROP COLUMN IF EXISTS mobile_verified;
