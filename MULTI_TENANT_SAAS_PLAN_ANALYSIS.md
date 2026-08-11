# Multi-Tenant SaaS Plan Analysis

## Review scope

This document reviews [MULTI_TENANT_SAAS_PLAN.md](./MULTI_TENANT_SAAS_PLAN.md)
against the repository's current person, organization, authorization, persistence,
and migration models.

The proposed shared-schema direction is viable, but the plan is not yet
implementation-ready. Its main issue is treating most application records as
tenant-owned. That conflicts with the established global-person and multi-scope user
models and would create incorrect isolation boundaries if implemented literally.

## Overall assessment

> **Current-state correction (2026-08-11):** the later Auth cutover supersedes this
> document's original assumption that `AuthUser` is global. `auth_persons` is now the
> global human identity authority and `auth_users` is a tenant account with mandatory
> `tenant_id`; a person may have one account per tenant. Active
> `AuthUserScopeAssignment` rows further constrain where that tenant account may
> operate. The implementation plan below uses this deployed model.

The plan should not begin with bulk `tenant_id` migrations or Hibernate filters.
Before implementation, it needs:

1. A table-by-table ownership classification.
2. A server-derived effective tenant-access context.
3. A precise cross-database tenant-reference strategy.
4. A narrower alternative to a general platform-administrator filter bypass.
5. A tested enforcement design covering ORM, native queries, background work, and
   direct-ID access.

## Critical findings

### 1. `kyc_person` must remain global

The plan identifies KYC persons as records that need row-ownership tenancy. This
contradicts the repository's domain rules:

- `KycPerson` is the global human identity.
- A person can participate in multiple organizations.
- A person can have multiple tenant-owned KYC relationships.
- Tenant-specific details and decisions belong to `KycPersonProfile` or another
  explicitly scoped owning record.

The existing model already expresses this separation:

- `KycPerson` stores global identity.
- `KycPersonProfile` stores the tenant/business/branch-owned KYC relationship.
- `KycPersonOrganizationMembership` records potentially multiple organizational
  relationships without granting application access.

Adding `tenant_id` or a Hibernate tenant filter to `kyc_person` would either duplicate
the same human across tenants or make a global identity inaccessible through another
authorized profile.

**Required correction:** explicitly classify `kyc_person` as global. Apply tenant
ownership to profiles, tenant-owned documents, photos, decisions, reviews, workflows,
and other scoped relationships.

### 2. `auth_users` is a tenant account, not the global person

The completed Auth cutover resolved the earlier identity-model decision:

- `AuthPerson` is the global human identity;
- each `AuthUser` belongs to exactly one tenant and references one `AuthPerson`;
- the same person may have a separate account in multiple tenants;
- username uniqueness is tenant-local after normalization;
- `AuthUserScopeAssignment` narrows the account to tenant/business/branch operating
  scopes and cannot cross the account tenant;
- account authorization remains independent of person organization memberships.

**Required correction:** tenant registration must provision a tenant account and its
scope assignments without duplicating the global person. It must never infer account
access from membership, domain input, or caller-supplied tenant identifiers.

### 3. Hibernate filters are not a complete isolation wall

The plan describes Hibernate `@Filter` as the actual wall that prevents cross-tenant
reads even when a service omits `DataScopeService`. This overstates what an ORM filter
can guarantee.

Hibernate filters do not automatically protect every data-access path, including:

- native SQL;
- `JdbcTemplate` or direct JDBC;
- Flyway and maintenance operations;
- some bulk update/delete paths;
- background jobs with no request context;
- entities or associations missing the appropriate annotation;
- new `EntityManager`/Hibernate `Session` instances where the filter was not enabled;
- exports, reconciliation tools, and integrations implemented outside Hibernate.

**Required correction:** describe Hibernate filters as an ORM defense-in-depth layer,
not the database security boundary. Evaluate PostgreSQL row-level security for tables
that require strong tenant isolation. At minimum, add architecture tests that require
every persistent table to declare its ownership classification and adversarial
integration tests for ORM, native, bulk, and direct-ID access paths.

### 4. A general platform-admin filter bypass is too broad

The plan proposes disabling the Layer 1 filter for a platform administrator with a
dedicated privilege. Once disabled, every repository call in that request may become
cross-tenant, regardless of which tenant or resource the operation intended to access.

A privilege answers whether the caller may perform an operation; it does not constrain
the tenant dataset on which that operation should act.

**Required correction:** separate control-plane and tenant-plane behavior:

- Tenant Administration may use narrowly scoped global repositories for tenant and
  domain metadata.
- Normal administration should require an explicit selected tenant.
- The backend must validate that tenant against platform privileges and client
  assignments.
- Tenant filters should then be enabled for the selected tenant.
- Truly unfiltered operations should exist only in small, separately reviewed
  control-plane services.
- Every cross-tenant action must be audited.

## High-priority architecture gaps

### 5. A table ownership matrix is required

The statement that effectively everything needs tenant ownership is too broad. The
repository contains records with materially different lifecycles:

| Classification | Representative records | Tenant treatment |
| --- | --- | --- |
| Global identity | `auth_persons` | No owning `tenant_id` |
| Tenant account | `auth_users` | Mandatory account `tenant_id`; scopes may only narrow it |
| Global platform catalog | privilege modules/actions, API registry definitions, some license plan metadata | Normally global |
| Tenant-owned resource | KYC profiles, tenant settings, subscriptions, workflow instances | Required owning `tenant_id` |
| Multi-tenant association | user scope assignments, person memberships, client-tenant assignments | Tenant ID describes the association |
| Tenant-configurable overlay | branding and layout assignments | Tenant-aware assignment or override |
| Global/shared reference data | countries, administrative boundaries, shared dictionaries | Decide explicitly; do not duplicate automatically |

Navigation definitions may be global templates with tenant-specific assignments or
overrides. Duplicating every navigation node for every tenant is not automatically the
correct model.

**Required correction:** add an appendix listing every persistent table with:

- owning module and database;
- global, tenant-owned, association, overlay, or shared-reference classification;
- whether a new `tenant_id` is required;
- nullability and backfill source;
- old and new unique keys;
- foreign-key or application-validation strategy;
- ORM and non-ORM enforcement path;
- platform-admin access rules.

No bulk tenant migration should start before this matrix is approved.

### 6. Cross-database tenant IDs cannot all be physical foreign keys

The plan puts `sys_tenant` in `system_db`, while tenant-owned records exist in
`auth_db`, `kyc_db`, `gis_db`, and `log_db`. Ordinary PostgreSQL foreign keys cannot
cross those physical databases.

Consequently, a `tenant_id` can be a physical foreign key only for records inside
`system_db`. Other databases need a documented consistency mechanism.

**Required correction:** define:

- physical foreign keys within `system_db`;
- application-level tenant validation in other databases;
- whether tenant identity is replicated locally per database;
- ID propagation and reconciliation;
- behavior during partial database outages;
- cancellation/deactivation rules;
- prevention of orphan tenant IDs.

### 7. Domain resolution selects a tenant but does not authorize it

Keeping JWTs tenant-agnostic can support users with access to multiple tenants, but the
hostname alone is not an authorization boundary. A valid token could otherwise be
replayed against another tenant domain.

For each authenticated request, effective access must be the intersection of:

```text
resolved domain tenant
AND authenticated user scope assignments
AND client application tenant assignments
AND active tenant status
AND applicable subscription/entitlement status
```

**Required correction:** introduce a server-derived `EffectiveTenantAccessContext`.
Domain resolution selects the requested tenant; it never grants access by itself.

### 8. Trusted proxy and custom-domain security are underspecified

Reading raw `Host` is insufficient behind a reverse proxy. Blindly accepting
`X-Forwarded-Host` would allow spoofing. The design must cover:

- an explicit trusted-proxy boundary;
- canonical forwarded-header processing;
- rejection of multiple or ambiguous host values;
- lowercase, port, trailing-dot, IPv6, and IDN normalization;
- domain ownership verification before activation;
- TLS certificate provisioning and renewal;
- uniqueness of active domains and primary domains;
- resolver caching and invalidation;
- fail-closed handling for unknown, suspended, or cancelled tenants.

Suspended or unknown tenants should be rejected before tenant-owned services are
invoked.

### 9. Filter activation needs a precise lifecycle

Hibernate filters belong to a Hibernate `Session`; they are not global switches on an
`EntityManagerFactory`. With five entity-manager factories, persistence contexts may
be created lazily after the servlet filter begins. Scheduled work, asynchronous work,
and message consumers may not have an HTTP request at all.

**Required correction:** specify how tenant enforcement is activated for every new
session or transaction, and define tenant/system context propagation for:

- scheduled jobs;
- asynchronous execution;
- events and queues;
- batch imports;
- internal module calls;
- reconciliation jobs;
- seeders and migrations;
- integration tests.

Tenant-owned repositories should fail closed when invoked without an explicit tenant
or narrowly defined system-execution context.

### 10. `DataScopeService` must intersect domain tenant and user scopes

The plan says the tenant source should switch from request data to
`TenantContextHolder`. That is necessary but incomplete. `DataScopeService` must still
validate that the authenticated user has an active assignment covering the resolved
tenant and requested business/branch.

The resulting rule should be:

```text
resolved tenant is mandatory
AND user has a compatible scope assignment
AND business/branch form a valid hierarchy
AND the operation does not widen the assignment
```

Request-body tenant IDs should either be removed or treated only as assertions that
must equal the resolved context. They must never select or widen tenant access.

## Migration and rollout findings

### 11. The proposed phases allow new null-owned rows

The plan adds nullable columns and backfills them before deploying enforcement and
`NOT NULL` constraints. Writes occurring between those deployments can create new
rows with `NULL tenant_id` after the backfill has completed.

**Required correction:** use an expand/backfill/contract rollout:

1. Add nullable ownership columns.
2. Deploy dual-write ownership assignment for every affected write path.
3. Backfill existing rows in bounded, restartable batches.
4. Reconcile and verify zero null, unknown, or contradictory owners.
5. Run tenant enforcement in observable shadow mode where practical.
6. Enable read enforcement.
7. Add `NOT NULL`, foreign-key, check, and scoped uniqueness constraints.
8. Remove compatibility and fallback paths.

Each module database needs its own migration sequence. System migration numbers do not
coordinate migration files in the auth, KYC, GIS, or log locations.

### 12. Uniqueness rules must be redesigned explicitly

Introducing tenant ownership changes whether identifiers are global or tenant-local.
Examples include:

- workflow codes;
- subscription and entitlement identifiers;
- layout profile codes;
- client application codes;
- organization identifiers;
- profile business keys;
- domain names.

Simply adding `tenant_id` without changing indexes can either reject valid values from
different tenants or weaken identifiers that must stay globally unique.

**Required correction:** the ownership matrix must document every old and new unique
constraint. Domain names and global identity keys will generally remain globally
unique, while selected business identifiers may become unique within a tenant.

### 13. Existing scoped columns should not be duplicated blindly

Some records already have a `tenant_id` that expresses their actual owning or scoping
relationship, including KYC profiles and workflow runtime records. Other columns are
association targets rather than row ownership.

**Required correction:** for each existing column, decide whether it is:

- already the authoritative owner;
- an authorization assignment target;
- an entitlement owner selector;
- denormalized context that must be validated;
- legacy data requiring replacement.

Do not add a second ownership column where the existing one already has the correct
meaning.

## Operational gaps

### 14. Tenant lifecycle behavior is incomplete

`ACTIVE`, `SUSPENDED`, and `CANCELLED` are proposed, but their effects are not defined.
The plan should specify:

- whether suspended tenants can authenticate;
- whether read-only access is allowed;
- whether scheduled jobs continue;
- whether API clients and refresh tokens are rejected;
- whether custom domains stay active;
- data retention and eventual deletion for cancelled tenants;
- reactivation rules;
- how license status interacts with tenant status.

Avoid keeping both `status` and `active` unless their independent semantics are clearly
defined; otherwise contradictory states become possible.

### 15. Observability and audit requirements are missing

Tenant isolation failures require strong operational evidence. Every request and
background operation should make the effective tenant available to structured logs,
metrics, traces, and audit events without trusting caller-supplied values.

Add:

- resolved tenant ID/code in structured diagnostic context;
- client application ID and authenticated actor alongside it;
- metrics for unknown domains and scope mismatches;
- audit events for tenant administration and cross-tenant operations;
- alerts for missing tenant context on tenant-owned repositories;
- redaction rules preventing tenant data from leaking into logs.

### 16. Foundation verification is too weak

Logging the tenant resolved for two domains does not adequately test a security
boundary. Foundation verification should include:

- unknown, inactive, suspended, and cancelled domains;
- Unicode/punycode and trailing-dot normalization;
- trusted and untrusted forwarded headers;
- context cleanup after successful and exceptional requests;
- sequential requests for different tenants on a reused thread;
- concurrent requests for different tenants;
- client-to-tenant assignment mismatch;
- user-to-tenant scope mismatch;
- token replay against another tenant domain;
- login and refresh on the wrong tenant domain;
- scheduled work with missing or explicit tenant context.

## Recommended target model

The architecture should keep four concepts separate:

```text
ResolvedTenant
    Tenant selected from a verified request domain or an approved internal context.

AuthenticatedUser
    Tenant AuthUser account linked to a global AuthPerson.

EffectiveTenantAccess
    Intersection of the resolved tenant, user scope assignments, client assignments,
    tenant status, and applicable platform policy.

ResourceOwnership
    tenant_id only on records genuinely owned by exactly one tenant.
```

Business and branch access remains subordinate to the effective tenant:

```text
tenant required
business optional
branch requires business
```

Membership remains separate from access:

```text
KycPersonOrganizationMembership != AuthUserScopeAssignment
```

## Tenant registration and onboarding implementation plan

### Objective and first release boundary

Implement a control-plane tenant registry and an idempotent onboarding workflow that
creates an active tenant, its verified domain binding, client assignments, and its
first tenant administrator without trusting caller-supplied ownership IDs.

The first release supports platform-administrator-assisted registration. Public
self-service registration remains disabled until email/domain verification, abuse
controls, commercial terms, and payment/subscription activation are implemented.

Registration must create this graph:

```text
sys_tenants
  +-- sys_tenant_domains
  +-- sys_acc_client_application_tenants
  +-- auth_persons (global initial administrator identity)
        +-- auth_users (tenant account)
              +-- auth_user_scope_assignments (tenant-level scope)
              +-- auth_user_roles (tenant administrator role)
```

Person membership and KYC profiles are deliberately not created during tenant
registration. Organizational participation and KYC relationships are separate
business actions and must not grant application access implicitly.

### Control-plane data model

Add the following `system_db` tables through new Flyway migrations.

#### `sys_tenants`

Required fields:

- `id bigint` primary key generated by the platform;
- immutable, normalized `tenant_code` with global case-insensitive uniqueness;
- `display_name`, optional `legal_name`, and optional registration/reference number;
- lifecycle `status`: `PENDING`, `ACTIVE`, `SUSPENDED`, `CANCELLED`;
- `default_locale`, `default_time_zone`, and optional billing contact fields;
- optional `subscription_id` once license ownership is finalized;
- `activated_at`, `suspended_at`, `cancelled_at` and reason fields;
- mandatory `created_by`, `updated_by`, `created_at`, and `updated_at`.

Tenant IDs are immutable and never recycled. Cancellation must not hard-delete a
tenant or make its code available for reuse.

#### `sys_tenant_domains`

Required fields:

- `id`, mandatory `tenant_id` foreign key to `sys_tenants`;
- normalized ASCII `hostname`, globally unique while retained;
- `domain_type`: `PLATFORM_SUBDOMAIN` or `CUSTOM`;
- `verification_status`: `PENDING`, `VERIFIED`, `FAILED`, `REVOKED`;
- hashed verification token/challenge, verification method, expiry and attempt data;
- `primary_domain`, `verified_at`, `last_checked_at`, `active`;
- complete actor and timestamp audit fields.

Only a verified, active domain of an active tenant may resolve request tenancy. The
database must enforce at most one primary active domain per tenant.

#### `sys_tenant_onboarding`

Use a durable workflow/saga record because Auth and System use separate databases:

- globally unique `idempotency_key` and normalized requested tenant code;
- requested administrator contact data and requested domain;
- state: `RECEIVED`, `SYSTEM_CREATED`, `AUTH_PROVISIONED`, `CLIENTS_ASSIGNED`,
  `COMPLETED`, `COMPENSATION_REQUIRED`, `FAILED`;
- IDs produced by each completed step;
- safe error code/details, retry count, and audit timestamps.

Do not store a plaintext initial password or raw domain verification secret in this
table.

### Registration API contracts

Add a narrowly scoped controller under `/api/v1/system/tenants`:

- `POST /register` — platform-only, accepts tenant metadata, administrator identity,
  initial credential delivery choice, and an idempotency key;
- `POST /search` and `POST /detail` — platform tenant registry reads;
- `POST /domain/request-verification` and `POST /domain/verify`;
- `POST /activate`, `/suspend`, `/reactivate`, and `/cancel`;
- `POST /retry-onboarding` for failed saga steps;
- `POST /administrator/reissue-invitation`, never returning a password hash.

Caller-provided `tenantId`, account ID, role ID, or status is not authoritative.
Registration resolves generated IDs from server-owned records. Direct-ID operations
must use control-plane repositories and require dedicated tenant-administration
privileges.

Define distinct privileges for tenant registry view, register, domain verification,
lifecycle management, onboarding retry, and initial-administrator management. Do not
reuse `ROLE_SYSTEM_ADMIN` as an unrestricted repository bypass.

### Onboarding orchestration

Implement onboarding as an idempotent saga rather than a cross-database transaction:

1. Validate and reserve the normalized tenant code and requested hostname in
   `system_db`.
2. Create `sys_tenants` in `PENDING` and its pending domain record.
3. Through an Auth gateway, resolve or create the global `AuthPerson` using reviewed
   identity matching rules.
4. Create exactly one `AuthUser` account for `(tenant_id, person_id)` with a normalized
   tenant-local username.
5. Create its tenant-level `AuthUserScopeAssignment`.
6. Create or resolve a tenant-owned `ROLE_TENANT_ADMIN`; assign only the approved
   tenant-administration privilege template, never `ROLE_SYSTEM_ADMIN`.
7. Assign approved tenant-facing client applications to the new tenant.
8. Issue a short-lived, single-use activation/invitation token through an out-of-band
   channel. Store only its hash.
9. Verify the domain and administrator credential activation.
10. Activate the tenant only when required Auth, domain, client, and subscription
    invariants reconcile successfully.

Every step records completion before the next begins. Retries reuse the onboarding
record and must not create duplicate people, accounts, roles, scopes, or client
assignments. A partial failure leaves the tenant non-active and inaccessible; it must
never silently broaden access as compensation.

### Cross-database contracts and reconciliation

Add explicit gateway operations instead of sharing repositories across modules:

- validate tenant existence and active status from Auth, KYC, Log, and integrations;
- provision/disable the tenant administrator account in Auth;
- reconcile tenant IDs referenced outside `system_db`;
- report missing, inactive, or contradictory tenant references;
- retry safe onboarding steps and flag manual compensation.

Run scheduled reconciliation for tenant registry versus Auth accounts/scopes, client
assignments, domains, and subscriptions. Unknown tenant IDs fail closed. System
outages must prevent new Auth/KYC tenant-owned writes rather than accepting an
unvalidated tenant ID.

### Domain resolution and effective access

After registration is stable, implement `EffectiveTenantAccessContext`:

```text
verified resolved domain tenant
AND tenant status is ACTIVE
AND client is actively assigned to tenant
AND authenticated account belongs to tenant
AND active user scope covers requested business/branch
AND subscription/entitlements allow the operation
```

Normalize hostnames using a trusted-proxy-aware resolver. Reject unknown, ambiguous,
unverified, suspended, or cancelled domains before business controllers execute.
Cache only successful verified mappings with bounded TTL and invalidate cache entries
on every domain or lifecycle mutation.

### Lifecycle behavior

- `PENDING`: onboarding and verification only; normal authentication is rejected.
- `ACTIVE`: normal tenant-plane access is allowed subject to all other checks.
- `SUSPENDED`: login, refresh, writes, and background tenant work are rejected; data
  remains retained for recovery and audit.
- `CANCELLED`: access remains disabled; retention/export/deletion follows an explicit
  policy and cannot be reversed by merely changing a frontend value.

Lifecycle transitions require confirmation, reason, actor audit, and optimistic
locking. Destructive cancellation needs step-up authentication and an asynchronous
retention workflow.

### Administration frontend

Add Tenant Administration to `frontendApplications/system-frontend-21`:

- tenant list with status, primary domain, subscription state, onboarding progress,
  created/updated audit data, and safe failure code;
- registration wizard for tenant metadata, initial administrator, domain, clients,
  and confirmation;
- detail screen with Overview, Domains, Administrator, Client Assignments,
  Subscription, Audit, and Reconciliation sections;
- explicit confirmation for suspend, reactivate, cancel, retry, and administrator
  replacement;
- platform-only controls hidden according to privilege metadata, with backend
  authorization remaining authoritative;
- no arbitrary tenant switcher until effective-access validation is deployed.

The UI must never display or retain a generated plaintext password. Prefer invitation
and first-login password setup. If an emergency one-time credential is supported, it
may be displayed exactly once and must expire quickly.

### Delivery phases

#### Registration Phase 0 — decisions and ownership matrix

- Approve tenant lifecycle, code/domain uniqueness, retention, administrator recovery,
  subscription activation, and public-registration policy.
- Complete the persistent-table ownership matrix and cross-database reference rules.

#### Registration Phase 1 — registry foundation

- Add tenant, domain, and onboarding tables, entities, repositories, DTOs, audit, and
  lifecycle constraints.
- Seed dedicated Tenant Administration privileges and navigation for the System app.

#### Registration Phase 2 — platform-assisted onboarding

- Implement registration API, idempotent saga, Auth provisioning gateway, tenant
  administrator role/template, client assignments, and reconciliation.
- Keep tenants pending and inaccessible on any partial failure.

#### Registration Phase 3 — domains and activation

- Implement trusted host normalization, platform subdomains, custom-domain challenge,
  verification, cache invalidation, and activation readiness checks.

#### Registration Phase 4 — effective tenant context

- Intersect domain, tenant status, client assignment, tenant account, user scope, and
  subscription policy for login, refresh, API requests, async work, and messages.

#### Registration Phase 5 — administration frontend

- Deliver registry, wizard, detail, lifecycle, audit, and retry screens in
  `system-frontend-21`.

#### Registration Phase 6 — self-service and commercial activation

- Only after the assisted path is stable, add rate-limited public registration,
  verified contacts, legal acceptance, payment/subscription activation, abuse
  controls, and support/recovery workflows.

### Verification and acceptance criteria

- Duplicate tenant codes and hostnames are rejected under case/Unicode normalization.
- Repeating the same idempotency key returns the original onboarding result.
- A failure after System creation but before Auth provisioning leaves no active tenant.
- Retry creates no duplicate person, account, role, scope, domain, or client mapping.
- The first administrator receives `ROLE_TENANT_ADMIN`, never platform privileges.
- A tenant administrator cannot register, suspend, inspect, or mutate another tenant.
- Unknown, pending, suspended, cancelled, and unverified-domain tenants cannot log in.
- A valid token for tenant A cannot be replayed through tenant B's domain/client.
- Direct-ID, native-query, bulk, async, scheduled, and message-driven paths fail closed
  without the effective tenant context.
- Cross-database reconciliation detects missing tenant, account, scope, client,
  domain, role, and subscription records.
- Invitation secrets are hashed, single-use, expiring, and absent from logs/audit.
- Every lifecycle and cross-tenant control-plane action records actor, reason, tenant,
  trace ID, and outcome.

## Recommended revised phasing

### Phase 0 — Ownership and threat model

- Complete the table ownership matrix.
- Decide global versus tenant-local uniqueness.
- Document domain, token-replay, admin-bypass, native-query, batch, and async threats.
- Define tenant lifecycle semantics.

### Phase 1 — Tenant control plane

- Add `sys_tenant` and `sys_tenant_domain`.
- Add domain ownership verification and lifecycle management.
- Add narrowly scoped Tenant Administration privileges and APIs.
- Add resolver caching and invalidation.

### Phase 2 — Effective tenant-access context

- Resolve and normalize the trusted host.
- Validate tenant status.
- Intersect the tenant with client assignments and authenticated user scopes.
- Populate a server-derived effective access context.
- Fail closed on missing or contradictory context.

### Phase 3 — Pilot one bounded module

- Select a small set of genuinely tenant-owned `system_db` tables.
- Deploy dual-write ownership.
- Backfill and reconcile.
- Implement ORM and, if selected, database enforcement.
- Run adversarial cross-tenant tests.

### Phase 4 — Expand module by module

- Extend to KYC profiles and their owned resources without changing `KycPerson`.
- Extend workflow runtime and tenant-owned definitions as appropriate.
- Extend license ownership based on its resolved owner model.
- Extend Auth tenant-account association records while keeping `AuthPerson` global.
- Handle GIS, logs, and shared reference data according to the approved matrix.

Implementation status (2026-08-12):

- KYC profiles, details, documents, and legacy KYC records are constrained to a
  server-derived tenant. `KycPerson` remains a global identity and is not tenant-owned.
- Workflow definitions, instances, and tasks require tenant ownership; composite
  foreign keys reject cross-tenant parent/child relationships.
- License plans remain global catalogs. Subscriptions, usage, audit events, and key
  administration are tenant-scoped, with subscription codes unique per tenant.
- Auth tenant-account, role, and scope associations were cut over in the preceding
  Auth phases. `AuthPerson` remains global.
- GIS administrative boundaries remain shared reference data. They are not assigned
  artificial tenant ownership.
- Request, audit, and error logs capture the resolved tenant and authenticated actor.
  Tenant remains nullable only for genuine platform/startup events without a request
  context; log rows always carry audit actor columns.

### Phase 5 — Tenant-specific configuration

- Add tenant-aware layout and branding assignments.
- Apply tenant-aware entitlements and limits.
- Define fallback from tenant override to global/default configuration.

Implementation status (2026-08-12):

- Client layout assignments are tenant-owned and may override branding fields while
  continuing to reuse global layout templates, themes, sizes, and fonts.
- Branding resolution is field-by-field: tenant/client assignment override, then
  global profile branding, then the built-in safe default context.
- License plans and plan entitlements remain global defaults. Tenant-owned
  subscription overrides apply explicit deny, allow, and limit overrides; missing
  overrides fall back to the selected global plan entitlement or limit.
- Configuration and entitlement administration reads resolve the effective tenant
  on the server and never select by subscription or assignment code alone.

### Phase 6 — Narrow platform administration

- Add explicit tenant selection for tenant-plane operations.
- Keep unfiltered control-plane repositories minimal.
- Audit all cross-tenant access.
- Add step-up authentication where appropriate for destructive platform operations.

Implementation status (2026-08-12):

- Tenant-plane layout administration requires an explicit `tenantId`; the selected
  tenant is intersected with the authenticated effective scope before repository access.
- Unfiltered tenant enumeration and lifecycle lookup remain confined to the tenant
  control-plane service and repository rather than exposed as a general bypass.
- Tenant registration, enumeration, domain verification, and lifecycle attempts are
  written to a dedicated platform-administration audit table in an independent
  transaction, including actor tenant, target tenant, action, reason, and trace ID.
- Domain verification and tenant lifecycle changes require a freshly issued access
  token plus `X-Step-Up-Authentication: reauthenticated`. The maximum token age is
  configurable and defaults to five minutes.

### Phase 7 — Frontend rollout

- Add Tenant Administration to `system-frontend-21`.
- Keep tenant-facing applications domain-selected by default.
- Add tenant switching only for users explicitly authorized for multiple tenants.
- Never treat a frontend-selected tenant as authoritative without backend validation.

Implementation status (2026-08-12):

- `system-frontend-21` includes Tenant Administration for registration, domain
  verification, lifecycle management, status visibility, and tenant search.
- Navigation and visible actions align with the bootstrap tenant privileges; every
  API remains independently authorized by the backend.
- Lifecycle and domain-verification calls send the Phase 6 step-up confirmation and
  surface backend rejection when the access token is no longer recent.
- Tenant-facing applications remain hostname-selected. No arbitrary tenant switcher
  is exposed: the verified domain and authenticated tenant account remain authoritative,
  while control-plane tenant IDs are explicit target identifiers only.

## Acceptance criteria before implementation

The plan is ready for task-level implementation only when all of the following are
true:

- `AuthPerson` is explicitly global, while `AuthUser` is explicitly a tenant account.
- Every persistent table has an approved ownership classification.
- Cross-database tenant validation is documented.
- Domain resolution is intersected with user and client authorization.
- Background and asynchronous tenant contexts are designed.
- The platform-admin path does not depend on a general repository-wide bypass.
- Native SQL and bulk operations have an enforcement/test strategy.
- Expand/backfill/contract migrations prevent new null-owned rows.
- Scoped uniqueness constraints are specified.
- Two-tenant adversarial tests are part of every rollout phase.

## Conclusion

Shared-schema multi-tenancy is compatible with this project, but tenancy must attach to
tenant accounts and owned business relationships rather than the global Auth person.
The `AuthPerson`/tenant-account, KYC profile, membership, and
`AuthUserScopeAssignment` separation should form the basis of the SaaS model.

The safest next deliverable is the table ownership matrix and effective tenant-access
context design. Adding tenant columns or Hibernate filters before those are approved
would risk both cross-tenant leaks and incorrect fragmentation of global identities.
