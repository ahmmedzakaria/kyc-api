# Layout Submodule Business And Implementation Plan

## Purpose

The layout submodule will extend `systemmodule` with backend-owned, client-specific layout configuration for NexaCore frontend applications. Each client application can choose from multiple layout options, themes, fonts, navigation modes, density presets, and branding rules without requiring a frontend rebuild.

This plan is written for the current project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Target module: `com.nexacore.systemmodule.layout`
- Existing client access package: `com.nexacore.systemmodule.accesscontrol`
- Existing privilege/application context package: `com.nexacore.systemmodule.privilege`
- Frontend companion plan: `LAYOUT_FRONTEND_CHANGE_PLAN.md`
- API response wrappers and common DTOs: `commonmodule`
- Flyway migration location: `src/main/resources/db/migration/system`

## Business Goals

- Let platform administrators configure layout options per client application.
- Support multiple frontend shells such as sidebar, horizontal navigation, rail navigation, compact operations layout, and public login layout.
- Return the effective layout configuration from the backend application context so the frontend renders configuration instead of hardcoding client behavior.
- Keep branding, theme, navigation mode, density, and feature visibility consistent for consuming frontend applications.
- Allow a client to have a default layout and optional selectable layout profiles for different user roles, devices, or modules.
- Keep layout configuration separate from user authorization. Layout may hide or arrange UI, but backend privilege and API access remain authoritative.

## Ownership Boundary

Layout configuration belongs in `systemmodule` because it is platform presentation governance for client applications.

```text
systemmodule/layout owns:
- layout profiles
- client layout assignments
- theme primaries, optional chrome overrides, font metadata, size primitives, and branding metadata
- navigation shell preference
- density and responsive behavior options
- layout option publication through application context
```

The Angular 21 frontend library owns rendering and client-side interactions.

```text
frontendApplications/frontend-libs-21 owns:
- Angular layout components
- sidebar/topbar/rail/horizontal renderers
- local collapsed state
- runtime computation and application of CSS variables from backend theme primaries, chrome overrides, font metadata, and size primitives
- responsive behavior
```

The backend must not store Angular component names, CSS implementation details, or frontend-only state that changes during a session. The frontend must not decide which commercial or client-specific layout profiles are available.

## Physical Structure Versus Logical Navigation Structure

NexaCore has two different structures that must stay separate.

### Physical Structure

Physical structure is the backend/codebase ownership model. It describes where code, persistence, APIs, gateways, logs, and internal services live.

Current physical module enum:

```text
APP_CONFIG
COMMON
ESB
GATEWAY
KYC
AUTH
GIS
LOG
POS
SERVICES
SYSTEM
```

This is represented today by `ApplicationModule` and `ApplicationSubmodule` in the privilege catalog. These values are used for:

- privilege code generation
- module/submodule ownership
- API registry ownership
- license and client-feature decisions
- backend package responsibility
- audit and troubleshooting context

Physical module names should not be used directly as the frontend navigation tree. Some physical modules are not user-facing (`COMMON`, `GATEWAY`, `LOG`), and some user-facing screens combine features from multiple physical modules.

### Logical Navigation Structure

Logical structure is the frontend experience model. It describes how a user finds work in the UI.

The Sentinel reference uses this tree:

```text
module group
  -> module
    -> category
      -> feature group
        -> feature
```

Example:

```text
Compliance
  -> KYC
    -> Operation
      -> Person KYC
        -> Customer Registration
        -> KYC Profile
    -> Setup
      -> KYC Configuration
        -> Document Type Setup
    -> Report
      -> Operational Reports
        -> KYC Summary
```

Logical navigation should be configurable from backend layout data and filtered by privilege, client feature permissions, and license entitlements.

`Operation`, `Setup`, and `Report` are current default category labels only. They must not be hard-coded in frontend code, backend enums, or navigation rendering logic. Business clients may replace them with labels such as `Service Desk`, `Configuration`, `Analytics`, `Approvals`, `Monitoring`, `Transactions`, or any other category structure required by their operating model.

### Required Design Rule

Do not overload `ApplicationModule` to mean logical frontend module.

Recommended naming:

| Concern | Recommended Name | Example |
| --- | --- | --- |
| Code ownership | `ApplicationModule` or future `PhysicalModule` | `KYC`, `AUTH`, `SYSTEM` |
| Code submodule | `ApplicationSubmodule` or future `PhysicalSubmodule` | `KYC_PERSON`, `SYSTEM_LICENSE` |
| Frontend module group | `LayoutModuleGroup` | `Compliance`, `Administration`, `Finance` |
| Frontend module | `LayoutNavigationModule` | `KYC`, `Privilege`, `License` |
| Frontend category | `LayoutNavigationCategory` | `Operation`, `Setup`, `Report`, `Analytics`, `Approvals` |
| Frontend feature group | `LayoutFeatureGroup` | `Person KYC`, `KYC Configuration` |
| Frontend feature leaf | `LayoutFeature` | `Customer Registration`, `KYC Profile` |

`ApplicationModule` can remain in place for privilege-code compatibility. If a rename is done later, add aliases/migrations and do not change existing privilege code semantics.

## Navigation Composition Model

The layout submodule should build the frontend navigation tree by joining logical navigation records to physical privilege/catalog records.

```text
physical catalog:
ApplicationModule -> ApplicationSubmodule -> FeatureType -> Feature -> Action

logical navigation:
ModuleGroup -> NavigationModule -> ConfiguredCategory -> FeatureGroup -> FeatureLeaf

mapping:
FeatureLeaf -> physical feature/action privilege codes
FeatureLeaf -> route path
FeatureLeaf -> client/license visibility rules
```

Frontend navigation should be returned only after applying these filters:

1. Client application access.
2. License entitlement.
3. Authenticated user privileges.
4. Active logical navigation node state.
5. Active route availability in the consuming frontend.

The existing `PrivilegeServiceImpl#getUserSidebarMenu` currently returns `Setup`, `Operations`, and `Report` as top-level menu nodes. That is useful as a transitional response only. The target logical model should put configured category records under each frontend module. The seeded defaults can match Sentinel's `Operation`, `Setup`, and `Report`, but a client-specific database configuration may use different categories.

Target response:

```json
{
  "navTree": [
    {
      "label": "Compliance",
      "type": "group",
      "icon": "shield-check",
      "children": [
        {
          "label": "KYC",
          "type": "module",
          "children": [
            {
              "label": "Operation",
              "type": "category",
              "children": [
                {
                  "label": "Person KYC",
                  "type": "featureGroup",
                  "children": [
                    {
                      "label": "Customer Registration",
                      "type": "feature",
                      "tCode": "KYC101",
                      "route": "person/create",
                      "privilegeCodes": ["01010200101"]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

The frontend should render the logical `layout.navTree` returned by backend. It should not reconstruct logical groups from physical module codes, `FeatureType`, `enabledModules`, or hard-coded category names.

## Proposed Logical Navigation Tables

Add logical navigation tables under `system_db`. These tables are separate from `sys_priv_modules`, `sys_priv_submodules`, `sys_priv_features`, and `sys_priv_privileges`.

### `sys_layout_module_groups`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `group_code` | Stable logical code, for example `COMPLIANCE` |
| `group_name` | Display label |
| `icon` | Safe icon key |
| `display_order` | Sort order |
| `active` | Boolean |

### `sys_layout_navigation_modules`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `module_group_id` | FK to `sys_layout_module_groups` |
| `navigation_module_code` | Stable logical code, for example `KYC` |
| `navigation_module_name` | Display label |
| `physical_module_code` | Optional reference to `sys_priv_modules.code` |
| `icon` | Safe icon key |
| `display_order` | Sort order |
| `active` | Boolean |

### `sys_layout_navigation_categories`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `navigation_module_id` | FK to `sys_layout_navigation_modules` |
| `category_code` | Stable client/business category code such as `OPERATION`, `SETUP`, `REPORT`, `ANALYTICS`, `APPROVALS` |
| `category_name` | Display label |
| `category_kind` | Optional semantic hint such as `WORK`, `CONFIGURATION`, `REPORTING`, `CUSTOM`; used for analytics/defaults only, not rendering rules |
| `icon` | Safe icon key |
| `display_order` | Sort order |
| `active` | Boolean |

Category rules:

- Categories are database records, not enums.
- `OPERATION`, `SETUP`, and `REPORT` are seed data for the default profile.
- A client layout can rename, hide, reorder, or replace categories without frontend code changes.
- The frontend must display categories in backend order and must not branch on category labels.

### `sys_layout_feature_groups`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `navigation_category_id` | FK to `sys_layout_navigation_categories` |
| `feature_group_code` | Stable logical code |
| `feature_group_name` | Display label |
| `display_order` | Sort order |
| `active` | Boolean |

### `sys_layout_features`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `feature_group_id` | FK to `sys_layout_feature_groups` |
| `feature_code` | Stable logical feature code |
| `t_code` | Optional quick navigation code shown/typed by users |
| `feature_name` | Display label |
| `route` | Frontend route, relative to the app root |
| `icon` | Safe icon key |
| `physical_module_code` | Optional physical module code |
| `physical_submodule_code` | Optional physical submodule code |
| `physical_feature_type_code` | Optional physical feature type code |
| `physical_feature_code` | Optional physical feature code |
| `required_privilege_codes` | Use join table in implementation, not comma-separated text |
| `display_order` | Sort order |
| `active` | Boolean |

### `sys_layout_feature_privileges`

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_feature_id` | FK to `sys_layout_features` |
| `privilege_id` | FK to `sys_priv_privileges` |
| `match_mode` | `ANY` or `ALL`; default `ANY` |
| `active` | Boolean |

`match_mode` defines how visibility is evaluated when one layout feature is linked to multiple privileges. `ANY` shows the feature when the user has at least one linked privilege. `ALL` shows the feature only when the user has every linked privilege. Use `ANY` as the Phase 1 default; `ALL` is for stricter business workflows where partial permission should not expose the menu item.

All logical navigation tables must include `created_by`, `updated_by`, `created_at`, and `updated_at`.

## Layout Codes And T-Code Generation

Logical layout records should have stable generated codes in addition to database primary keys. These internal codes are used for API payloads, seed data, client overrides, imports, exports, and frontend selection state.

Layout features may also expose a `t_code` for user quick navigation. A T-code is a short user-facing command that can be typed into a command palette, search box, or quick navigation field to open a feature directly.

Internal layout code rules:

- Generate codes on the backend; do not let the frontend create persistent layout codes.
- Keep generated codes stable after creation. Rename display labels without changing codes.
- Use uppercase snake case for human-readable business codes when manually configured, for example `CUSTOMER_ONBOARDING`.
- Use numeric sequence suffixes when the system auto-generates codes for layout records.
- Do not reuse deleted codes unless a controlled migration explicitly restores the same logical record.

T-code generation rules:

- Generate or validate `t_code` values on the backend; do not let the frontend invent persistent T-codes.
- Keep `t_code` short, unique within a client layout scope, and stable enough for users to learn.
- Reserve T-code numeric values `001`-`100` for system UI quick navigation and platform-owned default screens.
- Client/business-created T-codes must start from `101`.
- T-code navigation must still pass normal visibility checks: client access, license entitlement, active route state, and user privileges.

Recommended T-code reserved range:

| Range | Owner | Example Use |
| --- | --- | --- |
| `001`-`100` | System UI | Platform-owned quick links such as dashboard, profile, settings, default KYC screens |
| `101` and above | Client/business configuration | Client-specific quick links for configured feature leaves |

Example internal layout codes:

```text
COMPLIANCE
KYC
OPERATION
PERSON_KYC
CUSTOMER_REGISTRATION

CLIENT_APPROVALS
CLIENT_ANALYTICS
CLIENT_CUSTOMER_REVIEW
```

Example T-codes:

```text
SYS001
SYS002
KYC101
SET101
RPT101
```

## Relationship With Client Applications

Layout configuration is assigned to `sys_acc_client_applications`.

```text
WEB -> default authenticated sidebar layout
PRIVILEGE_WEB -> compact admin layout
POS_WEB -> horizontal sales layout
MOBILE_WEB -> mobile-first bottom navigation layout
```

The client code is resolved from one of these sources, in order:

1. Authenticated `ClientApplicationContextHolder` when API key/client credentials are used.
2. `X-Client-Code` request header.
3. Auth policy/default client configuration for public login and SSO configuration APIs.
4. System default layout when no client can be resolved.

## Relationship With Application Context

The existing application context response should include effective layout configuration after login and for public login screens where branding is needed.

Recommended response shape:

```json
{
  "clientCode": "WEB",
  "clientType": "FIRST_PARTY_WEB",
  "menus": [],
  "privilegeCodes": [],
  "layout": {
    "activeProfileCode": "WEB_DEFAULT",
    "navTree": [],
    "availableProfiles": [
      {
        "code": "WEB_DEFAULT",
        "name": "Default",
        "layoutType": "SIDEBAR",
        "navigationMode": "SIDEBAR",
        "themeMode": "LIGHT",
        "density": "COMFORTABLE",
        "topbarEnabled": true,
        "sidebarEnabled": true,
        "sidebarCollapsed": false,
        "rtlEnabled": false,
        "brand": {
          "displayName": "NexaCore KYC",
          "logoUrl": "/assets/brand/nexacore.svg",
          "faviconUrl": "/assets/brand/favicon.ico"
        }
      }
    ],
    "themes": [
      {
        "id": "purple",
        "label": "Purple Corporate",
        "base": "light",
        "swatch": "#6b3fa0",
        "primaries": {
          "text": "#1a222c",
          "paper": "#f3f5f7",
          "card": "#ffffff",
          "accent": "#6b3fa0",
          "amber": "#a8630b",
          "red": "#9f2b2b",
          "success": "#1c7a4c",
          "info": "#2f7dd1"
        }
      }
    ],
    "sizes": {
      "spaceUnit": 2,
      "radiusBase": 8,
      "fontSizeBase": 13.5,
      "headerHeight": 58,
      "statusBarHeight": 28,
      "railWidthCollapsed": 64,
      "railWidthExpanded": 230
    },
    "fonts": {
      "bodyFamily": "Inter",
      "headingFamily": "Inter",
      "monoFamily": "JetBrains Mono",
      "fontSource": "SYSTEM",
      "fallbackStack": "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
    }
  }
}
```

`PrivilegeServiceImpl#getApplicationContext` should call `LayoutContextService` and attach the effective layout response. Public APIs such as `/api/v1/auth/config` or `/api/v1/auth/application-context/public` may return a reduced public layout configuration for login branding.

## Proposed Package Structure

Add a new package under `systemmodule`:

```text
com.nexacore.systemmodule.layout
├── controller
├── dto
├── entity
├── enums
├── repository
├── service
│   ├── interfaces
│   └── implementations
└── validator
```

Recommended class responsibilities:

| Package | Responsibility |
| --- | --- |
| `controller` | Admin APIs to create profiles, assign profiles to clients, and fetch effective layout context |
| `dto` | Request and response DTOs; never expose entities directly |
| `entity` | `sys_layout_*` tables in `system_db` |
| `enums` | Layout type, navigation mode, theme mode, density, device target, assignment scope |
| `repository` | System database repositories |
| `service/interfaces` | Contracts consumed by auth, privilege context, and controllers |
| `service/implementations` | Effective layout resolution, validation, assignment, and profile management |
| `validator` | Token validation, URL validation, allowed option validation |

## Core Concepts

### Layout Profile

A reusable layout option that can be assigned to one or more client applications.

Examples:

```text
WEB_DEFAULT
WEB_COMPACT
PRIVILEGE_ADMIN
POS_COUNTER
MOBILE_BOTTOM_NAV
```

### Client Layout Assignment

Maps a client application to one or more allowed layout profiles and marks the default profile.

Rules:

- A client must have exactly one default active layout profile.
- A client may have multiple selectable profiles if enabled.
- Inactive profiles must not be returned in application context.
- A client assignment can optionally target role codes, privilege codes, device type, or module code in later phases.

### Theme Primaries And Size Primitives

Theme configuration should follow the Sentinel Angular 21 layout model. The backend stores semantic theme inputs, not raw CSS variables.

Use `themes[]` with `primaries`:

```json
{
  "id": "purple",
  "label": "Purple Corporate",
  "base": "light",
  "swatch": "#6b3fa0",
  "primaries": {
    "text": "#1a222c",
    "paper": "#f3f5f7",
    "card": "#ffffff",
    "accent": "#6b3fa0",
    "amber": "#a8630b",
    "red": "#9f2b2b",
    "success": "#1c7a4c",
    "info": "#2f7dd1"
  }
}
```

Use optional `chromeOverrides` only when a specific header/rail/status-bar surface cannot be derived cleanly from the theme primaries:

```json
{
  "chromeOverrides": {
    "accentSoft": "#2e2510",
    "bg": "#0b1a33",
    "border": "#1c2f4d",
    "borderStrong": "#081527",
    "hoverBg": "#142848",
    "activeBg": "#142848",
    "searchBg": "#0f2140",
    "searchBorder": "#1c2f4d",
    "searchText": "#ffffff",
    "searchPlaceholder": "#7488a8"
  }
}
```

Use `sizes` for layout scale:

```json
{
  "spaceUnit": 2,
  "radiusBase": 8,
  "fontSizeBase": 13.5,
  "headerHeight": 58,
  "statusBarHeight": 28,
  "railWidthCollapsed": 64,
  "railWidthExpanded": 230
}
```

Use `fonts` for typography selection:

```json
{
  "bodyFamily": "Inter",
  "headingFamily": "Inter",
  "monoFamily": "JetBrains Mono",
  "fontSource": "SYSTEM",
  "fallbackStack": "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
}
```

Backend font configuration must stay as controlled metadata: family names, a source type, an optional allowlisted asset URL, and a fallback stack. Do not store arbitrary `@font-face` CSS or script-based font loaders in the database.

The frontend should compute final CSS custom properties from these semantic inputs, following the Sentinel `computeThemeTokens()` and `computeSizeTokens()` approach, plus font variables such as `--layout-font-body`, `--layout-font-heading`, and `--layout-font-mono`.

Do not store arbitrary CSS, JavaScript, raw style blocks, or uncontrolled custom-property maps in the database. A 3-color contract such as `primaryColor`, `accentColor`, and `surfaceColor` is not sufficient for this layout system because it cannot safely derive text contrast, page/card surfaces, status colors, dark themes, or chrome-specific states.

## Database Design

All persistent tables must include `created_by`, `updated_by`, `created_at`, and `updated_at`.

### `sys_layout_profiles`

Stores reusable layout profile definitions.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `profile_code` | Unique stable code |
| `profile_name` | Display name for admin UI |
| `description` | Optional description |
| `layout_type` | `SIDEBAR`, `HORIZONTAL`, `RAIL`, `COMPACT`, `PUBLIC` |
| `navigation_mode` | `SIDEBAR`, `HORIZONTAL`, `RAIL`, `BOTTOM_NAV`, `NONE` |
| `theme_mode` | `LIGHT`, `DARK`, `SYSTEM` |
| `density` | `COMFORTABLE`, `COMPACT`, `DENSE` |
| `topbar_enabled` | Boolean |
| `sidebar_enabled` | Boolean |
| `sidebar_collapsed` | Boolean default |
| `footer_enabled` | Boolean |
| `breadcrumb_enabled` | Boolean |
| `command_bar_enabled` | Boolean |
| `rtl_enabled` | Boolean |
| `active` | Boolean |

`rtl_enabled` controls whether the frontend should render the layout in right-to-left direction for RTL languages such as Arabic, Hebrew, Persian, or Urdu. Keep it `false` for normal left-to-right clients; when `true`, the frontend should mirror directional layout behavior such as menu alignment, sidebar placement, spacing, and directional icons.

### `sys_layout_profile_themes`

Stores selectable themes for a layout profile.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `theme_id` | Stable frontend-safe id such as `light`, `dark`, `purple`, `navy` |
| `theme_label` | Display label |
| `base` | `LIGHT` or `DARK` |
| `swatch` | Hex swatch or safe symbolic value such as `sun` or `moon` |
| `active` | Boolean |

### `sys_layout_theme_primaries`

Stores semantic theme colors used by the frontend token engine.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_theme_id` | FK to `sys_layout_profile_themes` |
| `text_color` | `primaries.text` |
| `paper_color` | `primaries.paper` |
| `card_color` | `primaries.card` |
| `accent_color` | `primaries.accent` |
| `amber_color` | `primaries.amber` |
| `red_color` | `primaries.red` |
| `success_color` | `primaries.success` |
| `info_color` | `primaries.info` |
| `active` | Boolean |

### `sys_layout_theme_chrome_overrides`

Stores optional chrome overrides for exceptional themes.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_theme_id` | FK to `sys_layout_profile_themes` |
| `accent_soft` | Optional `chromeOverrides.accentSoft` |
| `background` | Optional `chromeOverrides.bg` |
| `border` | Optional `chromeOverrides.border` |
| `border_strong` | Optional `chromeOverrides.borderStrong` |
| `hover_background` | Optional `chromeOverrides.hoverBg` |
| `active_background` | Optional `chromeOverrides.activeBg` |
| `search_background` | Optional `chromeOverrides.searchBg` |
| `search_border` | Optional `chromeOverrides.searchBorder` |
| `search_text` | Optional `chromeOverrides.searchText` |
| `search_placeholder` | Optional `chromeOverrides.searchPlaceholder` |
| `active` | Boolean |

### `sys_layout_profile_sizes`

Stores layout scale primitives for a profile.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `space_unit` | Base spacing unit |
| `radius_base` | Base border radius |
| `font_size_base` | Base font size |
| `header_height` | Header height |
| `status_bar_height` | Status bar height |
| `rail_width_collapsed` | Collapsed rail width |
| `rail_width_expanded` | Expanded rail width |
| `active` | Boolean |

### `sys_layout_profile_fonts`

Stores controlled typography metadata for a profile.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `body_family` | Main UI/body font family |
| `heading_family` | Optional heading font family |
| `mono_family` | Optional monospace font family |
| `font_source` | `SYSTEM`, `ASSET`, `GOOGLE`, or `URL` |
| `font_url` | Optional font stylesheet or asset URL; must be allowlisted when used |
| `fallback_stack` | Safe fallback font stack |
| `active` | Boolean |

Font records must not contain raw CSS. The frontend should translate this metadata into controlled CSS variables and, only when allowed, load known font assets.

### `sys_layout_profile_branding`

Stores branding metadata for a profile.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `display_name` | Brand/application display name |
| `short_name` | Optional compact name |
| `logo_url` | Safe relative or configured public asset URL |
| `logo_dark_url` | Optional dark-mode logo |
| `favicon_url` | Optional favicon |
| `support_url` | Optional help/support link |
| `active` | Boolean |

### `sys_client_layout_profiles`

Assigns layout profiles to client applications.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `client_application_id` | FK to `sys_acc_client_applications` |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `default_profile` | Boolean |
| `selectable` | Boolean |
| `assignment_scope` | `CLIENT`, `ROLE`, `PRIVILEGE`, `DEVICE`, `MODULE` |
| `scope_value` | Optional role, privilege, device, or module code |
| `display_order` | Sort order for selectable profiles |
| `active` | Boolean |

Unique constraints:

- `(client_application_id, layout_profile_id, assignment_scope, scope_value)`
- One active default profile per client application for `assignment_scope = CLIENT`

## Service Contracts

Recommended interfaces:

```java
public interface LayoutProfileService {
    LayoutProfileDto saveProfile(LayoutProfileRequestDto request, String actor);
    List<LayoutProfileDto> listProfiles();
    LayoutProfileDto getProfile(String profileCode);
}

public interface ClientLayoutService {
    void assignProfiles(ClientLayoutAssignmentRequestDto request, String actor);
    List<ClientLayoutAssignmentDto> listAssignments(String clientCode);
}

public interface LayoutContextService {
    LayoutContextDto getEffectiveLayout(String clientCode, String username);
    PublicLayoutContextDto getPublicLayout(String clientCode, String origin);
}

public interface LayoutNavigationService {
    LayoutNavigationTreeDto getNavigationTree(String clientCode, String username);
    LayoutNavigationTreeDto getPreviewNavigationTree(LayoutNavigationPreviewRequestDto request);
    LayoutNavigationCategoryDto saveCategory(LayoutNavigationCategoryRequestDto request, String actor);
    void reorderCategories(LayoutNavigationCategoryOrderRequestDto request, String actor);
}
```

`LayoutContextService` should be the only layout service consumed by auth and privilege context code.

`LayoutNavigationService` should own logical navigation composition. It should not ask the frontend to infer grouping from `ApplicationModule`, `ApplicationSubmodule`, or `FeatureType`.

## API Design

All endpoints must use `/api/v1`.

Admin endpoints:

```text
POST /api/v1/system/layout/profile/save
POST /api/v1/system/layout/profile/list
POST /api/v1/system/layout/profile/detail
POST /api/v1/system/layout/client/assign
POST /api/v1/system/layout/client/list
POST /api/v1/system/layout/context
POST /api/v1/system/layout/navigation/group/save
POST /api/v1/system/layout/navigation/module/save
POST /api/v1/system/layout/navigation/category/save
POST /api/v1/system/layout/navigation/category/list
POST /api/v1/system/layout/navigation/category/reorder
POST /api/v1/system/layout/navigation/feature-group/save
POST /api/v1/system/layout/navigation/feature/save
POST /api/v1/system/layout/navigation/tree
```

Backend-to-frontend contract:

- Backend returns `layout.navTree` as the final logical navigation tree.
- Frontend renders `layout.navTree` exactly as received after applying only presentational behavior such as expansion/collapse.
- Frontend must not derive categories from `FeatureType`.
- Frontend must not assume category codes or labels such as `OPERATION`, `SETUP`, or `REPORT`.
- Frontend may use `type: "category"` to choose the visual row style, but the row label, icon, order, and children come from backend data.

Public/auth integration:

```text
POST /api/v1/auth/application-context/public
POST /api/v1/system/privilege/context
```

`/api/v1/system/layout/context` is useful for admin preview and troubleshooting. Normal frontend bootstrapping should use the existing application context endpoint to avoid extra round trips.

## Privilege Model

Add system privileges for layout administration:

```text
System / Layout / Setup / Layout Profile / View
System / Layout / Setup / Layout Profile / Create
System / Layout / Setup / Layout Profile / Update
System / Layout / Setup / Client Layout Assignment / View
System / Layout / Setup / Client Layout Assignment / Update
System / Layout / Setup / Logical Navigation / View
System / Layout / Setup / Logical Navigation / Create
System / Layout / Setup / Logical Navigation / Update
```

`systemmodule.layout` should publish these through a `LayoutPrivilegeProvider` implementing the existing `ModulePrivilegeProvider` pattern.

## Frontend Companion Plan

Angular 21 frontend changes are tracked separately in `LAYOUT_FRONTEND_CHANGE_PLAN.md`.

The backend contract in this plan must continue to support that frontend plan by returning:

- `layout.activeProfileCode`
- `layout.availableProfiles`
- `layout.navTree`
- `layout.themes`
- `layout.sizes`
- `layout.fonts`
- backend-provided `tCode` values on routable feature nodes

## Validation Rules

- `profile_code` must be uppercase snake case.
- Internal generated layout/navigation codes must be backend-owned and stable after creation.
- `t_code` must be uppercase, short, unique within the effective client layout scope, and assigned only to routable feature nodes.
- T-code numeric values `001`-`100` are reserved for system UI quick navigation; client/business-created T-codes must use `101` and above.
- Colors must be hex, rgb, hsl, or predefined safe token values.
- URLs must be relative asset paths or match configured public asset host allowlists.
- Font family names and fallback stacks must match a safe character allowlist.
- `font_url` is allowed only for approved `font_source` values and configured asset/font host allowlists.
- A profile must not enable conflicting navigation modes.
- Public layout profiles must not expose authenticated menu configuration.
- Only active profiles can be assigned to clients.
- A client cannot have more than one active client-scope default profile.

## Security Notes

- Layout configuration is presentation metadata only. Do not use it for backend authorization decisions.
- Never store raw CSS blocks, scripts, HTML snippets, or user-provided SVG markup in layout tables.
- Treat logo and asset URLs as untrusted until validated.
- Public layout context must not leak menus, privilege codes, tenant data, API keys, or internal client secrets.
- Admin layout APIs require user authentication and layout-management privileges.

## Migration Plan

1. Create `sys_layout_profiles`.
2. Create `sys_layout_profile_themes`.
3. Create `sys_layout_theme_primaries`.
4. Create `sys_layout_theme_chrome_overrides`.
5. Create `sys_layout_profile_sizes`.
6. Create `sys_layout_profile_fonts`.
7. Create `sys_layout_profile_branding`.
8. Create `sys_client_layout_profiles`.
9. Create logical navigation tables:
   - `sys_layout_module_groups`
   - `sys_layout_navigation_modules`
   - `sys_layout_navigation_categories`
   - `sys_layout_feature_groups`
   - `sys_layout_features`
   - `sys_layout_feature_privileges`
10. Seed default profiles:
   - `WEB_DEFAULT`
   - `WEB_COMPACT`
   - `PRIVILEGE_ADMIN`
   - `PUBLIC_LOGIN`
11. Seed Sentinel-compatible default themes, font metadata, and size primitives for each default profile.
12. Seed default logical categories for the initial client profile:
   - `OPERATION`
   - `SETUP`
   - `REPORT`
13. Seed system UI quick-navigation T-codes using the reserved `001`-`100` range.
14. Configure T-code generation so client/business-created quick-navigation codes start from `101`.
15. Seed initial logical navigation from existing KYC, Auth, System License, and Services privilege providers.
16. Assign `WEB_DEFAULT` to the default `WEB` client application.
17. Assign `PRIVILEGE_ADMIN` to the privilege frontend client if that client exists.

Use system actor values such as `system` or `migration` for seed data `created_by` and `updated_by`.

## Implementation Phases

### Phase 1: Backend Contract And Defaults

- Add entities, repositories, enums, DTOs, and migrations.
- Add profile save/list/detail APIs.
- Add client assignment APIs.
- Add `LayoutCodeGenerationService` for stable generated logical layout codes.
- Add T-code generation and validation for user quick navigation, including the reserved system UI `001`-`100` range.
- Add theme, primary color, chrome override, font metadata, and size primitive persistence.
- Add logical navigation entities, repositories, DTOs, and admin APIs.
- Add configurable logical category management; do not model categories as Java enums.
- Add `LayoutContextService`.
- Add `LayoutNavigationService`.
- Attach effective layout to authenticated and public application context responses.
- Attach filtered logical `navTree` to authenticated application context responses.
- Seed default profiles.
- Seed initial logical navigation tree from existing privilege/menu definitions.

### Phase 1B: Privilege Catalog Alignment

- Keep `ApplicationModule` as the physical code-ownership enum for compatibility.
- Add explicit logical navigation metadata instead of using `FeatureType` as a frontend navigation category.
- Update `PrivilegeFeatureDefinitionDto` or add a sibling DTO so providers can optionally publish:
  - logical module group code/name
  - logical navigation module code/name
  - logical category code/name
  - logical feature group code/name
  - route
  - icon
  - display order
- Update `SystemPrivilegeRegistryServiceImpl` or a new `LayoutNavigationSyncService` to sync provider metadata into logical navigation tables.
- Make `FeatureType` a privilege/catalog classification only. It may map to a seeded logical category by default, but the mapping must be overridable in DB.
- Keep existing `sys_priv_sub_menus` for backward compatibility until Angular 21 consumes `layout.navTree`.
- Mark `PrivilegeServiceImpl#getUserSidebarMenu` as transitional once `LayoutNavigationService` is active.

### Phase 2: Backend Contract Stabilization

- Keep `/api/v1/system/privilege/context` backward compatible while adding `layout`.
- Keep transitional `menus` response until Angular 21 fully consumes `layout.navTree`.
- Add `/api/v1/system/layout/context` for admin preview and troubleshooting.
- Ensure `layout.navTree` returns only visible nodes after client, license, route, and privilege filtering.
- Ensure routable feature nodes include backend-generated or backend-validated `tCode` values when configured.
- Ensure response DTOs use the same shape documented in `LAYOUT_FRONTEND_CHANGE_PLAN.md`.
- Add cache keys and invalidation rules for client layout assignments, profiles, navigation tree, themes, fonts, and sizes.

### Phase 3: Backend Admin Workflows

- Add backend support for layout profile management workflows.
- Add backend support for client layout assignment workflows.
- Add preview mode using `/api/v1/system/layout/context`.
- Add validation messages for invalid theme primaries, chrome overrides, font metadata, size primitives, and conflicting options.

### Phase 4: Advanced Targeting

- Add role, privilege, device, and module scoped layout assignments.
- Add tenant/business overrides only if client-level layout is not enough.
- Add audit trail for layout changes.
- Add cache invalidation for layout context.

## Testing Expectations

Backend:

- Unit test effective layout resolution.
- Unit test one-default-profile validation.
- Unit test public layout does not expose authenticated metadata.
- Controller tests for admin APIs and privilege enforcement.
- Migration smoke test through `mvn test`.

Frontend verification is tracked in `LAYOUT_FRONTEND_CHANGE_PLAN.md`.

## Open Questions

- Should profile selection be per user, per role, per client, or all three?
- Should uploaded logos live in MinIO/file service or only reference pre-approved frontend assets?
- Should theme primaries, font metadata, and size primitives support per-tenant override later, or should client application remain the highest configuration scope?
- Does mobile need a separate client code or device-scoped assignment under the same client?
- Should layout changes take effect immediately through context refresh or only after the next login?
