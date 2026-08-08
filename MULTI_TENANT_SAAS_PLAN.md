# Multi-Tenant SaaS Upgrade Plan

Net-new architecture plan (not a fix to an existing finding) to move the backend from
a single-organization deployment to a multi-tenant SaaS deployment: one shared
backend/application codebase, tenant-specific domains, tenant-specific business
configuration (branding/layout/entitlements), shared-schema data isolation
(`tenant_id` column everywhere within each existing database, no database-per-tenant).
Not yet implemented — this is the plan to review before starting any of it. Related but
separate from [SYSTEM_MODULE_AUDIT_FINDINGS.md](./SYSTEM_MODULE_AUDIT_FINDINGS.md) /
[SYSTEM_MODULE_RESOLUTION_PLAN.md](./SYSTEM_MODULE_RESOLUTION_PLAN.md), which fix
gaps in what already exists; this plan builds something that doesn't exist yet, on top
of that module once its Phase 1 (§1.1 permission seeding, §1.2 route/UI policy decision)
lands. Three specific overlaps with that plan, cross-referenced at the relevant section
below: migration numbering (both draw `V<n>` from the same `db/migration/system/`
sequence), `DataScopeService` (a modification target in both — see Resolution Plan §4.2),
and `SysPrivClientApplicationTenant` (a read-endpoint target in Resolution Plan §3.1,
whose `tenantId` field only becomes a real foreign key once this plan's `sys_tenant`
table exists).

## Decision made

**Shared schema, `tenant_id` column on every tenant-owned table, one physical database
per existing module** (`auth_db`/`system_db`/`kyc_db`/`gis_db`/`log_db` stay as they
are — "single DB" means no database-per-tenant split, not collapsing the existing
5-database module split into one). Chosen over schema-per-tenant or database-per-tenant
for operating cost and to keep the "one codebase, one deploy" story intact. The
tradeoff accepted: correctness now depends entirely on every query being tenant-scoped
— there is no database-level wall between tenants' data, so a missed `WHERE tenant_id =
?` is a cross-tenant data leak, not a slow query. Section "Enforcement" below exists
because of this tradeoff; it is the most important part of this plan.

## Current state (confirmed by code, not assumed)

- **5 physical databases today**, each with its own `DataSource`/
  `EntityManagerFactory`/`JpaTransactionManager` config class: `auth_db`
  (`authmodule/core/config/AuthDbConfig.java`), `system_db`
  (`systemmodule/config/SystemDbConfig.java`), `kyc_db`
  (`kycmodule/config/KycDbConfig.java`), `gis_db` (`gismodule/config/GisDbConfig.java`),
  `log_db` (`logmodule/config/LogDbConfig.java`). `workflow` has no database of its own
  — its entities live inside `system_db`.
- **No `Tenant`/`Business` entity or table exists anywhere.** `tenantId`/`businessId`
  appear as bare `Long` fields with no FK to any tenant table, scattered across
  `AuthUserScopeAssignment` (`auth_db`), `KycPersonProfile`/
  `KycPersonOrganizationMembership` (`kyc_db`), and `SysLicenseSubscription`/
  `SysLicenseAuditEvent`/`SysLicenseUsageSnapshot`/`SysWorkflowDefinition`/
  `SysWorkflowInstance`/`SysWorkflowTask`/`SysPrivClientApplicationTenant` (`system_db`).
- **These existing `tenantId` fields are scope-assignment-shaped, not
  row-ownership-shaped** — `AuthUserScopeAssignment.tenantId` means "this user is
  granted access to tenant X," it is not "this row belongs to tenant X." The tables
  that actually need row-ownership tenant scoping for this upgrade (users, client
  applications, license rows, KYC persons, workflow instances, layout profiles,
  nav tree nodes — effectively everything) mostly don't have that concept today. Reusing
  the existing fields is not suffient; most of the column-adding work below is new, not
  a rename of what's there.
- **A real manual scoping mechanism already exists**: `DataScopeService`
  (`systemmodule/accesscontrol/security/DataScopeService.java`) reads
  `AuthenticatedRequestContext.scopeAssignments` (populated by
  `AuthenticatedRequestContextFilter` from `AuthUserScopeAssignment` rows fetched via
  `AuthModuleGateway.getUserAccess(username)`) and exposes
  `restrictToCurrentScopes(...)`/`requireWritableScope(...)`, called manually and
  independently by individual services (`PersonService`, `LocalWorkflowEngine`,
  License services) using hardcoded attribute names. This is the pattern to extend, not
  replace — but see Enforcement below for why manual-only is not enough once tenants
  are paying customers on a shared schema. This class already carries an unresolved
  `@Todo` about simplifying tenant scoping (`SYSTEM_MODULE_AUDIT_FINDINGS.md` §4.1,
  resolution tracked in `SYSTEM_MODULE_RESOLUTION_PLAN.md` §4.2 as triage-only, needs
  the original author's input) — get that context before touching it, but design the
  actual change together with this plan's Enforcement §3 Layer 2 below rather than as
  two independent edits to the same class.
- **`SysPrivClientApplicationTenant` is an existing but currently-unbacked concept**:
  a join table mapping a client application to allowed tenant/business IDs
  (`systemmodule/accesscontrol/entity/SysPrivClientApplicationTenant.java`). Its
  `tenantId` is the same kind of bare, FK-less `Long` as everything else in this
  section — `SYSTEM_MODULE_RESOLUTION_PLAN.md` §3.1 plans to add a read-only
  `getTenantAssignments(clientApplicationId)` getter for it, which will correctly
  return whatever IDs exist but can't resolve them to a real tenant name/label until
  `sys_tenant` (this plan's §1, "Tenant identity") exists. Once it does, this table
  becomes the natural place to express "which tenants can this client application see
  data for" for cross-tenant clients like `SYSTEM_ADMIN_WEB` — worth revisiting
  alongside the platform-admin bypass design in Enforcement §3 below, rather than
  building a separate mechanism for the same concept.
- **`ApiDataScope`/`SysPrivApiRegistry.dataScope`** (`NONE`/`TENANT`/`BUSINESS`/
  `BRANCH`) is recorded per-API but never read back anywhere for enforcement — it's
  metadata today. Worth reusing as the annotation surface (`@AuthenticatedApi(dataScope
  = ApiDataScope.TENANT)` already exists on some endpoints), but only once something
  actually enforces it — otherwise this upgrade repeats the exact gap the system module
  audit already flagged as Finding §3 for this same enum.
- **No domain/hostname resolution exists anywhere.** Zero references to `Host`,
  `X-Forwarded-Host`, or `getServerName()` in the backend. The closest existing
  infrastructure is `ClientOriginPolicy` (`systemmodule/accesscontrol/security/
  ClientOriginPolicy.java`), which validates the browser's `Origin` header against
  `SysPrivClientApplication.allowedOrigins` — a working host-normalization utility
  (`normalize()`, `IDN.toASCII`, port handling) worth mirroring, but it answers "is this
  caller allowed," not "which tenant does this inbound domain belong to."
- **The `ClientApplicationContextHolder` pattern is the template to copy**: a
  `ThreadLocal<ClientApplicationContext>` (`ClientApplicationContextHolder.java`), set
  by `ClientApiAccessFilter`/`ClientApplicationAuthenticationFilter` from the
  `X-Client-Code` header, cleared in a single `finally` block in the outermost filter
  (`ClientApplicationAuthenticationFilter.java`). Note this holder is cleared by one
  filter on behalf of three others that write to it — reusing this exact shape for
  tenant context means the new tenant filter must run outermost too, or clear itself
  independently; don't silently inherit the same single-point-of-failure without
  deciding which.
- **The JWT carries no tenant claim** (`JwtUtil.generateToken` only adds `roles`/
  `token_type` plus standard claims). Tenant is not embedded in the session token
  today.
- **No Hibernate multi-tenancy support is configured** (`pom.xml`/
  `application.properties` have zero references to `hibernate.multiTenancy`,
  `MultiTenantConnectionProvider`, or `CurrentTenantIdentifierResolver`). Tenant
  scoping, if added, is either hand-rolled (matching the existing `DataScopeService`
  style) or a new adoption of Hibernate's native support — this is a real decision, not
  a given; see Enforcement below.

## Target architecture

### 1. Tenant identity

New tables in `system_db` (same database that already owns `SysPrivClientApplication`
— tenants and client applications are adjacent concepts: a client application is
"which frontend," a tenant is "which customer organization"):

- `sys_tenant`: `id`, `tenant_code` (unique, human-readable), `tenant_name`, `status`
  (`ACTIVE`/`SUSPENDED`/`CANCELLED` — mirrors `LicenseStatus`'s existing shape rather
  than inventing a new one), `active`, audit columns (matches every existing `sys_*`
  table's convention).
- `sys_tenant_domain`: `id`, `tenant_id` (FK), `domain` (unique, normalized via the
  same `IDN.toASCII`/lowercase logic `ClientOriginPolicy.normalize()` already has),
  `is_primary`, `active`. One tenant can own multiple domains (custom domain +
  default subdomain), which is why this is its own table rather than a column on
  `sys_tenant`.

### 2. Request-scoped tenant resolution

New `TenantResolutionFilter`, modeled directly on `ClientApiAccessFilter`: reads the
`Host` header (normalized the same way `ClientOriginPolicy` already normalizes
`Origin`), looks up `sys_tenant_domain`, and sets a `TenantContext {tenantId,
tenantCode}` into a new `TenantContextHolder` (ThreadLocal). Runs alongside, not
instead of, the existing client-resolution filter chain — a request has both a client
application (which frontend) and a tenant (which customer) once this lands.

**Placement decision needed**: `ClientApplicationContextHolder` lives inside
`systemmodule.accesscontrol.security`, but tenant context needs to be readable from
`auth_db`-backed code (`AuthenticatedRequestContextFilter`, login) and `kyc_db`-backed
code (`PersonService`) too. Recommend a new shared package outside any single module
(e.g. `com.nexacore.common.tenant`, alongside wherever other genuinely cross-module
infrastructure already lives) rather than nesting it inside `systemmodule` and creating
a reverse dependency from `authmodule`/`kycmodule` back into it.

**Resolution source decision**: resolve tenant from the request's `Host` header on
every request (matching how client resolution already works), not from a JWT claim.
Domain-based resolution is the actual point of "tenant-specific domains" — a user
should get the right tenant by virtue of which domain they hit, without the token
needing to encode it. Do not add a tenant claim to the JWT for this reason; keep the
token tenant-agnostic and let the filter chain resolve tenant fresh per request, same
as client code today.

**Local dev / no-DNS fallback**: `sys_tenant_domain` requires a real resolvable domain
per tenant, which doesn't exist in local dev. Add an `X-Tenant-Code` header override,
accepted only when a `spring.profiles.active` dev/local profile is set (mirror however
the codebase already gates dev-only behavior, if it does) — never accept this override
in a profile that also serves real traffic.

### 3. Enforcement — the decision that matters most

Two layers, not one, because the audit already showed what happens when scoping is
purely manual and per-developer-discipline (`ApiDataScope` recorded but never
enforced; `DataScopeService` invoked ad hoc with hardcoded attribute names). On a
shared schema, the failure mode of "someone forgot" is a real cross-tenant data leak,
not a metadata inconsistency — this changes the risk calculus enough to justify a
framework-level backstop that the current `systemmodule` audit didn't need to
recommend for anything else.

- **Layer 1 (defense-in-depth, framework-level)**: Hibernate `@Filter`/`@FilterDef`
  (`tenantFilter`, parameter `tenantId`) declared on every entity that gets a
  `tenant_id` column, enabled automatically per-`EntityManager` at the start of every
  request from `TenantContextHolder` (a `RequestContextFilter`-level hook, one per
  module's `EntityManagerFactory` config class — `SystemDbConfig`, `KycDbConfig`, etc.
  — since each already builds its own factory). This is the actual wall: even a
  service method that never calls `DataScopeService` still can't read another tenant's
  rows, because Hibernate silently appends the filter to every query issued through
  that `EntityManager`.
- **Layer 2 (existing pattern, extend don't replace)**: `DataScopeService` keeps doing
  what it does today for the finer-grained `BUSINESS`/`BRANCH` scoping that Hibernate
  filters don't cleanly express (cross-entity, attribute-name-based checks) — but its
  `tenantId` source should switch from "whatever the caller supplied in the request
  body" to `TenantContextHolder`'s resolved value, closing the gap where License
  endpoints today trust a caller-supplied `tenantId` field.
- **Platform-admin bypass**: `system-frontend-21` (`SYSTEM_ADMIN_WEB`) is a
  cross-tenant console — a platform admin managing tenants themselves cannot be
  tenant-filtered. This needs an explicit bypass of the Layer 1 filter, gated by its
  own privilege code (not merely "no `X-Tenant-Code` header present," which is too easy
  to trigger by accident) — treat this bypass itself as the highest-risk piece of the
  whole plan and design/review it separately before building anything else.

### 4. Data migration for existing tables

For every table gaining a real row-ownership `tenant_id` (distinct from the
scope-assignment `tenantId` fields that already exist and stay as-is): idempotent
migration per module database that (a) seeds one `sys_tenant` row representing the
current single-organization deployment (`tenant_code = 'DEFAULT'` or similar), (b)
adds `tenant_id BIGINT` nullable, backfills every existing row to that tenant's id, (c)
a later migration flips it `NOT NULL` once backfilled. Split into two migrations
(add+backfill, then constrain) so the constrain step can be verified separately —
matches the idempotent, reviewable-in-steps style every existing `db/migration/system/`
file already follows. These are `system_db` migrations sharing the same `V<n>` sequence
as `SYSTEM_MODULE_RESOLUTION_PLAN.md` §1.1's `V30__grant_system_admin_web_permissions.sql`
— confirm the actual next free number at implementation time rather than assuming `V31`,
since that plan may have landed additional migrations by then.

### 5. Tenant-specific business configuration

Reuse existing infrastructure rather than building new: `LayoutProfile`/
`ClientLayoutAssignment` (currently keyed by `clientApplicationId` only) extends to
also carry `tenantId`, so branding/layout can differ per tenant on the same client
application. License `Entitlement`s already support a `tenantId` owner — this becomes
the real enforcement point for "which features/limits does this tenant's plan
include" once Layer 1 enforcement (above) makes `tenantId` trustworthy instead of
caller-supplied.

### 6. Frontend impact

- `kyc-frontend-21` (`WEB` client, tenant-facing) needs no explicit tenant-switching
  UI — tenant is resolved transparently from the domain the browser is already on.
- `system-frontend-21` (`SYSTEM_ADMIN_WEB`, cross-tenant) needs a new admin module —
  **Tenant Administration** — following the exact pattern already established for
  License/Layout this session (list + create/edit `DynamicList`/`DynamicForm` pair for
  `sys_tenant`, a nested tab or detail page for that tenant's domains). This is
  logically Phase 5 of that app's module set once this backend work lands; not
  scoped or designed here.

## Phasing (high level — not broken into task-level detail until the earlier
`SYSTEM_MODULE_RESOLUTION_PLAN.md` Phase 1 bootstrapping work actually ships, since
this depends on client/permission bootstrapping already being solid)

1. **Foundation**: `sys_tenant`/`sys_tenant_domain` tables, `TenantResolutionFilter`,
   `TenantContextHolder`, dev-mode `X-Tenant-Code` override. No enforcement yet —
   tenant is resolved and available, but nothing reads it. Verifiable in isolation
   (log the resolved tenant per request, confirm it's correct for a couple of test
   domains) before anything depends on it.
2. **Backfill migration**: add nullable `tenant_id` + backfill to a `DEFAULT` tenant
   across every table identified in "Data migration" above, one module database at a
   time (`system_db` first — it already has the most `tenantId`-adjacent tables).
3. **Enforcement Layer 1**: Hibernate filters wired to `TenantContextHolder`, `NOT
   NULL` constraint migration follows once confirmed working. This is the phase where
   a mistake is most costly (silently over- or under-scoping data) — plan for a
   staging pass with two seeded tenants and adversarial cross-tenant read attempts
   before this touches anything with real data.
4. **Enforcement Layer 2 + platform-admin bypass**: extend `DataScopeService`, design
   and review the bypass mechanism separately as called out above.
5. **Business configuration**: extend `ClientLayoutAssignment`/License `Entitlement`
   to carry `tenantId` meaningfully now that it's trustworthy.
6. **Frontend**: Tenant Administration module in `system-frontend-21`.

## Explicitly not doing (in this plan)

- Database-per-tenant or schema-per-tenant — explicitly rejected in favor of the
  shared-schema model above.
- Collapsing the existing 5-database module split into one physical database —
  "single DB" refers to not splitting per tenant, not merging `auth_db`/`system_db`/
  `kyc_db`/`gis_db`/`log_db`.
- Embedding tenant in the JWT — resolved per-request from the domain instead (see
  "Resolution source decision" above).
- Task-level breakdown of phases 2 onward — intentionally left at the phase level
  until Foundation (phase 1) is built and the `TenantResolutionFilter` placement/
  bypass-mechanism decisions above are actually made, not just proposed.
