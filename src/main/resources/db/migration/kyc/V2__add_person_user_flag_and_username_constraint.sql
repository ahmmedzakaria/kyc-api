ALTER TABLE kyc_person
    ADD COLUMN IF NOT EXISTS is_user BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE kyc_person
    ALTER COLUMN username DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_kyc_person_username
    ON kyc_person (LOWER(username))
    WHERE username IS NOT NULL;
