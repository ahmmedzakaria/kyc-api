-- Explicit destructive reset: retain the KYC schema and Flyway history, but remove
-- every application record owned by the KYC database.

TRUNCATE TABLE
    kyc_person_document,
    kyc_person_details,
    kyc_person_organization_memberships,
    kyc_person_profiles,
    kyc_record,
    kyc_person
RESTART IDENTITY CASCADE;
