-- Enum validity is enforced by the Java domain model. Keep these columns as
-- varchar so enum evolution does not require PostgreSQL CHECK constraint edits.
ALTER TABLE sys_layout_profiles
    DROP CONSTRAINT IF EXISTS sys_layout_profiles_density_check,
    DROP CONSTRAINT IF EXISTS sys_layout_profiles_layout_type_check,
    DROP CONSTRAINT IF EXISTS sys_layout_profiles_navigation_mode_check,
    DROP CONSTRAINT IF EXISTS sys_layout_profiles_theme_mode_check;

-- Map profiles created with the former navigation enum to the two supported
-- navigation presentations.
UPDATE sys_layout_profiles
SET navigation_mode = CASE
    WHEN navigation_mode = 'RAIL' THEN 'MODULE_GROUP_MEGA_PANEL'
    ELSE 'MODULE_LIST'
END
WHERE navigation_mode IN ('SIDEBAR', 'HORIZONTAL', 'RAIL', 'BOTTOM_NAV', 'NONE');
