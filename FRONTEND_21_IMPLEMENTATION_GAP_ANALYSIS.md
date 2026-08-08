# Angular 21 Frontend Implementation Gap Analysis

## Scope

This document reviews the integration between:

- `frontend-libs-21`
- `kyc-frontend-21`
- The backend application-context and layout contracts
- The current person form implementation

The former `/kyc` feature route and `/api/v1/kyc/**` API surface have been intentionally removed. They are therefore not considered missing functionality. Person management remains available through `/api/v1/person/**`.

## Executive Summary

The Angular 21 application builds and loads the shared authenticated shell. Backend themes, sizes, fonts, branding, and favicon configuration are partially applied. The implementation is not yet fully backend-driven, however. Its most important gaps are:

1. `layout.navTree` is ignored in favor of the legacy `menus` response.
2. Backend layout-profile behavior flags are modeled but not applied.
3. Application routes and actions are not protected by feature privileges.
4. Several header and status-bar features display hardcoded proof-of-concept data.
5. Application context is loaded more than once and cached layout state is not cleared completely.
6. Language, breadcrumb, and layout state are not consistently synchronized.
7. The person form has validation, typing, subscription, accessibility, and data-consistency gaps.
8. Shared-library tests and root workspace scripts are not currently reliable.

## 1. Backend Navigation Contract

### Current implementation

The backend returns an authoritative navigation tree through `layout.navTree`. Each node provides:

- `code`
- `tCode`
- `label`
- `type`
- `icon`
- `route`
- `privilegeCodes`
- `children`

The frontend declares `navTree` as `unknown[]` and does not consume it. `RailNavComponent` instead reconstructs navigation from the legacy `ApplicationContext.menus` collection and expects a `path` property.

It also infers categories by comparing displayed labels with the English words `Operation`, `Setup`, and `Report`.

### Impact

- Navigation configured through the backend layout module does not control the shared rail.
- Backend node types and translation codes are discarded.
- Translated category labels can break navigation grouping.
- Legacy menus and layout navigation can disagree.
- The frontend cannot validate the backend navigation contract at compile time.

### Required work

- Add a typed `NavNode` model matching the backend DTO.
- Prefer `layout.navTree` as the authoritative navigation source.
- Map node behavior from `type` and `route`, not label text.
- Use `tCode` for translated labels with `label` as the fallback.
- Keep legacy-menu conversion only as a temporary compatibility adapter if it is still required by another client.
- Add tests for nested nodes, missing routes, ordering, external routes, privilege filtering, and translated labels.

## 2. Layout Profile Behavior

The frontend receives but does not apply these active-profile properties:

- `layoutType`
- `navigationMode`
- `themeMode`
- `density`
- `topbarEnabled`
- `sidebarEnabled`
- `sidebarCollapsed`
- `footerEnabled`
- `breadcrumbEnabled`
- `commandBarEnabled`
- `rtlEnabled`

The shared layout currently renders the header, rail, breadcrumb, content outlet, and status bar unconditionally.

### Required work

- Resolve the active profile once in `LayoutConfigService`.
- Expose each behavior setting as a computed signal.
- Conditionally compose shell regions based on profile settings.
- Initialize rail state from `sidebarCollapsed`.
- Drive rail/grouped/horizontal behavior from `navigationMode` and `layoutType` rather than `enabledModules`.
- Map density to a documented token set.
- Define how user theme selection interacts with backend `themeMode`.
- Apply `rtlEnabled` together with language direction rules.
- Define a real command-bar slot before enabling `commandBarEnabled`.

## 3. Application Context Ownership

Authentication loads and caches application context after login. `RailNavComponent` then requests the same context again when it is constructed.

### Impact

- Duplicate HTTP calls after login.
- Cached layout may flash before the second response arrives.
- Navigation construction owns global application initialization.
- Privileges, layout, and menus do not share a single reactive source of truth.

### Required work

Introduce an application-context store in a shared library that:

- Loads context once per authenticated session.
- Exposes context, navigation, privileges, layout, loading, and error signals.
- Supports an explicit refresh operation.
- Deduplicates concurrent requests.
- Clears all user/client state during logout.
- Allows the route layer to wait for context when authorization decisions require it.

## 4. Authentication and Authorization

The authenticated route tree uses `authGuard`, but feature routes and page actions do not use feature privileges.

### Required work

- Add a privilege-aware route guard or `canMatch` helper.
- Declare required privileges in route data or a typed route policy.
- Protect create, update, preview, and destructive actions in the UI.
- Preserve backend authorization as the final authority.
- Handle forbidden deep links with a dedicated access-denied page rather than redirecting silently to the dashboard.
- Remove raw decoded-token logging from `AuthService`.

## 5. Header and Status Bar

Several shared-shell elements are still proof-of-concept implementations:

- Global search logs to the console.
- The SSO application list and launcher are hardcoded.
- Tenants are hardcoded and selection has no backend effect.
- Notifications and pending tasks are fabricated.
- User name, role, and initials are hardcoded.
- Profile and account-settings entries do not navigate.
- Environment, health, synchronization, audit, session expiry, and version values are hardcoded.
- Logout uses the browser-native `confirm()` dialog.

### Required work

- Hide unsupported shell capabilities through configuration until APIs exist.
- Populate user identity from the authenticated principal/application context.
- Replace placeholder tenants and SSO applications with typed backend data.
- Connect search through an app-provided search adapter rather than embedding KYC-specific search concepts in the shared layout.
- Use the shared confirmation dialog for logout.
- Derive application version from build metadata.
- Only show operational status when backed by actual status data.

Shared layout libraries should provide extension points and presentation contracts; KYC-specific search, task, and notification implementations should remain in the consuming application.

## 6. Cached Layout Cleanup

`SidebarMenuService` stores `layoutConfig` in local storage. Logout clears menu and privilege keys but currently leaves `layoutConfig` behind.

### Risks

- A subsequent user can briefly see the previous user's client branding or layout.
- Removed font and favicon settings can survive logout.
- Inline CSS variables can remain after an empty or failed configuration response.

### Required work

- Remove `layoutConfig` during logout.
- Add `LayoutConfigService.clear()`.
- Clear injected font links, favicon overrides, and inline CSS variables.
- Namespace context cache keys by client/user if cross-session caching is retained.

## 7. Internationalization and Direction

Changing language updates Transloco and document direction, but the selected language is not stored in `nexacore.locale`. The language interceptor reads that local-storage key when setting `Accept-Language`.

The header also always displays the first language label rather than the active language.

### Required work

- Persist the active language using one shared locale service.
- Initialize Transloco from the stored locale.
- Make `Accept-Language`, document `lang`, and document `dir` derive from the same state.
- Display the active language in the header.
- Translate shell labels, backend navigation labels, wizard steps, form labels, placeholders, validation messages, and status text.
- Define precedence between language-driven RTL and profile `rtlEnabled`.

## 8. Breadcrumbs and Routing

Breadcrumb state is currently updated only when navigation originates from the rail. Direct URLs, refreshes, browser history, redirects, and navigation from page buttons can leave the displayed breadcrumb stale.

### Required work

- Derive breadcrumbs from activated-route metadata or the matching navigation node.
- Update breadcrumbs on router navigation completion.
- Provide route metadata for dynamic titles such as person create, edit, and preview.
- Replace the wildcard redirect with an explicit not-found page so configuration errors are visible.

## 9. Person Form Analysis

Reviewed files:

- `../frontendApplications/kyc-frontend-21/src/app/pages/person/person-form.component.html`
- `../frontendApplications/kyc-frontend-21/src/app/pages/person/person-form.component.ts`
- `../frontendApplications/kyc-frontend-21/src/app/pages/person/person-form.component.scss`

### 9.1 Component conventions

The component uses decorator-based `@Input()` and `@Output()` and constructor injection. The Angular 21 repository convention prefers signal-based `input()`/`output()` and `inject()`.

`HttpClientModule` is imported by the standalone form even though HTTP is already provided at application bootstrap and the component does not inject `HttpClient` directly.

### 9.2 Weak typing

The form is an untyped `FormGroup`. Location values, API responses, result collections, flattened values, and service responses use `any`.

Required improvements:

- Use `NonNullableFormBuilder` or typed form groups.
- Add typed person create/update payloads.
- Add typed GIS search and location-detail responses.
- Replace `(this as any)[resultKey]` dynamic assignment with explicit state.
- Type `PersonService` and `GisService` return values.

### 9.3 Unused location-search logic

The visible `LocationDropdownComponent` owns location search and emits a selected location. The parent form also subscribes to changes on the hidden location ID controls and maintains `currentLocationResults` and `permanentLocationResults`.

Because selected IDs are normally numbers/IDs rather than user-entered search strings, this duplicate debounced search path appears obsolete. `selectLocation()` also appears unused by the template.

Required work:

- Keep search responsibility in `LocationDropdownComponent`.
- Remove `setupLocationSearch()`, result arrays, and `selectLocation()` if no external consumer uses them.

### 9.4 Subscription lifecycle

The `sameAddress.valueChanges`, location-search subscriptions, photo request, and location-detail requests are not tied to component destruction. Only object-URL cleanup is currently performed.

Required work:

- Use `takeUntilDestroyed()` with `DestroyRef` for component-owned subscriptions.
- Cancel or supersede stale location/photo requests where appropriate.

### 9.5 Same-address synchronization

Checking “same address” copies the current address once. Later changes to the current address or location do not update the permanent address while the checkbox remains checked.

The permanent controls also remain editable, allowing the UI to claim the addresses are the same while storing different values.

Required work:

- Continuously mirror current-address fields while `sameAddress` is enabled, or calculate permanent values at submission.
- Disable permanent controls and location selection while mirroring.
- Restore or clear values according to a documented UX rule when unchecked.
- Mirror the displayed permanent-location label as well as the stored ID/type.

### 9.6 Validation inconsistencies

- Mobile fields use `type="number"`, which is unsuitable for telephone identifiers and can discard leading zeroes.
- Template `minLength`/`maxLength` inputs are not represented by equivalent form validators.
- Only email and primary mobile are required at form-model level.
- National ID minimum length is too permissive for a real identity contract unless intentionally generic.
- No cross-field or server validation mapping is visible.
- The form can call `submit()` without an explicit final validity guard.

Required work:

- Use telephone/text input mode with numeric filtering instead of number input.
- Keep component constraints and reactive-form validators aligned.
- Define validation from the backend DTO/domain rules.
- Mark controls touched and stop submission when invalid.
- Surface backend field errors through the shared validation-message system.

### 9.7 Data correctness

- The `O-` blood-group option has value `O`, losing the negative suffix.
- Username is always read-only, including create mode, without documented population behavior.
- `patchValue()` sends the complete flat person object into every nested group. It works because unknown keys are ignored, but explicit mapping would make contract drift visible.
- Dates are displayed in review using their raw value rather than the shared date utilities.
- FormData serializes every non-file value with `toString()`, which may not preserve the backend's desired null/empty/date semantics.

### 9.8 File upload

- The accepted file pattern is limited to `*.png`; MIME-type acceptance should be confirmed against the backend.
- Existing-preview lifecycle is handled correctly with `URL.revokeObjectURL()`.
- Upload errors and preview-loading errors are silently discarded.
- There is no visible upload progress or server-side validation feedback.

### 9.9 Accessibility and shared-component consistency

Relation, education level, and passing year use native select shells while gender and blood group use the shared smart dropdown. This creates inconsistent validation, keyboard, and styling behavior.

The native select labels are not explicitly associated through `for`/`id`. Clickable language/header patterns elsewhere also use non-button elements, but those belong to the shared-shell review.

Required work:

- Use the appropriate shared dropdown component consistently, or properly associate native labels and controls.
- Ensure wizard invalid-step feedback is announced.
- Add an accessible submission/loading state.
- Disable duplicate submission while a request is pending.

### 9.10 Error and completion handling

Submission only handles success. There is no error callback, pending state, retry behavior, or duplicate-submit prevention.

Required work:

- Add a submitting signal.
- Disable wizard completion during submission.
- Handle and display normalized API errors.
- Ensure the emitted `saved` event includes the saved person or identifier when useful to the parent.

## 10. API and Type Safety

`PersonService` and `GisService` currently expose `Observable<any>` contracts. `ApiService.post()` also accepts mutable `any` bodies and appends `source` to them.

### Required work

- Create request/response models for person, GIS, pagination, and file endpoints.
- Use `unknown` plus explicit narrowing at generic infrastructure boundaries.
- Avoid mutating caller-owned request objects in `ApiService`.
- Add tests for response unwrapping, multipart calls, binary responses, and normalized errors.
- Remove response-body and raw API-error console logging from production paths.

## 11. Testing and Build Health

Observed verification state during the analysis:

- `kyc-frontend-21` production build passes.
- The application test suite passes, but contains only one endpoint-catalog test.
- The initial application bundle is approximately 527 kB and exceeds the 500 kB warning budget.
- All shared libraries build when named individually.
- `frontend-libs-21` root `npm run build` fails because `ng build` is ambiguous in a multi-project workspace.
- Shared layout tests contain failures around theme effects and incomplete `SidebarMenuService` mocks.
- Some shared projects have no tests, causing the aggregate test target to exit unsuccessfully.

### Required work

- Replace the root build script with explicit ordered project builds.
- Provide project-specific and aggregate test scripts that tolerate projects with no tests only when intentional.
- Repair `ThemeService` tests for signal/effect scheduling.
- Update rail test mocks for the current context/layout service contract.
- Add tests for application-context caching and cleanup.
- Add person-form tests for create/edit mapping, validation, same-address behavior, photo handling, and error states.
- Add Playwright coverage for authenticated shell loading, backend navigation, language/RTL, responsive rail behavior, and direct routes.

## Recommended Delivery Order

### Phase 1: Contract correctness

1. Type and consume `layout.navTree`.
2. Introduce the shared application-context store.
3. Apply active layout-profile behavior flags.
4. Clear all context/layout state on logout.

### Phase 2: Access and navigation

1. Add privilege-aware route and action policies.
2. Derive breadcrumbs from router/navigation metadata.
3. Add access-denied and not-found pages.
4. Ensure every backend navigation route has an implemented frontend destination.

### Phase 3: Person form hardening

1. Introduce typed forms and DTOs.
2. Remove duplicate location-search state.
3. Fix subscription cleanup and same-address synchronization.
4. Align validation with backend rules.
5. Add pending/error states and tests.

### Phase 4: Shell completion

1. Replace or hide placeholder header/status features.
2. Unify locale persistence and direction handling.
3. Add app-provided adapters for search, identity, tenants, applications, notifications, and tasks.
4. Resolve shared test failures and bundle-budget warnings.

## Completion Criteria

The integration can be considered complete when:

- Backend `layout.navTree` is the navigation source of truth.
- Every supported profile flag produces observable shell behavior.
- Context is loaded once and cleared fully on logout.
- Routes and actions consistently reflect privileges.
- No fabricated operational/user data is displayed.
- Locale, direction, theme, and breadcrumbs survive direct navigation and refresh correctly.
- The person form is typed, validated, leak-free, and reports submission errors.
- Application and shared-library builds/tests pass through documented root scripts.
- Browser smoke tests cover the backend-configured authenticated layout.
