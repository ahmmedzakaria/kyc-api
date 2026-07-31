# Layout Frontend Change Plan

## Purpose

This plan defines the Angular 21 frontend changes needed to consume backend-owned layout configuration from `systemmodule.layout`.

Scope:

- `frontendApplications/frontend-libs-21/`
- `frontendApplications/kyc-frontend-21/`

Excluded:

- Legacy `frontend-libs/`
- Backend entity, migration, and service implementation details

The target shape follows the Sentinel `layout-config.json` model: backend-provided `layout.navTree`, `themes[].primaries`, optional `themes[].chromeOverrides`, `sizes`, `fonts`, and layout profile metadata.

## Current Compatibility

| Area | Current State | Required Change |
| --- | --- | --- |
| API base path | `@nexacore/api-common` uses `/api/v1` | Keep this path; remove or update stale app-local `/api` constants if still used |
| Application context | `SidebarMenuService` loads `menus`, `enabledModules`, `enabledSubmodules`, and `enabledFeatures` | Extend context model to read `layout` from `/api/v1/system/privilege/context` |
| Navigation tree | `RailNavComponent` reconstructs modules/categories from backend `menus` | Render backend `layout.navTree` directly |
| Categories | `Operation`, `Setup`, and `Report` are TypeScript union values and label-matched in code | Replace with backend `NavNode` records where category labels/order/icons are data |
| Layout mode | `LayoutService` supports `default`, `compact`, and `horizontal` locally | Replace with backend profile fields: `layoutType`, `navigationMode`, `density`, and shell flags |
| Themes | `ThemeService` uses static `THEMES` and toggles body attributes | Load backend `themes`; compute CSS variables from `primaries` and `chromeOverrides` |
| Sizes | SCSS has CSS custom properties but no backend size application | Compute and apply size variables from backend `sizes` |
| Fonts | CSS has font variables but no backend font config | Apply font variables from backend `fonts`; only load allowlisted font URLs |
| RTL | `DirectionService` derives RTL from language only | Allow active profile `rtlEnabled` to set/override document direction |
| T-code quick navigation | Not available in `frontend-libs-21` | Add quick navigation over visible feature nodes using backend `tCode` values |
| Header/status bar | Header apps, tenants, search types, notifications, and status labels are mostly local/static | Move configurable header/status metadata behind layout context when backend exposes it |

## Frontend Contract

```ts
export type LayoutType = 'SIDEBAR' | 'HORIZONTAL' | 'RAIL' | 'COMPACT' | 'PUBLIC';
export type NavigationMode = 'SIDEBAR' | 'HORIZONTAL' | 'RAIL' | 'BOTTOM_NAV' | 'NONE';
export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';
export type LayoutDensity = 'COMFORTABLE' | 'COMPACT' | 'DENSE';
export type FontSource = 'SYSTEM' | 'ASSET' | 'GOOGLE' | 'URL';

export interface LayoutContext {
  activeProfileCode: string;
  availableProfiles: LayoutProfile[];
  navTree: NavNode[];
  themes: ThemeConfigEntry[];
  sizes: SizeConfig;
  fonts: FontConfig;
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
}

export interface ThemeColorPrimaries {
  text: string;
  paper: string;
  card: string;
  accent: string;
  amber: string;
  red: string;
  success: string;
  info: string;
}

export interface ThemeChromeOverrides {
  accentSoft?: string;
  bg?: string;
  border?: string;
  borderStrong?: string;
  hoverBg?: string;
  activeBg?: string;
  searchBg?: string;
  searchBorder?: string;
  searchText?: string;
  searchPlaceholder?: string;
}

export interface ThemeConfigEntry {
  id: string;
  label: string;
  base: 'light' | 'dark';
  swatch: string;
  primaries: ThemeColorPrimaries;
  chromeOverrides?: ThemeChromeOverrides;
}

export interface SizeConfig {
  spaceUnit: number;
  radiusBase: number;
  fontSizeBase: number;
  headerHeight: number;
  statusBarHeight: number;
  railWidthCollapsed: number;
  railWidthExpanded: number;
}

export interface FontConfig {
  bodyFamily: string;
  headingFamily?: string;
  monoFamily?: string;
  fontSource: FontSource;
  fontUrl?: string;
  fallbackStack: string;
}

export type NavNodeType = 'group' | 'module' | 'category' | 'featureGroup' | 'feature';

export interface NavNode {
  code?: string;
  tCode?: string;
  label: string;
  type: NavNodeType;
  icon?: string;
  route?: string;
  privilegeCodes?: string[];
  children?: NavNode[];
}
```

## `frontendApplications/frontend-libs-21/` Changes

Add shared layout models:

```text
projects/layout/src/lib/core/models/layout-context.model.ts
projects/layout/src/lib/core/models/nav-tree.model.ts
projects/layout/src/lib/core/models/layout-profile.model.ts
```

Model rules:

- Keep `NavNode.type` generic: `group`, `module`, `category`, `featureGroup`, `feature`.
- Include `code`, `tCode`, `label`, `icon`, `route`, `privilegeCodes`, and `children`.
- Do not model categories as `Operation | Setup | Report`.
- Keep theme models compatible with Sentinel `ThemeColorPrimaries`, `ThemeChromeOverrides`, `ThemeConfigEntry`, and `SizeConfig`.
- Add `FontConfig` for backend-provided font metadata.

Add a backend-aware layout context service:

```text
projects/layout/src/lib/core/services/layout-context.service.ts
```

Responsibilities:

- Load the authenticated application context from `/api/v1/system/privilege/context`.
- Unwrap both direct and `{ data }` response envelopes.
- Expose signals/computed values for active profile, available profiles, visible `navTree`, themes, sizes, fonts, branding, and `rtlEnabled`.
- Cache only safe client state: selected profile code, collapsed state, selected theme id, and last language.
- Keep `SidebarMenuService` as a transitional adapter until all consumers switch to `LayoutContextService`.

Refactor navigation rendering:

- Replace `NavCategory = 'Operation' | 'Setup' | 'Report'` with backend `NavNode`.
- Update `RailNavComponent` to traverse the five-level backend tree: group, module, category, feature group, feature.
- Render category labels, icons, order, and children exactly as returned by backend.
- Navigate only from nodes with `type: 'feature'` and a valid `route`.
- Keep existing `menus` adapter as fallback during backend migration, but mark it transitional.

Add runtime token application:

```text
projects/layout/src/lib/core/theme/color-math.ts
projects/layout/src/lib/core/theme/font-tokens.ts
```

Responsibilities:

- Port Sentinel `computeThemeTokens()` and `computeSizeTokens()` into `frontend-libs-21`.
- Add `computeFontTokens()` to produce controlled variables such as `--layout-font-body`, `--layout-font-heading`, and `--layout-font-mono`.
- Apply computed variables as inline CSS custom properties on `document.body`.
- Keep `_tokens.scss` fallback defaults for login/public pages and failed context loads.

Update shell/profile state:

- Expand `LayoutService` to store the active backend `LayoutProfile`.
- Map backend `layoutType` and `navigationMode` to shell rendering decisions.
- Use `topbarEnabled`, `sidebarEnabled`, `sidebarCollapsed`, `footerEnabled`, `breadcrumbEnabled`, and `commandBarEnabled` when the backend starts returning them.
- Support multiple `availableProfiles` by exposing a profile switch API that persists only the selected profile code.

Update direction handling:

- Let `DirectionService` consume `rtlEnabled`.
- If `rtlEnabled` is explicitly `true`, set `dir="rtl"`.
- If `rtlEnabled` is explicitly `false`, set `dir="ltr"` unless the product decides language should override profile.
- Keep language-driven RTL as fallback before layout context loads.

Add T-code quick navigation:

```text
projects/layout/src/lib/core/services/quick-nav.service.ts
```

Responsibilities:

- Build an index from visible `layout.navTree` feature nodes only.
- Use backend-provided `tCode`; do not generate persistent T-codes in frontend.
- Normalize user input for search, but preserve backend `tCode` for display.
- Resolve exact T-code matches first, then optionally search labels for suggestions.
- Navigate only when the matched feature is visible and has a route.

Update public exports:

- Export new models and services from `projects/layout/src/public-api.ts`.
- Preserve existing exports until consuming apps are migrated.

## `frontendApplications/kyc-frontend-21/` Changes

Bootstrap and auth flow:

- Keep `API_ENVIRONMENT` values on `/api/v1`.
- After login and SSO callback, ensure the layout context is loaded before the authenticated shell depends on navigation data.
- Replace direct menu assumptions with `LayoutContextService`.
- Keep route definitions in the app; backend layout `route` values must match app routes such as `dashboard`, `person`, and `person/create`.

Shell integration:

- Continue loading `LayoutComponent` from `@nexacore/layout`.
- Allow the shared shell to decide whether to show rail/sidebar/topbar/status bar from active profile flags.
- Use backend branding for display name, logo, favicon, and public login branding when supplied.

Quick navigation:

- Add a header or command-palette entry point for T-code search.
- Search only the visible backend nav tree.
- If a T-code points to an unavailable route, show a controlled not-available state and do not navigate.

Cleanup:

- Remove or update stale app-local API constants that still point to `/api`.
- Remove frontend logic that derives categories from `enabledModules`.
- Keep `menus` fallback only until the backend returns `layout.navTree` consistently.

## Migration Order

1. Add layout context models and `LayoutContextService` to `frontend-libs-21`.
2. Add Sentinel-compatible theme, size, and font token computation.
3. Refactor `ThemeService`, `DirectionService`, and `LayoutService` to consume layout context.
4. Refactor `RailNavComponent` to render `layout.navTree` directly.
5. Add `QuickNavService` using backend `tCode`.
6. Integrate `LayoutContextService` into `kyc-frontend-21` login/authenticated bootstrap flow.
7. Keep fallback rendering from old `menus` during backend rollout.
8. Remove fallback `menus` dependency after backend `/api/v1/system/privilege/context` always returns `layout`.

## Verification

- Build `frontendApplications/frontend-libs-21/`.
- Build `frontendApplications/kyc-frontend-21/`.
- Test login loads application context and applies the active layout profile.
- Test backend category labels other than `Operation`, `Setup`, and `Report`.
- Test theme switching with backend `primaries`, `chromeOverrides`, `sizes`, and `fonts`.
- Test `rtlEnabled: true` and `rtlEnabled: false`.
- Test T-code navigation only reaches visible routed feature nodes.
- Test fallback behavior when `layout` is missing from application context.
