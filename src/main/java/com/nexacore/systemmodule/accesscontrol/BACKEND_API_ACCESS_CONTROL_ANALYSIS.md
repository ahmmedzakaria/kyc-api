# Backend API Access Control Analysis

## Executive Summary

The backend has a sound multi-layer access-control design, but enforcement is incomplete and currently fail-open for APIs that are not registered in `sys_priv_api_registry`.

Authentication is required for most endpoints. Fine-grained client and user authorization, however, is only applied when a request matches an active API-registry record. No controller currently uses `@ClientSecuredApi`, `@PreAuthorize`, `@Secured`, or `@RolesAllowed`. As a result, sensitive business and platform-administration operations may be called by any authenticated user unless matching registry records were created manually.

| Layer | Assessment |
| --- | --- |
| JWT authentication | Implemented, with token-purpose gaps |
| Client application authentication | Implemented for registered APIs |
| Client API permission | Implemented but fail-open for unregistered APIs |
| Client feature permission | Implemented |
| User privilege enforcement | Implemented only for registered APIs |
| Controller/method authorization | Effectively absent |
| Tenant/business/branch isolation | Designed but not enforced consistently |
| Origin/IP/rate-limit controls | Stored but not enforced |
| Administrative API protection | Insufficient |

## Current Request Flow

```text
Request
  -> ClientApplicationAuthenticationFilter
  -> ClientApiAccessFilter
  -> JwtAuthenticationFilter
  -> UserPrivilegeApiAccessFilter
  -> Spring Security authenticated check
  -> Controller
```

The intended authorization decision is:

```text
valid client credential
AND client allowed to call API
AND client licensed for feature
AND user JWT valid
AND user owns required privilege
```

This model is appropriate, but it activates only when the request resolves to an active `SysPrivApiRegistry` entry.

## Critical Findings

### 1. Unregistered APIs bypass authorization

`ClientApiAccessFilter` allows a request to continue when no API-registry entry is found:

```java
if (api.isEmpty()) {
    filterChain.doFilter(request, response);
    return;
}
```

`UserPrivilegeApiAccessFilter` also bypasses privilege evaluation for an unresolved API.

This is fail-open behavior. A newly added, forgotten, or incorrectly patterned endpoint is available to every authenticated user.

Recommended behavior:

- Deny unresolved protected `/api/**` requests.
- Allow only explicitly public endpoints to bypass client and privilege checks.
- If gradual rollout is required, introduce `access-control.enforcement-mode=REPORT|ENFORCE`, with production using `ENFORCE`.

### 2. Controllers do not declare access metadata

No current controller methods use:

```java
@ClientSecuredApi
@PreAuthorize
@Secured
@RolesAllowed
```

Therefore, `ClientApiRegistryServiceImpl#syncFromAnnotations` has no annotated API mappings to synchronize.

Every protected controller operation should declare its module, submodule, feature, action, and public/private status. A build or integration test should fail when a protected endpoint lacks metadata.

### 3. Access-control administration is available to any authenticated user

`ClientApplicationController` exposes operations to:

- Create or update client applications.
- Rotate client API keys.
- Assign client API permissions.
- Assign client feature permissions.
- Assign client tenants.

`ApiRegistryController` permits authenticated users to:

- Add or modify API registry records.
- List registry records.
- Trigger annotation synchronization.

These operations require dedicated platform-administrator privileges. Without them, an ordinary user can potentially grant access or rotate client credentials.

### 4. Refresh tokens are not distinguished from access tokens

Access and refresh tokens use the same claim structure and signing key. Their primary difference is expiration time.

`JwtAuthenticationFilter` accepts any correctly signed bearer token without checking token purpose. A refresh token can therefore potentially authenticate an ordinary API request.

Tokens should contain a mandatory claim:

```json
{
  "token_type": "ACCESS"
}
```

or:

```json
{
  "token_type": "REFRESH"
}
```

Normal API authentication must accept only `ACCESS` tokens. The refresh operation must accept only `REFRESH` tokens.

### 5. The refresh endpoint is not in the authentication whitelist

`POST /api/v1/auth/refresh-token` is not included in `SecurityConfig.AUTH_WHITELIST`.

This can require a valid access token to obtain a new access token. Once the access token expires, the refresh request may be rejected before its refresh token is processed.

The refresh endpoint should be public at the user-authentication layer while retaining appropriate client, origin, abuse, and refresh-token validation.

### 6. Browser API keys are not confidential credentials

An Angular application cannot securely store an `X-API-Key`. A key embedded in a frontend bundle or sent by a browser is observable by the user.

For browser clients:

- Treat `X-Client-Code` as application context, not secret authentication.
- Rely on user authentication, origin controls, CSP, privileges, and tenant isolation.
- Use OAuth public-client flows with PKCE where appropriate.
- Reserve API keys and client secrets for confidential server-to-server clients.

## High-Priority Gaps

### Tenant, business, and branch restrictions are not enforced

The schema includes `sys_priv_client_application_tenants`, but request filters do not apply these assignments. Tenant, business, and branch constraints must be enforced in service and repository queries before returning or mutating data.

Menu filtering alone is not an authorization boundary.

### Configured client restrictions are unused

`SysPrivClientApplication` stores:

- `allowedOrigins`
- `allowedIps`
- `rateLimitPerMinute`

`ClientCredentialServiceImpl` currently validates client status, API-key hash, credential activity, and expiration, but does not enforce the configured origin, IP, or rate limit.

### Sensitive business endpoints only require authentication

Person APIs include operations to create, update, search, and delete people and to upload or download identity documents. These APIs process sensitive PII but have no explicit method or controller privilege checks.

They require action-specific permissions such as create, view, update, delete, upload, and download. Ownership or tenant restrictions must also be applied where appropriate.

### License and platform administration are broadly accessible

License plan, subscription, entitlement, key-generation, layout, workflow, privilege, API-registry, and client-administration endpoints require dedicated system privileges. Authentication alone is insufficient.

## Medium-Priority Findings

### Public paths are duplicated and inconsistent

Public route patterns are independently maintained in:

- `SecurityConfig`
- `ClientApplicationAuthenticationFilter`
- `ClientApiAccessFilter`
- `UserPrivilegeApiAccessFilter`

The lists already differ. Public-route configuration should have one authoritative source or derive from explicit API-registry metadata.

### Origin URLs are incorrectly included as request matchers

`SecurityConfig.AUTH_WHITELIST` contains entries such as:

```text
http://localhost:4200
http://localhost:4300
http://localhost:5300
```

Spring request matchers evaluate server request paths, not browser origins. These values belong in CORS configuration, not the authorization whitelist.

### CORS is hard-coded for development

Allowed origins are fixed to localhost values. Deployment origins should be environment-driven and, when appropriate, constrained by registered client configuration.

### Local JWTs lack security context claims

Local JWTs contain subject, roles, issue time, and expiration, but no:

- Issuer (`iss`)
- Audience (`aud`)
- Token ID (`jti`)
- Token type/purpose
- Client code
- Tenant or business context

Issuer, audience, and token type should be validated. A token ID improves revocation and audit correlation.

### JWT role claims are trusted until expiration

Authorities are loaded from token claims. A removed role may remain effective until the token expires unless session invalidation occurs. Sensitive administrative operations should use short-lived access tokens or validate a user/session security version.

### Malformed role claims may cause server errors

`JwtAuthenticationFilter` calls `roles.stream()` without treating a missing or malformed roles claim as an authentication failure. Token parsing should validate claim shape and convert all malformed-token conditions into a consistent `401` response.

### API pattern resolution can be ambiguous

Registry resolution chooses the matching pattern with the greatest non-wildcard length. Similar patterns can have equal specificity. Add explicit priority or overlap validation and a uniqueness rule for method/path mappings.

### User privileges are loaded for each request

The user privilege filter obtains privilege codes on every protected request. A short-lived cache with invalidation on assignment changes would reduce database load without weakening correctness.

## Positive Aspects

- API keys are stored as password hashes rather than raw secrets.
- Credential expiration and activity are checked.
- Client access and user access are modeled separately.
- Public API status is explicit in the API registry.
- Client authorization evaluates both API and feature permission.
- JWT sessions can be invalidated through logout state.
- Spring Security is stateless.
- HTTP Basic and form login are disabled.
- Client credential usage timestamps are recorded.
- Client request context is cleared in a `finally` block.
- Trace IDs are generated and returned.
- Access-control tables include ownership and timestamp audit fields.
- Decision-service tests cover core allow/deny behavior.

## Recommended Enforcement Model

Use defense in depth:

```text
Spring Security authentication
  -> client identity/context validation
  -> mandatory API registry resolution
  -> client API permission
  -> client feature/license permission
  -> user privilege permission
  -> method-level authorization
  -> tenant/business/branch query restrictions
```

Database-driven authorization should not be the only protection for platform-administration endpoints. Bootstrap administrator operations should also use method-level checks so an accidentally missing registry entry does not expose them.

## Remediation Order

1. Protect client, API-registry, privilege, layout, workflow, and license administration with explicit administrator privileges.
2. Add token-type claims and reject refresh tokens from normal APIs.
3. Correct refresh-endpoint accessibility and validation.
4. Annotate every controller operation with `@ClientSecuredApi`.
5. Add an automated registry-coverage test.
6. Change unresolved protected APIs from allow to deny.
7. Seed or synchronize the API registry and assign the default web client explicitly.
8. Add method-level `@PreAuthorize` checks as defense in depth.
9. Enforce tenant, business, and branch restrictions in service/repository queries.
10. Implement configured origin, IP, and rate-limit policies.
11. Centralize public-route and CORS configuration.
12. Add full filter-chain integration tests covering authentication and authorization decisions.

## Step-by-Step Implementation Plan

The implementation must be delivered in dependency order. Do not switch the runtime directly from the current fail-open behavior to enforcement until API metadata, client grants, user privileges, and coverage tests are in place.

### Phase 0: Establish the security baseline

#### Step 0.1: Capture the current API inventory

Implementation status: **implemented**. `ApiInventoryService` enumerates application-owned `/api/**` mappings from `RequestMappingHandlerMapping`; `POST /api/v1/system/api-registry/inventory` returns the inventory for review. Undeclared ownership, client audience, data scope, and authorization decisions are deliberately marked `REVIEW_REQUIRED` until a security owner approves them.

Use Spring's `RequestMappingHandlerMapping` to enumerate every application-owned controller mapping and record:

- HTTP method.
- Path pattern.
- Controller and method.
- Whether authentication is required.
- Intended module, submodule, feature, and action.
- Whether the API is genuinely public.
- Intended browser, mobile, service, or partner clients.
- Required tenant/business/branch scope.

Exclude framework endpoints such as error handling and actuator internals unless they are intentionally exposed.

Deliverable: a reviewed API inventory with an owner and access decision for every mapping.

#### Step 0.2: Define response semantics

Implementation status: **implemented**. Security filters and the JWT authentication entry point now return the standard `ApiResponse` JSON shape through `ApiResponseJsonWriter`. `AccessControlError` is the canonical access-control status/code/message catalog; client-required decisions are normalized to `401 INVALID_CLIENT_CREDENTIALS`. The `INVALID_TOKEN_TYPE`, `DATA_SCOPE_NOT_ALLOWED`, and `API_NOT_REGISTERED` codes are defined now and are used when their enforcement phases are activated.

Adopt one consistent decision contract:

| Condition | Status | Code |
| --- | --- | --- |
| Missing/invalid user token | `401` | `AUTHENTICATION_REQUIRED` |
| Wrong token type | `401` | `INVALID_TOKEN_TYPE` |
| Missing/invalid confidential-client credential | `401` | `INVALID_CLIENT_CREDENTIALS` |
| Client lacks API permission | `403` | `CLIENT_API_NOT_ALLOWED` |
| Client lacks feature permission | `403` | `CLIENT_FEATURE_NOT_ALLOWED` |
| User lacks privilege | `403` | `USER_PRIVILEGE_NOT_ALLOWED` |
| Tenant/business/branch outside scope | `403` | `DATA_SCOPE_NOT_ALLOWED` |
| Protected API missing registry metadata | `403` | `API_NOT_REGISTERED` |

Return the standard `ApiResponse` JSON shape instead of servlet-container HTML from `sendError`.

#### Step 0.3: Add configuration switches

Implementation status: **implemented**. `AccessControlProperties` binds all five switches and `EnforcementMode` defines `DISABLED`, `REPORT`, and `ENFORCE`. Disabled evaluation bypasses access-control authorization filters while retaining the normal JWT security chain; report mode logs unresolved protected mappings; enforce mode returns `403 API_NOT_REGISTERED` when registry coverage is enabled. Browser requests are identified by `Origin` or `Sec-Fetch-Site`; other requests use the confidential-client requirement. Startup rejects disabled access control under the `prod` or `production` profile.

Create typed access-control properties under the privilege access-control package:

```properties
access-control.enabled=true
access-control.enforcement-mode=REPORT
access-control.require-client-for-browser=false
access-control.require-client-for-confidential=true
access-control.registry-coverage-enabled=true
```

Supported enforcement modes:

- `DISABLED`: authentication only; intended only for emergency local diagnosis.
- `REPORT`: calculate and log decisions but do not deny unresolved registry mappings.
- `ENFORCE`: deny every unauthorized or unresolved protected API.

Production acceptance criterion: `DISABLED` is rejected or prominently warned against in production profiles.

### Phase 1: Protect security administration immediately

This phase must land before registry-driven enforcement because these APIs can change the authorization system itself.

#### Step 1.1: Define bootstrap administration privileges

Implementation status: **implemented**. `BootstrapAdministrationPrivilegeProvider` defines the administration catalog using the existing 11-character module/submodule/feature-type/feature/action composition. `BootstrapAdministrationPrivileges` exposes stable constants for method-level authorization. System submodules now explicitly cover Access Control (`02`), Layout (`04`), and Workflow (`05`). Actions use category ranges: read-only/low-impact `01`–`09`, data creation/modification `10`–`19`, workflow decisions `20`–`29`, lifecycle changes `30`–`39`, destructive operations `40`–`49`, and system administration `80`–`89`. This step defines catalog metadata only; persistent seeding and bootstrap-role assignment are handled by Step 1.2.

Extend the system privilege catalog with explicit actions for:

- Client application view/manage.
- Client credential rotate.
- Client API permission assign.
- Client feature permission assign.
- Client tenant assignment.
- API registry view/manage/synchronize.
- Privilege catalog view/synchronize/assign.
- Layout administration.
- Workflow administration.
- License administration.

Use existing module/submodule/feature/action code composition. Do not introduce ad hoc role-name checks as the long-term authorization model.

#### Step 1.2: Seed bootstrap privileges safely

Implementation status: **implemented**. `V21__seed_bootstrap_administration_privileges.sql` idempotently inserts or reconciles the System module, administration submodules, setup features, and all 18 bootstrap privilege records using system actor `0`. It also adds the missing actor audit columns to privilege and role-assignment records. Because roles are owned by `auth_db` while privilege assignments are owned by `system_db`, the migration deliberately does not assume a numeric role ID. `DataSeeder` resolves `ROLE_ADMIN` by name from `auth_db` and assigns the provider-derived privilege codes through `SystemPrivilegeRegistryService`; the default KYC roles receive only their explicit KYC privilege sets.

Add the next system Flyway migration after `V13__prefix_privilege_owned_tables.sql` to:

- Insert the administration catalog records into `sys_priv_modules`, `sys_priv_submodules`, `sys_priv_features`, and `sys_priv_privileges`.
- Assign them to the designated bootstrap administrator role.
- Preserve existing assignments with idempotent inserts or conflict handling.
- Set `created_by` and `updated_by` to the documented system actor for seed data.

Never assign these privileges to the default user role.

#### Step 1.3: Apply method-level checks

Implementation status: **implemented**. `PrivilegeAuthorizer` evaluates the authenticated username against effective privilege codes through the privilege module gateway, independently of API-registry mappings. Action-specific `@PreAuthorize` checks protect client application and API-registry administration, privilege catalog mutations and assignments, layout administration, workflow definition administration, and license plan/subscription/entitlement/key administration. Read-only administration endpoints require their corresponding VIEW privilege; specialized credential rotation, assignment, and synchronization operations use their narrower bootstrap privileges.

Add `@PreAuthorize` to administrative controllers, starting with:

- `ClientApplicationController`.
- `ApiRegistryController`.
- `PrivilegeController` mutation operations.
- `LayoutController` mutation operations.
- `WorkflowDefinitionController` mutation/publish/retire operations.
- License plan, subscription, entitlement, and key-management operations.

Use a privilege-aware authorization bean, for example:

```java
@PreAuthorize("@privilegeAuthorizer.has(authentication, 'requiredPrivilegeCode')")
```

Acceptance criteria:

- An ordinary authenticated user receives `403` for every security-administration mutation.
- The bootstrap administrator can perform the operation.
- Tests verify both cases independently of API-registry state.

### Phase 2: Separate access and refresh tokens

#### Step 2.1: Define token types

Introduce a token-type enum such as:

```text
ACCESS
REFRESH
```

Update `JwtUtil` so generated tokens contain:

- `token_type`.
- `iss`.
- `aud`.
- `jti`.
- Subject.
- Issued-at and expiration.
- Roles only where needed.

Keep secrets and issuer/audience values environment-driven.

#### Step 2.2: Enforce token purpose

Update `JwtAuthenticationFilter` to:

- Accept only `ACCESS` tokens.
- Treat missing/malformed roles as authentication failure rather than a server error.
- Validate issuer and audience.
- Convert parsing and claim-shape errors to a consistent `401` response.
- Avoid logging raw tokens or claims containing sensitive data.

Update refresh processing to:

- Accept only `REFRESH` tokens.
- Reject access tokens at the refresh endpoint.
- Verify session/revocation state.
- Rotate the refresh token when configured.

#### Step 2.3: Correct refresh routing

Add `/api/v1/auth/refresh-token` to the authoritative public authentication paths. It is public only in the sense that it does not require an access token; it still requires a valid refresh token and applicable client/origin controls.

Acceptance criteria:

- Access token used for refresh: `401`.
- Refresh token used as bearer on a protected API: `401`.
- Expired access token plus valid refresh token: refresh succeeds.
- Revoked/expired refresh token: `401`.

### Phase 3: Centralize public-route and client classification

#### Step 3.1: Create one public-route source

Implementation status: **implemented**. `PublicRoutePolicy` is the authoritative public-path list used by `SecurityConfig`, `ClientApplicationAuthenticationFilter`, `ClientApiAccessFilter`, and `UserPrivilegeApiAccessFilter`. The refresh endpoint is included as an unauthenticated-access-token route, and browser origin URLs have been removed from Spring request matchers.

Remove duplicated arrays from the security filters. Define one shared public-route policy used by:

- `SecurityConfig`.
- `ClientApplicationAuthenticationFilter`.
- `ClientApiAccessFilter`.
- `UserPrivilegeApiAccessFilter`.

Remove origin URLs such as `http://localhost:4200` from request matchers. Origins belong in CORS policy.

#### Step 3.2: Distinguish public and confidential clients

Implementation status: **implemented**. `ClientApplicationType` classifies WEB and MOBILE as public clients and POS, ERP, PARTNER_PORTAL, and INTERNAL_SERVICE as confidential clients. Public clients establish application context with an active client code and do not treat a bundled API key as a secret requirement. Confidential clients require a valid API key. Resolved browser clients are also checked against their normalized per-client allowed-origin list.

Define client authentication behavior by `ClientApplicationType`:

- Browser SPA/public client: client code is context, not a secret credential.
- Mobile/public client: use an appropriate public-client flow and attestation where required.
- Internal service/partner/confidential client: require API key, client secret, mTLS, or client-credential token.

Do not ship a confidential API key inside Angular bundles.

#### Step 3.3: Externalize CORS

Implementation status: **implemented**. `CorsProperties` binds allowed origins, methods, headers, exposed headers, credential behavior, and preflight max age from environment-backed configuration. `SecurityConfig` consumes these properties rather than compiled localhost values, while `ClientOriginPolicy` enforces the resolved client's narrower origin allowlist.

Move allowed origins, methods, and headers to typed configuration. Validate requested origin against deployment policy and, where applicable, the resolved client application's allowed origins.

Acceptance criteria: all filters agree on public paths and browser origins are changed without recompiling the backend.

### Phase 4: Annotate and register every API

#### Step 4.1: Improve `@ClientSecuredApi`

Keep the existing annotation as the source for registry metadata. Where useful, add:

- A required privilege-code override for exceptional mappings.
- A client-authentication requirement.
- A data-scope requirement.
- Clear method-overrides-type semantics.

Avoid making `publicApi=true` the default.

#### Step 4.2: Annotate controllers in risk order

Annotate controller methods in this order:

1. Client application and API registry.
2. Privilege, layout, workflow, and license administration.
3. Person and document APIs.
4. GIS APIs.
5. Authentication context APIs.
6. Remaining controllers.

Use action-specific privilege codes. For example, person search and document download should not share create/update permissions.

#### Step 4.3: Make synchronization deterministic

Update `ClientApiRegistryServiceImpl#syncFromAnnotations` to:

- Normalize paths and HTTP methods.
- Reject mappings without an explicit HTTP method.
- Detect duplicate API codes and overlapping patterns.
- Update existing annotated entries deterministically.
- Mark removed annotation-managed entries inactive rather than deleting audit history.
- Distinguish annotation-managed records from manually managed records.
- Produce a synchronization report with added, changed, unchanged, deactivated, and conflicted counts.

If required, add registry fields such as `source`, `priority`, and `last_synchronized_at` through a Flyway migration.

#### Step 4.4: Add registry coverage tests

Create an integration test using `RequestMappingHandlerMapping` that fails when:

- An application-owned `/api/**` mapping lacks `@ClientSecuredApi`.
- A mapping is accidentally declared public without appearing in the approved public-route set.
- Two protected mappings generate the same API code.
- Required privilege composition is invalid.

Acceptance criterion: 100% of application-owned API mappings have reviewed metadata.

### Phase 5: Seed client permissions before enforcement

#### Step 5.1: Synchronize API registry in a controlled environment

Run annotation synchronization using an authorized system actor. Review the generated registry before enabling enforcement.

#### Step 5.2: Assign client API permissions

For each registered client:

- Grant only the APIs it calls.
- Grant only the matching feature privileges.
- Assign allowed tenants/businesses where applicable.
- Generate confidential credentials only for clients capable of protecting them.

Do not interpret an empty permission set as unrestricted access.

#### Step 5.3: Validate frontend traffic in report mode

Run normal Angular workflows with `enforcement-mode=REPORT`. Record would-deny decisions by:

- API code.
- Client code.
- Username or safe user identifier.
- Required privilege code.
- Denial reason.
- Trace ID.

Never log API keys, bearer tokens, raw PII, or request bodies.

Acceptance criterion: normal approved workflows produce no unexplained would-deny events.

### Phase 6: Change unresolved APIs to fail closed

#### Step 6.1: Resolve once per request

Avoid resolving the API registry independently in multiple filters. Introduce a single resolver filter or request-scoped decision context containing:

- Resolved API registry record.
- Client application.
- Client decision.
- User decision.
- Required privilege.
- Trace ID.

This prevents inconsistent database reads and matching results.

#### Step 6.2: Enforce unresolved-route denial

In `ENFORCE` mode:

```text
protected /api/** + no active registry match -> deny API_NOT_REGISTERED
```

Public routes must be explicitly approved. Do not infer public access merely because an endpoint is absent from the registry.

#### Step 6.3: Roll out progressively

Recommended rollout:

1. Local/test: `ENFORCE` immediately after coverage tests pass.
2. Staging: `REPORT`, then `ENFORCE` after traffic validation.
3. Production: canary instance or limited client cohort, then full `ENFORCE`.

Rollback changes only the enforcement mode. Do not delete registry or permission data during rollback.

### Phase 7: Enforce tenant, business, and branch scope

#### Step 7.1: Define authenticated request context

Create an immutable request context containing:

- User ID and username.
- Client application ID/code.
- Tenant ID.
- Business ID.
- Branch ID.
- Trace ID.
- Effective privilege codes.

Values must come from validated token/client state and server-side assignments, not blindly from caller headers.

#### Step 7.2: Validate client scope

Use `sys_priv_client_application_tenants` to verify that the resolved client can access the requested tenant/business context.

#### Step 7.3: Apply data scope in services and repositories

Update sensitive queries so scope is part of the database predicate. Start with:

- Person search and detail.
- Photos and documents.
- Workflow tasks and history.
- Client, privilege, layout, and license configuration.

Do not fetch cross-tenant data and filter it in memory.

#### Step 7.4: Test object-level authorization

Add tests proving that a user/client authorized for tenant A cannot read, update, delete, or download tenant B records, including direct-ID requests.

### Phase 8: Enforce client operational policy

#### Step 8.1: Allowed origins

Parse and normalize configured origins. Reject mismatched origins for browser traffic where origin validation applies. Treat absent `Origin` correctly for server-to-server calls.

#### Step 8.2: Allowed IP addresses

Support explicit IP/CIDR rules. Trust forwarded headers only when the application is behind a configured trusted proxy; otherwise use the remote address.

#### Step 8.3: Rate limiting

Implement per-client and, where needed, per-route limits using Redis or the gateway layer. Return `429` with a stable error code and safe retry metadata.

Do not rely on an in-memory counter in a multi-instance deployment.

### Phase 9: Improve registry matching and performance

#### Step 9.1: Make route precedence explicit

Add deterministic matching rules:

1. Exact method and exact path.
2. Most specific path template.
3. Explicit registry priority.
4. Reject unresolved ties.

Add a unique constraint where possible and synchronization-time overlap validation.

#### Step 9.2: Cache safe authorization data

Cache:

- Active API registry mappings.
- Client API/feature grants.
- User effective privilege codes.

Invalidate caches after registry sync, permission assignment, user/role privilege changes, client status changes, or credential rotation. Keep TTLs short enough to bound stale authorization.

#### Step 9.3: Avoid writes on every API-key request

`lastUsedAt` updates currently write during credential validation. Throttle or asynchronously aggregate these updates to avoid turning every authenticated request into a database write.

### Phase 10: Audit and observability

#### Step 10.1: Emit structured authorization events

Record safe fields:

- Trace ID.
- API code and path template.
- Client code.
- Safe user identifier.
- Decision and denial code.
- Required privilege code.
- Tenant/business/branch identifiers where non-sensitive.
- Duration.

#### Step 10.2: Add metrics

Track at minimum:

- Allowed and denied request counts.
- Denials by reason.
- Unregistered API attempts.
- Invalid client credential attempts.
- Invalid token-type attempts.
- Registry-resolution latency.
- Authorization cache hit rate.

Alert on spikes in administrative denials, invalid credentials, and unregistered API access.

### Phase 11: Complete automated verification

#### Step 11.1: Unit tests

Cover token type, route matching, client decisions, user privilege decisions, tenant scope, origin/IP parsing, and cache invalidation.

#### Step 11.2: Filter-chain integration tests

Use MockMvc or Spring Boot integration tests to verify the complete filter order and response contract for every row in the required test matrix below.

#### Step 11.3: Database migration tests

Run Flyway against:

- An empty system database.
- A snapshot using pre-`sys_priv_` table names.
- A current database at migration `V13`.

Verify data, foreign keys, indexes, and audit records after migration.

#### Step 11.4: Security regression tests

Add explicit regressions for:

- Refresh token used as access token.
- Missing registry record.
- Ordinary user rotating a client key.
- Direct-ID cross-tenant document access.
- Disabled client with otherwise valid credentials.
- Removed privilege while a session is active.

### Phase 12: Production readiness and operating procedure

Before production enforcement, confirm:

- API registry coverage is 100%.
- Public endpoints have security-owner approval.
- Every production client has reviewed API and feature grants.
- Browser clients do not contain confidential keys.
- Bootstrap administrator privileges are limited and audited.
- Token issuer/audience/type validation is enabled.
- Tenant/business/branch isolation tests pass.
- CORS origins are environment-specific.
- Rate limits use shared infrastructure.
- Dashboards and alerts are active.
- Rollback to `REPORT` mode is documented and tested.

## Implementation Work Packages

| Work package | Primary code area | Depends on | Completion gate |
| --- | --- | --- | --- |
| WP-1 Admin protection | Controllers, privilege authorizer, privilege catalog | Phase 0 | Ordinary user denied admin mutations |
| WP-2 Token hardening | `JwtUtil`, JWT filter, auth service | Phase 0 | Access/refresh misuse tests pass |
| WP-3 Public/client policy | Security config and filters | WP-2 | One authoritative public-path policy |
| WP-4 API metadata | Controllers, annotation, registry sync | WP-1 | Registry coverage reaches 100% |
| WP-5 Client grants | Access-control services and migrations | WP-4 | Approved workflows clean in report mode |
| WP-6 Fail-closed enforcement | Resolver/decision filters | WP-4, WP-5 | Unregistered protected APIs denied |
| WP-7 Data scope | Context, services, repositories | WP-6 | Cross-tenant tests pass |
| WP-8 Client operations | Origin, IP, rate-limit policy | WP-3, WP-6 | Policy integration tests pass |
| WP-9 Performance/audit | Cache, logs, metrics | WP-6 | Load and observability gates pass |
| WP-10 Production rollout | Configuration and operations | All | Production readiness checklist approved |

## Required Test Matrix

| Scenario | Expected result |
| --- | --- |
| Public registered API without JWT/client | Allowed |
| Private registered API without client | `401` or `403` according to policy |
| Invalid or expired client credential | `401` |
| Client lacks API permission | `403` |
| Client lacks feature permission | `403` |
| Missing or invalid access JWT | `401` |
| Refresh token used as bearer access token | `401` |
| User lacks required privilege | `403` |
| User and client both allowed | Allowed |
| Unregistered protected endpoint | Denied in enforcement mode |
| Client outside allowed origin/IP | Denied |
| User requests another tenant's data | Denied or filtered |
| Ordinary user calls API-registry/client administration | `403` |

## Final Assessment

The backend's main risk is not absence of authentication. The principal risk is that authorization metadata is optional and unresolved APIs are allowed through. Closing the registry fail-open path, protecting administrative controllers, and distinguishing access tokens from refresh tokens should be treated as the first security milestone.
