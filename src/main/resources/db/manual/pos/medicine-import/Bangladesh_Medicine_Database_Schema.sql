-- ============================================================
-- Bangladesh Medicine Database Schema
-- PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS medicine_price CASCADE;
DROP TABLE IF EXISTS medicine CASCADE;
DROP TABLE IF EXISTS generic_indication CASCADE;
DROP TABLE IF EXISTS generic_drug_class CASCADE;
DROP TABLE IF EXISTS generic CASCADE;
DROP TABLE IF EXISTS indication CASCADE;
DROP TABLE IF EXISTS drug_class CASCADE;
DROP TABLE IF EXISTS dosage_form CASCADE;
DROP TABLE IF EXISTS manufacturer CASCADE;

-- ============================================================
-- MANUFACTURER
-- ============================================================

CREATE TABLE manufacturer
(
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) UNIQUE,
    generic_count   INTEGER DEFAULT 0,
    brand_count     INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manufacturer_name
    ON manufacturer(name);

-- ============================================================
-- DOSAGE FORM
-- ============================================================

CREATE TABLE dosage_form
(
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    slug            VARCHAR(255) UNIQUE,
    brand_count     INTEGER DEFAULT 0
);

CREATE INDEX idx_dosage_form_name
    ON dosage_form(name);

-- ============================================================
-- DRUG CLASS
-- ============================================================

CREATE TABLE drug_class
(
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255),
    generic_count   INTEGER DEFAULT 0
);

CREATE INDEX idx_drug_class_name
    ON drug_class(name);

-- ============================================================
-- INDICATION
-- ============================================================

CREATE TABLE indication
(
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(500) NOT NULL,
    slug            VARCHAR(500),
    generic_count   INTEGER DEFAULT 0
);

CREATE INDEX idx_indication_name
    ON indication(name);

-- ============================================================
-- GENERIC
-- ============================================================

CREATE TABLE generic
(
    id                              BIGINT PRIMARY KEY,

    name                            VARCHAR(255) NOT NULL,
    slug                            VARCHAR(255),

    monograph_link                  TEXT,

    therapeutic_class               TEXT,
    pharmacology                    TEXT,
    dosage                          TEXT,
    administration                  TEXT,
    interaction                     TEXT,
    contraindication                TEXT,
    side_effect                     TEXT,
    pregnancy_lactation             TEXT,
    precaution                      TEXT,
    pediatric_usage                 TEXT,
    overdose                        TEXT,
    duration_of_treatment           TEXT,
    reconstitution                  TEXT,
    storage_condition               TEXT,

    description_count               INTEGER DEFAULT 0,

    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_generic_name
    ON generic(name);

-- ============================================================
-- GENERIC <-> DRUG CLASS
-- ============================================================

CREATE TABLE generic_drug_class
(
    generic_id          BIGINT NOT NULL,
    drug_class_id       BIGINT NOT NULL,

    PRIMARY KEY (generic_id, drug_class_id),

    CONSTRAINT fk_gdc_generic
        FOREIGN KEY (generic_id)
            REFERENCES generic(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_gdc_drug_class
        FOREIGN KEY (drug_class_id)
            REFERENCES drug_class(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_gdc_generic
    ON generic_drug_class(generic_id);

CREATE INDEX idx_gdc_drug_class
    ON generic_drug_class(drug_class_id);

-- ============================================================
-- GENERIC <-> INDICATION
-- ============================================================

CREATE TABLE generic_indication
(
    generic_id          BIGINT NOT NULL,
    indication_id       BIGINT NOT NULL,

    PRIMARY KEY (generic_id, indication_id),

    CONSTRAINT fk_gi_generic
        FOREIGN KEY (generic_id)
            REFERENCES generic(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_gi_indication
        FOREIGN KEY (indication_id)
            REFERENCES indication(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_gi_generic
    ON generic_indication(generic_id);

CREATE INDEX idx_gi_indication
    ON generic_indication(indication_id);

-- ============================================================
-- MEDICINE
-- ============================================================

CREATE TABLE medicine
(
    id                      BIGINT PRIMARY KEY,

    brand_name              VARCHAR(255) NOT NULL,

    medicine_type           VARCHAR(50),

    slug                    VARCHAR(255),

    generic_id              BIGINT NOT NULL,

    dosage_form_id          BIGINT,

    manufacturer_id         BIGINT,

    strength                VARCHAR(255),

    package_container       TEXT,

    package_size            TEXT,

    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_medicine_generic
        FOREIGN KEY (generic_id)
            REFERENCES generic(id),

    CONSTRAINT fk_medicine_dosage_form
        FOREIGN KEY (dosage_form_id)
            REFERENCES dosage_form(id),

    CONSTRAINT fk_medicine_manufacturer
        FOREIGN KEY (manufacturer_id)
            REFERENCES manufacturer(id)
);

CREATE INDEX idx_medicine_brand_name
    ON medicine(brand_name);

CREATE INDEX idx_medicine_slug
    ON medicine(slug);

CREATE INDEX idx_medicine_generic
    ON medicine(generic_id);

CREATE INDEX idx_medicine_manufacturer
    ON medicine(manufacturer_id);

CREATE INDEX idx_medicine_dosage_form
    ON medicine(dosage_form_id);

-- ============================================================
-- MEDICINE PRICE
-- One medicine may have multiple package prices
-- ============================================================

CREATE TABLE medicine_price
(
    id                      BIGSERIAL PRIMARY KEY,

    medicine_id             BIGINT NOT NULL,

    package_description     TEXT,

    price                   NUMERIC(10,2),

    currency                VARCHAR(10) DEFAULT 'BDT',

    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_price_medicine
        FOREIGN KEY (medicine_id)
            REFERENCES medicine(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_medicine_price_medicine
    ON medicine_price(medicine_id);

-- ============================================================
-- OPTIONAL FULL TEXT SEARCH
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_medicine_brand_trgm
    ON medicine
    USING gin (brand_name gin_trgm_ops);

CREATE INDEX idx_generic_name_trgm
    ON generic
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_manufacturer_name_trgm
    ON manufacturer
    USING gin (name gin_trgm_ops);

-- ============================================================
-- SAMPLE SEARCHES
-- ============================================================

-- Find all brands of a generic
/*
SELECT
    m.brand_name,
    g.name AS generic,
    mf.name AS manufacturer,
    d.name AS dosage_form,
    m.strength
FROM medicine m
JOIN generic g ON g.id = m.generic_id
JOIN manufacturer mf ON mf.id = m.manufacturer_id
LEFT JOIN dosage_form d ON d.id = m.dosage_form_id
WHERE g.name ILIKE '%Paracetamol%';
*/

-- Find medicines by manufacturer
/*
SELECT *
FROM medicine m
JOIN manufacturer mf
ON mf.id = m.manufacturer_id
WHERE mf.name = 'ACME Laboratories Ltd.';
*/

-- Find medicines by indication
/*
SELECT DISTINCT
    m.brand_name,
    g.name
FROM medicine m
JOIN generic g
    ON g.id = m.generic_id
JOIN generic_indication gi
    ON gi.generic_id = g.id
JOIN indication i
    ON i.id = gi.indication_id
WHERE i.name ILIKE '%Diabetes%';
*/

-- ============================================================
-- END
-- ============================================================