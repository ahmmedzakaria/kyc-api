-- 1. Tenant/client layout assignments and resolved profile details
-- [tenant_id:7, client_code:'CATALOG']
SELECT
    tenant.id AS tenant_id,
    tenant.tenant_code,
    tenant.display_name AS tenant_name,
    client.id AS client_application_id,
    client.client_code,
    client.client_name,
    assignment.id AS assignment_id,
    assignment.assignment_scope,
    assignment.role_code,
    assignment.privilege_code,
    assignment.device_target,
    assignment.module_code,
    assignment.default_profile,
    assignment.selectable,
    assignment.display_order AS assignment_order,
    assignment.active AS assignment_active,
    profile.id AS layout_profile_id,
    profile.profile_code,
    profile.profile_name,
    profile.description,
    profile.layout_type,
    profile.navigation_mode,
    profile.theme_mode,
    profile.density,
    profile.topbar_enabled,
    profile.sidebar_enabled,
    profile.sidebar_collapsed,
    profile.footer_enabled,
    profile.breadcrumb_enabled,
    profile.command_bar_enabled,
    profile.rtl_enabled,
    profile.active AS profile_active,
    COALESCE(assignment.brand_display_name, branding.display_name) AS resolved_display_name,
    COALESCE(assignment.brand_short_name, branding.short_name) AS resolved_short_name,
    COALESCE(assignment.brand_logo_url, branding.logo_url) AS resolved_logo_url,
    COALESCE(assignment.brand_logo_dark_url, branding.logo_dark_url) AS resolved_logo_dark_url,
    COALESCE(assignment.brand_favicon_url, branding.favicon_url) AS resolved_favicon_url,
    COALESCE(assignment.brand_support_url, branding.support_url) AS resolved_support_url,
    sizes.space_unit,
    sizes.radius_base,
    sizes.font_size_base,
    sizes.header_height,
    sizes.status_bar_height,
    sizes.rail_width_collapsed,
    sizes.rail_width_expanded,
    fonts.body_family,
    fonts.heading_family,
    fonts.mono_family,
    fonts.font_source,
    fonts.font_url,
    fonts.fallback_stack,
    assignment.created_by,
    assignment.updated_by,
    assignment.created_at,
    assignment.updated_at
FROM sys_client_layout_profiles assignment
         JOIN sys_tenants tenant
              ON tenant.id = assignment.tenant_id
         JOIN sys_acc_client_applications client
              ON client.id = assignment.client_application_id
         JOIN sys_layout_profiles profile
              ON profile.id = assignment.layout_profile_id
         LEFT JOIN sys_layout_profile_branding branding
                   ON branding.layout_profile_id = profile.id
         LEFT JOIN sys_layout_profile_sizes sizes
                   ON sizes.layout_profile_id = profile.id
         LEFT JOIN sys_layout_profile_fonts fonts
                   ON fonts.layout_profile_id = profile.id
WHERE tenant.id = :tenant_id
  AND client.client_code = :client_code
ORDER BY assignment.default_profile DESC,
         assignment.display_order,
         profile.profile_code;

SELECT * FROM sys_client_layout_profiles where client_application_id=11 and tenant_id=7;
SELECT * FROM sys_acc_client_applications WHERE id=11;
SELECT * FROM sys_layout_profiles where id=2;

--2. Themes, semantic colors, and chrome overrides for assigned profiles
SELECT
    tenant.tenant_code,
    client.client_code,
    profile.profile_code,
    theme.id AS profile_theme_id,
    theme.theme_id,
    theme.theme_label,
    theme.base,
    theme.swatch,
    theme.active AS theme_active,
    primary_color.text_color,
    primary_color.paper_color,
    primary_color.card_color,
    primary_color.accent_color,
    primary_color.amber_color,
    primary_color.red_color,
    primary_color.success_color,
    primary_color.info_color,
    chrome.accent_soft,
    chrome.background,
    chrome.border,
    chrome.border_strong,
    chrome.hover_background,
    chrome.active_background,
    chrome.search_background,
    chrome.search_border,
    chrome.search_text,
    chrome.search_placeholder
FROM sys_client_layout_profiles assignment
         JOIN sys_tenants tenant
              ON tenant.id = assignment.tenant_id
         JOIN sys_acc_client_applications client
              ON client.id = assignment.client_application_id
         JOIN sys_layout_profiles profile
              ON profile.id = assignment.layout_profile_id
         JOIN sys_layout_profile_themes theme
              ON theme.layout_profile_id = profile.id
         LEFT JOIN sys_layout_theme_primaries primary_color
                   ON primary_color.layout_profile_theme_id = theme.id
         LEFT JOIN sys_layout_theme_chrome_overrides chrome
                   ON chrome.layout_profile_theme_id = theme.id
WHERE tenant.id = :tenant_id
  AND client.client_code = :client_code
ORDER BY profile.profile_code, theme.theme_id;

--3. Navigation hierarchy and required privileges
SELECT
    module_group.group_code,
    module_group.group_name,
    module_group.display_order AS group_order,
    navigation_module.navigation_module_code,
    navigation_module.navigation_module_name,
    navigation_module.physical_module_code,
    navigation_module.display_order AS module_order,
    category.category_code,
    category.category_name,
    category.category_kind,
    category.feature_type_code,
    category.display_order AS category_order,
    feature_group.feature_group_code,
    feature_group.feature_group_name,
    feature_group.display_order AS feature_group_order,
    feature.feature_code,
    feature.t_code,
    feature.feature_name,
    feature.route,
    feature.icon,
    feature.physical_module_code AS feature_physical_module_code,
    feature.physical_submodule_code,
    feature.physical_feature_type_code,
    feature.physical_feature_code,
    feature.display_order AS feature_order,
    feature.active AS feature_active,
    COALESCE(
            string_agg(
                DISTINCT privilege.privilege_code || ' [' || feature_privilege.match_mode || ']',
                    ', '
            ) FILTER (
                WHERE feature_privilege.active
                    AND privilege.active
                        ),
            '(public/no linked privilege)'
    ) AS required_privileges
FROM sys_layout_module_groups module_group
         JOIN sys_priv_modules navigation_module
              ON navigation_module.module_group_id = module_group.id
         JOIN sys_layout_navigation_categories category
              ON category.navigation_module_id = navigation_module.id
         JOIN sys_layout_feature_groups feature_group
              ON feature_group.navigation_category_id = category.id
         JOIN sys_layout_features feature
              ON feature.feature_group_id = feature_group.id
         LEFT JOIN sys_layout_feature_privileges feature_privilege
                   ON feature_privilege.layout_feature_id = feature.id
         LEFT JOIN sys_priv_privileges privilege
                   ON privilege.id = feature_privilege.privilege_id
WHERE module_group.active
  AND navigation_module.active
  AND navigation_module.navigation_enabled
  AND category.active
  AND feature_group.active
GROUP BY
    module_group.id,
    navigation_module.id,
    category.id,
    feature_group.id,
    feature.id
ORDER BY
    module_group.display_order,
    navigation_module.display_order,
    category.display_order,
    feature_group.display_order,
    feature.display_order,
    feature.feature_code;

select * from sys_layout_route_policies;
--4. Route policies and required privileges for the selected client
WITH selected_client AS (
    SELECT id, client_code
    FROM sys_acc_client_applications
    WHERE client_code = :client_code
)
SELECT
    client.client_code AS policy_client_code,
    route_policy.id AS route_policy_id,
    route_policy.route_url,
    route_policy.match_mode,
    route_policy.active AS policy_active,
    COALESCE(
            string_agg(DISTINCT privilege.privilege_code, ', ')
            FILTER (WHERE policy_privilege.active AND privilege.active),
            '(no linked privilege)'
    ) AS required_privileges
FROM sys_layout_route_policies route_policy
         LEFT JOIN selected_client client
                   ON client.id = route_policy.client_application_id
         LEFT JOIN sys_layout_route_policy_privileges policy_privilege
                   ON policy_privilege.route_policy_id = route_policy.id
         LEFT JOIN sys_priv_privileges privilege
                   ON privilege.id = policy_privilege.privilege_id
WHERE route_policy.client_application_id IS NULL
   OR route_policy.client_application_id = (SELECT id FROM selected_client)
GROUP BY client.client_code, route_policy.id
ORDER BY route_policy.route_url;

--5. UI action policies and required privileges for the selected client
WITH selected_client AS (
    SELECT id, client_code
    FROM sys_acc_client_applications
    WHERE client_code = :client_code
)
SELECT
    client.client_code AS policy_client_code,
    ui_policy.id AS ui_policy_id,
    ui_policy.action_code,
    ui_policy.match_mode,
    ui_policy.active AS policy_active,
    COALESCE(
            string_agg(DISTINCT privilege.privilege_code, ', ')
            FILTER (WHERE policy_privilege.active AND privilege.active),
            '(no linked privilege)'
    ) AS required_privileges
FROM sys_layout_ui_policies ui_policy
         LEFT JOIN selected_client client
                   ON client.id = ui_policy.client_application_id
         LEFT JOIN sys_layout_ui_policy_privileges policy_privilege
                   ON policy_privilege.ui_policy_id = ui_policy.id
         LEFT JOIN sys_priv_privileges privilege
                   ON privilege.id = policy_privilege.privilege_id
WHERE ui_policy.client_application_id IS NULL
   OR ui_policy.client_application_id = (SELECT id FROM selected_client)
GROUP BY client.client_code, ui_policy.id
ORDER BY ui_policy.action_code;