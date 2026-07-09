# API Client Access Control Implementation Plan

## Purpose

Every frontend application, such as Web, Mobile, POS, ERP, or Partner Portal, should receive a system-managed API key or client credential. That client identity will be granted access to specific modules, submodules, features, and APIs.

This adds an application-level access layer on top of the existing user privilege system. The final authorization model should verify both:

- The frontend client application is allowed to call the API and use the feature.
- The authenticated user is allowed to perform the action.

## Business Objectives

- Secure backend APIs from unauthorized frontend or partner access.
- Allow multiple frontend applications to consume the same backend safely.
- Enable module, submodule, feature, and API-level licensing.
- Support SaaS and multi-tenant deployment.
- Manage frontend/client permissions without backend code deployment.
- Keep frontend menus and feature visibility controlled by backend configuration.

## Existing Project Context

The backend already has a strong base for this system:

- Privileges are registered in `systemmodule/privilege`.
- `SysPrivilege` already models module, submodule, feature type, feature, and action codes.
- `SysSubMenu` already maps privileges to sidebar menu entries.
- `PrivilegeServiceImpl#getApplicationContext` returns sidebar menus and privilege codes.
- The frontend layout library loads this context from `system/privilege/context` in `frontend-libs/layout/src/lib/sidebar-menu.service.ts`.
- Sidebar menu ordering should be controlled by backend configuration so each client application can receive the same allowed features in a predictable order.

The new client access layer should extend this existing privilege model instead of creating a separate permission concept.

## Target Access Model

Each protected request should be evaluated in this order:

1. If the API is explicitly public, allow it without client validation.
2. Validate the client application using `X-Client-Code` and `X-API-Key`, or a future client credential token.
3. Reject disabled, expired, unknown, or origin/IP-blocked clients.
4. Resolve the requested API from the API registry.
5. Check whether the client application is allowed to call that API.
6. Check whether the required feature is enabled for that client.
7. Validate the user JWT for APIs that require a user session.
8. Check whether the authenticated user has the required privilege.
9. Apply tenant, business, and branch filtering before returning data.

Client access and user access must stay separate:

- Client access controls application licensing and API consumption.
- User access controls authenticated user authorization.

## Proposed Module Structure

Add a new package under `systemmodule`:

```text
com.nexacore.systemmodule.clientaccess
├── controller
├── dto
├── entity
├── enums
├── repository
├── security
└── service
    ├── interfaces
    └── implementations
```

This belongs in `systemmodule` because it is platform configuration and access governance. It should not be implemented separately inside KYC, POS, Auth, GIS, or other business modules.

## Database Design

Use the `sys_` table prefix because these tables belong to system-level platform configuration.

All persistent tables must include:

- `created_by`
- `updated_by`
- `created_at`
- `updated_at`

### `sys_client_applications`

Stores each registered frontend or integration application.

```text
id
client_code
client_name
client_type
status
allowed_origins
allowed_ips
rate_limit_per_minute
description
created_by
updated_by
created_at
updated_at
```

Suggested `client_type` values:

```text
WEB
MOBILE
POS
ERP
PARTNER_PORTAL
INTERNAL_SERVICE
```

Suggested `status` values:

```text
ACTIVE
DISABLED
SUSPENDED
EXPIRED
```

### `sys_client_credentials`

Stores hashed API keys or client secrets.

```text
id
client_application_id
client_id
api_key_hash
client_secret_hash
expires_at
last_used_at
active
menu_order
sub_menu_order
created_by
updated_by
created_at
updated_at
```

Never store raw API keys, raw client secrets, or generated credentials in logs.

### `sys_api_registry`

Stores backend APIs that can be protected and licensed.

```text
id
api_code
http_method
path_pattern
module_code
module_name
submodule_code
submodule_name
feature_type_code
feature_type_name
feature_code
feature_name
action_code
action_name
required_privilege_code
public_api
active
created_by
updated_by
created_at
updated_at
```

Example:

```text
POST /kyc/person/save -> KYC / Person / Operations / Person / Create
POST /kyc/person/list -> KYC / Person / Operations / Person / View
POST /system/privilege/context -> System / Privilege / Setup / Context / View
```

### `sys_client_api_permissions`

Maps client applications to APIs.

```text
id
client_application_id
api_registry_id
active
created_by
updated_by
created_at
updated_at
```

### `sys_client_feature_permissions`

Maps client applications to existing system privileges.

```text
id
client_application_id
privilege_id
active
created_by
updated_by
created_at
updated_at
```

This reuses existing privilege codes instead of duplicating feature definitions.

### Existing `sys_sub_menus` Ordering Update

Extend the existing `sys_sub_menus` table with ordering fields:

```text
menu_order
sub_menu_order
```

Use `menu_order` to sort the main sidebar group, such as Setup, Operations, or Report. Use `sub_menu_order` to sort child menu entries inside that group.

Recommended defaults:

```text
menu_order = 0
sub_menu_order = 0
```

When multiple rows have the same order value, use a stable fallback sort:

```text
menu_order
sub_menu_order
module_code
submodule_code
feature_type_code
feature_code
name
```

If different frontend applications need different menu ordering later, add a client-specific override table instead of changing the global menu order:

```text
sys_client_menu_order
- id
- client_application_id
- sub_menu_id
- menu_order
- sub_menu_order
- active
- created_by
- updated_by
- created_at
- updated_at
```

### `sys_client_application_tenants`

Maps client applications to SaaS tenants, businesses, or organizations.

```text
id
client_application_id
tenant_id
business_id
active
created_by
updated_by
created_at
updated_at
```

Use this table to ensure that a frontend or partner client cannot access tenants or businesses outside its configuration.

## Backend Components

### Entities

Add entities matching the new system tables:

- `SysClientApplication`
- `SysClientCredential`
- `SysApiRegistry`
- `SysClientApiPermission`
- `SysClientFeaturePermission`
- `SysClientApplicationTenant`

All entities should use explicit `@Table(name = "...")` mappings.

Update the existing `SysSubMenu` entity to include:

```text
menuOrder
subMenuOrder
```

Both fields should be nullable-safe in service sorting, using `0` as the default when the database value is null.

### Repositories

Add repositories under:

```text
com.nexacore.systemmodule.clientaccess.repository
```

Required lookup methods:

- Find active client application by `clientCode`.
- Find active credential by client application.
- Find active API registry entry by HTTP method and path pattern.
- Find client API permissions.
- Find client feature permissions by privilege code.
- Find tenant/business mappings for a client.

### Services

Add service interfaces:

```text
ClientApplicationService
ClientCredentialService
ClientApiRegistryService
ClientAccessDecisionService
ClientApplicationContextService
```

Responsibilities:

- Create and update client applications.
- Generate and rotate API keys.
- Hash and validate API keys.
- Assign API permissions.
- Assign feature permissions.
- Resolve requested APIs.
- Decide whether a client can access a requested API.
- Filter available menus and privileges by client permissions.
- Sort main sidebar menus by `menuOrder`.
- Sort child sidebar menus by `subMenuOrder`.

## Request Headers

Each frontend application should send:

```http
X-Client-Code: WEB
X-API-Key: generated-api-key
Authorization: Bearer user-jwt
Accept-Language: en
```

For public endpoints, `Authorization` may be absent. For protected user APIs, both client credentials and user JWT should be required.

## Security Filters

### `ClientApplicationAuthenticationFilter`

Add this filter to validate the frontend application identity.

Responsibilities:

- Read `X-Client-Code`.
- Read `X-API-Key`.
- Validate the API key against the stored hash.
- Check client status.
- Check expiration.
- Check allowed origins and allowed IPs when configured.
- Store the resolved client application in request context.
- Reject invalid clients with `401` or `403`.

### `ClientApiAccessFilter`

Add this filter after client authentication and before controller execution.

Responsibilities:

- Resolve request method and path.
- Find the matching active API registry entry.
- Skip checks for APIs marked `public_api = true`.
- Verify that the client has API permission.
- Verify that the client has the required feature permission.
- Reject missing permissions with `403`.

### User JWT Filter

Keep the existing `JwtAuthenticationFilter` for user authentication.

The final protected flow should be:

```text
ClientApplicationAuthenticationFilter
ClientApiAccessFilter
JwtAuthenticationFilter
Controller
```

The exact filter order can be adjusted during implementation, but client access must be enforced before business logic executes.

## API Protection Annotation

Add a new annotation:

```java
@ClientSecuredApi(
        moduleCode = "01",
        moduleName = "KYC",
        submoduleCode = "01",
        submoduleName = "Person",
        featureTypeCode = "02",
        featureTypeName = "Operations",
        featureCode = "001",
        featureName = "Person",
        actionCode = "01",
        actionName = "Create"
)
@PostMapping("/save")
public ResponseEntity<ApiResponse<PersonDto>> savePerson(@RequestBody PersonDto requestDto) {
    return ResponseEntity.ok(ApiResponse.success(personService.save(requestDto), "Person saved"));
}
```

Use this annotation on controller methods to describe the API-to-feature mapping.

This annotation can later be used by an API registry sync service to populate or update `sys_api_registry`.

## Admin APIs

Add secured admin endpoints under:

```text
/system/client-app
/system/api-registry
```

Suggested APIs:

```text
POST /system/client-app/save
POST /system/client-app/list
POST /system/client-app/detail
POST /system/client-app/disable
POST /system/client-app/enable
POST /system/client-app/rotate-api-key
POST /system/client-app/assign-api-permissions
POST /system/client-app/assign-feature-permissions
POST /system/client-app/assign-tenants
POST /system/api-registry/list
POST /system/api-registry/save
POST /system/api-registry/sync
```

These APIs should use explicit request and response DTOs. Do not return entities directly.

## Application Context and Sidebar Menu

The current frontend service loads application context from:

```text
system/privilege/context
```

The backend response should be extended from:

```json
{
  "menus": [],
  "privilegeCodes": []
}
```

to:

```json
{
  "clientCode": "WEB",
  "clientType": "WEB",
  "menus": [],
  "privilegeCodes": [],
  "enabledModules": [],
  "enabledSubmodules": [],
  "enabledFeatures": []
}
```

Each menu item should include ordering metadata when useful for debugging or client-side fallback sorting:

```json
{
  "label": "Operations",
  "icon": "fa fa-briefcase",
  "menuOrder": 20,
  "children": [
    {
      "label": "Person",
      "path": "/kyc/person",
      "subMenuOrder": 10,
      "privilegeCodes": []
    }
  ]
}
```

The menu builder should filter sidebar entries using both:

- User role/user privileges.
- Client application feature permissions.

Example:

```text
User has KYC Person Create privilege.
Client application is POS and does not have KYC feature permission.
Result: KYC Person menu is not returned.
```

This keeps the frontend simple. It renders only the menus and feature flags returned by the backend.

The backend should return menus already sorted. The frontend sidebar should render the returned order directly and only use `menuOrder` or `subMenuOrder` as a fallback if it needs to re-sort cached menu data.

## Frontend Integration

Update the shared API layer used by frontend applications so every request includes:

```text
X-Client-Code
X-API-Key
Authorization, when logged in
Accept-Language
```

Each frontend application should keep its client configuration in environment-specific configuration:

```typescript
export const environment = {
    clientCode: 'WEB',
    nexacoreApiKey: 'replace-with-environment-api-key'
};
```

The layout/sidebar library should continue using `system/privilege/context`, but should be updated to store the extended context:

```text
clientCode
clientType
privilegeCodes
enabledModules
enabledSubmodules
enabledFeatures
sidebarMenus
menuOrder
subMenuOrder
```

Frontend route guards should use the returned context for UI behavior, but backend APIs must still enforce the same access rules.

## SaaS and Multi-Tenant Enforcement

Client application access must be checked against tenant or business mappings.

For tenant-aware requests:

1. Resolve client application.
2. Resolve authenticated user.
3. Resolve requested tenant, business, or branch.
4. Confirm the client is allowed for that tenant/business.
5. Confirm the user is allowed for that tenant/business/branch.
6. Apply repository-level filtering.

This prevents one frontend or partner application from accessing another tenant's data.

## Logging and Audit

Access decisions should be logged through `logmodule`.

Log fields:

```text
client_code
api_code
http_method
path
username
business_id
decision
deny_reason
timestamp
```

Do not log:

```text
raw API key
raw client secret
Authorization header
OTP
password
token
card data
full PII payload
Firebase credential
```

## Migration Plan

Add Flyway migrations under the existing datasource-specific migration structure:

```text
src/main/resources/db/migration/system
```

Suggested migration order:

1. Create `sys_client_applications`.
2. Create `sys_client_credentials`.
3. Create `sys_api_registry`.
4. Create `sys_client_api_permissions`.
5. Create `sys_client_feature_permissions`.
6. Create `sys_client_application_tenants`.
7. Add `menu_order` and `sub_menu_order` to `sys_sub_menus`.
8. Seed default menu and submenu order values for existing menus.
9. Seed initial Web client application.
10. Seed required public/system APIs.
11. Seed initial permissions for existing frontend applications.

Use a clear system actor value for seed data.

## Testing Plan

Add unit tests under matching packages in:

```text
src/test/java
```

Required tests:

- API key validation succeeds for active client.
- API key validation fails for missing key.
- API key validation fails for wrong key.
- Disabled client is rejected.
- Expired credential is rejected.
- Client without API permission receives `403`.
- Client without feature permission receives `403`.
- Public API bypasses client permission checks only when explicitly marked public.
- Sidebar context excludes features not licensed for the client.
- Sidebar context still applies user privileges.
- Sidebar context sorts main menus by `menuOrder`.
- Sidebar context sorts child menus by `subMenuOrder`.
- Sidebar context uses stable fallback sorting when order values are equal.
- Tenant/business access is rejected when the client is not mapped.
- Sensitive credential values are never logged or returned.

Run:

```bash
mvn test
```

## Implementation Phases

### Phase 1: Data Model and Admin Configuration

- Add Flyway migration scripts.
- Add menu and submenu ordering columns to `sys_sub_menus`.
- Add entities, repositories, DTOs, and service interfaces.
- Add basic admin APIs for client applications.
- Add API key generation and rotation.

### Phase 2: Client Authentication

- Add API key hashing and validation.
- Add `ClientApplicationAuthenticationFilter`.
- Add request-scoped client context.
- Add tests for valid, invalid, disabled, and expired clients.

### Phase 3: API Registry and Enforcement

- Add `sys_api_registry`.
- Add `@ClientSecuredApi`.
- Add manual API registry management.
- Add `ClientApiAccessFilter`.
- Add tests for API permission decisions.

### Phase 4: Feature Licensing

- Map client applications to existing `SysPrivilege` records.
- Filter sidebar menus by client feature permissions.
- Sort sidebar menu groups and child menu entries from backend configuration.
- Extend application context response.
- Update the frontend API layer and sidebar context storage.

### Phase 5: SaaS Controls

- Add tenant/business mapping.
- Enforce client tenant/business access.
- Connect checks to business and branch filtering.

### Phase 6: Audit, Hardening, and Operations

- Add access decision logging through `logmodule`.
- Add rate limit support if required.
- Add API key rotation workflow.
- Add admin screens for client and permission management.
- Document operational procedures for onboarding and disabling clients.

## Final Result

After implementation, the platform will support runtime-managed access like:

```text
WEB client:
- KYC module enabled
- Auth module enabled
- POS module disabled

MOBILE client:
- KYC view features enabled
- KYC create/update disabled
- Reports disabled

POS client:
- POS module enabled
- KYC module disabled
- Auth login/context APIs enabled
```

This provides scalable frontend application access, feature-level licensing, SaaS tenant control, and backend-enforced API security without requiring code changes for each permission update.
