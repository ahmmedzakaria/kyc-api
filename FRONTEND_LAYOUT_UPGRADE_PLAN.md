# Frontend Layout Upgrade Plan

## Purpose

Upgrade the frontend shell so NexaCore can support large KYC, Auth, reporting, workflow, administration, and future business modules without relying on a long expanding sidebar.

The frontend layout must be configuration-driven. `ApplicationContextDto` should publish layout, navigation, theme, tenant, branding, and enabled-feature configuration so the frontend renders the correct experience for each tenant/client/application.

## Business Goals

- Provide a reusable enterprise layout for KYC, Auth, and future modules.
- Support dense operational workflows for banking, government, healthcare, finance, and compliance users.
- Keep global actions visible: search, notifications, help, language, profile, tenant, and settings.
- Keep business context visible through breadcrumbs, page title, action buttons, and status bar.
- Scale navigation when modules have many features.
- Allow tenant-specific branding and theme selection without frontend code changes.
- Allow feature visibility to be controlled by privileges, enabled modules, enabled submodules, enabled features, and tenant configuration.
- Keep frontend behavior consistent with backend authorization and application context.

## Target Shell Layout

The application shell should use five stable regions:

```text
+------------------------------------------------------------------------------------+
| Logo | Global Search | Notifications | Help | User Profile | Tenant | Settings     |
+----------------------+-------------------------------------------------------------+
|                      | Breadcrumb                                                  |
|                      +-------------------------------------------------------------+
|                      | Page Title                  Action Buttons                  |
| Left Navigation      +-------------------------------------------------------------+
|                      |                                                             |
|                      |                 Main Content                                |
|                      |                                                             |
|                      +-------------------------------------------------------------+
|                      | Status Bar                                                  |
+----------------------+-------------------------------------------------------------+
```

Configurable regions:

- Header: logo and configured top navigation items such as global search, notifications, help, language, user profile, tenant selector, settings, or tenant-specific actions.
- Breadcrumb: current business location such as `Home > Customers > John Smith > Documents`; shown only when enabled by configuration.
- Page command bar: page title and primary/secondary action buttons.
- Left navigation: feature-type navigation with hover/focus submenu panel.
- Main content: page-specific routed content.
- Status bar: application version, environment, tenant, and logged-in user summary; shown only when enabled by configuration.

Visibility rules:

- Breadcrumb must be shown or hidden from `ApplicationContextDto.layout.breadcrumbEnabled`.
- Status bar must be shown or hidden from `ApplicationContextDto.layout.statusBarEnabled` and `ApplicationContextDto.statusBar.enabled`.
- Header/top navigation items must be injected from configuration.
- Frontend must not hardcode header actions except for safe fallback behavior when context is missing.
- Header items should be filtered by privilege, enabled feature, tenant policy, and display order.

Example page shell:

```text
+----------------------------------------------------------------------------------------+
| Logo | Search | Notifications | Help | Language | User Profile                         |
+----------------------------------------------------------------------------------------+
| Home > Customers > John Smith > Documents                                              |
+----------------------------------------------------------------------------------------+
| Customer Details                                             [Approve] [Reject] [Save] |
+----------------------------------------------------------------------------------------+
|                                                                                Content |
+----------------------------------------------------------------------------------------+
| KYC v1.0.0 | PROD | Tenant: CLIENT-001 |        | Logged in: Zakaria Ahmmed            |
+----------------------------------------------------------------------------------------+
```

## Navigation Model

The left navigation should use a multi-panel model instead of a long collapsible sidebar.

Structure:

- Icon rail: compact module or feature-type icons.
- Feature-type panel: major areas such as Dashboard, Customers, KYC, Documents, Verification, Reports, Admin.
- Submenu panel: loaded on hover, keyboard focus, or click for the selected feature type.
- Main content: remains stable and does not shift unexpectedly.
- View toggle button: allows the user to switch the left navigation between icon-only view and detailed view.

Target behavior:

```text
+-----------+----------------------+---------------------------------------------------+
|           |                      |                                                   |
| Home      | Dashboard            |                                                   |
| Customers | Customers        >   |                                                   |
| KYC       | KYC              >   |              Main Content                         |
| Documents | Documents        >   |                                                   |
| Verify    | Verification     >   |                                                   |
| Reports   | Reports          >   |                                                   |
| Admin     | Admin            >   |                                                   |
+-----------+----------------------+---------------------------------------------------+
```

Submenu example:

```text
+-----------+----------------------+------------------------------+
| Customers | Customers            | Customer List                |
|           |                      | New Customer                 |
|           |                      | Import Customers             |
|           |                      | Customer Groups              |
|           |                      | Blacklisted Customers        |
|           |                      | Merge Duplicate              |
|           |                      | Export                       |
|           |                      | Archive                      |
+-----------+----------------------+------------------------------+
```

Left navigation display modes:

| Mode | Behavior |
| --- | --- |
| Icon only | Shows only the compact icon rail. Useful for maximum content space. Tooltips must expose labels. |
| Detail | Shows icon rail plus feature labels. Useful for discovery and daily operational work. |
| Expanded with submenu | Shows icon rail, feature labels, and the active submenu panel. Triggered by hover, keyboard focus, or click. |

Toggle rules:

- A visible toggle button should switch between icon-only and detail view.
- The selected view may be stored as a user preference if tenant policy allows it.
- Tenant/application configuration should define the default navigation view.
- The submenu panel should still open from icon-only mode when the user hovers, focuses, or clicks a feature.
- Keyboard users must be able to switch view and open submenus without hover.
- The main content region should resize predictably when the user changes the navigation view.

Feature types should be configurable, but the recommended initial groups are:

| Feature Type | Purpose |
| --- | --- |
| Operation | Daily work: onboarding, KYC cases, verification, approvals, document review |
| Setup | Configuration: workflows, document types, risk rules, tenants, roles |
| Report | Reporting: KYC status, SLA, risk, audit, approval, integration reports |
| Administration | Users, roles, privileges, tenant settings, system settings |

Navigation item rules:

- Every item must have a stable code.
- Every item may declare module, submodule, feature, route, icon, privilege codes, order, and display group.
- Frontend must hide items not present in context or not allowed by privilege.
- Backend APIs must still enforce authorization even when the frontend hides items.
- Deep links should work if the user is authorized.
- Submenus must support keyboard access, not hover only.

## Theme Model

Themes should be selected by tenant/application configuration and delivered through application context.

Recommended built-in themes:

| Theme | Header | Sidebar | Content | Background | Primary | Accent/Status |
| --- | --- | --- | --- | --- | --- | --- |
| Light Default | `#FFFFFF` | `#F8FAFC` | `#FFFFFF` | `#F1F5F9` | `#2563EB` | Success `#16A34A`, Warning `#F59E0B`, Danger `#DC2626` |
| Dark | `#111827` | `#1F2937` | `#1E293B` | `#0F172A` | `#3B82F6` | Success `#22C55E`, Warning `#FBBF24`, Danger `#EF4444` |
| Blue Enterprise | `#0F3D91` | `#123C69` | `#FFFFFF` | `#F4F8FC` | `#2563EB` | Accent `#06B6D4` |
| Navy Banking | `#102A43` | `#243B53` | `#FFFFFF` | `#F8FAFC` | `#1D4ED8` | Accent `#38BDF8` |
| Green Compliance | `#14532D` | `#166534` | `#FFFFFF` | `#F8FAFC` | `#22C55E` | Warning `#F59E0B`, Danger `#DC2626` |
| Purple Corporate | `#5B21B6` | `#6D28D9` | `#FFFFFF` | `#F8FAFC` | `#8B5CF6` | Accent `#A78BFA` |
| Gray Professional | `#374151` | `#4B5563` | `#FFFFFF` | `#F9FAFB` | `#3B82F6` | Border `#E5E7EB` |

Default recommendation:

- Use Light Default for most deployments.
- Use Blue Enterprise for KYC-heavy enterprise clients.
- Use Dark only when explicitly selected by user or tenant.

Theme implementation rules:

- Store theme tokens, not hardcoded component colors.
- Frontend components should consume CSS variables generated from context.
- Tenant theme should provide defaults.
- User preference may override tenant theme only if tenant policy allows it.
- Accessibility contrast must be validated before a theme is enabled.

### Theme Token Structure

Recommended token names:

```text
--nc-header-bg
--nc-header-text
--nc-header-border
--nc-sidebar-bg
--nc-sidebar-text
--nc-sidebar-hover-bg
--nc-sidebar-active-bg
--nc-sidebar-active-text
--nc-submenu-bg
--nc-submenu-text
--nc-content-bg
--nc-page-bg
--nc-surface-bg
--nc-text
--nc-text-muted
--nc-border
--nc-primary
--nc-primary-hover
--nc-accent
--nc-success
--nc-warning
--nc-danger
--nc-focus-ring
--nc-shadow-sm
--nc-shadow-md
```

Recommended layout dimension tokens:

```text
--nc-header-height
--nc-statusbar-height
--nc-sidebar-icon-width
--nc-sidebar-detail-width
--nc-submenu-width
--nc-content-padding
--nc-border-radius
```

Recommended default values:

```scss
:root {
  --nc-header-height: 56px;
  --nc-statusbar-height: 28px;
  --nc-sidebar-icon-width: 64px;
  --nc-sidebar-detail-width: 220px;
  --nc-submenu-width: 280px;
  --nc-content-padding: 16px;
  --nc-border-radius: 6px;
}
```

### Proposed Theme Definitions

Use `data-theme` or a theme class on the shell root. The existing app already uses `data-bs-theme`; keep Bootstrap compatibility and add NexaCore variables beside it.

Recommended theme selector pattern:

```scss
[data-nc-theme='light'] {
  --nc-header-bg: #ffffff;
  --nc-header-text: #1e293b;
  --nc-header-border: #e2e8f0;
  --nc-sidebar-bg: #f8fafc;
  --nc-sidebar-text: #1e293b;
  --nc-sidebar-hover-bg: #e0f2fe;
  --nc-sidebar-active-bg: #dbeafe;
  --nc-sidebar-active-text: #1d4ed8;
  --nc-submenu-bg: #ffffff;
  --nc-submenu-text: #1e293b;
  --nc-content-bg: #ffffff;
  --nc-page-bg: #f1f5f9;
  --nc-surface-bg: #ffffff;
  --nc-text: #1e293b;
  --nc-text-muted: #64748b;
  --nc-border: #e2e8f0;
  --nc-primary: #2563eb;
  --nc-primary-hover: #1d4ed8;
  --nc-accent: #06b6d4;
  --nc-success: #16a34a;
  --nc-warning: #f59e0b;
  --nc-danger: #dc2626;
  --nc-focus-ring: rgba(37, 99, 235, 0.35);
}
```

Built-in theme codes:

| Code | Display Name | Default Use |
| --- | --- | --- |
| `light` | Light Default | Default for most deployments |
| `dark` | Dark | Long working hours or user preference |
| `blue-enterprise` | Blue Enterprise | Recommended KYC enterprise theme |
| `navy-banking` | Navy Banking | Banking and financial institutions |
| `green-compliance` | Green Compliance | Audit and compliance-heavy environments |
| `purple-corporate` | Purple Corporate | SaaS/corporate deployments |
| `gray-professional` | Gray Professional | Neutral internal operations |

### Proposed CSS Structure

Current layout styles are located in:

```text
frontend-libs/layout/src/lib/layout/layout.component.scss
frontend-libs/layout/src/lib/sidebar/sidebar.component.scss
frontend-libs/layout/src/lib/topbar/topbar.component.scss
```

Recommended structure:

```text
frontend-libs/layout/src/lib/styles/
├── _layout-tokens.scss
├── _theme-light.scss
├── _theme-dark.scss
├── _theme-blue-enterprise.scss
├── _theme-navy-banking.scss
├── _theme-green-compliance.scss
├── _theme-purple-corporate.scss
├── _theme-gray-professional.scss
├── _layout-shell.scss
├── _navigation.scss
├── _top-navigation.scss
├── _breadcrumb.scss
├── _command-bar.scss
└── _status-bar.scss
```

Recommended usage:

- Keep shared design tokens and theme variables in `frontend-libs/layout/src/lib/styles`.
- Keep only component-specific layout overrides in each component SCSS file.
- Import shared layout SCSS from `layout.component.scss` or expose one layout stylesheet through the library build.
- Avoid adding tenant-specific colors directly to component SCSS.
- Components should use variables such as `var(--nc-sidebar-bg)` and `var(--nc-primary)`.
- Runtime tenant-provided theme values should be applied through inline CSS variables on the shell root or through a generated theme style block.

Example component usage:

```scss
.app-layout {
  min-height: 100vh;
  color: var(--nc-text);
  background: var(--nc-page-bg);
}

.app-header {
  height: var(--nc-header-height);
  color: var(--nc-header-text);
  background: var(--nc-header-bg);
  border-bottom: 1px solid var(--nc-header-border);
}

.feature-navigation {
  color: var(--nc-sidebar-text);
  background: var(--nc-sidebar-bg);
  border-right: 1px solid var(--nc-border);
}

.feature-navigation__item.active {
  color: var(--nc-sidebar-active-text);
  background: var(--nc-sidebar-active-bg);
}
```

### Theme Loading Flow

Recommended flow:

1. Frontend loads `ApplicationContextDto`.
2. `LayoutService` reads `theme.themeCode`, `theme.tokens`, and `theme.allowUserOverride`.
3. If user override is allowed and a saved preference exists, use the saved user theme.
4. Apply `data-nc-theme` to the shell root.
5. Apply tenant token overrides as CSS variables on the shell root.
6. Components render using `var(--nc-*)` tokens.

Fallback rules:

- If no theme context is provided, use `light`.
- If an unknown `themeCode` is provided, use `light`.
- If a token is missing, fallback to the built-in theme variable.
- If dark mode is enabled, keep `data-bs-theme="dark"` synchronized for Bootstrap components.

## Application Context Contract

Current `ApplicationContextDto` includes:

```text
clientCode
clientType
menus
privilegeCodes
enabledModules
enabledSubmodules
enabledFeatures
```

Recommended layout additions:

```text
layout
branding
theme
navigation
header
statusBar
tenant
localization
```

Recommended DTO shape:

```java
public class ApplicationContextDto {
    private String clientCode;
    private String clientType;
    private LayoutContextDto layout;
    private BrandingContextDto branding;
    private ThemeContextDto theme;
    private NavigationContextDto navigation;
    private HeaderContextDto header;
    private StatusBarContextDto statusBar;
    private TenantContextDto tenant;
    private LocalizationContextDto localization;
    private List<SidebarMenuDto> menus;
    private Set<String> privilegeCodes;
    private Set<String> enabledModules;
    private Set<String> enabledSubmodules;
    private Set<String> enabledFeatures;
}
```

Recommended `LayoutContextDto`:

```text
shellType
headerEnabled
breadcrumbEnabled
commandBarEnabled
leftNavigationEnabled
statusBarEnabled
navigationMode
defaultNavigationView
allowNavigationViewToggle
contentWidth
density
```

Recommended `NavigationContextDto`:

```text
mode
supportedViews
defaultView
allowUserViewPreference
featureTypes
defaultFeatureType
submenuTrigger
collapseBehavior
items
```

Recommended `NavigationItemDto`:

```text
code
labelCode
fallbackLabel
icon
route
moduleCode
submoduleCode
featureCode
featureType
parentCode
order
visible
disabled
privilegeCodes
children
```

Recommended `ThemeContextDto`:

```text
themeCode
allowUserOverride
tokens
```

Recommended theme tokens:

```text
header
sidebar
content
background
primary
accent
success
warning
danger
text
mutedText
border
focusRing
```

Recommended `HeaderContextDto`:

```text
enabled
items
globalSearchEnabled
notificationsEnabled
helpEnabled
languageSelectorEnabled
tenantSelectorEnabled
settingsEnabled
profileMenuEnabled
```

Recommended `TopNavigationItemDto`:

```text
code
type
labelCode
fallbackLabel
icon
route
action
position
order
visible
disabled
privilegeCodes
featureCode
configuration
```

Recommended top navigation item types:

| Type | Purpose |
| --- | --- |
| Logo | Tenant/application logo and home route |
| Search | Global or scoped application search |
| Notification | Notification center entry point |
| Help | Help/documentation/support entry point |
| Language | Language selector |
| Tenant | Tenant selector or tenant display |
| Profile | User profile menu |
| Settings | Settings entry point |
| Link | Configured route link |
| Action | Configured frontend command |
| Custom | Tenant-specific extension item |

Recommended `StatusBarContextDto`:

```text
enabled
items
applicationName
version
environment
tenantCode
loggedInUserDisplayName
```

Recommended `StatusBarItemDto`:

```text
code
type
labelCode
fallbackLabel
value
order
visible
privilegeCodes
```

## Frontend Component Plan

Recommended shell components:

- `AppShell`: owns the page grid and shared layout regions.
- `AppHeader`: logo, search, notifications, help, language, tenant, profile, settings.
- `TopNavigationRenderer`: renders configured top navigation items from context.
- `BreadcrumbBar`: route-aware breadcrumb display shown only when enabled by context.
- `PageCommandBar`: title and page action buttons.
- `FeatureNavigation`: icon rail and feature-type panel.
- `NavigationViewToggle`: switches the left navigation between icon-only and detail view.
- `NavigationSubmenuPanel`: secondary menu shown for selected feature type.
- `StatusBar`: configured status items shown only when enabled by context.
- `ThemeProvider`: converts context theme tokens into CSS variables.
- `ApplicationContextProvider`: fetches, caches, and exposes context.

## Existing Component Impact Analysis

Current frontend structure:

- Main app root: `frontend/src/app/app.component.*`
- Authenticated shell route: `@nexacore/layout` `LayoutComponent`
- Existing shared layout components:
  - `LayoutComponent`
  - `TopbarComponent`
  - `SidebarComponent`
- Existing layout services:
  - `LayoutService`
  - `SidebarMenuService`

The current frontend already has the base shell, topbar, sidebar, theme state, and application context menu loading. The upgrade should reuse these instead of creating a parallel shell.

Recommended reuse:

| Existing Item | Upgrade Decision | Reason |
| --- | --- | --- |
| `LayoutComponent` | Modify | Already owns topbar, sidebar, and routed content. Extend it to render breadcrumb, command bar, status bar, and configurable layout regions. |
| `TopbarComponent` | Modify | Already owns language, theme, profile, and logout. Extend it to render configured top navigation items. |
| `SidebarComponent` | Major refactor | Already loads menu context. Refactor it into the new icon/detail/submenu navigation behavior. |
| `LayoutService` | Extend | Already owns theme and collapsed state. Add breadcrumb/status visibility, nav view, theme tokens, and layout config. |
| `SidebarMenuService` | Rename or extend | Already loads `ApplicationContext`. Extend it to load layout, navigation, header, theme, and status bar context. |

Minimum new UI components to create:

| New Component | Required | Purpose |
| --- | --- | --- |
| `TopNavigationRendererComponent` | Yes | Renders configured top navigation/header items instead of hardcoded topbar actions. |
| `BreadcrumbBarComponent` | Yes | Shows route/entity breadcrumb only when enabled by context. |
| `PageCommandBarComponent` | Yes | Shows page title and configured/current page actions. |
| `NavigationViewToggleComponent` | Yes | Switches left navigation between icon-only and detail view. |
| `NavigationSubmenuPanelComponent` | Yes | Shows submenu items for the selected feature/module without expanding a long sidebar. |
| `StatusBarComponent` | Yes | Shows configured status items only when enabled by context. |

Recommended new component count:

```text
6 new UI components
```

Optional component split:

| Optional Component | When To Create |
| --- | --- |
| `FeatureNavigationComponent` | Create if `SidebarComponent` becomes too complex during refactor. Otherwise keep `SidebarComponent` and change its behavior. |
| `ThemePreviewComponent` | Create only for an admin settings screen where tenants configure themes. Not required for the shell upgrade. |
| `GlobalSearchComponent` | Create only when global search has real search behavior beyond a configured topbar item. |
| `NotificationCenterComponent` | Create only when notification list, unread counts, and notification actions are implemented. |

Recommended final count for the first implementation:

```text
Modify existing components: 3
Extend existing services: 2
Create new UI components: 6
Optional later components: 4
```

Do not create new page-level components in `frontend/src/app/pages` for this layout upgrade. The page components should continue to render inside the upgraded shared shell.

Routing rules:

- Routes should reference navigation item codes where possible.
- Hidden navigation does not mean the route is authorized.
- Unauthorized routes must show a proper forbidden state.
- Unknown routes must show a proper not-found state.
- Page title and breadcrumb should be route metadata plus optional entity state.

## Backend Configuration Plan

Configuration ownership:

- Auth module publishes authenticated application context.
- System/admin module can own tenant UI configuration if a dedicated system module is used.
- KYC module contributes KYC navigation items and feature flags.
- Other business modules contribute their own module navigation and feature flags.
- Backend merges module contributions into one `ApplicationContextDto`.

Persistence options:

- Store tenant UI config in module-prefixed tables if configuration becomes dynamic.
- Keep seed/default config in backend code or migration data for the first phase.
- Keep user-specific preferences separate from tenant defaults.

Recommended tables if persisted later:

| Table | Purpose |
| --- | --- |
| `auth_application_context_config` | Client/application context settings owned by Auth |
| `auth_ui_theme_config` | Theme tokens and selected theme code |
| `auth_ui_navigation_config` | Navigation item ordering and visibility |
| `auth_user_ui_preference` | User theme/density preference where allowed |

Every new table must include `created_by`, `updated_by`, `created_at`, and `updated_at`.

## Implementation Phases

### Phase 1 - Static Shell Upgrade

- Build the new shell layout with header, breadcrumb, command bar, left navigation, main content, and status bar.
- Use current `menus`, `enabledModules`, `enabledSubmodules`, `enabledFeatures`, and `privilegeCodes`.
- Implement Light Default and Blue Enterprise themes as static frontend token sets.
- Implement the left navigation toggle for icon-only and detail view.
- Read breadcrumb and status bar visibility from context.
- Render top navigation items from context with safe defaults.
- Keep existing routes working.

### Phase 2 - Context-Driven Navigation

- Extend `ApplicationContextDto` with layout, theme, navigation, header, top navigation items, and status bar fields.
- Convert existing `SidebarMenuDto` into the new feature-type navigation model or add a new navigation DTO.
- Make feature type, submenu, route visibility, ordering, and icons context-driven.
- Add frontend fallback behavior when context fields are missing.

### Phase 3 - Tenant Theme And Branding

- Add tenant branding: logo, application name, favicon, primary theme, allowed themes.
- Publish theme tokens through context.
- Add optional user theme preference if allowed by tenant policy.
- Add accessibility validation before enabling tenant-defined themes.

### Phase 4 - Module Contribution Model

- Let KYC, Auth, Reports, Workflow, Documents, and Administration contribute navigation definitions through backend module gateways or configuration.
- Merge module navigation based on enabled modules, privileges, and tenant config.
- Add tests for context merging, privilege filtering, disabled modules, and menu ordering.

### Phase 5 - Operational Hardening

- Add responsive behavior for tablet and small desktop screens.
- Add keyboard navigation for feature panels and submenus.
- Add route-level forbidden and not-found states.
- Add audit-safe UI logging for navigation/config loading failures.
- Add visual regression coverage for supported themes if frontend test tooling supports it.

## Acceptance Criteria

- Frontend shell supports header, breadcrumb, page title/actions, main content, left navigation, and status bar.
- Breadcrumb can be shown or hidden from configuration.
- Status bar can be shown or hidden from configuration.
- Top navigation bar items are injected from configuration.
- Left navigation supports icon rail, feature-type panel, and submenu panel.
- User can switch the left navigation between icon-only and detail view with a button.
- Navigation can represent large modules without a long expanding sidebar.
- Theme can be selected from configuration.
- Application context is the source of truth for visible modules, features, privileges, theme, and layout behavior.
- Frontend hides unauthorized or disabled features, and backend still rejects unauthorized API calls.
- Breadcrumb and page title reflect the active route and current entity context.
- Status bar shows version, environment, tenant, and logged-in user when configured.
- Layout remains usable with many submenu items.
