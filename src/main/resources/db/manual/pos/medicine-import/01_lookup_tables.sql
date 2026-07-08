-- ============================================================
-- 01_lookup_tables.sql
-- Lookup Tables
-- ============================================================

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

CREATE UNIQUE INDEX uq_drug_class_name
    ON drug_class(lower(name));

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

CREATE UNIQUE INDEX uq_indication_name
    ON indication(lower(name));

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

CREATE UNIQUE INDEX uq_generic_name
    ON generic(lower(name));

CREATE INDEX idx_generic_name
    ON generic(name);

CREATE INDEX idx_generic_slug
    ON generic(slug);

-- ============================================================
-- FULL TEXT SEARCH
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_generic_trgm
    ON generic
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_manufacturer_trgm
    ON manufacturer
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_drug_class_trgm
    ON drug_class
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_indication_trgm
    ON indication
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_dosage_form_trgm
    ON dosage_form
    USING gin (name gin_trgm_ops);