-- ============================================================
-- 03_load_lookup_tables.sql
-- Load lookup tables from staging
--
-- Normalization rules:
--   - trim leading/trailing whitespace
--   - collapse repeated internal whitespace
--   - compare names case-insensitively for deduplication and joins
--   - use staging import_id values as generated primary keys
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

WITH normalized AS (
    SELECT
        import_id,
        manufacturer_id,
        NULLIF(REGEXP_REPLACE(BTRIM(manufacturer_name), '[[:space:]]+', ' ', 'g'), '') AS name,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        COALESCE(generic_count, 0) AS generic_count,
        COALESCE(brand_count, 0) AS brand_count
    FROM staging.manufacturer_raw
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(name)
               ORDER BY import_id
           ) AS row_number
    FROM normalized
    WHERE name IS NOT NULL
)
INSERT INTO manufacturer
(
    id,
    name,
    slug,
    generic_count,
    brand_count
)
SELECT
    import_id,
    name,
    slug,
    generic_count,
    brand_count
FROM ranked
WHERE row_number = 1;

-- ============================================================
-- DOSAGE FORM
-- ============================================================

WITH normalized AS (
    SELECT
        import_id,
        dosage_form_id,
        NULLIF(REGEXP_REPLACE(BTRIM(dosage_form_name), '[[:space:]]+', ' ', 'g'), '') AS name,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        COALESCE(brand_count, 0) AS brand_count
    FROM staging.dosage_form_raw
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(name)
               ORDER BY import_id
           ) AS row_number
    FROM normalized
    WHERE name IS NOT NULL
)
INSERT INTO dosage_form
(
    id,
    name,
    slug,
    brand_count
)
SELECT
    import_id,
    name,
    slug,
    brand_count
FROM ranked
WHERE row_number = 1;

-- ============================================================
-- DRUG CLASS
-- ============================================================

WITH normalized AS (
    SELECT
        import_id,
        drug_class_id,
        NULLIF(REGEXP_REPLACE(BTRIM(drug_class_name), '[[:space:]]+', ' ', 'g'), '') AS name,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        COALESCE(generic_count, 0) AS generic_count
    FROM staging.drug_class_raw
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(name)
               ORDER BY import_id
           ) AS row_number
    FROM normalized
    WHERE name IS NOT NULL
)
INSERT INTO drug_class
(
    id,
    name,
    slug,
    generic_count
)
SELECT
    import_id,
    name,
    slug,
    generic_count
FROM ranked
WHERE row_number = 1;

-- ============================================================
-- INDICATION
-- ============================================================

WITH normalized AS (
    SELECT
        import_id,
        indication_id,
        NULLIF(REGEXP_REPLACE(BTRIM(indication_name), '[[:space:]]+', ' ', 'g'), '') AS name,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        COALESCE(generic_count, 0) AS generic_count
    FROM staging.indication_raw
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(name)
               ORDER BY import_id
           ) AS row_number
    FROM normalized
    WHERE name IS NOT NULL
)
INSERT INTO indication
(
    id,
    name,
    slug,
    generic_count
)
SELECT
    import_id,
    name,
    slug,
    generic_count
FROM ranked
WHERE row_number = 1;

-- ============================================================
-- GENERIC
-- ============================================================

WITH normalized AS (
    SELECT
        import_id,
        generic_id,
        NULLIF(REGEXP_REPLACE(BTRIM(generic_name), '[[:space:]]+', ' ', 'g'), '') AS name,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        NULLIF(REGEXP_REPLACE(BTRIM(monograph_link), '[[:space:]]+', ' ', 'g'), '') AS monograph_link,
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
        COALESCE(descriptions_count, 0) AS descriptions_count
    FROM staging.generic_raw
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(name)
               ORDER BY import_id
           ) AS row_number
    FROM normalized
    WHERE name IS NOT NULL
)
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
    import_id,
    name,
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
    descriptions_count
FROM ranked
WHERE row_number = 1;

-- ============================================================
-- MEDICINE
-- ============================================================

WITH normalized_medicine AS (
    SELECT
        import_id,
        brand_id,
        NULLIF(REGEXP_REPLACE(BTRIM(brand_name), '[[:space:]]+', ' ', 'g'), '') AS brand_name,
        NULLIF(REGEXP_REPLACE(BTRIM(medicine_type), '[[:space:]]+', ' ', 'g'), '') AS medicine_type,
        NULLIF(REGEXP_REPLACE(BTRIM(slug), '[[:space:]]+', ' ', 'g'), '') AS slug,
        NULLIF(REGEXP_REPLACE(BTRIM(generic_name), '[[:space:]]+', ' ', 'g'), '') AS generic_name,
        NULLIF(REGEXP_REPLACE(BTRIM(dosage_form), '[[:space:]]+', ' ', 'g'), '') AS dosage_form,
        NULLIF(REGEXP_REPLACE(BTRIM(manufacturer_name), '[[:space:]]+', ' ', 'g'), '') AS manufacturer_name,
        NULLIF(REGEXP_REPLACE(BTRIM(strength), '[[:space:]]+', ' ', 'g'), '') AS strength,
        NULLIF(REGEXP_REPLACE(BTRIM(package_container), '[[:space:]]+', ' ', 'g'), '') AS package_container,
        NULLIF(REGEXP_REPLACE(BTRIM(package_size), '[[:space:]]+', ' ', 'g'), '') AS package_size
    FROM staging.medicine_raw
),
identity_deduped AS (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY
                   LOWER(brand_name),
                   LOWER(generic_name),
                   COALESCE(LOWER(dosage_form), ''),
                   COALESCE(LOWER(manufacturer_name), ''),
                   COALESCE(LOWER(strength), ''),
                   COALESCE(LOWER(medicine_type), ''),
                   COALESCE(LOWER(package_container), ''),
                   COALESCE(LOWER(package_size), '')
               ORDER BY import_id
           ) AS identity_row_number
    FROM normalized_medicine
    WHERE brand_name IS NOT NULL
      AND generic_name IS NOT NULL
)
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
SELECT
    m.import_id,
    m.brand_name,
    m.medicine_type,
    m.slug,
    g.id,
    df.id,
    mf.id,
    m.strength,
    m.package_container,
    m.package_size
FROM identity_deduped m
INNER JOIN generic g
    ON LOWER(g.name) = LOWER(m.generic_name)
LEFT JOIN dosage_form df
    ON LOWER(df.name) = LOWER(m.dosage_form)
LEFT JOIN manufacturer mf
    ON LOWER(mf.name) = LOWER(m.manufacturer_name)
WHERE m.identity_row_number = 1;

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

-- Medicine rows skipped because no normalized generic match exists.
WITH normalized_medicine AS (
    SELECT
        NULLIF(REGEXP_REPLACE(BTRIM(generic_name), '[[:space:]]+', ' ', 'g'), '') AS generic_name
    FROM staging.medicine_raw
)
SELECT m.generic_name AS missing_generic_name, COUNT(*) AS raw_row_count
FROM normalized_medicine m
LEFT JOIN generic g
    ON LOWER(g.name) = LOWER(m.generic_name)
WHERE m.generic_name IS NOT NULL
  AND g.id IS NULL
GROUP BY m.generic_name
ORDER BY raw_row_count DESC, missing_generic_name;
