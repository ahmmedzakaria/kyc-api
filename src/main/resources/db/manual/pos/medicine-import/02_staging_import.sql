-- ============================================================
-- 02_staging_import.sql
-- Import raw CSVs into staging tables
-- ============================================================

DROP SCHEMA IF EXISTS staging CASCADE;
CREATE SCHEMA staging;

-- ============================================================
-- MEDICINE
-- ============================================================

CREATE TABLE staging.medicine_raw
(
    brand_id            BIGINT,
    brand_name          TEXT,
    medicine_type       TEXT,
    slug                TEXT,
    dosage_form         TEXT,
    generic_name        TEXT,
    strength            TEXT,
    manufacturer_name   TEXT,
    package_container   TEXT,
    package_size        TEXT
);

\copy staging.medicine_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/medicine.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8'

-- ============================================================
-- MANUFACTURER
-- ============================================================

CREATE TABLE staging.manufacturer_raw
(
    manufacturer_id     BIGINT,
    manufacturer_name   TEXT,
    slug                TEXT,
    generic_count       INTEGER,
    brand_count         INTEGER
);

\copy staging.manufacturer_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/manufacturer.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- ============================================================
-- DOSAGE FORM
-- ============================================================

CREATE TABLE staging.dosage_form_raw
(
    dosage_form_id      BIGINT,
    dosage_form_name    TEXT,
    slug                TEXT,
    brand_count         INTEGER
);

\copy staging.dosage_form_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/dosage form.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- ============================================================
-- DRUG CLASS
-- ============================================================

CREATE TABLE staging.drug_class_raw
(
    drug_class_id       BIGINT,
    drug_class_name     TEXT,
    slug                TEXT,
    generic_count       INTEGER
);

\copy staging.drug_class_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/drug class.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- ============================================================
-- INDICATION
-- ============================================================

CREATE TABLE staging.indication_raw
(
    indication_id       BIGINT,
    indication_name     TEXT,
    slug                TEXT,
    generic_count       INTEGER
);

\copy staging.indication_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/indication.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- ============================================================
-- GENERIC
-- ============================================================

CREATE TABLE staging.generic_raw
(
    generic_id                     BIGINT,
    generic_name                   TEXT,
    slug                           TEXT,
    monograph_link                 TEXT,
    drug_class                     TEXT,
    indication                     TEXT,
    indication_description         TEXT,
    therapeutic_class_description  TEXT,
    pharmacology_description       TEXT,
    dosage_description             TEXT,
    administration_description     TEXT,
    interaction_description        TEXT,
    contraindications_description  TEXT,
    side_effects_description       TEXT,
    pregnancy_lactation_description TEXT,
    precautions_description        TEXT,
    pediatric_usage_description    TEXT,
    overdose_effects_description   TEXT,
    duration_of_treatment_description TEXT,
    reconstitution_description     TEXT,
    storage_conditions_description TEXT,
    descriptions_count             INTEGER
);

\copy staging.generic_raw FROM '/home/zahmmed/volume2/kyc-project/assorted-medicine-dataset-of-bangladesh/generic.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- ============================================================
-- Verify imports
-- ============================================================

SELECT 'medicine'      AS table_name, COUNT(*) FROM staging.medicine_raw
UNION ALL
SELECT 'manufacturer', COUNT(*) FROM staging.manufacturer_raw
UNION ALL
SELECT 'dosage_form',  COUNT(*) FROM staging.dosage_form_raw
UNION ALL
SELECT 'drug_class',   COUNT(*) FROM staging.drug_class_raw
UNION ALL
SELECT 'indication',   COUNT(*) FROM staging.indication_raw
UNION ALL
SELECT 'generic',      COUNT(*) FROM staging.generic_raw;

-- ============================================================
-- Optional duplicate checks
-- ============================================================

-- Medicine IDs
SELECT brand_id, COUNT(*)
FROM staging.medicine_raw
GROUP BY brand_id
HAVING COUNT(*) > 1;

-- Generic IDs
SELECT generic_id, COUNT(*)
FROM staging.generic_raw
GROUP BY generic_id
HAVING COUNT(*) > 1;

-- Manufacturer IDs
SELECT manufacturer_id, COUNT(*)
FROM staging.manufacturer_raw
GROUP BY manufacturer_id
HAVING COUNT(*) > 1;

-- Dosage Form IDs
SELECT dosage_form_id, COUNT(*)
FROM staging.dosage_form_raw
GROUP BY dosage_form_id
HAVING COUNT(*) > 1;

-- Drug Class IDs
SELECT drug_class_id, COUNT(*)
FROM staging.drug_class_raw
GROUP BY drug_class_id
HAVING COUNT(*) > 1;

-- Indication IDs
SELECT indication_id, COUNT(*)
FROM staging.indication_raw
GROUP BY indication_id
HAVING COUNT(*) > 1;

-- ============================================================
-- End
-- ============================================================