# Layout Submodule Business And Implementation Plan

## Purpose

The layout submodule will extend `systemmodule` with backend-owned, client-specific layout configuration for NexaCore frontend applications. Each client application can choose from multiple layout options, themes, navigation modes, density presets, and branding rules without requiring a frontend rebuild.

This plan is written for the current project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Target module: `com.nexacore.systemmodule.layout`
- Existing client access package: `com.nexacore.systemmodule.privilege.accesscontrol`
- Existing privilege/application context package: `com.nexacore.systemmodule.privilege`
- Target Angular 21 shared frontend library: `frontendApplications/frontend-libs-21/`
- Target Angular 21 KYC frontend: `frontendApplications/kyc-frontend-21/`
- API response wrappers and common DTOs: `commonmodule`
- Flyway migration location: `src/main/resources/db/migration/system`

## Business Goals

- Let platform administrators configure layout options per client application.
- Support multiple frontend shells such as sidebar, horizontal navigation, rail navigation, compact operations layout, and public login layout.
- Return the effective layout configuration from the backend application context so the frontend renders configuration instead of hardcoding client behavior.
- Keep branding, theme, navigation mode, density, and feature visibility consistent between the Angular 21 shared library and `kyc-frontend-21`.
- Allow a client to have a default layout and optional selectable layout profiles for different user roles, devices, or modules.
- Keep layout configuration separate from user authorization. Layout may hide or arrange UI, but backend privilege and API access remain authoritative.

## Ownership Boundary

Layout configuration belongs in `systemmodule` because it is platform presentation governance for client applications.

```text
systemmodule/layout owns:
- layout profiles
- client layout assignments
- theme tokens and branding metadata
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
- runtime application of theme and density tokens
- responsive behavior
```

The backend must not store Angular component names, CSS implementation details, or frontend-only state that changes during a session. The frontend must not decide which commercial or client-specific layout profiles are available.

## Relationship With Client Applications

Layout configuration is assigned to `sys_client_applications`.

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
        },
        "tokens": {
          "primaryColor": "#0f766e",
          "accentColor": "#2563eb",
          "surfaceColor": "#ffffff"
        }
      }
    ]
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

### Theme Tokens

Tokens are safe presentation values consumed by frontend CSS variables.

Recommended initial tokens:

```text
primary_color
accent_color
surface_color
text_color
sidebar_background
topbar_background
border_color
```

Do not store arbitrary CSS or JavaScript in the database.

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

### `sys_layout_profile_tokens`

Stores validated token key/value pairs.

| Column | Notes |
| --- | --- |
| `id` | Primary key |
| `layout_profile_id` | FK to `sys_layout_profiles` |
| `token_key` | Stable token name |
| `token_value` | Validated value |
| `active` | Boolean |

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
| `client_application_id` | FK to `sys_client_applications` |
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
```

`LayoutContextService` should be the only layout service consumed by auth and privilege context code.

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
```

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
```

`systemmodule.layout` should publish these through a `LayoutPrivilegeProvider` implementing the existing `ModulePrivilegeProvider` pattern.

## Angular 21 Frontend Contract

Extend `frontendApplications/frontend-libs-21/` to consume `layout` from application context. The legacy Angular 19 shared library is out of scope for this work.

Recommended frontend model:

```ts
export type LayoutType = 'SIDEBAR' | 'HORIZONTAL' | 'RAIL' | 'COMPACT' | 'PUBLIC';
export type NavigationMode = 'SIDEBAR' | 'HORIZONTAL' | 'RAIL' | 'BOTTOM_NAV' | 'NONE';
export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';
export type LayoutDensity = 'COMFORTABLE' | 'COMPACT' | 'DENSE';

export interface LayoutContext {
  activeProfileCode: string;
  availableProfiles: LayoutProfile[];
}

export interface LayoutProfile {
  code: string;
  name: string;
  layoutType: LayoutType;
  navigationMode: NavigationMode;
  themeMode: ThemeMode;
  density: LayoutDensity;
  topbarEnabled: boolean;
  sidebarEnabled: boolean;
  sidebarCollapsed: boolean;
  rtlEnabled: boolean;
  brand?: LayoutBrand;
  tokens?: Record<string, string>;
}
```

Frontend behavior:

- Apply the default active profile after login/application-context load.
- Persist only the selected profile code locally, not the full backend profile.
- Revalidate selected profile against `availableProfiles` on every context refresh.
- Fall back to a built-in safe default if the backend layout is missing or invalid.
- Do not execute arbitrary CSS, HTML, SVG, or JavaScript from layout configuration.

## Validation Rules

- `profile_code` must be uppercase snake case.
- Colors must be hex, rgb, hsl, or predefined safe token values.
- URLs must be relative asset paths or match configured public asset host allowlists.
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
2. Create `sys_layout_profile_tokens`.
3. Create `sys_layout_profile_branding`.
4. Create `sys_client_layout_profiles`.
5. Seed default profiles:
   - `WEB_DEFAULT`
   - `WEB_COMPACT`
   - `PRIVILEGE_ADMIN`
   - `PUBLIC_LOGIN`
6. Assign `WEB_DEFAULT` to the default `WEB` client application.
7. Assign `PRIVILEGE_ADMIN` to the privilege frontend client if that client exists.

Use system actor values such as `system` or `migration` for seed data `created_by` and `updated_by`.

## Implementation Phases

### Phase 1: Backend Contract And Defaults

- Add entities, repositories, enums, DTOs, and migrations.
- Add profile save/list/detail APIs.
- Add client assignment APIs.
- Add `LayoutContextService`.
- Attach effective layout to authenticated and public application context responses.
- Seed default profiles.

### Phase 2: Frontend Consumption

- Extend `frontendApplications/frontend-libs-21/` models and layout renderer services.
- Integrate the resolved layout context in `frontendApplications/kyc-frontend-21/`.
- Apply layout type, navigation mode, theme, density, and branding from backend context.
- Support client profile selection when `availableProfiles` has multiple selectable profiles.
- Keep fallback defaults for offline or misconfigured layout context.

### Phase 3: Admin UI

- Add layout profile management screens in the Angular 21 admin/client-management frontend when that application is selected.
- Add client layout assignment screens.
- Add preview mode using `/api/v1/system/layout/context`.
- Add validation messages for invalid tokens and conflicting options.

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

Frontend:

- Build `frontendApplications/frontend-libs-21/` and `frontendApplications/kyc-frontend-21/` after shared layout changes.
- Test fallback behavior when layout context is missing.
- Test switching between allowed profiles.
- Test that invalid selected local profile is ignored.

## Open Questions

- Should profile selection be per user, per role, per client, or all three?
- Should uploaded logos live in MinIO/file service or only reference pre-approved frontend assets?
- Should theme tokens support per-tenant override later, or should client application remain the highest configuration scope?
- Does mobile need a separate client code or device-scoped assignment under the same client?
- Should layout changes take effect immediately through context refresh or only after the next login?
