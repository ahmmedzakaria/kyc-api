ALTER TABLE sys_layout_navigation_categories
    ADD COLUMN feature_type_code varchar(2);

COMMENT ON COLUMN sys_layout_navigation_categories.feature_type_code IS
    'Privilege catalog feature type code represented by this navigation category';

UPDATE sys_layout_navigation_categories
SET feature_type_code = CASE
    WHEN category_code = 'SETUP' OR category_kind = 'CONFIGURATION' THEN '01'
    WHEN category_code IN ('OPERATION', 'OPERATIONS') OR category_kind = 'WORK' THEN '02'
    WHEN category_code = 'REPORT' OR category_kind = 'REPORTING' THEN '03'
    ELSE NULL
END;

ALTER TABLE sys_layout_navigation_categories
    ADD CONSTRAINT ck_sys_layout_navigation_categories_feature_type_code
        CHECK (feature_type_code IS NULL OR feature_type_code ~ '^[0-9]{2}$');

CREATE INDEX idx_sys_layout_navigation_categories_feature_type_code
    ON sys_layout_navigation_categories(feature_type_code)
    WHERE feature_type_code IS NOT NULL;
