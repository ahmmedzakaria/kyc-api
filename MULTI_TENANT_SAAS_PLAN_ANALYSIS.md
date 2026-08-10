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

### 2. `auth_users` must not become single-tenant

The plan also lists users among the records needing row ownership. The current model
deliberately separates a global login from its authorization scopes:

- Each `AuthUser` references one global `KycPerson`.
- An `AuthUser` can have multiple `AuthUserScopeAssignment` rows.
- Scope assignments independently grant tenant/business/branch access.
- The user's authorization scopes do not have to equal the linked person's
  organization memberships.

A single `auth_users.tenant_id` would contradict this normalized model:

```text
one login -> multiple tenant/business/branch authorization scopes
```

**Required correction:** keep `auth_users` global and derive tenant availability from
active `AuthUserScopeAssignment` records. If tenant-specific logins are desired, that
must be treated as a separate identity-model decision covering username uniqueness,
person linking, external identities, and provisioning.

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
| Global identity | `kyc_person`, `auth_users` | No owning `tenant_id` |
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
    Global AuthUser linked to a global KycPerson.

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
- Extend auth association records without making `AuthUser` tenant-owned.
- Handle GIS, logs, and shared reference data according to the approved matrix.

### Phase 5 — Tenant-specific configuration

- Add tenant-aware layout and branding assignments.
- Apply tenant-aware entitlements and limits.
- Define fallback from tenant override to global/default configuration.

### Phase 6 — Narrow platform administration

- Add explicit tenant selection for tenant-plane operations.
- Keep unfiltered control-plane repositories minimal.
- Audit all cross-tenant access.
- Add step-up authentication where appropriate for destructive platform operations.

### Phase 7 — Frontend rollout

- Add Tenant Administration to `system-frontend-21`.
- Keep tenant-facing applications domain-selected by default.
- Add tenant switching only for users explicitly authorized for multiple tenants.
- Never treat a frontend-selected tenant as authoritative without backend validation.

## Acceptance criteria before implementation

The plan is ready for task-level implementation only when all of the following are
true:

- `KycPerson` and `AuthUser` are explicitly classified as global.
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
owned business relationships rather than global identity records. The existing
`KycPerson`/`KycPersonProfile`, membership, and `AuthUserScopeAssignment` separation is
an architectural strength and should form the basis of the SaaS model.

The safest next deliverable is the table ownership matrix and effective tenant-access
context design. Adding tenant columns or Hibernate filters before those are approved
would risk both cross-tenant leaks and incorrect fragmentation of global identities.
