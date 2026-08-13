# Tenant, Privilege, and API Access-Control Test Roadmap

## Purpose

This roadmap verifies the complete authorization path from tenant resolution through authenticated request-context construction, client/API authorization, user privilege enforcement, and object-level data isolation.

The primary component under test is `AuthenticatedRequestContextFilter`. It is the boundary that converts authenticated token identity plus server-owned account assignments into the immutable authorization context consumed by downstream privilege and data-scope controls.

## Security invariants

Every test layer must preserve these rules:

1. The request hostname, token account tenant, client tenant assignment, and effective user scope must agree.
2. Tenant, business, and branch authority comes only from server-owned scope assignments; request headers and DTO fields cannot grant scope.
3. A branch scope cannot widen to its business or tenant. A business scope may cover its branches, and a tenant scope may cover its descendants.
4. The effective privilege set is an immutable, client-filtered snapshot for one request.
5. Protected APIs fail closed when the client, API registry record, authentication, privilege, or tenant scope is missing or contradictory.
6. Direct-ID reads, mutations, histories, photos, documents, and downloads include scope in the database predicate.
7. Request-scoped holders are cleared on success, denial, and unexpected downstream exceptions.
8. Denial responses use stable HTTP statuses and error codes without revealing cross-tenant object existence.

## Authorization path under test

```text
TenantResolutionFilter
  -> ClientApplicationAuthenticationFilter
  -> ClientApiAccessFilter
  -> JwtAuthenticationFilter
  -> AuthenticatedRequestContextFilter
       -> AuthModuleGateway.getUserAccess(accountId, tokenTenantId)
       -> PrivilegeService.getUserPrivilegeCodes(username)
       -> EffectiveTenantAccessResolver.resolve(...)
       -> AuthenticatedRequestContextHolder
       -> EffectiveTenantAccessContextHolder
       -> enriched ClientApplicationContextHolder
  -> UserPrivilegeApiAccessFilter
  -> controller / service / scoped repository predicate
```

The configured ordering in `SecurityConfig` is itself a security contract and must have an integration test.

## Current coverage baseline

| Area | Existing evidence | Remaining focus |
|---|---|---|
| Full security chain | `AccessControlFilterChainIntegrationTest` | Add business/branch matrices, context cleanup, malformed principal, and client/tenant mismatch |
| Tenant intersection | `EffectiveTenantAccessResolverTest` | Cover all denial branches and multi-assignment filtering |
| Context immutability | `AuthenticatedRequestContextTest` | Exercise construction by the filter, not only the record |
| User API privilege | `UserPrivilegeApiAccessFilterTest` | Add missing context/API metadata and privilege snapshot consistency cases |
| API metadata | `ApiMetadataCoverageTest`, `ApiRouteMatcherTest` | Keep as mandatory CI gates |
| Object authorization | Person and workflow object-authorization tests | Expand to every tenant-owned resource and HTTP entry point |
| Filter-specific behavior | Indirectly covered | Add `AuthenticatedRequestContextFilterTest` |

## Phase 1 — Unit-contract tests for `AuthenticatedRequestContextFilter`

Create:

`backend/src/test/java/com/nexacore/systemmodule/accesscontrol/security/AuthenticatedRequestContextFilterTest.java`

Use Mockito, `MockHttpServletRequest`, `MockHttpServletResponse`, a real `ApiResponseJsonWriter`, and a recording filter chain. Clear every holder and `SecurityContextHolder` in `@AfterEach`.

| ID | Scenario | Expected result |
|---|---|---|
| ARCF-01 | No authentication | Chain continues; gateways are not called; no request contexts are created |
| ARCF-02 | Unauthenticated authentication object | Same behavior as no authentication |
| ARCF-03 | Valid `TenantAccountUserDetails`, client, scopes, and privileges | Context contains DB user ID, authenticated username, client ID/code, trace ID, effective scopes, and privileges |
| ARCF-04 | Auth gateway lookup | Called with principal `accountId` and principal `tenantId`, never caller-provided values |
| ARCF-05 | Null account scope assignments | Resolver receives an empty set; request fails closed if no effective scope exists |
| ARCF-06 | Multiple tenant assignments | Only resolver-returned assignments for the verified tenant enter both request contexts |
| ARCF-07 | Valid authentication with a non-tenant principal | Chain does not reach the endpoint; define and assert the stable authentication error contract |
| ARCF-08 | Resolver throws `DataScopeAccessDeniedException` | Response is `403 DATA_SCOPE_NOT_ALLOWED`; chain is not invoked |
| ARCF-09 | Downstream chain throws | Exception propagates, while authenticated and effective-tenant holders are still cleared |
| ARCF-10 | Successful request cleanup | Both holders are populated inside the chain and empty after the filter returns |
| ARCF-11 | Denied request cleanup | Both holders are empty after the denial response |
| ARCF-12 | Existing client context enrichment | Registry, required privilege, client/user decisions, denial reasons, and trace ID are preserved; user ID and effective scopes are added |
| ARCF-13 | Missing client context | Tenant resolution fails closed; no partial authenticated context survives |
| ARCF-14 | Mutable privilege/scope source sets | Later mutation cannot alter the stored request snapshot |
| ARCF-15 | Sequential requests on one thread | Request B cannot observe request A's user, scope, client, trace, or privileges |

Phase gate: all filter branches are covered, including `finally`, and no assertion depends only on implementation internals.

## Phase 2 — Tenant intersection and hierarchy

Extend `EffectiveTenantAccessResolverTest` with this decision matrix:

| ID | Verified host tenant | Token tenant | Client assigned | User scope | Result |
|---|---:|---:|---|---|---|
| TEN-01 | A | A | A | tenant A | Allow |
| TEN-02 | A | B | A | tenant B | Deny token replay |
| TEN-03 | A | A | B only | tenant A | Deny client/tenant mismatch |
| TEN-04 | A | A | A | tenant B only | Deny empty effective scope |
| TEN-05 | A | A | A | tenant A plus tenant B | Allow only tenant A assignments |
| TEN-06 | A | A | A | business A1 | Allow A1 and its authorized branches only |
| TEN-07 | A | A | A | branch A1-X | Allow branch A1-X; deny business A1 and sibling branch A1-Y |
| TEN-08 | Missing | A | A | tenant A | Deny missing verified tenant |
| TEN-09 | Host A | A | A | tenant A, but resolved hostname differs | Deny contradictory context |
| TEN-10 | Normalized Host A with port/case | A | A | tenant A | Allow after canonical normalization |

Also test invalid `UserScopeAssignment` construction: null tenant and branch without business must be rejected.

Phase gate: every allow decision is the intersection of verified tenant, account tenant, client assignment, and account scope.

## Phase 3 — Privilege-to-API decision matrix

Extend `UserPrivilegeApiAccessFilterTest` and the filter-chain integration test:

| ID | API policy | Client grant | User privilege | Mode | Expected result |
|---|---|---|---|---|---|
| PRIV-01 | Public | Not required | Not required | ENFORCE | Allow |
| PRIV-02 | Authenticated | Allowed | No specific privilege | ENFORCE | Allow authenticated user |
| PRIV-03 | Privileged P1 | P1 granted | User has P1 | ENFORCE | Allow |
| PRIV-04 | Privileged P1 | API grant missing | User has P1 | ENFORCE | `403 CLIENT_API_NOT_ALLOWED` |
| PRIV-05 | Privileged P1 | Feature grant missing | User has P1 | ENFORCE | `403 CLIENT_FEATURE_NOT_ALLOWED` |
| PRIV-06 | Privileged P1 | P1 granted | User lacks P1 | ENFORCE | `403 USER_PRIVILEGE_NOT_ALLOWED` |
| PRIV-07 | Privileged P1 | P1 granted | User has P1 only through another client | ENFORCE | Deny; effective client-filtered set excludes P1 |
| PRIV-08 | Privileged P1 | P1 granted | User privilege revoked between requests | ENFORCE | First request allowed, next request denied without requiring logout |
| PRIV-09 | Privileged P1 | P1 granted | User lacks P1 | REPORT | Continue and record would-deny event |
| PRIV-10 | Unregistered protected API | Any | Any | ENFORCE | `403 API_NOT_REGISTERED` |
| PRIV-11 | Ambiguous registry match | Any | Any | ENFORCE | `403 API_REGISTRY_AMBIGUOUS` |
| PRIV-12 | Missing authenticated request context | Allowed | Any | ENFORCE | Fail closed; never fall back to raw roles or caller data |

For every denial, assert the stable error code, HTTP status, chain non-invocation where applicable, and the stored decision/reason used by audit logging.

Phase gate: no privileged endpoint is authorized from a JWT role alone; the exact resolved API record and effective privilege snapshot drive the decision.

## Phase 4 — End-to-end filter-chain tests

Expand `AccessControlFilterChainIntegrationTest` using parameterized scenarios where practical.

Required flows:

1. Valid host tenant + client tenant assignment + access token tenant + account scope + API grant + privilege returns 2xx.
2. Change each input independently to another tenant and prove `403 DATA_SCOPE_NOT_ALLOWED`.
3. Verify the endpoint is never invoked after tenant, client, API, authentication, or privilege denial.
4. Capture contexts inside a harness controller and assert the same client, API, trace, user, scope, and privilege decision travels through the chain.
5. Perform two requests on the same MockMvc/test thread with different users and tenants to detect thread-local leakage.
6. Assert public routes do not accidentally trigger protected-context requirements.
7. Assert an authenticated-only endpoint receives tenant context but does not require an unrelated privilege.
8. Assert error precedence follows filter order; for example, an invalid client is rejected before JWT or user privilege evaluation.

Phase gate: the actual `SecurityConfig` filter order is tested rather than recreated manually.

## Phase 5 — Repository-backed object authorization

For each tenant-owned aggregate, test list/search and direct-ID operations. Use repository/service tests for predicate behavior and MockMvc integration tests for the HTTP contract.

| Resource operation | Same scope | Different tenant | Different business | Sibling branch | Missing scope |
|---|---|---|---|---|---|
| Search/list | Visible | Excluded | Excluded unless tenant assignment covers it | Excluded unless parent scope covers it | Denied |
| Read by ID | Returned | Not found | Not found | Not found | Denied |
| Update by ID | Updated | Not found/no write | Not found/no write | Not found/no write | Denied |
| Delete by ID | Deleted | Not found/no delete | Not found/no delete | Not found/no delete | Denied |
| History/task action | Allowed | Not found/no mutation | Not found/no mutation | Not found/no mutation | Denied |
| Photo/document download | Returned | Not found; storage untouched | Not found; storage untouched | Not found; storage untouched | Denied |

Priority resources are KYC profiles, details, photos, documents, workflow tasks/instances/history, and any newly introduced tenant-owned records. Cross-scope direct IDs should normally return not found to avoid object-existence disclosure.

Phase gate: SQL/JPA predicates contain scope; tests verify that code does not fetch cross-scope data and filter it in memory.

## Phase 6 — Persistence, migration, and cache consistency

Add integration coverage for:

- Unique mandatory `auth_users.person_id` and valid server-side scope assignments.
- Client-to-tenant assignments and client feature/API grants, including inactive records.
- Privilege assignment activation/deactivation and cache invalidation.
- Effective privilege cache separation by user and client/tenant context.
- Flyway migrations and seed privileges with `created_by` and `updated_by` populated.
- Unresolved legacy ownership remaining inaccessible instead of being silently inferred.

Use PostgreSQL/Testcontainers where query semantics or constraints matter; keep pure filter decisions as fast unit tests.

Phase gate: authorization changes are observable on the next request or within the explicitly documented cache bound, without cross-tenant cache bleed.

## Phase 7 — Abuse, concurrency, and observability

Add negative and operational tests for:

- Spoofed tenant/business/branch headers and DTO identifiers.
- Token replay across hostnames/tenants.
- Disabled client, inactive tenant assignment, inactive privilege, and removed API grant.
- Concurrent requests on reused container threads with different tenant contexts.
- Async processing: either explicitly propagate an immutable approved context or prove access fails closed outside the request thread.
- `REPORT` versus `ENFORCE` behavior using the same denial inputs.
- Audit events containing trace ID, safe username, client/API code, tenant IDs, required privilege, and denial reason.
- Audit/log output excluding bearer tokens, API keys, request bodies, and PII.
- Stable metrics labels that do not create unbounded user/tenant cardinality.

Phase gate: concurrency and diagnostics do not leak authorization state or secrets.

## CI execution plan

Run the narrowest checks first:

```bash
cd backend
mvn -Dtest=AuthenticatedRequestContextFilterTest,EffectiveTenantAccessResolverTest,UserPrivilegeApiAccessFilterTest test
mvn -Dtest=AccessControlFilterChainIntegrationTest,ApiMetadataCoverageTest test
mvn -Dtest=PersonServiceObjectAuthorizationTest,WorkflowObjectAuthorizationTest test
mvn test
```

Recommended CI gates:

1. Pull request: phases 1–4, metadata coverage, and affected object-authorization tests.
2. Main branch: full backend unit/integration suite.
3. Nightly or environment-capable job: PostgreSQL/Testcontainers migrations, repository isolation, cache invalidation, and concurrency tests.
4. Deployment promotion: run synthetic tenant A/tenant B allow/deny probes in `REPORT`, inspect would-deny events, then repeat in `ENFORCE`.

## Definition of done

The roadmap is complete when:

- Every protected controller has explicit API metadata and an automated coverage assertion.
- Every `AuthenticatedRequestContextFilter` branch and cleanup path has a focused test.
- Tenant, client, API, and privilege decisions are covered independently and end to end.
- Every tenant-owned direct-ID operation has a cross-tenant denial test.
- Business/branch hierarchy cannot widen privileges or data visibility.
- Revocation, cache isolation, thread-local cleanup, and audit safety are verified.
- The full backend suite passes with enforcement mode enabled.

