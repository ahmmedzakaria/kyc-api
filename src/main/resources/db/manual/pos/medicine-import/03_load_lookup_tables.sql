-- ============================================================
-- 03_load_lookup_tables.sql
-- Load lookup tables from staging
-- ============================================================

BEGIN;

-- Clear tables (safe because no dependent tables exist yet)
TRUNCATE TABLE
    generic,
    indication,
    drug_class,
    dosage_form,
    manufacturer,
    medicine
RESTART IDENTITY CASCADE;

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
SELECT DISTINCT ON (LOWER(TRIM(manufacturer_name)))
       manufacturer_id,
       TRIM(manufacturer_name),
       NULLIF(TRIM(slug),''),
       COALESCE(generic_count,0),
       COALESCE(brand_count,0)
FROM staging.manufacturer_raw
ORDER BY LOWER(TRIM(manufacturer_name)), manufacturer_id;

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
SELECT DISTINCT ON (LOWER(TRIM(dosage_form_name)))
       dosage_form_id,
       TRIM(dosage_form_name),
       NULLIF(TRIM(slug),''),
       COALESCE(brand_count,0)
FROM staging.dosage_form_raw
ORDER BY LOWER(TRIM(dosage_form_name)), dosage_form_id;

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
SELECT DISTINCT ON (LOWER(TRIM(drug_class_name)))
       drug_class_id,
       TRIM(drug_class_name),
       NULLIF(TRIM(slug),''),
       COALESCE(generic_count,0)
FROM staging.drug_class_raw
ORDER BY LOWER(TRIM(drug_class_name)), drug_class_id;

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
SELECT DISTINCT ON (LOWER(TRIM(indication_name)))
       indication_id,
       TRIM(indication_name),
       NULLIF(TRIM(slug),''),
       COALESCE(generic_count,0)
FROM staging.indication_raw
ORDER BY LOWER(TRIM(indication_name)), indication_id;

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
SELECT DISTINCT ON (LOWER(TRIM(generic_name)))
       generic_id,
       TRIM(generic_name),
       NULLIF(TRIM(slug),''),
       NULLIF(TRIM(monograph_link),''),
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
ORDER BY LOWER(TRIM(generic_name)), generic_id;

-- ============================================================
-- MEDICINE
-- ============================================================

INSERT INTO medicine
(
    id,
    brand_name,
    medicine_type,
    slug,
    generic_id,
    dosage_form_id,
    manufacturer_id,
    strength,
    package_container,
    package_size
)
SELECT DISTINCT ON (m.brand_id)

       m.brand_id,
       TRIM(m.brand_name),
       NULLIF(TRIM(m.medicine_type), ''),
       NULLIF(TRIM(m.slug), ''),

       g.id,
       df.id,
       mf.id,

       NULLIF(TRIM(m.strength), ''),
       NULLIF(TRIM(m.package_container), ''),
       NULLIF(TRIM(m.package_size), '')

FROM staging.medicine_raw m

INNER JOIN generic g
    ON LOWER(TRIM(g.name)) = LOWER(TRIM(m.generic_name))

LEFT JOIN dosage_form df
    ON LOWER(TRIM(df.name)) = LOWER(TRIM(m.dosage_form))

LEFT JOIN manufacturer mf
    ON LOWER(TRIM(mf.name)) = LOWER(TRIM(m.manufacturer_name))

ORDER BY m.brand_id;

COMMIT;

-- ============================================================
-- VERIFY
-- ============================================================

SELECT 'manufacturer' AS table_name, COUNT(*) FROM manufacturer
UNION ALL
SELECT 'dosage_form', COUNT(*) FROM dosage_form
UNION ALL
SELECT 'drug_class', COUNT(*) FROM drug_class
UNION ALL
SELECT 'indication', COUNT(*) FROM indication
UNION ALL
SELECT 'generic', COUNT(*) FROM generic
UNION ALL
SELECT 'medicine', COUNT(*) FROM medicine
ORDER BY table_name;