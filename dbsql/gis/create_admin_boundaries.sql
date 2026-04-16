-- =====================================================================
-- Administrative Boundaries Table Creation Script
-- Levels: 0 (Country) → 6 (Village)
-- Requirements: PostgreSQL + PostGIS + pgcrypto
-- =====================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- LEVEL 0 - COUNTRY
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_0 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR,
    country_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 1 - STATE
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_1 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code VARCHAR,
    state_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 2 - DIVISION
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_2 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    division_code VARCHAR,
    division_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    state_id UUID,
    state_code VARCHAR,
    state_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 3 - DISTRICT
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_3 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_code VARCHAR,
    district_name VARCHAR,
    division_id UUID,
    division_code VARCHAR,
    division_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    state_id UUID,
    state_code VARCHAR,
    state_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 4 - SUB-DISTRICT
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_4 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_district_code VARCHAR,
    sub_district_name VARCHAR,
    district_id UUID,
    district_code VARCHAR,
    district_name VARCHAR,
    division_id UUID,
    division_code VARCHAR,
    division_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    state_id UUID,
    state_code VARCHAR,
    state_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 5 - UNION
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_5 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    union_code VARCHAR,
    union_name VARCHAR,
    sub_district_id UUID,
    sub_district_code VARCHAR,
    sub_district_name VARCHAR,
    district_id UUID,
    district_code VARCHAR,
    district_name VARCHAR,
    division_id UUID,
    division_code VARCHAR,
    division_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    state_id UUID,
    state_code VARCHAR,
    state_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- LEVEL 6 - VILLAGE
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.administrative_boundaries_level_6 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    village_code VARCHAR,
    village_name VARCHAR,
    union_id UUID,
    union_code VARCHAR,
    union_name VARCHAR,
    sub_district_id UUID,
    sub_district_code VARCHAR,
    sub_district_name VARCHAR,
    district_id UUID,
    district_code VARCHAR,
    district_name VARCHAR,
    division_id UUID,
    division_code VARCHAR,
    division_name VARCHAR,
    country_id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    state_id UUID,
    state_code VARCHAR,
    state_name VARCHAR,
    area_type VARCHAR,
    source VARCHAR,
    center_point geometry(Point, 4326),
    point geometry(Point, 4326),
    wkb_geometry geometry(Geometry, 4326)
    );

-- =====================================================================
-- INDEXES FOR GEOMETRY PERFORMANCE
-- =====================================================================
DO $$
DECLARE
lvl INT;
BEGIN
FOR lvl IN 0..6 LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_adm%d_geom ON public.administrative_boundaries_level_%d USING GIST (wkb_geometry);', lvl, lvl);
EXECUTE format('CREATE INDEX IF NOT EXISTS idx_adm%d_center ON public.administrative_boundaries_level_%d USING GIST (center_point);', lvl, lvl);
END LOOP;
END $$;

-- =====================================================================
-- END OF SCRIPT
-- =====================================================================
