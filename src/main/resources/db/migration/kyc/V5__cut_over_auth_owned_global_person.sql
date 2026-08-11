-- Phase 6: person_id is now an application-level reference to auth_db.auth_persons.
-- Keep kyc_person intact for rollback/reconciliation, but remove physical ownership
-- constraints that force new profiles and evidence through the legacy table.

ALTER TABLE kyc_person_profiles
    ADD COLUMN tenant_email varchar(254),
    ADD COLUMN tenant_mobile varchar(30),
    ADD COLUMN national_id varchar(100);

UPDATE kyc_person_profiles profile
SET tenant_email = legacy.email,
    tenant_mobile = legacy.mobile_number,
    national_id = legacy.national_id
FROM kyc_person legacy
WHERE legacy.id = profile.person_id;

DO $$
DECLARE item record;
BEGIN
    FOR item IN
        SELECT conrelid::regclass AS table_name, conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND confrelid = 'kyc_person'::regclass
          AND conrelid IN (
              'kyc_person_profiles'::regclass,
              'kyc_person_organization_memberships'::regclass,
              'kyc_person_details'::regclass,
              'kyc_person_document'::regclass
          )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', item.table_name, item.conname);
    END LOOP;
END $$;

COMMENT ON COLUMN kyc_person_profiles.person_id IS
    'Application-level reference to auth_db.auth_persons.id';
COMMENT ON COLUMN kyc_person_organization_memberships.person_id IS
    'Application-level reference to auth_db.auth_persons.id';
COMMENT ON COLUMN kyc_person_details.person_id IS
    'Application-level reference to auth_db.auth_persons.id';
COMMENT ON COLUMN kyc_person_document.person_id IS
    'Application-level reference to auth_db.auth_persons.id';
