# Tenant- and Client-Scoped Authentication Policy Implementation Plan

## 1. Decision

NexaCore supports exactly one active primary login method for each tenant and client application pair.

```text
(tenantId, clientCode) -> one active primary login method
```

Examples:

| Tenant | Client code | Primary login method | Login identifier |
|---|---|---|---|
| Tenant A | `WEB` | `PASSWORD` | `USERNAME` |
| Tenant A | `SYSTEM_ADMIN_WEB` | `SSO` | `USERNAME` |
| Tenant B | `WEB` | `OTP` | `MOBILE` |

Multiple simultaneous primary login methods are not supported. `MFA` is not a primary login method; it is a separate second-factor policy applied after the primary method succeeds. Registration, verification, invitation, and activation rules are also separate policies.

The system must fail closed when the tenant or client is missing, inactive, ambiguous, or unauthorized, and when zero or multiple active authentication policies are found.

## 2. Current State and Gaps

`auth_client_auth_policy` is currently scoped only by `client_code`. Its unique index includes `login_method` and `login_identifier_type`, so the database permits multiple enabled policies for the same client. `AuthClientPolicyService` hides this ambiguity by selecting the first login method. That behavior depends on row/collection order and is not a valid enforcement mechanism.

The public authentication configuration accepts `X-Client-Code` and may derive a client from `Origin`. Neither value alone is trusted proof of a tenant. The current pre-login path therefore cannot enforce tenant-and-client uniqueness safely without an authoritative tenant/client resolution step.

For authenticated requests, `AuthenticatedRequestContextFilter` already establishes the relevant trusted context:

- tenant/account identity comes from `TenantAccountUserDetails`;
- user access is loaded using the principal's account and tenant IDs;
- client identity comes from `ClientApplicationContextHolder`;
- `EffectiveTenantAccessResolver` validates the effective tenant and scopes;
- the resulting tenant/client data is published through request-scoped context holders and cleared in `finally`.

This filter must remain the authenticated request boundary. Authentication-policy resolution should consume its validated tenant/client context where available, not repeat or weaken that validation.

Anonymous endpoints such as `/api/v1/auth/config`, `/authenticate`, `/login-status`, and `/sso/authenticate` execute before an authenticated request context exists. They require a dedicated public tenant/client resolver.

## 3. Target Domain Model

### 3.1 Primary authentication policy

Add tenant ownership to `AuthClientAuthPolicy`:

```text
tenant_id             bigint       not null
client_code           varchar(100) not null
login_method          varchar(50)  not null
login_identifier_type varchar(50)  not null
enabled               boolean      not null
created_by            bigint       not null
updated_by            bigint       not null
created_at             timestamp    not null
updated_at             timestamp    not null
```

The canonical invariant is one row per `(tenant_id, normalized_client_code)`. Prefer updating that row when the policy changes:

```sql
UNIQUE (tenant_id, LOWER(client_code))
```

If disabled historical alternatives must remain in the same table, use a partial unique index instead:

```sql
CREATE UNIQUE INDEX ux_auth_client_auth_policy_active
    ON auth_client_auth_policy (tenant_id, LOWER(client_code))
    WHERE enabled = true;
```

The first option is preferred because audit/history should be recorded in a dedicated history or audit mechanism, not represented by ambiguous inactive policy rows.

`tenant_id` is an application-level reference because Auth and System use separate databases. Do not add a cross-database foreign key. Validate tenant/client ownership through a module gateway and include reconciliation for orphaned references.

### 3.2 Login-method classification

Primary methods may include:

- `PASSWORD`
- `OTP`
- `MAGIC_LINK`
- `OAUTH`
- `SSO`
- `BIOMETRIC`
- `PASSKEY`

Remove `MFA` from primary-method selection or reject it during policy validation. Introduce a separate second-factor policy, for example:

```text
second_factor_mode = DISABLED | OPTIONAL | REQUIRED
second_factor_method = OTP | TOTP | PASSKEY | null
```

The second-factor model may be implemented in a later migration, but the primary-policy API must not advertise `MFA` as an alternative primary method.

### 3.3 Registration policy

Apply the same tenant/client scope to `auth_client_registration_policy` if registration credentials vary by tenant and client. Decide explicitly whether registration allows one credential model or several:

- if unique, enforce one row per `(tenant_id, client_code)` and expose a singular value;
- if multiple models are a real product requirement, retain a collection and enforce uniqueness per `(tenant_id, client_code, registration_credential_model)`.

Do not derive the primary login policy from a registration-policy collection.

## 4. Trusted Context Resolution

### 4.1 Authenticated requests

Extend `AuthenticatedRequestContext` only if it does not already expose all required values. Authentication-policy consumers must use:

```text
tenantId     <- TenantAccountUserDetails / EffectiveTenantAccessContext
clientCode   <- validated SysAccClientApplication in ClientApplicationContext
```

Do not use a raw `X-Client-Code`, `Origin`, host header, or caller-supplied tenant header after authenticated context has been established.

If a request requires a client-scoped authentication policy but `AuthenticatedRequestContextFilter` cannot resolve a client, reject it rather than falling back to a global policy.

### 4.2 Anonymous pre-login requests

Create a `PublicAuthPolicyContextResolver` behind an Auth-to-System module gateway. It should resolve:

```text
request origin + requested client code -> active client application -> owning/selected tenant
```

Resolution rules:

1. Normalize `X-Client-Code` using trim plus a documented case strategy.
2. Resolve an active `SysAccClientApplication` by client code.
3. Validate the request origin against the client's registered allowed origins.
4. Resolve the tenant from a trusted client-to-tenant assignment.
5. Reject clients assigned to no tenant.
6. Reject ambiguous multi-tenant assignments unless the request uses a server-issued tenant discovery/selection token.
7. Return an immutable context containing `tenantId`, canonical `clientCode`, OAuth client ID, and validated redirect/logout URIs.

`X-Client-Code` identifies the requested application but is not authentication and is not a secret. It must be sent independently of `X-API-Key`. It must never directly determine tenant ownership without server-side lookup.

For clients legitimately shared by multiple tenants, introduce a two-step flow rather than trusting a tenant header:

1. discover/select tenant using an approved domain, invitation, organization identifier, or username discovery mechanism;
2. issue a short-lived, signed pre-auth context token binding tenant, client, origin, purpose, and expiry;
3. require that token for config, password, OTP, and SSO exchange requests.

Do not infer tenant from username alone when duplicate usernames may exist across tenants.

## 5. Backend Changes

### Phase 1: Schema and migration

**Status: Additive preparation implemented in V17.** The migration adds nullable tenant ownership, normalizes audit data, replaces client-only indexes with tenant-aware resolved/unresolved indexes, and creates a reconciliation queue maintained for both migrated and post-migration unresolved rows. Trusted backfill and the final `tenant_id NOT NULL` cutover remain intentionally pending because authoritative client-to-tenant assignments live in `system_db` and cannot be inferred safely inside the Auth Flyway migration.

1. Add the next immutable Auth Flyway migration; do not edit `V7__add_client_auth_registration_policy.sql`.
2. Add nullable `tenant_id` initially.
3. Backfill tenant IDs only from trusted client-to-tenant assignments obtained through an explicit migration input or reconciliation job.
4. Leave unresolved rows inaccessible and report them for repair. Do not assign a default tenant silently.
5. Detect duplicates by `(tenant_id, LOWER(client_code))` before adding the unique constraint.
6. Select a surviving row only through an approved deterministic repair rule; otherwise stop migration/reconciliation and require operator action.
7. Make `tenant_id`, `created_by`, `updated_by`, and audit timestamps non-null after successful repair.
8. Add the unique constraint and lookup index.
9. Apply equivalent tenant ownership to registration policy if it is tenant-specific.

Suggested diagnostic query before enforcement:

```sql
SELECT tenant_id, LOWER(client_code), COUNT(*)
FROM auth_client_auth_policy
WHERE enabled = true
GROUP BY tenant_id, LOWER(client_code)
HAVING COUNT(*) <> 1;
```

### Phase 2: Entity and repository

**Status: Implemented on 2026-08-14.** `AuthClientAuthPolicy` now carries `tenantId`; repository reads require tenant plus normalized client code and return a singular optional policy. The Flyway functional unique index remains authoritative because JPA table metadata cannot accurately express `LOWER(client_code)` plus the resolved-row predicate.

Update `AuthClientAuthPolicy` with `tenantId` and explicit table-level uniqueness metadata for documentation. Keep the Flyway constraint authoritative.

Replace list-returning active lookup methods with a singular repository contract:

```java
Optional<AuthClientAuthPolicy> findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(
        Long tenantId,
        String clientCode
);
```

For defense in depth, the service should still detect non-unique results during the compatibility rollout. Do not use `.findFirst()`, stream `.limit(1)`, or unspecified ordering to resolve duplicates.

### Phase 3: Policy service

**Status: Implemented on 2026-08-14.** Runtime property fallback and first-row selection were removed. `ResolvedAuthPolicy` is the canonical result, unresolved/missing policies fail with `AUTH_POLICY_NOT_CONFIGURED`, method mismatch fails with `LOGIN_METHOD_NOT_ALLOWED`, invalid/MFA primary policies fail integrity validation, and password, SSO, configuration, application-context, and login-status paths resolve the same tenant/client policy after trusted tenant resolution. Bootstrap properties are now used only to seed tenant-owned policy rows.

Replace collection-based policy resolution with a required singular result:

```java
ResolvedAuthPolicy resolveRequiredPolicy(long tenantId, String clientCode);
```

`ResolvedAuthPolicy` should contain at least:

```text
tenantId
clientCode
loginMethod
loginIdentifierType
registrationCredentialModel or registration policy reference
policyVersion
```

Remove `firstLoginMethodOnly()`. Configuration-property defaults may be used only for controlled seed/bootstrap creation, not as a runtime fallback after tenant-scoped policies are enabled. Missing database policy must fail closed with a stable error such as `AUTH_POLICY_NOT_CONFIGURED`.

All enforcement points must call the same resolver:

- public authentication config;
- password authentication;
- OTP authentication;
- SSO authentication/exchange;
- login-status checks where method-specific behavior matters;
- registration credential selection.

The config endpoint and the authentication endpoint must resolve the same tenant/client policy so the UI cannot advertise a method the server later evaluates under a different context.

### Phase 4: DTO and API contract

**Status: Implemented on 2026-08-14.** Public and authenticated context DTOs expose singular login method, identifier, registration credential model, disabled second-factor policy, and policy version. Compatibility collections are deprecated and generated as exactly one item from the singular policy. `/auth/config` now returns one canonical top-level configuration with layout rather than embedding a duplicate `applicationContext`; the active Angular 21 platform client prefers singular fields and normalizes legacy one-item arrays during rollout. The legacy `frontend-libs` repository is intentionally unchanged.

Introduce singular canonical fields:

```json
{
  "tenantId": 10,
  "clientCode": "SYSTEM_ADMIN_WEB",
  "loginMethod": "SSO",
  "loginIdentifierType": "USERNAME",
  "registrationCredentialModel": "ENTERPRISE_SSO",
  "secondFactorPolicy": {
    "mode": "DISABLED",
    "method": null
  }
}
```

Do not expose `tenantId` publicly if it is considered sensitive; an opaque tenant/context reference can be returned instead. Internally, the resolved context must still contain the real tenant ID.

During one compatibility window, retain these projections:

```json
{
  "enabledLoginMethods": ["SSO"],
  "loginIdentifierTypes": ["USERNAME"],
  "enabledRegistrationCredentialModels": ["ENTERPRISE_SSO"]
}
```

Each compatibility array must contain exactly one item and must be generated from the singular canonical policy. Mark the collection fields deprecated and remove them after all Angular applications migrate.

Consolidate duplicated top-level and `applicationContext` authentication properties. There must be one canonical object, not two independently populated representations.

### Phase 5: Client and SSO configuration

**Status: Implemented on 2026-08-14.** OAuth client IDs and exact login/logout redirect URI allowlists are persisted on each System client application and exposed through the administration contract. Auth resolves this configuration through a module gateway, validates the request origin, and returns only a registered URI; raw origins are never concatenated into redirects. The active Angular 21 platform uses the server-returned OAuth values, including logout redirects, and sends `X-Client-Code` independently of API-key configuration. The legacy `frontend-libs` repository remains unchanged.

Replace port-based OAuth client selection with configuration stored against the resolved client application:

```text
clientCode
oauthClientId
allowedOrigins
allowedRedirectUris
allowedLogoutRedirectUris
```

Return only registered redirect URIs. Never construct a security-sensitive redirect by concatenating an unvalidated `Origin` header.

Update the Angular API client so `X-Client-Code` is sent whenever configured, independently of `X-API-Key`. The frontend should consume the server-resolved OAuth client and redirect configuration without overwriting it using `window.location.port`.

## 6. Error Contract

Use stable, non-enumerating public errors:

| Condition | HTTP status | Code |
|---|---:|---|
| Missing client context | 400 | `CLIENT_CONTEXT_REQUIRED` |
| Unknown, inactive, or origin-disallowed client | 403 | `CLIENT_CONTEXT_NOT_ALLOWED` |
| Tenant cannot be resolved safely | 403 | `TENANT_CONTEXT_NOT_ALLOWED` |
| No active policy | 403 | `AUTH_POLICY_NOT_CONFIGURED` |
| Multiple active policies detected | 500 | `AUTH_POLICY_INTEGRITY_ERROR` |
| Requested method differs from configured method | 403 | `LOGIN_METHOD_NOT_ALLOWED` |

Public responses should not reveal whether a specific tenant, username, or inactive client exists. Log the internal reason with trace ID and tenant/client identifiers where available.

## 7. Seeding and Administration

**Administration UI implemented on 2026-08-14.** The active System Angular 21 application now provides `/access-control/auth-policies`, reached from Client Applications, for tenant selection and tenant/client policy listing and editing. Its Auth-backed administration API enforces authenticated tenant scope, validates the active System client-to-tenant assignment through a module gateway, rejects `MFA` as a primary method, validates method/identifier compatibility, and records the authenticated actor. System route, UI-action, and API grants are provisioned by V61 and startup reconciliation. The legacy `frontend-libs` repository remains unchanged.

System user creation now includes an explicit active-tenant selector for authorized platform administrators. A cross-tenant target is accepted only for `ROLE_SYSTEM_ADMIN`, validated through a System-to-Auth tenant provisioning gateway, and receives its mandatory tenant-level `AuthUserScopeAssignment` in the same Auth transaction. Tenant administrators continue to create accounts only inside their effective tenant.

Replace global client-only seeding with tenant/client seeding. Seeder inputs must include a trusted tenant ID and must be idempotent on `(tenant_id, client_code)`.

Administration APIs must:

- require tenant-scoped authorization;
- validate that the client is assigned to the tenant;
- update the single policy atomically;
- reject `MFA` as a primary method;
- validate method/identifier compatibility;
- record authenticated `created_by` and `updated_by` actors;
- increment a policy/authorization version to invalidate relevant caches.

Recommended compatibility validation includes:

| Primary method | Allowed identifier examples |
|---|---|
| `PASSWORD` | `USERNAME`, `EMAIL`, `MOBILE`, `PERSON_ID` |
| `OTP` | `MOBILE`, optionally `EMAIL` if implemented |
| `MAGIC_LINK` | `EMAIL` |
| `SSO` | `USERNAME` or provider-subject abstraction |
| `PASSKEY` | `USERNAME` or discoverable credential mode |

Do not assume every enum combination is implemented merely because both values exist.

## 8. Cache and Transaction Requirements

If policies are cached, use at least `(tenantId, normalizedClientCode, policyVersion)` as the key. Never cache solely by client code.

Policy reads and authentication must use the Auth datasource and `authTransactionManager`. System client/tenant validation must pass through a module gateway; do not directly join or couple Auth repositories to System database entities.

Policy updates must evict the exact tenant/client cache entry. A policy change for one tenant must not affect another tenant using the same client code.

## 9. Verification Plan

### Unit tests

- resolves the configured method for the exact tenant/client pair;
- same client code resolves different policies for two tenants;
- same tenant resolves different policies for two clients;
- missing policy fails closed;
- duplicate active policies produce an integrity error, never first-row selection;
- inactive policy does not fall back to global properties;
- method/identifier incompatibility is rejected;
- `MFA` cannot be stored as a primary method;
- compatibility arrays contain exactly the canonical singular value.

### Repository and migration tests

- unique constraint rejects two active policies for one tenant/client;
- identical client codes are allowed for different tenants;
- case variants of a client code collide within the same tenant;
- audit and tenant columns are non-null after migration;
- unresolved legacy rows remain inaccessible and are reported;
- migration succeeds on a clean database and an upgraded database.

### Request-context integration tests

- authenticated policy lookup uses the principal/effective tenant and validated client from `AuthenticatedRequestContextFilter`;
- a caller cannot change the effective policy using a tenant header;
- a raw `X-Client-Code` cannot widen authenticated client context;
- public config rejects an origin not registered for the client;
- ambiguous multi-tenant public client resolution fails closed;
- public config and subsequent authentication resolve the same tenant/client policy;
- request context holders are cleared after success and failure.

### Frontend tests

- `X-Client-Code` is sent without an API key;
- exactly one login flow is rendered;
- the frontend does not select the first item from an ambiguous array;
- SSO uses server-returned OAuth client and validated redirect URI;
- policy/config errors show a generic safe failure state.

Run the complete backend suite after the narrow tests:

```bash
cd backend
mvn test
```

Build each Angular consumer changed during the migration.

## 10. Rollout Sequence

1. Add tenant-aware schema in compatibility mode.
2. Add trusted tenant/client resolution and reconciliation reporting.
3. Backfill only unambiguous legacy policies.
4. Repair unresolved and duplicate policies operationally.
5. Enforce database uniqueness and non-null ownership.
6. Deploy the singular backend resolver while emitting compatibility arrays.
7. Update pre-login config and every authentication enforcement endpoint.
8. Update Angular clients to consume singular policy fields and send client code correctly.
9. Remove runtime property fallback and `firstLoginMethodOnly()`.
10. Remove deprecated collection fields after all consumers migrate.

Do not enable the final uniqueness constraint or remove compatibility fields until reconciliation metrics show no unresolved records and all deployed clients support the singular contract.

## 11. Acceptance Criteria

The implementation is complete when:

- the database enforces one active primary authentication policy per tenant/client;
- tenant identity is never inferred from an untrusted caller header;
- authenticated resolution uses the context established by `AuthenticatedRequestContextFilter`;
- anonymous resolution validates active client, origin, and unambiguous tenant assignment;
- config and authentication enforcement use the same canonical policy resolver;
- missing and ambiguous policies fail closed;
- `MFA` is modeled separately from the primary login method;
- no service silently selects the first of multiple methods or rows;
- caches and repository predicates include tenant and client;
- frontend applications display only the configured primary login method;
- migration, unit, integration, and frontend verification pass.
