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

This model is appropriate, but it activates only when the request resolves to an active `SysApiRegistry` entry.

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

`SysClientApplication` stores:

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
