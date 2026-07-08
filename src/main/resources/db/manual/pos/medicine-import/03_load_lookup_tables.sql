-- ============================================================
-- 03_load_lookup_tables.sql
-- Populate lookup/master tables from staging
-- ============================================================

BEGIN;

-- ============================================================
-- MANUFACTURER
-- ============================================================

INSERT INTO manufacturer
(
    id,
    name,
    slug,
    generic_count,
    brand_count
)
SELECT
    manufacturer_id,
    TRIM(manufacturer_name),
    slug,
    COALESCE(generic_count,0),
    COALESCE(brand_count,0)
FROM staging.manufacturer_raw
    ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- DOSAGE FORM
-- ============================================================

INSERT INTO dosage_form
(
    id,
    name,
    slug,
    brand_count
)
SELECT
    dosage_form_id,
    TRIM(dosage_form_name),
    slug,
    COALESCE(brand_count,0)
FROM staging.dosage_form_raw
    ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- DRUG CLASS
-- ============================================================

INSERT INTO drug_class
(
    id,
    name,
    slug,
    generic_count
)
SELECT
    drug_class_id,
    TRIM(drug_class_name),
    slug,
    COALESCE(generic_count,0)
FROM staging.drug_class_raw
    ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- INDICATION
-- ============================================================

INSERT INTO indication
(
    id,
    name,
    slug,
    generic_count
)
SELECT
    indication_id,
    TRIM(indication_name),
    slug,
    COALESCE(generic_count,0)
FROM staging.indication_raw
    ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- GENERIC
-- ============================================================

INSERT INTO generic
(
    id,
    name,
    slug,
    monograph_link,
    therapeutic_class,
    pharmacology,
    dosage,
    administration,
    interaction,
    contraindication,
    side_effect,
    pregnancy_lactation,
    precaution,
    pediatric_usage,
    overdose,
    duration_of_treatment,
    reconstitution,
    storage_condition,
    description_count
)
SELECT
    generic_id,
    TRIM(generic_name),
    slug,
    monograph_link,
    therapeutic_class_description,
    pharmacology_description,
    dosage_description,
    administration_description,
    interaction_description,
    contraindications_description,
    side_effects_description,
    pregnancy_lactation_description,
    precautions_description,
    pediatric_usage_description,
    overdose_effects_description,
    duration_of_treatment_description,
    reconstitution_description,
    storage_conditions_description,
    COALESCE(descriptions_count,0)
FROM staging.generic_raw
    ON CONFLICT (id) DO NOTHING;

COMMIT;

-- ============================================================
-- VERIFY
-- ============================================================

SELECT 'manufacturer', COUNT(*) FROM manufacturer
UNION ALL
SELECT 'dosage_form', COUNT(*) FROM dosage_form
UNION ALL
SELECT 'drug_class', COUNT(*) FROM drug_class
UNION ALL
SELECT 'indication', COUNT(*) FROM indication
UNION ALL
SELECT 'generic', COUNT(*) FROM generic;