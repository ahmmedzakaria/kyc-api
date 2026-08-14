# Tenant Authentication Change-set: System Impact Review and Implementation Plan

## 1. Purpose

This document reviews the current uncommitted tenant, client, authentication, user, role, and person-list changes across the backend and the active system frontend. It identifies system-level security, tenancy, migration, operational, and compatibility gaps and defines the implementation plan required before the change-set is considered production-ready.

The review covers:

- Tenant resolution for centralized API requests.
- Browser CORS and client-origin enforcement.
- Cross-tenant user, role, scope, and client administration.
- Global role-template administration.
- Client configuration bootstrap behavior.
- Tenant-domain normalization migration V62.
- Person-list pagination and sorting.
- Active system frontend behavior.
- Repository and release hygiene.

The legacy root `frontend-libs/` repository is outside the implementation scope and must not be changed.

## 2. Executive conclusion

The person-list correction is valid and the system frontend compiles, but the complete change-set should not be released yet. Three issues are release blockers:

1. Tenant identity is inferred from the caller-controlled `Origin` header before the client and origin are authenticated.
2. The read-level `TENANT_VIEW` privilege is treated as unrestricted cross-tenant administrative authority.
3. Migration V62 can normalize multiple rows to the same hostname and fail application startup on a unique constraint.

Additional work is required to make client bootstrap concurrency-safe, document configuration ownership, add missing authorization tests, and remove unrelated staged/generated files from the release scope.

## 3. Current behavior and impact

### 3.1 Tenant resolution from Origin — release blocker

`RequestTenantHostnameResolver` currently prefers the HTTP `Origin` header and falls back to `HttpServletRequest.getServerName()` when Origin is absent. Both `TenantResolutionFilter` and `AuthenticatedRequestContextFilter` use this resolver.

This solves the local browser case where:

- The frontend is hosted at `http://bdcom.localhost:5301`.
- The centralized API is hosted at `http://localhost:9100`.
- The browser sends the frontend origin to the API.

However, Origin is request input and is not an authenticated tenant identity. Command-line clients, integrations, mobile clients, and malicious callers can supply any Origin value. Tenant resolution also runs before `ClientApplicationAuthenticationFilter`, and public authentication routes may skip client-origin enforcement entirely.

Requests without Origin create a second gap. A tenant user calling the centralized API from a non-browser client resolves `localhost`, normally the system tenant, rather than the user/client tenant. This can cause valid tenant requests to be denied or evaluated under the wrong pre-authentication context.

#### Required security invariant

Tenant context must be derived from a trusted or subsequently validated binding. A request header alone must never grant tenant scope.

The following values must agree where applicable:

- Tenant-facing hostname or explicit tenant selector.
- Resolved active tenant.
- Client application.
- Client-to-tenant assignment.
- Client allowed origin for browser requests.
- Access-token tenant and user tenant for authenticated requests.
- Effective user scope assignments.

Any missing, ambiguous, inactive, or mismatched value must fail closed.

### 3.2 `TENANT_VIEW` used as a global administration bypass — release blocker

The current backend and frontend use `BootstrapAdministrationPrivileges.TENANT_VIEW` to decide whether a user may operate outside the effective tenant. This behavior appears in user administration, role administration, authorized scope lookup, client scope editing, and global role-template creation.

`TENANT_VIEW` is semantically a read privilege. Treating it as platform-wide scope authority creates privilege coupling: any role granted tenant visibility may acquire cross-tenant mutation capabilities when combined with an operation privilege such as role or user management.

Examples of affected operations include:

- Listing users or roles for an arbitrary active tenant.
- Creating or editing a user in another tenant.
- Reading a role by ID outside the effective tenant.
- Replacing user or client scope assignments across tenants.
- Creating or editing global role templates.
- Loading all tenant/business/branch choices.

#### Required authorization invariant

Operation permission and data-scope permission must be independent checks.

For example, editing a role in tenant 6 requires both:

- `ROLE_ADMINISTRATION_MANAGE`; and
- authority to administer tenant 6.

Creating a global role requires a separate platform-global privilege. Tenant visibility must not imply global mutation authority.

### 3.3 V62 hostname normalization collision — release blocker

`V62__normalize_url_form_tenant_hostnames.sql` converts URL-form domain values into hostnames. Its collision check compares a normalized candidate to existing raw hostname values, but does not detect two candidate rows that normalize to the same hostname during the same update.

Example:

```text
http://example.localhost:5300
https://example.localhost:5301
```

Both rows normalize to `example.localhost`. If the table has the expected unique hostname constraint, the migration can fail and prevent application startup.

Other migration gaps include:

- URL user-info may be converted into invalid hostname data.
- Ambiguous rows are silently left without a repair report.
- `updated_at` changes without recording the migration/system actor in `updated_by`.
- The SQL normalization rules are not fully aligned with runtime `HostnameNormalizer` behavior.

Because V62 may already be applied in some environments, it must not be edited after deployment. A corrective migration must use the next available system migration version.

### 3.4 Client bootstrap behavior

The new `createIfAbsent` behavior prevents `DataSeeder` from overwriting administrator-managed WEB client metadata at every startup. This is the correct ownership direction for:

- OAuth client ID.
- Allowed origins.
- Redirect URIs.
- Logout redirect URIs.
- Administrator-managed description and status.

The implementation performs a read followed by an insert. Two application instances starting concurrently can both observe a missing client and race on the unique client code. One instance may fail startup.

Configuration ownership also remains mixed:

| Configuration | Current owner |
| --- | --- |
| Client metadata and redirect/origin values | Administrator after initial creation |
| Tenant assignments | Additive bootstrap plus administrator |
| Feature permissions | Seeder replaces the complete set |
| API permissions | Seeder replaces the complete set |

This ownership contract must be intentional and documented in the UI and operations guide. Otherwise an administrator may expect feature/API edits to persist when startup synchronization will replace them.

### 3.5 Client scope replacement UI

The active system frontend now preserves assignments that the current user cannot edit and submits them with the requested replacement. This prevents accidental removal of locked scopes.

The UI also treats `TENANT_VIEW` as making every scope editable. That behavior inherits the privilege-design issue described above. Frontend visibility is not a security boundary, but it should mirror the corrected backend capability so the UI does not offer operations that the server must reject.

### 3.6 Role administration UI and backend

The role page now supports tenant selection, tenant-specific lists, tenant-aware creation, and tenant display during editing. The hard-coded `ROLE_SYSTEM_ADMIN` frontend check has been removed.

The remaining gap is the replacement authorization rule. Global template creation and cross-tenant administration currently depend on `TENANT_VIEW`, which is too broad. Role detail also has a fallback that loads a role from any active tenant when the caller has this privilege.

Tenant IDs received from the browser are correctly treated as requested targets, but they need validation against a dedicated cross-tenant administrative capability rather than tenant visibility.

### 3.7 Person-list fix

The person list previously sorted `KycPersonProfile` using `person.firstName`. The profile no longer has a JPA `person` relationship; global identity is referenced using `personId`, potentially across a database boundary. Spring Data therefore raised `PropertyReferenceException` before executing the scoped query.

The new sort uses profile-owned columns:

```text
createdAt DESC, id DESC
```

This is a valid deterministic pagination fix and preserves database-level tenant filtering. It changes the visible ordering from alphabetical to most recently created. If alphabetical ordering is required, it needs an explicit cross-database search/index contract rather than restoring an invalid JPA relationship.

### 3.8 Repository and release hygiene

The root repository and legacy frontend repositories contain staged additions whose working-tree files are deleted (`AD` status). A normal commit may still record the staged additions. The root staged set includes Angular compiler cache/database files.

Generated `.angular/`, `dist/`, compiler databases, and unrelated legacy frontend files must not be included in this change-set. Existing user work must not be destructively reset; the index should be reviewed and cleaned intentionally before committing.

## 4. Target architecture

### 4.1 Tenant request context

Introduce a single tenant-request context pipeline with distinct pre-authentication and authenticated validation stages.

#### Direct tenant host request

For an API served through a tenant-specific host:

```text
Trusted Host/Forwarded Host
    -> normalize hostname
    -> verified active tenant domain
    -> client assigned to tenant
    -> token/user tenant and scope validation
```

Only honor forwarded-host headers when the request came through a configured trusted proxy. Do not trust arbitrary `X-Forwarded-Host` input from the public request path.

#### Centralized API browser request

For a centralized API such as `localhost:9100`, Origin may be used only as a candidate that must be bound to the client:

```text
Origin candidate + X-Client-Code
    -> resolve active client
    -> exact allowed-origin validation
    -> origin hostname resolves to verified active tenant
    -> client has assignment covering tenant
    -> establish pre-auth tenant context
```

Public authentication routes must perform this binding even if normal API authorization is skipped.

#### Authenticated request

After JWT authentication:

```text
pre-auth tenant
    == token tenant
    == account tenant
    covered by active user scope
    covered by active client scope
```

Any mismatch returns a stable 403 error with an authorization event and trace ID.

#### Non-browser request

Non-browser centralized API callers need an explicit, authenticated selection mechanism. Acceptable designs include:

- Tenant-specific API hostname; or
- Signed/confidential-client tenant context validated against the client assignment; or
- Access-token tenant for authenticated routes, with a separate explicit tenant code for pre-login routes that is validated against client credentials.

Do not silently map a missing Origin to the system tenant for tenant-owned operations.

### 4.2 Administrative capability model

Add explicit privileges with stable constants and catalog entries. Suggested semantics:

| Capability | Purpose |
| --- | --- |
| `TENANT_VIEW` | View tenants allowed by current data scope |
| `TENANT_CROSS_SCOPE_VIEW` | View other active tenants from the platform control plane |
| `TENANT_CROSS_SCOPE_ADMINISTER` | Mutate tenant-owned administration data outside effective tenant scope |
| `GLOBAL_ROLE_MANAGE` | Create or edit global role templates |

Exact codes must follow the existing privilege taxonomy and must be added through the next Flyway migration/catalog synchronization process. Frontend code must import semantic constants rather than hard-code privilege codes.

Platform authority should be represented centrally, for example by an `AdministrationScopeAuthorizer`, rather than repeated `effectivePrivilegeCodes().contains(...)` checks in multiple services.

## 5. Implementation plan

### Phase 1 — Freeze and characterize current behavior

Status: **Completed on 2026-08-15.** Phase 1 intentionally keeps the normal build green. Unsafe current behavior is captured by executable characterization tests; the inverse security expectations below are release gates that must replace those characterizations during Phases 2 and 3.

1. [x] Record all modified and untracked files in backend and active system frontend.
2. [x] Identify unrelated staged root/legacy frontend files without deleting, resetting, or otherwise changing user work.
3. [x] Add characterization and release-gate contracts for:
   - Spoofed Origin cannot select an arbitrary tenant.
   - Missing Origin does not silently select the system tenant for a tenant request.
   - Client origin and tenant assignment must agree.
   - Token tenant must agree with resolved tenant.
   - `TENANT_VIEW` alone cannot administer another tenant.
   - `TENANT_VIEW` alone cannot create a global role.
   - Two URL-form domain rows can normalize to the same hostname.
4. [x] Capture current public-route behavior for auth config, password login, SSO initiation/callback, refresh, logout, and session status.

Deliverable: a reproducible green baseline plus explicit security contracts and expected error codes for the next phases.

#### Phase 1 change inventory

The functional change-set is confined to the backend and `frontendApplications/system-frontend-21`. The backend contains tenant resolution/CORS changes, tenant-aware user and role administration, additive client bootstrap behavior, migration V62, the person-list sort correction, and related tests. The active system frontend contains tenant-aware role/user controls and locked client-scope replacement behavior.

The following are deliberately excluded from the functional change-set:

- Legacy root `frontend-libs/`.
- Root `layout/` staged additions and Angular compiler cache files.
- Staged-then-deleted files in the legacy `frontend/` and `privilege-frontend/` repositories.
- Generated `.angular/` and `dist/` output.
- Unrelated nested repositories and existing user work.

Phase 1 did not mutate the Git index because unstaging unrelated user work is outside the authorization of an analysis/baseline phase.

#### Current authentication route classification

| Route | Current classification | Current consequence |
| --- | --- | --- |
| `/api/v1/auth/login` | Public | Normal client/JWT enforcement is skipped |
| `/api/v1/auth/authenticate` | Public | Normal client/JWT enforcement is skipped |
| `/api/v1/auth/config` | Public | Pre-login configuration is available after tenant filter resolution |
| `/api/v1/auth/application-context/public` | Public | Public application context uses pre-auth tenant resolution |
| `/api/v1/auth/sso/authenticate` | Public | SSO token exchange is pre-authentication |
| `/api/v1/auth/login-status` | Public | Login-status check is pre-authentication |
| `/api/v1/auth/refresh-token` | Public | Refresh token is validated by the auth service rather than bearer JWT filter |
| `/oauth2/**` | Public | SSO initiation/callback paths are permitted by wildcard |
| `/api/v1/auth/logout` | Protected | Requires authenticated request processing |
| `/api/v1/auth/session-status` | Protected | Requires authenticated request processing |
| `/api/v1/auth/application-context` | Protected | Requires authenticated request processing |

#### Executable characterization tests

- `TenantResolutionFilterTest` proves that a syntactically valid caller-supplied Origin currently overrides the centralized API hostname and that missing Origin falls back to the API server hostname.
- `PublicAuthenticationRouteCharacterizationTest` freezes the public/protected route matrix.
- `AdministrativePrivilegeCharacterizationTest` records that global-role and cross-tenant administration are currently coupled to `TENANT_VIEW`.
- `UserAdminServiceObjectAuthorizationTest` records that `TENANT_VIEW` currently permits cross-tenant user creation.
- `HostnameNormalizerTest` proves that distinct URL-form rows can normalize to one hostname, establishing the V62 collision case.

#### Pending security contracts

The following expectations are intentionally not asserted as passing behavior until their enforcement exists:

| Contract | Expected denial |
| --- | --- |
| Unvalidated Origin selects another tenant | 403 `TENANT_ORIGIN_MISMATCH` |
| Tenant context is absent on a centralized tenant request | 400/403 `TENANT_CONTEXT_REQUIRED` |
| Valid client is not assigned to resolved tenant | 403 `TENANT_CLIENT_NOT_ASSIGNED` |
| Access-token tenant differs from resolved tenant | 403 `TOKEN_TENANT_MISMATCH` |
| `TENANT_VIEW` actor attempts cross-tenant mutation | 403 data-scope denial |
| Role manager without global authority creates global template | 403 privilege denial |
| Multiple domain rows normalize to one hostname | Migration reports/quarantines conflict without unique-key startup failure |

Phases 2, 3, and 5 must invert or replace the corresponding current-behavior tests when these contracts are implemented.

### Phase 2 — Replace Origin-as-authority tenant resolution

1. Split `RequestTenantHostnameResolver` into candidate extraction and validated tenant-context resolution.
2. Add configuration for trusted proxy hosts/networks if forwarded-host support is required.
3. Resolve direct tenant hosts from the trusted request host.
4. For centralized browser requests:
   - Require `X-Client-Code`.
   - Resolve the active client before accepting Origin as a tenant candidate.
   - Validate Origin using the existing exact `ClientOriginPolicy`.
   - Resolve the verified active tenant domain from the validated origin hostname.
   - Require a client scope assignment covering the tenant.
5. Ensure public auth routes run the minimum client/origin/tenant binding checks even when they skip normal API privilege evaluation.
6. After JWT authentication, validate resolved tenant against token tenant, account tenant, user scope, and client scope.
7. Define the supported non-browser flow and return a clear error when tenant context is required but absent.
8. Introduce stable errors such as:
   - `TENANT_CONTEXT_REQUIRED`
   - `TENANT_ORIGIN_MISMATCH`
   - `TENANT_CLIENT_NOT_ASSIGNED`
   - `TOKEN_TENANT_MISMATCH`

Deliverable: one fail-closed tenant resolution pipeline shared by pre-login and authenticated requests.

### Phase 3 — Separate tenant visibility from tenant administration

1. Add explicit platform cross-scope and global-role privileges to the privilege catalog using the next migration version.
2. Create a central `AdministrationScopeAuthorizer` or equivalent service with methods such as:
   - `canViewTenant(Long tenantId)`
   - `requireTenantAdministration(Long tenantId)`
   - `requireGlobalRoleAdministration()`
3. Replace `canAdministerTenants()` implementations based on `TENANT_VIEW` in:
   - `UserAdminServiceImpl`
   - `AuthorizedScopeLookupService`
   - Auth policy administration
   - Client scope administration
   - Any other cross-tenant service found by repository search
4. Keep operation privileges mandatory in addition to the scope capability.
5. Update role detail access so a role ID cannot bypass the selected/effective tenant predicate.
6. Require the global-role privilege on global role create/update and privilege replacement.
7. Validate user role assignments and scope replacement using the target user tenant plus caller administrative authority.
8. Emit authorization audit events for platform cross-tenant operations, including target tenant and actor.

Deliverable: explicit and testable separation between view, operation, target scope, and platform-global authority.

### Phase 4 — Align the system frontend with backend capabilities

1. Add semantic privilege constants for cross-tenant administration and global role management.
2. Replace frontend use of `TENANT_PRIVILEGES.view` as an administrative bypass.
3. Show the tenant selector only when cross-tenant viewing is allowed.
4. Enable create/edit/remove actions only when cross-tenant administration is allowed for the selected target.
5. Show global role controls only with `GLOBAL_ROLE_MANAGE`.
6. Preserve locked client/user scope assignments during replacement.
7. Display a locked indicator and reason for assignments outside the caller’s writable scope.
8. Prevent stale list responses when the selected tenant changes by cancelling or sequencing requests.
9. Keep all privilege references as imported semantic constants; do not hard-code numeric codes in components.

Deliverable: UI behavior mirrors backend authority without relying on role-name checks.

### Phase 5 — Repair tenant-domain migration safely

1. Determine whether V62 has been deployed in every target environment.
2. If V62 has been deployed anywhere, do not edit it. Add the next migration version.
3. Build a candidate query that:
   - Accepts only supported HTTP/HTTPS URL forms.
   - Rejects user-info, query, fragment, invalid port, and invalid hostname forms.
   - Applies normalization consistent with `HostnameNormalizer`.
   - Groups by normalized hostname and updates only groups with exactly one unambiguous source row.
4. Detect conflicts against both existing hostname rows and other normalized candidates.
5. Write unresolved conflicts to a repair/audit table or fail with a clear diagnostic, depending on the agreed deployment policy.
6. Set `updated_at` and `updated_by` to the migration/system actor.
7. Add a unique index/constraint if one is not already present after conflicts are resolved.
8. Add migration tests for:
   - Single valid URL.
   - Duplicate normalized candidates.
   - Collision with an existing hostname.
   - URL user-info.
   - Query/fragment/path.
   - IPv4, IPv6, IDN, case, trailing dot, and ports.
   - Idempotent rerun behavior in the migration test harness.

Deliverable: deterministic migration that cannot fail unexpectedly from candidate-to-candidate collisions.

### Phase 6 — Make client bootstrap idempotent and concurrency-safe

1. Replace read-then-insert client creation with a database-supported insert-if-absent or equivalent unique-conflict recovery.
2. Reload and return the existing row after a concurrent insert conflict.
3. Keep administrator-managed metadata unchanged after initial creation.
4. Validate that bootstrap tenant grants are additive and idempotent.
5. Validate the bootstrap tenant exists and is active before inserting assignments.
6. Document whether API and feature assignments are authoritative seed state or administrator-managed.
7. If administrator-managed, change startup synchronization to additive or migration-driven grants.
8. If seed-authoritative, label those controls appropriately in the UI or make them read-only.
9. Add multi-instance/concurrency and restart persistence tests.

Deliverable: startup can run repeatedly and concurrently without overwriting managed client configuration or failing on uniqueness races.

### Phase 7 — Person-list behavior and performance

1. Retain the profile-owned deterministic sort as the immediate correctness fix.
2. Confirm product requirements for alphabetical versus recent-first ordering.
3. If alphabetical order is required, define a cross-database identity-search result that returns ordered person IDs and supports stable pagination.
4. Avoid an unbounded ID list or in-memory tenant filtering.
5. Ensure the KYC profile query always includes tenant/business/branch predicates in the database.
6. Add controller/service tests for invalid page values, empty search, no matches, stable ordering, and tenant isolation.

Deliverable: predictable pagination without an invalid ORM relationship or cross-scope filtering.

### Phase 8 — Audit, operational readiness, and cleanup

1. Remove temporary `System.out` diagnostic logging from authentication and client filters or replace it with structured debug logging.
2. Ensure logs never contain access tokens, passwords, API keys, or complete sensitive headers.
3. Include trace ID, actor, client, resolved tenant, target tenant, decision, and denial code in authorization events.
4. Review session persistence: the current in-memory active-session registry invalidates tokens after restart. Decide whether Redis/database durability is required before production.
5. Remove generated build artifacts from the staged index and strengthen `.gitignore` where necessary.
6. Confirm only backend and active `system-frontend-21` changes are included; do not modify legacy `frontend-libs`.
7. Update the main tenant/client authentication implementation plan with final architecture and configuration ownership.

Deliverable: clean release scope with actionable operational diagnostics and documented restart behavior.

## 6. Required test matrix

### 6.1 Tenant and client resolution

| Scenario | Expected result |
| --- | --- |
| Direct verified active tenant host | Tenant resolves |
| Unknown host | 403 `TENANT_DOMAIN_NOT_RECOGNIZED` |
| Inactive tenant domain | 403 `TENANT_NOT_ACTIVE` |
| Valid centralized browser origin assigned to client and tenant | Tenant resolves |
| Spoofed Origin not allowed for client | 403 |
| Origin tenant not assigned to client | 403 |
| Missing Origin on tenant browser pre-login request | Explicit tenant-context error |
| Authenticated token tenant differs from resolved tenant | 403 |
| User has no active scope covering resolved tenant | 403 |
| Browser CORS preflight for allowed subdomain | Allowed |
| Browser CORS preflight for unconfigured port/domain | Denied |

### 6.2 Administrative authorization

| Actor | Operation | Expected result |
| --- | --- | --- |
| Tenant-scoped user with `TENANT_VIEW` | View permitted tenant | Allowed |
| Tenant-scoped user with `TENANT_VIEW` | Edit another tenant | Denied |
| Cross-scope viewer | View another tenant | Allowed |
| Cross-scope viewer without manage capability | Mutate another tenant | Denied |
| Cross-scope administrator plus operation privilege | Mutate target tenant | Allowed |
| Role manager without global-role privilege | Create global role | Denied |
| Global-role administrator | Create/update global role | Allowed |
| Tenant role administrator | Assign role from another tenant | Denied |

### 6.3 Bootstrap and migration

| Scenario | Expected result |
| --- | --- |
| Existing WEB client with managed OAuth/origin values | Values unchanged after restart |
| Missing WEB client | Created once |
| Two instances seed missing WEB client concurrently | One row, both startups succeed |
| Existing additional tenant assignment | Preserved |
| Missing bootstrap tenant assignment | Added once |
| Two URL domains normalize to same hostname | Conflict handled deterministically |
| URL candidate conflicts with existing hostname | Existing row preserved; conflict reported |

## 7. Verification commands

Run the narrow suites during implementation, followed by full builds before release:

```bash
cd backend
mvn -Dtest=TenantResolutionFilterTest,AccessControlFilterChainIntegrationTest test
mvn -Dtest=UserAdminServiceObjectAuthorizationTest,ClientApplicationServiceBootstrapTest test
mvn -Dtest=PersonControllerTest,PersonServiceObjectAuthorizationTest test
mvn -Dtest=SystemDatabaseMigrationTest test
mvn test

cd ../frontendApplications/system-frontend-21
npm run build
npm test
```

Also validate the target migration against a database snapshot containing real tenant-domain variants before production rollout.

## 8. Rollout plan

1. Inventory tenant domains, client origins, redirect URIs, client-tenant assignments, user tenants, and user scopes in each environment.
2. Resolve or explicitly record hostname collisions before applying the corrective migration.
3. Deploy new privilege catalog entries and assign platform capabilities only to the intended platform administration role.
4. Deploy backend tenant-binding and authorization changes.
5. Deploy the aligned system frontend.
6. Require affected users to refresh application context or sign in again so new privileges are reflected.
7. Monitor authorization denial codes, tenant mismatch events, login failures, and client-origin failures.
8. Retain a rollback path for frontend and backend binaries, but do not roll back an applied Flyway migration by editing its history.

## 9. Completion criteria

The change-set is ready only when all of the following are true:

- Tenant selection cannot be controlled by an unvalidated Origin or arbitrary header.
- Browser, direct-host, and supported non-browser tenant flows are explicitly defined and tested.
- Client origin, client tenant assignment, token tenant, account tenant, and user scope agree.
- `TENANT_VIEW` no longer grants cross-tenant mutation or global-role authority.
- Cross-tenant and global administration use dedicated semantic privileges.
- V62 is either proven safe before deployment or followed by a non-destructive corrective migration.
- Client bootstrap is idempotent under concurrent startup.
- Administrator-managed client metadata survives restart.
- Configuration ownership for client metadata, scopes, APIs, and features is documented.
- Person search executes without invalid JPA properties and retains database-level scope predicates.
- Required backend tests and frontend builds pass.
- Generated and unrelated staged files are excluded from the release.
- No legacy `frontend-libs/` changes are included.

## 10. Current verification baseline

At the time of this review:

- The active system frontend production build succeeds.
- The initial frontend bundle is approximately 567.68 kB and exceeds the configured 500 kB budget by approximately 67.68 kB.
- The focused person controller and object-authorization tests pass: four tests, zero failures.
- Backend and active system frontend diff whitespace checks pass.
- The security, migration, authorization, and test gaps described above remain open.
