CREATE TABLE IF NOT EXISTS sys_layout_profiles (
    id bigserial PRIMARY KEY,
    profile_code varchar(80) NOT NULL UNIQUE,
    profile_name varchar(150) NOT NULL,
    description text,
    layout_type varchar(40) NOT NULL,
    navigation_mode varchar(40) NOT NULL,
    theme_mode varchar(40) NOT NULL,
    density varchar(40) NOT NULL,
    topbar_enabled boolean NOT NULL DEFAULT true,
    sidebar_enabled boolean NOT NULL DEFAULT true,
    sidebar_collapsed boolean NOT NULL DEFAULT false,
    footer_enabled boolean NOT NULL DEFAULT true,
    breadcrumb_enabled boolean NOT NULL DEFAULT true,
    command_bar_enabled boolean NOT NULL DEFAULT true,
    rtl_enabled boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_profile_themes (
    id bigserial PRIMARY KEY,
    layout_profile_id bigint NOT NULL REFERENCES sys_layout_profiles(id),
    theme_id varchar(80) NOT NULL,
    theme_label varchar(150) NOT NULL,
    base varchar(20) NOT NULL,
    swatch varchar(40) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_profile_themes_profile_theme
    ON sys_layout_profile_themes(layout_profile_id, theme_id);

CREATE TABLE IF NOT EXISTS sys_layout_theme_primaries (
    id bigserial PRIMARY KEY,
    layout_profile_theme_id bigint NOT NULL UNIQUE REFERENCES sys_layout_profile_themes(id),
    text_color varchar(40) NOT NULL,
    paper_color varchar(40) NOT NULL,
    card_color varchar(40) NOT NULL,
    accent_color varchar(40) NOT NULL,
    amber_color varchar(40) NOT NULL,
    red_color varchar(40) NOT NULL,
    success_color varchar(40) NOT NULL,
    info_color varchar(40) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_theme_chrome_overrides (
    id bigserial PRIMARY KEY,
    layout_profile_theme_id bigint NOT NULL UNIQUE REFERENCES sys_layout_profile_themes(id),
    accent_soft varchar(40),
    background varchar(40),
    border varchar(40),
    border_strong varchar(40),
    hover_background varchar(40),
    active_background varchar(40),
    search_background varchar(40),
    search_border varchar(40),
    search_text varchar(40),
    search_placeholder varchar(40),
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_profile_sizes (
    id bigserial PRIMARY KEY,
    layout_profile_id bigint NOT NULL UNIQUE REFERENCES sys_layout_profiles(id),
    space_unit numeric(8,2) NOT NULL,
    radius_base numeric(8,2) NOT NULL,
    font_size_base numeric(8,2) NOT NULL,
    header_height numeric(8,2) NOT NULL,
    status_bar_height numeric(8,2) NOT NULL,
    rail_width_collapsed numeric(8,2) NOT NULL,
    rail_width_expanded numeric(8,2) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_profile_fonts (
    id bigserial PRIMARY KEY,
    layout_profile_id bigint NOT NULL UNIQUE REFERENCES sys_layout_profiles(id),
    body_family varchar(120) NOT NULL,
    heading_family varchar(120),
    mono_family varchar(120),
    font_source varchar(30) NOT NULL,
    font_url varchar(255),
    fallback_stack varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_profile_branding (
    id bigserial PRIMARY KEY,
    layout_profile_id bigint NOT NULL UNIQUE REFERENCES sys_layout_profiles(id),
    display_name varchar(150) NOT NULL,
    short_name varchar(80),
    logo_url varchar(255),
    logo_dark_url varchar(255),
    favicon_url varchar(255),
    support_url varchar(255),
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_client_layout_profiles (
    id bigserial PRIMARY KEY,
    client_application_id bigint NOT NULL REFERENCES sys_client_applications(id),
    layout_profile_id bigint NOT NULL REFERENCES sys_layout_profiles(id),
    assignment_scope varchar(30) NOT NULL DEFAULT 'CLIENT',
    role_code varchar(80),
    privilege_code varchar(80),
    device_target varchar(30) NOT NULL DEFAULT 'ANY',
    module_code varchar(80),
    default_profile boolean NOT NULL DEFAULT false,
    selectable boolean NOT NULL DEFAULT true,
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_client_layout_profiles_one_default
    ON sys_client_layout_profiles(client_application_id)
    WHERE default_profile = true AND active = true;

CREATE TABLE IF NOT EXISTS sys_layout_module_groups (
    id bigserial PRIMARY KEY,
    group_code varchar(80) NOT NULL UNIQUE,
    group_name varchar(150) NOT NULL,
    icon varchar(80),
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_navigation_modules (
    id bigserial PRIMARY KEY,
    module_group_id bigint NOT NULL REFERENCES sys_layout_module_groups(id),
    navigation_module_code varchar(80) NOT NULL UNIQUE,
    navigation_module_name varchar(150) NOT NULL,
    physical_module_code varchar(80),
    icon varchar(80),
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS sys_layout_navigation_categories (
    id bigserial PRIMARY KEY,
    navigation_module_id bigint NOT NULL REFERENCES sys_layout_navigation_modules(id),
    category_code varchar(80) NOT NULL,
    category_name varchar(150) NOT NULL,
    category_kind varchar(80),
    icon varchar(80),
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_navigation_categories_module_code
    ON sys_layout_navigation_categories(navigation_module_id, category_code);

CREATE TABLE IF NOT EXISTS sys_layout_feature_groups (
    id bigserial PRIMARY KEY,
    navigation_category_id bigint NOT NULL REFERENCES sys_layout_navigation_categories(id),
    feature_group_code varchar(80) NOT NULL,
    feature_group_name varchar(150) NOT NULL,
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_feature_groups_category_code
    ON sys_layout_feature_groups(navigation_category_id, feature_group_code);

CREATE TABLE IF NOT EXISTS sys_layout_features (
    id bigserial PRIMARY KEY,
    feature_group_id bigint NOT NULL REFERENCES sys_layout_feature_groups(id),
    feature_code varchar(80) NOT NULL,
    t_code varchar(30),
    feature_name varchar(150) NOT NULL,
    route varchar(255) NOT NULL,
    icon varchar(80),
    physical_module_code varchar(80),
    physical_submodule_code varchar(80),
    physical_feature_type_code varchar(80),
    physical_feature_code varchar(80),
    display_order integer NOT NULL DEFAULT 100,
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_features_group_code
    ON sys_layout_features(feature_group_id, feature_code);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_features_t_code_active
    ON sys_layout_features(t_code)
    WHERE t_code IS NOT NULL AND active = true;

CREATE TABLE IF NOT EXISTS sys_layout_feature_privileges (
    id bigserial PRIMARY KEY,
    layout_feature_id bigint NOT NULL REFERENCES sys_layout_features(id),
    privilege_id bigint NOT NULL REFERENCES sys_privileges(id),
    match_mode varchar(20) NOT NULL DEFAULT 'ANY',
    active boolean NOT NULL DEFAULT true,
    created_by bigint,
    updated_by bigint,
    created_at timestamp,
    updated_at timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_layout_feature_privileges_feature_privilege
    ON sys_layout_feature_privileges(layout_feature_id, privilege_id);

INSERT INTO sys_layout_profiles (
    profile_code, profile_name, description, layout_type, navigation_mode, theme_mode, density,
    topbar_enabled, sidebar_enabled, sidebar_collapsed, footer_enabled, breadcrumb_enabled,
    command_bar_enabled, rtl_enabled, active, created_by, updated_by, created_at, updated_at
)
SELECT 'WEB_DEFAULT', 'Default', 'Default authenticated web layout', 'RAIL', 'RAIL', 'LIGHT', 'COMFORTABLE',
       true, true, false, true, true, true, false, true, 0, 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sys_layout_profiles WHERE profile_code = 'WEB_DEFAULT');

INSERT INTO sys_layout_profile_themes (
    layout_profile_id, theme_id, theme_label, base, swatch, active, created_by, updated_by, created_at, updated_at
)
SELECT p.id, 'purple', 'Purple Corporate', 'light', '#6b3fa0', true, 0, 0, now(), now()
FROM sys_layout_profiles p
WHERE p.profile_code = 'WEB_DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_profile_themes t
      WHERE t.layout_profile_id = p.id AND t.theme_id = 'purple'
  );

INSERT INTO sys_layout_theme_primaries (
    layout_profile_theme_id, text_color, paper_color, card_color, accent_color,
    amber_color, red_color, success_color, info_color, active, created_by, updated_by, created_at, updated_at
)
SELECT t.id, '#1a222c', '#f3f5f7', '#ffffff', '#6b3fa0',
       '#a8630b', '#9f2b2b', '#1c7a4c', '#2f7dd1', true, 0, 0, now(), now()
FROM sys_layout_profile_themes t
JOIN sys_layout_profiles p ON p.id = t.layout_profile_id
WHERE p.profile_code = 'WEB_DEFAULT'
  AND t.theme_id = 'purple'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_theme_primaries tp WHERE tp.layout_profile_theme_id = t.id
  );

INSERT INTO sys_layout_profile_sizes (
    layout_profile_id, space_unit, radius_base, font_size_base, header_height,
    status_bar_height, rail_width_collapsed, rail_width_expanded, active,
    created_by, updated_by, created_at, updated_at
)
SELECT p.id, 2, 8, 13.5, 58, 28, 64, 230, true, 0, 0, now(), now()
FROM sys_layout_profiles p
WHERE p.profile_code = 'WEB_DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_profile_sizes s WHERE s.layout_profile_id = p.id);

INSERT INTO sys_layout_profile_fonts (
    layout_profile_id, body_family, heading_family, mono_family, font_source,
    fallback_stack, active, created_by, updated_by, created_at, updated_at
)
SELECT p.id, 'Inter', 'Inter', 'JetBrains Mono', 'SYSTEM',
       'system-ui, -apple-system, BlinkMacSystemFont, ''Segoe UI'', sans-serif',
       true, 0, 0, now(), now()
FROM sys_layout_profiles p
WHERE p.profile_code = 'WEB_DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_profile_fonts f WHERE f.layout_profile_id = p.id);

INSERT INTO sys_layout_profile_branding (
    layout_profile_id, display_name, short_name, logo_url, favicon_url,
    active, created_by, updated_by, created_at, updated_at
)
SELECT p.id, 'NexaCore KYC', 'KYC', '/assets/brand/nexacore.svg', '/assets/brand/favicon.ico',
       true, 0, 0, now(), now()
FROM sys_layout_profiles p
WHERE p.profile_code = 'WEB_DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_profile_branding b WHERE b.layout_profile_id = p.id);

INSERT INTO sys_client_layout_profiles (
    client_application_id, layout_profile_id, assignment_scope, device_target,
    default_profile, selectable, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT c.id, p.id, 'CLIENT', 'ANY', true, true, 100, true, 0, 0, now(), now()
FROM sys_client_applications c
JOIN sys_layout_profiles p ON p.profile_code = 'WEB_DEFAULT'
WHERE c.client_code = 'WEB'
  AND NOT EXISTS (
      SELECT 1 FROM sys_client_layout_profiles clp
      WHERE clp.client_application_id = c.id
        AND clp.default_profile = true
        AND clp.active = true
  );

INSERT INTO sys_layout_module_groups (
    group_code, group_name, icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT 'COMPLIANCE', 'Compliance', 'shield-check', 10, true, 0, 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sys_layout_module_groups WHERE group_code = 'COMPLIANCE');

INSERT INTO sys_layout_navigation_modules (
    module_group_id, navigation_module_code, navigation_module_name, physical_module_code,
    icon, display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT g.id, 'KYC', 'KYC', 'KYC', 'id-card', 10, true, 0, 0, now(), now()
FROM sys_layout_module_groups g
WHERE g.group_code = 'COMPLIANCE'
  AND NOT EXISTS (SELECT 1 FROM sys_layout_navigation_modules WHERE navigation_module_code = 'KYC');

INSERT INTO sys_layout_navigation_categories (
    navigation_module_id, category_code, category_name, category_kind, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT m.id, seed.code, seed.name, seed.kind, seed.icon, seed.sort, true, 0, 0, now(), now()
FROM sys_layout_navigation_modules m
CROSS JOIN (
    VALUES
      ('OPERATION', 'Operation', 'WORK', 'bolt', 10),
      ('SETUP', 'Setup', 'CONFIGURATION', 'gear', 20),
      ('REPORT', 'Report', 'REPORTING', 'bar-chart', 30)
) AS seed(code, name, kind, icon, sort)
WHERE m.navigation_module_code = 'KYC'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_navigation_categories c
      WHERE c.navigation_module_id = m.id AND c.category_code = seed.code
  );
