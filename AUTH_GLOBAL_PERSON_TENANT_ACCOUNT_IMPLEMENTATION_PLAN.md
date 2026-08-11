# Global Person and Tenant Account Implementation Plan

## Status

Phases 0 through 4 completed on 2026-08-11. Tenant-aware authentication is active;
constraint and person-authority cutovers remain in Phases 5 and 6.

## Phase 0 execution record

### Approved decisions

| Decision | Approved rule | Consequence for implementation |
|---|---|---|
| Global-person ownership | Move canonical global human identity ownership to Auth as `auth_persons`. | KYC retains a transitional `kyc_person` representation during dual-write/stabilization, then becomes profile/evidence owned. |
| Accounts per person and tenant | At most one `auth_users` account for a person in a tenant. The same person may have one separate account in each tenant. | Enforce unique `(tenant_id, person_id)`. Any future need for multiple same-tenant accounts requires a new decision and migration. |
| Username uniqueness | Username is unique only inside a tenant after server-side normalization. | Enforce unique `(tenant_id, normalized_username)` and remove global uniqueness only during the constraint cutover. |
| Canonical contacts | Canonical email/mobile belong to the global Auth person, but neither is globally unique. | Do not add unique constraints to canonical contacts. Use normalized contact rows if multiple contacts/history become necessary. |
| Tenant contacts | Tenant-observed email/mobile remain profile-owned and may differ from canonical contacts. | Profile updates never silently overwrite canonical contacts. APIs must label canonical versus tenant-declared values. |
| Global roles | A role with `tenant_id IS NULL` is a platform-managed reusable template. | Tenant administrators may assign visible templates but cannot create, rename, deactivate, or change their privileges. |
| Tenant roles | A role with `tenant_id IS NOT NULL` is owned and managed by that tenant. | Assignment requires `role.tenant_id = user.tenant_id`; use service validation plus a PostgreSQL constraint trigger. |
| Role identifiers | Add immutable `role_code`; display name and description may be edited. | Normalize codes using `trim -> Unicode NFKC -> uppercase Locale.ROOT`; permit only `A-Z`, `0-9`, and `_`; require uniqueness globally for templates and within each tenant for custom roles. Existing `ROLE_*` values become global template codes. |
| Role ownership changes | Role ownership is immutable after creation and role codes are immutable after first assignment/use. | Moving a role between global and tenant ownership is prohibited; create a replacement role and explicitly migrate assignments. |
| External identities | One external subject may produce one account per authorized tenant. | Enforce `(tenant_id, external_provider, external_subject)` and bind SSO provisioning to verified tenant context. |
| Tenant resolution | Resolve tenant from a verified domain/client mapping before account lookup. | Headers and request DTO tenant IDs are not authority. Missing or ambiguous resolution fails closed. |
| Cross-database integrity | Person/profile and role/privilege links remain application-level references. | Gateways validate synchronously; reconciliation reports missing/stale/mismatched references. No unavailable gateway may be treated as authorization success. |

### Current schema and data snapshot

Inventory was taken from the local development databases on 2026-08-11 using
aggregate-only queries.

| Area | Current result | Migration significance |
|---|---:|---|
| Auth users | 7 | All require tenant and normalized-username backfill. |
| Auth roles | 6 | Existing roles will initially become global templates. |
| User-role assignments | 9 | Must be validated before enabling the role-tenant trigger. |
| User-scope assignments | 2 | Scope cannot provide an unambiguous tenant for every account. |
| Users with no active tenant scope | 5 | Must be quarantined or assigned from another trusted source; never infer from a request header. |
| Users with exactly one active scope tenant | 2 | Eligible for deterministic tenant backfill after validation. |
| Users with multiple active scope tenants | 0 | No current account split is immediately required by scope data. |
| Duplicate Auth person groups | 0 | Current global `person_id` uniqueness is intact. |
| KYC persons | 14 | IDs must be preserved when copied to `auth_persons`. |
| KYC profiles | 5 | Continue using profile ID as the scoped resource ID. |
| Organization memberships | 5 | Membership remains independent from login authorization. |
| Persons with no active profile | 9 | A global person remains valid without a tenant KYC profile. |
| Persons represented in one tenant | 5 | Eligible for direct person/profile reconciliation. |
| Persons represented in multiple tenants | 0 | Multi-tenant behavior still requires adversarial tests even though local seed data lacks this case. |

The five users without an active scope tenant are a Phase 3 blocking data-quality
category for constraint cutover, not a reason to infer ownership automatically.

### Production-code dependency inventory

#### Global `findByUsername`

| File | Current dependency | Required replacement |
|---|---|---|
| `authmodule/core/repository/UserRepository.java` | Declares global username lookup. | Add tenant plus normalized-username lookup; retain global lookup only behind platform reconciliation during transition. |
| `authmodule/security/service/MyUserDetailsService.java` | Loads authentication principal globally. | Accept resolved tenant/account identity and load within that tenant. |
| `authmodule/core/service/implementations/UserAdminServiceImpl.java` | Checks username uniqueness globally. | Check normalized username inside effective tenant and scope direct-ID operations. |
| `authmodule/sso/service/KeycloakSsoService.java` | Provisions and generates usernames globally. | Resolve tenant first and use tenant-aware external/person/username keys. |
| `authmodule/api/AuthModuleGatewayImpl.java` | Resolves access globally by username. | Resolve by authenticated account ID or tenant-bound account key. |
| `authmodule/startup/DataSeeder.java` | Finds seed accounts globally. | Seed explicit tenant accounts or use platform-only deterministic bootstrap rules. |
| `kycmodule/person/repository/PersonRepository.java` | Finds global person by compatibility username. | Remove username authority from KYC person. |
| `kycmodule/person/api/KycPersonModuleGateway.java` | Finds/promotes people using username. | Replace with Auth-owned global-person contract and canonical identifiers. |
| `keycloak2/.../NexaCoreUserRepository.java` and `NexaCoreUserStorageProvider.java` | Federate users by global case-insensitive username. | Include verified tenant context and normalized username in SPI lookup. |

#### Global `findByPersonId`

| File | Current dependency | Required replacement |
|---|---|---|
| `authmodule/core/repository/UserRepository.java` | Declares one global account per person. | Replace authentication/admin usage with `(tenant_id, person_id)`. |
| `authmodule/sso/service/KeycloakSsoService.java` | Maps SSO person to one global account. | Provision/select one account per authorized tenant. |
| `keycloak2/.../NexaCoreUserRepository.java` | Email lookup maps KYC person to the first Auth account by person ID. | Resolve tenant and query the matching tenant account; never select an arbitrary account. |

#### `AuthUser` consumers

- `authmodule/core/entity/AuthUser.java`: currently globally unique `username` and
  `person_id`; no account `tenant_id` or `normalized_username`.
- `authmodule/core/entity/AuthUserScopeAssignment.java`: scope has tenant/business/
  branch, but its tenant is not database-constrained to the user account.
- `authmodule/core/repository/UserRepository.java`: global username, person, and
  external-subject methods.
- `authmodule/core/service/implementations/UserAdminServiceImpl.java`: account create,
  update, list, and role replacement without tenant-account semantics.
- `authmodule/security/service/MyUserDetailsService.java`: password authentication by
  global username.
- `authmodule/sso/service/KeycloakSsoService.java`: global SSO provisioning.
- `authmodule/api/AuthModuleGatewayImpl.java`: username-based access projection used
  by request authorization.
- `authmodule/startup/DataSeeder.java`: globally named seed accounts and exact roles.
- `systemmodule/accesscontrol/security/AuthenticatedRequestContextFilter.java` and
  `systemmodule/privilege/service/implementations/PrivilegeServiceImpl.java`: consume
  the username-keyed Auth gateway result and therefore inherit its ambiguity.

#### `KycPerson` consumers

- `kycmodule/person/entity/KycPerson.java`: currently stores canonical name, DOB,
  gender, national ID, photo, contacts, verification flags, plus compatibility
  `username` and `is_user` fields.
- `kycmodule/person/entity/KycPersonProfile.java`: uses a physical relation to
  `KycPerson`; target state requires an application-level Auth person ID.
- `KycPersonDetails`, `KycPersonDocument`, and
  `KycPersonOrganizationMembership`: still point to the KYC-owned global entity;
  details/documents need explicit global-versus-profile ownership classification.
- `kycmodule/person/repository/PersonRepository.java`: global username/email/mobile
  lookups and uniqueness assumptions.
- `kycmodule/person/service/implementations/PersonService.java`: creates/updates the
  global person, creates profiles/memberships, composes DTOs, and owns document flows.
- `kycmodule/person/api/KycPersonModuleGateway.java`: lets Auth find, create, promote,
  and validate the KYC-owned global person.
- `kycmodule/person/repository/PersonDocumentRepository.java`: profile-scoped document
  access is already present and must remain profile-owned.

#### Keycloak SPI SQL inventory

`keycloak2/user-storage-spi/.../NexaCoreUserRepository.java` currently:

- selects `auth_users` without tenant columns;
- finds username with `LOWER(u.username) = LOWER(?)` globally;
- lists, searches, and counts accounts across all tenants;
- maps email through `kyc_db.kyc_person`, then selects Auth by `person_id`;
- loads names, contacts, and verification flags directly from `kyc_person`;
- loads roles without role-active or tenant-compatibility predicates.

The SPI configuration currently requires both Auth and KYC JDBC connections. After
person cutover it should obtain canonical person/account data from Auth and remove the
KYC database dependency. Tenant resolution for Keycloak must be designed before its
queries are changed; a global username-only Keycloak lookup is incompatible with
duplicate usernames across tenants.

#### Database migration inventory

- Auth migrations `V5`, `V6`, `V9`, `V10`, and `V11` introduced the current person
  reference, removed duplicated contacts, normalized scope assignments, and made
  `person_id` mandatory/unique. These migrations are immutable; Phase 1 adds a new
  Auth migration.
- KYC migrations `V2`, `V3`, and `V4` introduced `is_user`, compatibility username,
  organizational assignments, profiles, and memberships. They remain immutable;
  transitional changes require new migrations.
- `auth_roles` currently has only globally unique `name`; it lacks tenant ownership,
  stable code, active state, description, and required audit columns.
- `auth_user_roles` currently cannot enforce tenant compatibility.

#### Frontend inventory

`frontendApplications/system-frontend-21` currently administers users by global
`username`, `personId`, and `roleIds`. Its user list/editor and `UserService` do not
display an account tenant, normalized username, role ownership, or separate global
template and tenant-role choices. This is deferred to Phase 7; no frontend contract is
changed during Phase 0.

### Phase 0 exit assessment

- All Phase 0 architecture choices are resolved; no open decision blocks Phase 1.
- Current code and schema remain unchanged, so runtime behavior still follows the
  legacy KYC-owned global-person and globally unique Auth-user model.
- Phase 1 may add schema and normalization components only. It must not switch reads,
  remove constraints, split accounts, or transfer production authority.
- Before Phase 3, each of the five locally observed unscoped users needs an explicit,
  trusted tenant disposition or quarantine record.

## Phase 1 execution record

Phase 1 was implemented additively in Auth migration `V12`. The migration has been
applied successfully to the local `auth_db` and recorded by Flyway.

Delivered foundation:

- created `auth_persons` with canonical identity, lifecycle, verification, audit, and
  timestamp fields;
- added the `AuthPerson` entity, `AuthPersonStatus`, and `AuthPersonRepository`;
- added nullable `tenant_id` and `normalized_username` to `auth_users`;
- added account lock and credential-lifecycle fields;
- added missing Auth user actor-audit fields with safe system defaults;
- added nullable role tenant ownership, stable role code, description, active state,
  actor audit, and timestamps;
- added partial tenant/person, tenant/username, and tenant/external-identity indexes;
- added partial global/tenant role-name and role-code indexes;
- retained the deployed global username, person, and role-name uniqueness constraints;
- added a shared `UsernameNormalizer` implementing trim, Unicode NFKC normalization,
  and `Locale.ROOT` lowercase with a 150-character post-normalization limit;
- added focused normalizer and Auth migration tests.

Local verification after Flyway application:

```text
Flyway version: 12 (success)
auth_persons: present
auth_users Phase 1 columns: 7/7 present
auth_roles Phase 1 columns: 8/8 present
Phase 1 partial indexes: 7/7 present
```

No existing user was assigned a tenant or normalized username, no role was reclassified,
and no authentication/repository lookup was switched. Those changes remain Phase 2 and
later work.

## Phase 2 execution record

Phase 2 was implemented on 2026-08-11 as a transitional dual-write. KYC remains the
read authority until the later backfill and cutover phases; this phase does not remove
legacy constraints or switch authentication lookups.

Delivered application behavior:

- added a module-neutral `GlobalPersonIdentityGateway` and an Auth implementation that
  creates or updates `auth_persons` while preserving the KYC global person ID;
- synchronized the Auth person projection after global KYC person creation, update,
  user promotion, and user-driven person provisioning;
- required a trusted effective tenant for every newly created Auth account and wrote
  both `tenant_id` and the shared normalized username;
- retained explicit legacy update paths for existing accounts whose tenant has not yet
  been resolved by the Phase 3 backfill;
- added tenant-qualified account and role repository methods alongside the legacy
  methods, without changing login behavior prematurely;
- rejected inactive roles and tenant-owned roles belonging to a different tenant when
  assigning roles in the administration service;
- added Auth migration `V13` with database triggers enforcing the same assignment rule
  and preventing tenant ownership changes for an already-assigned role;
- added Micrometer gauges for legacy users missing `tenant_id` or
  `normalized_username`;
- exposed `tenantId` and `normalizedUsername` in the user administration DTOs.

Verification:

```text
mvn test: passed (140 tests, 0 failures, 0 errors, 5 skipped)
V13 SQL parsed and created both triggers in local auth_db transaction: passed/rolled back
Testcontainers migration suites: 4 tests skipped because Docker socket access is unavailable
```

Transitional constraints remain intentional. The deployed global username/person
uniqueness constraints still prevent duplicate cross-tenant accounts until Phase 5.
Existing users are not assigned tenants in this phase. A platform administrator with no
trusted tenant scope cannot create a tenant account; Phase 3 must resolve or quarantine
legacy scope ambiguity rather than accepting tenant ownership from an untrusted request.

## Phase 3 execution record

Phase 3 was implemented and executed locally on 2026-08-11. Auth migration `V14`
performs the deterministic Auth-local account backfill and records unresolved accounts
in `auth_user_backfill_quarantine`. A guarded, idempotent application runner copies and
reconciles global people across the physical KYC/Auth database boundary.

Delivered behavior:

- normalized legacy usernames using PostgreSQL NFKC normalization, trim, and lowercase,
  matching the server-side normalizer;
- assigned an account tenant only when exactly one distinct active
  `auth_user_scope_assignments.tenant_id` existed;
- created auditable quarantine rows for no-scope, ambiguous-scope, invalid-username,
  and normalized-username-conflict cases rather than guessing ownership;
- retained unresolved accounts for explicit review; authentication behavior is not
  switched until Phase 4;
- classified all existing roles as global templates and backfilled stable role codes
  from their existing names;
- added a quarantine entity/repository and an unresolved-quarantine Micrometer gauge;
- added `GlobalPersonBackfillService` and a fail-closed startup runner controlled by
  `AUTH_PERSON_BACKFILL_ENABLED`, with bounded paging and a report of failed IDs;
- corrected the preserved-ID Auth person insert path discovered during the live
  backfill, and advanced the Auth identity sequence safely after explicit-ID inserts.

Local execution and reconciliation result:

```text
Flyway auth version: 14 (success)
KYC persons copied to Auth: 14/14; IDs preserved 1..14
Auth users with normalized usernames: 7/7
Auth users resolved to one trusted tenant: 2/7
Auth users quarantined with NO_ACTIVE_SCOPE: 5/7
Ambiguous multi-tenant users: 0
Auth users referencing a missing Auth person: 0
Existing roles classified as global templates with role codes: 6/6
KYC profiles referencing a missing KYC person: 0
KYC memberships referencing a missing KYC person: 0
System privilege assignments: 70, referencing existing Auth role IDs 1..5
auth_persons identity sequence: 14
mvn test: 140 tests, 0 failures, 0 errors, 5 Docker-dependent tests skipped
```

No legacy account had multiple trusted tenant assignments, so account splitting and a
credential/username migration policy were not required for this dataset. The five
no-scope accounts require an explicit trusted scope assignment or a reviewed platform-
account disposition before Phase 4/5 can make tenant ownership mandatory.

For a later environment, run the person reconciliation during one controlled restart:

```bash
export AUTH_PERSON_BACKFILL_ENABLED=true
export AUTH_PERSON_BACKFILL_PAGE_SIZE=250
```

After confirming the completion report, disable `AUTH_PERSON_BACKFILL_ENABLED`; normal
Phase 2 dual-write continues to synchronize subsequent person changes.

This plan changes the current identity rule from one global `AuthUser` per person to:

```text
one global person
    -> many tenant-specific authentication accounts
    -> one or more scopes inside each account's tenant
    -> global role templates and tenant-defined custom roles
```

It also moves the global person record from `kyc_db` to `auth_db`. This is a deliberate
domain-boundary change and must update the repository documentation that currently
defines `KycPerson` as the global identity owned by the KYC module.

## Goals

- Maintain exactly one global human identity across the platform.
- Allow the same person to have a different username and password in each tenant.
- Keep organization-specific KYC facts and decisions in `kyc_db`.
- Keep credentials, authentication factors, sessions, roles, and access scopes in
  `auth_db`.
- Allow global role templates and tenant-defined custom roles.
- Prevent tenant accounts, custom roles, role assignments, and scopes from crossing
  tenant boundaries.
- Preserve explicit application-level validation for references crossing physical
  databases.

## Non-goals

- Do not merge KYC profile data into Auth.
- Do not store passwords, MFA secrets, or refresh tokens in `kyc_db`.
- Do not add `tenant_id` to the global person record.
- Do not make organization membership automatically grant application access.
- Do not allow a global role template to carry tenant-specific state.
- Do not rely on a frontend-provided tenant, person, role, or scope as authority.

## Target domain model

```text
auth_db

auth_persons                         global human identity
    1
    +-- * auth_users                 tenant-specific login account
              +-- * auth_user_roles
              +-- * auth_user_scope_assignments
              +-- * auth_user_contact_methods
              +-- * auth_refresh_sessions

auth_roles
    tenant_id NULL                   global role template
    tenant_id NOT NULL               tenant-defined custom role

kyc_db

kyc_person_profiles                  tenant-owned KYC relationship
    person_id                        application reference to auth_persons.id
    tenant_id
    organization observations and KYC state
```

Example:

```text
auth_persons #100
    Rahim Ahmed

auth_users #801
    person_id = 100
    tenant_id = Bank A
    username = rahim.bankA

auth_users #902
    person_id = 100
    tenant_id = Bank B
    username = rahim.bankB

kyc_person_profiles #501
    person_id = 100
    tenant_id = Bank A
    status = APPROVED

kyc_person_profiles #702
    person_id = 100
    tenant_id = Bank B
    status = PENDING
```

## Data ownership rules

### Global person

`auth_persons` owns canonical platform identity:

- name;
- date of birth;
- gender;
- blood group;
- canonical contact information, if the business accepts global contacts;
- identity lifecycle and merge status.

It must not contain `tenant_id`, organization username, password, KYC status, risk
rating, organization documents, or organization decisions.

### Tenant account

`auth_users` owns one tenant-specific login account:

- `person_id`;
- `tenant_id`;
- username and normalized username;
- password hash or external identity;
- enabled/locked state;
- last-login and credential lifecycle fields.

The default rule is at most one account for the same person in the same tenant. If the
business later needs multiple accounts per person per tenant, that constraint must be
revisited explicitly rather than silently removed.

### Tenant KYC profile

`kyc_person_profiles` owns the organization's relationship with the person:

- tenant/business/branch;
- customer number;
- declared or observed identity values;
- local email/mobile/address;
- KYC status and risk rating;
- organization documents, photos, reviews, decisions, and workflows.

Profile observations do not silently overwrite canonical global person fields.

## Proposed Auth schema

The DDL below defines the target shape. Production migrations must be additive and
versioned; do not edit deployed migrations.

### `auth_persons`

```sql
CREATE TABLE auth_persons (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    first_name varchar(100) NOT NULL,
    middle_name varchar(100),
    last_name varchar(100),
    date_of_birth date,
    gender varchar(30),
    blood_group varchar(10),
    primary_email varchar(254),
    primary_mobile varchar(30),
    email_verified boolean NOT NULL DEFAULT false,
    mobile_verified boolean NOT NULL DEFAULT false,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    active boolean NOT NULL DEFAULT true,
    created_by bigint NOT NULL,
    updated_by bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_auth_person_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MERGED', 'DECEASED'))
);
```

Do not make email/mobile globally unique until the business confirms that shared
contacts are prohibited. Prefer normalized contact tables if multiple contacts,
verification history, or reuse must be supported.

### Updated `auth_users`

Target columns:

```sql
ALTER TABLE auth_users
    ADD COLUMN tenant_id bigint,
    ADD COLUMN normalized_username varchar(150),
    ADD COLUMN locked boolean NOT NULL DEFAULT false,
    ADD COLUMN password_changed_at timestamp,
    ADD COLUMN credentials_expire_at timestamp;
```

After backfill and application rollout:

```sql
ALTER TABLE auth_users
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN normalized_username SET NOT NULL;
```

Replace current global uniqueness:

```sql
DROP INDEX IF EXISTS ux_auth_users_person_id;
DROP INDEX IF EXISTS ux_auth_users_username;

CREATE UNIQUE INDEX ux_auth_users_tenant_person
    ON auth_users (tenant_id, person_id);

CREATE UNIQUE INDEX ux_auth_users_tenant_username
    ON auth_users (tenant_id, normalized_username);

CREATE UNIQUE INDEX ux_auth_users_tenant_external_identity
    ON auth_users (tenant_id, external_provider, external_subject)
    WHERE external_provider IS NOT NULL
      AND external_subject IS NOT NULL;

ALTER TABLE auth_users
    ADD CONSTRAINT uk_auth_users_id_tenant UNIQUE (id, tenant_id);
```

`normalized_username` is produced only by a shared server-side normalizer. At minimum:

```text
trim -> Unicode normalization -> lowercase with Locale.ROOT
```

Do not accept a caller-calculated normalized value as authoritative.

### Updated `auth_user_scope_assignments`

Keep the existing normalized scope table. Enforce that every scope belongs to the
account's tenant:

```sql
ALTER TABLE auth_user_scope_assignments
    DROP CONSTRAINT IF EXISTS auth_user_scope_assignments_user_id_fkey;

ALTER TABLE auth_user_scope_assignments
    ADD CONSTRAINT fk_auth_scope_user_tenant
    FOREIGN KEY (user_id, tenant_id)
    REFERENCES auth_users(id, tenant_id)
    ON DELETE CASCADE;
```

Preserve the hierarchy constraint:

```text
tenantId is required
branchId requires businessId
scope tenant must equal account tenant
business/branch must belong to that tenant
```

### Tenant-aware `auth_roles`

Add nullable `tenant_id`:

```sql
ALTER TABLE auth_roles
    ADD COLUMN tenant_id bigint,
    ADD COLUMN description varchar(255),
    ADD COLUMN active boolean NOT NULL DEFAULT true,
    ADD COLUMN created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

Meaning:

```text
auth_roles.tenant_id IS NULL      global role template
auth_roles.tenant_id IS NOT NULL  tenant-defined custom role
```

Replace global name uniqueness with separate global and tenant uniqueness:

```sql
ALTER TABLE auth_roles
    DROP CONSTRAINT IF EXISTS auth_roles_name_key;

CREATE UNIQUE INDEX ux_auth_roles_global_name
    ON auth_roles (LOWER(name))
    WHERE tenant_id IS NULL;
dynamic model added
CREATE UNIQUE INDEX ux_auth_roles_tenant_name
    ON auth_roles (tenant_id, LOWER(name))
    WHERE tenant_id IS NOT NULL;
```

This permits:

```text
global: ROLE_KYC_OPERATOR
Bank A: ROLE_BRANCH_REVIEWER
Bank B: ROLE_BRANCH_REVIEWER
```

The two tenant roles have different IDs and may receive different privilege sets.

### Updated `auth_user_roles`

The existing join table may retain `(user_id, role_id)` as its primary key, but the
database must also enforce the tenant compatibility rule:

```text
role.tenant_id IS NULL
OR role.tenant_id = user.tenant_id
```

A normal `CHECK` constraint cannot compare rows in `auth_users` and `auth_roles`.
Choose one of these implementations during Phase 2:

#### Option A — service enforcement plus database trigger

- Load the account and role in the same Auth transaction.
- Reject inactive accounts or roles.
- Reject tenant roles whose `tenant_id` differs from the account tenant.
- Add a PostgreSQL constraint trigger as a database backstop.

This most directly expresses global-role compatibility.

#### Option B — normalized assignment rows

Add `tenant_id` and `role_tenant_id` to the assignment and use composite constraints.
This provides more declarative integrity but makes nullable global-role matching more
complex and duplicates tenant data.

**Recommended option:** Option A. Keep the join table simple, make the service rule
authoritative, and use a small database trigger to prevent direct-SQL violations.

Required trigger behavior:

```sql
IF role.tenant_id IS NOT NULL AND role.tenant_id <> user.tenant_id THEN
    RAISE EXCEPTION 'Tenant role cannot be assigned to an account in another tenant';
END IF;
```

The same validation must run when an existing role's `tenant_id` changes. Once a role
has assignments, moving it between global and tenant ownership should be prohibited;
create a new role instead.

## Global role template semantics

A global role is a reusable platform role definition, not a row copied automatically
into every tenant.

Rules:

- Only platform administrators can create, rename, deactivate, dynamic model addedor assign privileges to
  global roles.
- Tenant administrators may view assignable global roles but cannot mutate them.
- A tenant administrator can create and manage roles only for the effective tenant.
- A tenant role can be assigned only to accounts in the same tenant.
- A global role can be assigned to an account in any tenant, subject to client and
  entitlement policy.
- Role names are immutable after production use when external systems depend on them;
  otherwise use stable `role_code` plus editable `role_name`.
- Deactivating a role must make its assignments ineffective without deleting audit
  history.

Prefer adding a stable code:

```sql
ALTER TABLE auth_roles ADD COLUMN role_code varchar(100);
```

Then make `role_code` unique globally or within a tenant using the same partial-index
pattern as role names.

## Privilege assignments across databases

Role definitions live in `auth_db`, while `sys_priv_role_privileges` lives in
`system_db` and stores Auth role IDs without a physical cross-database foreign key.

The privilege assignment service must:

1. Resolve the current effective tenant and actor.
2. Load role metadata through `AuthModuleGateway`.
3. Verify that the role exists and is active.
4. For a tenant role, require `role.tenantId == effectiveTenantId`.
5. For a global role, require platform-level global-role management authority for
   mutation.
6. Validate every privilege code and entitlement restriction.
7. Replace the role's privilege set transactionally in `system_db`.
8. Record tenant ID, role ID, actor, and before/after values in the audit log.

Because the update spans databases, strict ACID atomicity is unavailable. Add a
reconciliation job for missing roles, stale privilege assignments, and tenant
mismatches. Do not silently treat an unavailable Auth gateway as authorization.

## Tenant resolution and authentication

Authentication must resolve tenant before username lookup:

```text
verified request domain
    -> resolved tenant
    -> validate tenant and client application
    -> normalize username
    -> find auth_users by (tenant_id, normalized_username)
    -> validate password or external identity
    -> create tenant-bound authenticated session
```

Repository replacements:

```java
Optional<AuthUser> findByTenantIdAndNormalizedUsername(
        Long tenantId,
        String normalizedUsername
);

Optional<AuthUser> findByTenantIdAndPersonId(
        Long tenantId,
        Long personId
);
```

Remove authentication-time reliance on global `findByUsername` and
`findByPersonId`. Internal reconciliation may retain explicit cross-tenant search
methods behind platform-only services.

Refresh sessions must be bound to account, tenant, and client application. A token
issued for Bank A must not select or authenticate the Bank B account for the same
person.

## SSO and Keycloak impact

The current SSO provisioning flow finds users globally by username/person ID. It must
be changed to tenant-aware lookup.

For external identity uniqueness use:

```text
(tenant_id, external_provider, external_subject)
```

Decide explicitly whether the same external subject creates:

- one account per authorized tenant; or
- one home account with separately granted tenant scopes.

This plan selects one account per tenant to remain consistent with tenant-specific
usernames and credentials. Keycloak federation queries and the Keycloak 26 user-storage
SPI must be updated together with the Auth schema.

## Person migration from KYC DB to Auth DB

This is a cross-database move and must use staged dual-read/dual-write migration rather
than a single destructive migration.

### Source-to-target mapping

```text
kyc_person.id               -> auth_persons.id
kyc_person.first_name       -> auth_persons.first_name
kyc_person.last_name        -> auth_persons.last_name
kyc_person.date_of_birth    -> auth_persons.date_of_birth
kyc_person.gender           -> auth_persons.gender
kyc_person.blood_grop       -> auth_persons.blood_group
kyc_person.email            -> auth_persons.primary_email
kyc_person.mobile_number    -> auth_persons.primary_mobile
kyc_person.email_verified   -> auth_persons.email_verified
kyc_person.mobile_verified  -> auth_persons.mobile_verified
```

Do not copy these into the global person:

```text
kyc_person.username       authentication concern
kyc_person.is_user       derived from account existence
kyc_person.documents     move ownership to KYC profile
kyc_person.photo_url     classify as global canonical photo or profile evidence first
```

Preserve person IDs during migration so existing `auth_users.person_id`, profiles, and
memberships do not need mass ID translation. Advance the new identity sequence above
the imported maximum ID.

## KYC profile changes

Retain `kyc_person_profiles.person_id` as an application-level reference to
`auth_persons.id`. Add explicit organization observation fields only when required:

```text
declared_name
declared_date_of_birth
declared_email
declared_mobile
```

Do not create a complete uncontrolled copy of the global person. API responses may
compose canonical identity and tenant observations, but must label their source and
apply profile-scoped database predicates.

Scoped KYC APIs continue to use profile ID as the resource ID and expose global
`personId` separately.

## Backend changes

### Auth module

- Add `AuthPerson` entity and repository.
- Change `AuthUser` from global user to tenant account.
- Add tenant-aware username normalization and repository methods.
- Update registration, login, refresh, SSO provisioning, password reset, and account
  administration.
- Update `AuthModuleGateway` DTOs to expose account tenant and role tenant safely.
- Add tenant-aware role create/list/update/deactivate services.
- Enforce global-template and tenant-role assignment rules.

### Gateway contracts

- Move global person lookup/provisioning ownership from `PersonModuleGateway` to a
  neutral identity contract owned outside KYC.
- Keep KYC profile operations behind the KYC gateway.
- Add explicit Auth role metadata lookup for the privilege module.
- Cross-database failures must fail closed and return stable error codes.

### System privilege module

- Make role privilege assignment tenant-aware.
- Prevent tenant administrators from mutating global role templates.
- Filter role administration by effective tenant plus visible global templates.
- Reconcile `sys_priv_role_privileges` against Auth role existence and tenant metadata.

### KYC module

- Replace the local global-person entity with a gateway-backed global identity
  reference or transitional read model.
- Keep profiles, memberships, evidence, and KYC decisions in `kyc_db`.
- Change documents and tenant KYC photos to be profile-owned.
- Preserve scope predicates on every profile read/write/history/download path.

## API shape

### Tenant account creation

The effective tenant comes from authenticated request context, not from an unrestricted
request field.

```json
{
  "personId": 100,
  "username": "rahim.bankA",
  "password": "write-only-secret",
  "scopeAssignments": [
    {
      "businessId": 10,
      "branchId": 100
    }
  ],
  "roleIds": [20, 31]
}
```

The backend supplies `tenantId`, `normalizedUsername`, audit actor, and password hash.

### Tenant custom role creation

```json
{
  "roleCode": "BRANCH_REVIEWER",
  "roleName": "Branch Reviewer",
  "description": "Reviews branch-level KYC submissions",
  "privilegeCodes": [
    "01010200106",
    "01010200107"
  ]
}
```

The backend sets `auth_roles.tenant_id` from effective tenant context.

Global role creation must use a separate platform-only endpoint or explicit operation,
never a caller-provided `tenantId: null` convention.

## Migration phases

### Phase 0 — decisions and inventory — completed 2026-08-11

- [x] Approve global person ownership moving to Auth.
- [x] Confirm whether one person may have more than one account per tenant.
- [x] Confirm global versus tenant contact uniqueness.
- [x] Decide global-role immutability and tenant role-code conventions.
- [x] Inventory every use of global `findByUsername`, `findByPersonId`, `AuthUser`,
  `KycPerson`, and Keycloak SPI SQL.
- [x] Update repository design documentation before code changes.

### Phase 1 — additive Auth foundation — completed 2026-08-11

- [x] Create `auth_persons`.
- [x] Add nullable `tenant_id` and `normalized_username` to `auth_users`.
- [x] Add tenant/audit columns to `auth_roles`.
- [x] Add indexes that do not conflict with existing production data.
- [x] Implement shared username normalization.
- [x] Do not remove current constraints yet.

### Phase 2 — application dual-write — completed 2026-08-11

- [x] Copy/create the global Auth person when a global KYC person is created.
- [x] Write tenant and normalized username for all new accounts.
- [x] Add tenant-aware repositories and services alongside legacy methods.
- [x] Enforce custom-role tenant compatibility in service and database trigger.
- [x] Add metrics for legacy rows missing tenant or normalized username.

### Phase 3 — data backfill — completed 2026-08-11

- [x] Copy `kyc_person` rows to `auth_persons` while preserving IDs.
- [x] Backfill each existing account's tenant from trusted active scope assignments.
- [x] Quarantine accounts with zero or multiple ambiguous tenant assignments.
- [x] If a legacy account legitimately covers multiple tenants, create one account per
  tenant with an explicit username/credential migration policy.
  No such account existed in the executed dataset, so no split was performed.
- [x] Backfill normalized usernames.
- [x] Classify existing roles as global templates by leaving `tenant_id` null.
- [x] Reconcile KYC profiles, memberships, users, roles, and privilege mappings.

Never infer an account tenant from caller-controlled headers or arbitrary profile
selection.

### Phase 4 — authentication cutover — completed 2026-08-11

- [x] Resolve tenant before account lookup.
- [x] Switch login, refresh, reset, SSO, gateway, and Keycloak queries to tenant-aware
  account identity.
- [x] Bind sessions and tokens to the authenticated account and effective tenant.
- [x] Run dual-read comparison telemetry before disabling legacy global lookups.

Delivered behavior:

- client application mappings must resolve to exactly one active tenant before local
  password or SSO account lookup; missing and multi-tenant mappings fail closed;
- password and SSO lookup use `(tenant_id, normalized_username)`, tenant/person, and
  tenant/external-subject keys as applicable;
- access and refresh tokens carry mandatory `account_id` and `tenant_id` claims;
- logout and refresh-session state is keyed by the account/tenant pair;
- authenticated request authorization reloads the exact account using `(id, tenant)`;
- Micrometer dual-read comparison counters remain enabled by default during the
  stabilization window and can be disabled with
  `AUTH_CUTOVER_DUAL_READ_COMPARISON_ENABLED=false`;
- each Keycloak federation component now requires an explicit tenant ID and all user,
  search, count, credential, email, and role queries are constrained to that tenant.

There is no password-reset flow in the current backend, so there was no reset lookup
to migrate in this phase. Any future reset implementation must use the same resolved
tenant account key.

### Phase 5 — constraint cutover — implemented; deployment data gate open

- [x] Drop global username/person uniqueness.
- [x] Add tenant-scoped username/person uniqueness.
- [x] Make `tenant_id` and `normalized_username` non-null.
- [x] Add composite user/scope foreign key.
- [x] Enable role-assignment database trigger.
- [x] Reject role tenant changes after assignment.

Phase 5 is implemented in Auth migration `V15`. The migration has a fail-closed
preflight for missing tenant/normalized username values, cross-tenant scopes, and
cross-tenant role assignments. It replaces the Phase 1 partial indexes with final
tenant-scoped unique indexes, removes legacy global username/person uniqueness, makes
the account tenant fields mandatory, and replaces the scope's user-only foreign key
with `(user_id, tenant_id) -> auth_users(id, tenant_id)`.

Application mappings now reflect mandatory tenant account fields and the composite
scope relationship. User administration direct-ID updates are tenant-predicated,
legacy global-username reservation checks are removed, cutover comparison telemetry
defaults off, and default account seeding requires the explicit trusted
`AUTH_BOOTSTRAP_TENANT_ID` setting. Request authorization now rejects any principal
that is not account-and-tenant bound.

The migration SQL was exercised successfully through `ROLLBACK` against the local
PostgreSQL schema. It has not been applied to the local database because five legacy
accounts still have no trusted tenant; current readiness is `missing tenant=5`,
`missing normalized username=0`. Those accounts require reviewed dispositions before
Flyway can apply V15. This is the intentional Phase 3/5 data gate, not a migration
fallback or inferred assignment.

### Phase 6 — person ownership cutover

- Make Auth the authoritative global person service.
- Switch KYC reads to composed Auth person plus tenant profile DTOs.
- Stop writes to the old `kyc_person` table.
- Verify reconciliation for a defined stabilization period.
- Retire the old table only through a later, separately approved migration; do not
  delete it during initial cutover.

### Phase 7 — administration frontends

- Add tenant account administration with tenant-scoped usernames.
- Add role administration showing two sections: assignable global templates and
  tenant custom roles.
- Hide global-role mutation controls from tenant administrators.
- Require confirmation and audit display for role/privilege replacement operations.
- Add person/profile views that distinguish canonical and organization-declared data.

## Verification requirements

### Migration tests

- Existing person IDs are preserved.
- No Auth account lacks tenant or normalized username after backfill.
- Duplicate usernames are permitted across tenants but rejected within one tenant.
- One person can have Bank A and Bank B accounts.
- Scope tenant cannot differ from account tenant.
- Existing roles migrate as global templates.
- Tenant role names may repeat across tenants.

### Authorization tests

- Bank A cannot find or authenticate Bank B's account using the same username.
- Bank A cannot assign a Bank B custom role.
- Bank A can assign an allowed global template.
- Tenant administrators cannot edit global roles or their privilege definitions.
- Platform administrators can manage global templates through the platform-only path.
- Direct-ID account and role operations enforce tenant predicates.
- Missing tenant context fails closed.

### Cross-database tests

- Account creation rejects a nonexistent person ID.
- Profile creation rejects a nonexistent global person ID.
- Reconciliation detects missing people, accounts, roles, and privilege mappings.
- Auth or System gateway outages fail closed for authorization changes.
- Deactivating a tenant role removes its effective privileges without deleting audit
  history.

### SSO and session tests

- The same external subject can be provisioned correctly for two authorized tenants.
- Bank A refresh tokens cannot select the Bank B account.
- Tenant resolution and account lookup are case-normalized and domain-bound.
- Keycloak federation returns the correct tenant account and roles.

## Rollback strategy

- Retain the original `kyc_person` table throughout dual-write and stabilization.
- Retain legacy Auth lookup methods behind a disabled-by-default compatibility flag
  until tenant-aware authentication is proven.
- Do not drop old unique indexes until conflicting tenant accounts are ready to be
  created.
- Take database backups before each constraint/cutover phase.
- Roll back application traffic to legacy reads only while both representations remain
  reconciled; do not attempt rollback after accepting divergent writes without a
  documented reverse-sync process.

## Completion criteria

Implementation is complete only when:

- Auth is authoritative for global persons and tenant accounts.
- `auth_persons` contains no tenant ownership.
- every `auth_user` has exactly one tenant and one global person;
- tenant username uniqueness is enforced;
- each account scope belongs to the account tenant;
- global role templates have `tenant_id IS NULL`;
- tenant custom roles have the owning tenant ID;
- role assignments enforce `role.tenant_id IS NULL OR role.tenant_id = user.tenant_id`;
- cross-database role privilege and person/profile references are reconciled;
- login, refresh, SSO, Keycloak, and administration flows are tenant-aware;
- adversarial two-tenant tests pass;
- the old KYC-owned global-person write path is disabled.
