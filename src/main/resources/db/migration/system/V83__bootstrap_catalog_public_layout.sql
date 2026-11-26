-- Reproduce the approved public Catalog layout template. Tenant/client assignment
-- is created when both tenant 7 and the CATALOG client have already been provisioned.

INSERT INTO sys_layout_profiles (
    profile_code, profile_name, description, layout_type, navigation_mode,
    theme_mode, density, topbar_enabled, sidebar_enabled, sidebar_collapsed,
    footer_enabled, breadcrumb_enabled, command_bar_enabled, rtl_enabled, active,
    created_by, updated_by, created_at, updated_at
)
VALUES (
    'WEB_CATALOG_LAYOUT', 'Web Catalog Layout', NULL, 'PUBLIC', 'MODULE_LIST',
    'LIGHT', 'COMFORTABLE', true, false, false,
    true, true, false, false, true,
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (profile_code) DO UPDATE
SET profile_name = EXCLUDED.profile_name,
    description = EXCLUDED.description,
    layout_type = EXCLUDED.layout_type,
    navigation_mode = EXCLUDED.navigation_mode,
    theme_mode = EXCLUDED.theme_mode,
    density = EXCLUDED.density,
    topbar_enabled = EXCLUDED.topbar_enabled,
    sidebar_enabled = EXCLUDED.sidebar_enabled,
    sidebar_collapsed = EXCLUDED.sidebar_collapsed,
    footer_enabled = EXCLUDED.footer_enabled,
    breadcrumb_enabled = EXCLUDED.breadcrumb_enabled,
    command_bar_enabled = EXCLUDED.command_bar_enabled,
    rtl_enabled = EXCLUDED.rtl_enabled,
    active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_profile_themes (
    layout_profile_id, theme_id, theme_label, base, swatch, active,
    created_by, updated_by, created_at, updated_at
)
SELECT profile.id, 'purple', 'Purple Corporate', 'light', '#6b3fa0', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_profiles profile
WHERE profile.profile_code = 'WEB_CATALOG_LAYOUT'
ON CONFLICT (layout_profile_id, theme_id) DO UPDATE
SET theme_label = EXCLUDED.theme_label,
    base = EXCLUDED.base,
    swatch = EXCLUDED.swatch,
    active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_theme_primaries (
    layout_profile_theme_id, text_color, paper_color, card_color, accent_color,
    amber_color, red_color, success_color, info_color, active,
    created_by, updated_by, created_at, updated_at
)
SELECT theme.id, '#1a222c', '#f3f5f7', '#ffffff', '#6b3fa0',
       '#a8630b', '#9f2b2b', '#1c7a4c', '#2f7dd1', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_profile_themes theme
JOIN sys_layout_profiles profile ON profile.id = theme.layout_profile_id
WHERE profile.profile_code = 'WEB_CATALOG_LAYOUT'
  AND theme.theme_id = 'purple'
ON CONFLICT (layout_profile_theme_id) DO UPDATE
SET text_color = EXCLUDED.text_color,
    paper_color = EXCLUDED.paper_color,
    card_color = EXCLUDED.card_color,
    accent_color = EXCLUDED.accent_color,
    amber_color = EXCLUDED.amber_color,
    red_color = EXCLUDED.red_color,
    success_color = EXCLUDED.success_color,
    info_color = EXCLUDED.info_color,
    active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_profile_sizes (
    layout_profile_id, space_unit, radius_base, font_size_base, header_height,
    status_bar_height, rail_width_collapsed, rail_width_expanded, active,
    created_by, updated_by, created_at, updated_at
)
SELECT profile.id, 2, 8, 13.5, 58, 28, 64, 230, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_profiles profile
WHERE profile.profile_code = 'WEB_CATALOG_LAYOUT'
ON CONFLICT (layout_profile_id) DO UPDATE
SET space_unit = EXCLUDED.space_unit,
    radius_base = EXCLUDED.radius_base,
    font_size_base = EXCLUDED.font_size_base,
    header_height = EXCLUDED.header_height,
    status_bar_height = EXCLUDED.status_bar_height,
    rail_width_collapsed = EXCLUDED.rail_width_collapsed,
    rail_width_expanded = EXCLUDED.rail_width_expanded,
    active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_layout_profile_fonts (
    layout_profile_id, body_family, heading_family, mono_family, font_source,
    font_url, fallback_stack, active,
    created_by, updated_by, created_at, updated_at
)
SELECT profile.id, 'Inter', 'Inter', 'JetBrains Mono', 'SYSTEM', NULL,
       'system-ui, -apple-system, BlinkMacSystemFont, ''Segoe UI'', sans-serif', true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_layout_profiles profile
WHERE profile.profile_code = 'WEB_CATALOG_LAYOUT'
ON CONFLICT (layout_profile_id) DO UPDATE
SET body_family = EXCLUDED.body_family,
    heading_family = EXCLUDED.heading_family,
    mono_family = EXCLUDED.mono_family,
    font_source = EXCLUDED.font_source,
    font_url = EXCLUDED.font_url,
    fallback_stack = EXCLUDED.fallback_stack,
    active = EXCLUDED.active,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP;

-- A tenant/client mapping is a prerequisite for resolving a client layout.
INSERT INTO sys_acc_client_application_tenants (
    client_application_id, tenant_id, business_id, branch_id, active,
    created_by, updated_by, created_at, updated_at
)
SELECT client.id, tenant.id, NULL, NULL, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_tenants tenant ON tenant.id = 7
WHERE client.client_code = 'CATALOG'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_acc_client_application_tenants mapping
      WHERE mapping.client_application_id = client.id
        AND mapping.tenant_id = tenant.id
        AND mapping.business_id IS NULL
        AND mapping.branch_id IS NULL
  );

UPDATE sys_acc_client_application_tenants mapping
SET active = true,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
WHERE mapping.client_application_id = client.id
  AND client.client_code = 'CATALOG'
  AND mapping.tenant_id = 7
  AND mapping.business_id IS NULL
  AND mapping.branch_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_client_layout_profiles assignment
        JOIN sys_acc_client_applications client
          ON client.id = assignment.client_application_id
        JOIN sys_layout_profiles profile
          ON profile.id = assignment.layout_profile_id
        WHERE assignment.tenant_id = 7
          AND client.client_code = 'CATALOG'
          AND assignment.default_profile
          AND assignment.active
          AND profile.profile_code <> 'WEB_CATALOG_LAYOUT'
    ) THEN
        RAISE EXCEPTION
            'Tenant 7 CATALOG already has a different active default layout profile';
    END IF;

    IF (
        SELECT count(*)
        FROM sys_client_layout_profiles assignment
        JOIN sys_acc_client_applications client
          ON client.id = assignment.client_application_id
        JOIN sys_layout_profiles profile
          ON profile.id = assignment.layout_profile_id
        WHERE assignment.tenant_id = 7
          AND client.client_code = 'CATALOG'
          AND profile.profile_code = 'WEB_CATALOG_LAYOUT'
          AND assignment.assignment_scope = 'CLIENT'
          AND assignment.device_target = 'ANY'
          AND assignment.role_code IS NULL
          AND assignment.privilege_code IS NULL
          AND assignment.module_code IS NULL
    ) > 1 THEN
        RAISE EXCEPTION
            'Tenant 7 CATALOG has ambiguous WEB_CATALOG_LAYOUT assignments';
    END IF;
END $$;

INSERT INTO sys_client_layout_profiles (
    tenant_id, client_application_id, layout_profile_id,
    assignment_scope, role_code, privilege_code, device_target, module_code,
    default_profile, selectable, display_order, active,
    created_by, updated_by, created_at, updated_at
)
SELECT tenant.id, client.id, profile.id,
       'CLIENT', NULL, NULL, 'ANY', NULL,
       true, false, 100, true,
       0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_tenants tenant
JOIN sys_acc_client_applications client ON client.client_code = 'CATALOG'
JOIN sys_layout_profiles profile ON profile.profile_code = 'WEB_CATALOG_LAYOUT'
WHERE tenant.id = 7
  AND NOT EXISTS (
      SELECT 1
      FROM sys_client_layout_profiles assignment
      WHERE assignment.tenant_id = tenant.id
        AND assignment.client_application_id = client.id
        AND assignment.layout_profile_id = profile.id
        AND assignment.assignment_scope = 'CLIENT'
        AND assignment.device_target = 'ANY'
        AND assignment.role_code IS NULL
        AND assignment.privilege_code IS NULL
        AND assignment.module_code IS NULL
  );

UPDATE sys_client_layout_profiles assignment
SET default_profile = true,
    selectable = false,
    display_order = 100,
    active = true,
    brand_display_name = NULL,
    brand_short_name = NULL,
    brand_logo_url = NULL,
    brand_logo_dark_url = NULL,
    brand_favicon_url = NULL,
    brand_support_url = NULL,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_acc_client_applications client,
     sys_layout_profiles profile
WHERE assignment.client_application_id = client.id
  AND assignment.layout_profile_id = profile.id
  AND assignment.tenant_id = 7
  AND client.client_code = 'CATALOG'
  AND profile.profile_code = 'WEB_CATALOG_LAYOUT'
  AND assignment.assignment_scope = 'CLIENT'
  AND assignment.device_target = 'ANY'
  AND assignment.role_code IS NULL
  AND assignment.privilege_code IS NULL
  AND assignment.module_code IS NULL;

