# System Privilege Service

The privilege system controls access to modules, menus, features, and actions through a fixed-width privilege code. Privilege contracts live in `systemmodule`, while auth integrates them with users, roles, and authorization checks.

## Concept

The application can contain many modules. Each module can expose features under three major groups:

| Group | Code | Purpose |
| --- | --- | --- |
| Setup | `01` | Configuration and master-data setup screens |
| Operations | `02` | Business data entry and workflow execution |
| Reports | `03` | Reports, dashboards, exports, and read-only analysis |

`Operations` is the recommended professional name for the old `Data Create` group. It covers create, update, delete, approve, reject, send back, and similar workflow actions.

Each feature is treated as an object of a module. Actions are permissions that can be performed on that feature.

Example:

```text
KYC module -> Operations -> Person feature -> Create action
```

## Privilege Code Format

Privilege codes use this structure:

```text
moduleCode(2) + submoduleCode(2) + featureTypeCode(2) + featureCode(3) + actionCode(2)
```

Example:

```text
01010200101
```

Segment breakdown:

```text
01 01 02 001 01
|  |  |  |   |
|  |  |  |   action code
|  |  |  feature/object code
|  |  feature type code
|  submodule code
module code
```

Meaning:

| Segment | Length | Value | Meaning |
| --- | ---: | --- | --- |
| Module | 2 | `01` | KYC module |
| Submodule | 2 | `01` | Person submodule |
| Feature type | 2 | `02` | Operations |
| Feature/object | 3 | `001` | Person |
| Action | 2 | `01` | Create |

So `01010200101` means:

```text
KYC -> Person -> Operations -> Person -> Create
```

## Current Code Standards

### Modules

| Module | Code |
| --- | --- |
| KYC | `01` |
| Auth | `02` |
| GIS | `03` |
| Services | `04` |

### Submodules

Submodule codes are two-digit values scoped by module.

| Module | Submodule | Code |
| --- | --- | --- |
| KYC | Person | `01` |
| Auth | Privilege | `01` |

### Feature Types

| Feature Type | Code |
| --- | --- |
| Setup | `01` |
| Operations | `02` |
| Reports | `03` |

### Actions

| Action | Code |
| --- | --- |
| Create | `01` |
| Update | `02` |
| Delete | `03` |
| Reject | `04` |
| Send Back | `05` |
| View | `06` |
| Approve | `07` |
| Search | `08` |

### Feature Codes

Feature codes are module/submodule-owned, three-digit values.

Example:

| Module | Submodule | Feature Type | Feature | Code |
| --- | --- | --- | --- | --- |
| KYC | Person | Operations | Person | `001` |

New feature codes should be assigned deliberately and should not be reused for a different feature inside the same module/submodule/type.

## Backend Model

The privilege system uses these core tables:

| Table | Purpose |
| --- | --- |
| `system_db.sys_modules` | Module catalog with module `id`, `code`, and `name` |
| `system_db.sys_submodules` | Submodule catalog linked to `sys_modules` |
| `system_db.sys_features` | Feature catalog linked to `sys_submodules`; owns feature type and feature `code`/`name` |
| `system_db.sys_privileges` | Action-level privilege catalog linked to `sys_features` by `feature_id` |
| `system_db.sys_sub_menus` | Menu entries exposed by privilege providers, linked to `sys_features` by `feature_id` |
| `system_db.sys_role_privileges` | Privileges assigned to auth role IDs |
| `system_db.sys_user_privileges` | Direct privileges assigned to auth user IDs |
| `auth_roles` | Existing role table |
| `auth_users` | Existing user table |

Module, submodule, and feature code/name values should not be duplicated in `sys_privileges`. The privilege code remains stable, but the descriptive catalog is normalized:

```text
sys_modules -> sys_submodules -> sys_features -> sys_privileges
```

Effective user access is calculated as:

```text
role privileges + direct user privileges
```

This means a user can receive access through a role, and exceptional access can also be assigned directly to the user.

## Backend Classes

Main implementation files:

| File | Responsibility |
| --- | --- |
| `SysModule.java` | Module catalog entity |
| `SysSubmodule.java` | Submodule catalog entity |
| `SysFeature.java` | Feature catalog entity |
| `SysPrivilege.java` | Action-level privilege catalog entity |
| `Role.java` | Role to privilege mapping |
| `User.java` | User to privilege mapping |
| `systemmodule.privilege.repository.PrivilegeRepository` | System-owned privilege persistence |
| `systemmodule.privilege.service.interfaces.PrivilegeService` | System privilege API/admin service contract |
| `systemmodule.privilege.service.implementations.PrivilegeServiceImpl` | Code generation, assignment, and check logic |
| `gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway` | Loose-coupled synchronous privilege decision gateway contract |
| `gatewaymodule.auth.service.interfaces.AuthModuleGateway` | Loose-coupled auth user/role lookup gateway contract |
| `systemmodule.privilege.api.SystemPrivilegeModuleGateway` | System-owned gateway implementation for synchronous privilege decisions |
| `authmodule.api.AuthModuleGatewayImpl` | Auth-owned gateway implementation for user and role lookups |
| `ModulePrivilegeProvider.java` | Interface implemented by business modules |
| `systemmodule.privilege.controller.PrivilegeController` | System-owned privilege HTTP APIs |
| `DataSeeder.java` | Seeds provider-owned privileges for admin |
| `KycPrivilegeProvider.java` | KYC module implementation of the provider interface |

Enums:

| Enum | Purpose |
| --- | --- |
| `ApplicationModule` | Module code registry |
| `ApplicationSubmodule` | Submodule code registry |
| `FeatureType` | Setup, Operations, Reports |
| `PrivilegeAction` | Action code registry |

System privilege enums define the code registry. `systemmodule` owns the provider interface, and each business module owns the implementation for its own features, actions, menus, and submenus.

## Module Provider Contract

Auth does not hardcode module features. Instead, it asks every module for its privilege metadata through this interface:

```java
public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
```

Each module implementation provides:

- module code and name
- submodule code and name
- feature type code and name
- feature/object code and name
- supported action codes
- menu and submenu structure
- privilege codes required for each menu item

Example owner:

```text
KYC module owns Person, KYC Record, and KYC Report feature definitions.
```

This keeps inter-module communication interface-based and loosely coupled. Auth only depends on the interface and DTOs, not on KYC internals.

## API Contract

All APIs use `POST`.

### Save Privilege

Endpoint:

```text
POST /system/privilege/save
```

Request:

```json
{
  "moduleCode": "01",
  "moduleName": "KYC",
  "submoduleCode": "01",
  "submoduleName": "Person",
  "featureTypeCode": "02",
  "featureTypeName": "Operations",
  "featureCode": "001",
  "featureName": "Person",
  "actionCode": "01",
  "actionName": "Create",
  "active": true
}
```

The backend generates:

```text
01010200101
```

If the code already exists, the catalog row is updated.

### List Privileges

Endpoint:

```text
POST /system/privilege/list
```

Returns all privilege catalog records.

### List Module Definitions

Endpoint:

```text
POST /system/privilege/definitions
```

Returns all feature/action/menu definitions provided by application modules.

This is used by the privilege management frontend to understand what modules and features exist.

### Sidebar Menu

Endpoint:

```text
POST /system/privilege/sidebar-menu
```

Returns the authenticated user's sidebar menu.

The backend groups all module-provided menu items under:

```text
Setup
Operations
Reports
```

The frontend should render this response directly instead of hardcoding menus.

### Check Privilege

Endpoint:

```text
POST /system/privilege/check
```

Option 1, check by full code:

```json
{
  "username": "admin",
  "privilegeCode": "01010200101"
}
```

Option 2, check by code parts:

```json
{
  "username": "admin",
  "moduleCode": "01",
  "submoduleCode": "01",
  "featureTypeCode": "02",
  "featureCode": "001",
  "actionCode": "01"
}
```

If `username` is omitted, the backend checks the currently authenticated user.

Response data:

```json
{
  "username": "admin",
  "privilegeCode": "01010200101",
  "allowed": true
}
```

### Current User Privileges

Endpoint:

```text
POST /system/privilege/my-codes
```

Returns the effective privilege codes for the logged-in user.

### Assign Privileges To Role

Endpoint:

```text
POST /system/privilege/assign-role
```

Request:

```json
{
  "roleId": 1,
  "privilegeCodes": [
    "01010200101",
    "01010200102",
    "01010200106"
  ]
}
```

This replaces the role's privilege set.

### Assign Privileges To User

Endpoint:

```text
POST /system/privilege/assign-user
```

Request:

```json
{
  "userId": 1,
  "privilegeCodes": [
    "01010200101"
  ]
}
```

This replaces the user's direct privilege set.

## Login Response

Login and refresh responses include only tokens. After authentication, the frontend calls the application context API to load menus, sub menus, privilege codes, and future setup data.

Example:

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

## Application Context

```http
POST /system/privilege/context
```

Returns the authenticated user's dynamic sidebar menu and effective privilege codes.

## Frontend Usage

The separate privilege management frontend is located at:

```text
privilege-frontend
```

It provides:

- privilege catalog create/update
- generated code preview
- privilege list and search
- role/user assignment
- privilege check

The main page is:

```text
/privileges
```

Frontend code should treat privilege codes as the source of truth for access decisions.

The KYC frontend sidebar is generated dynamically from:

```text
POST /system/privilege/sidebar-menu
```

The sidebar does not own menu definitions. Modules provide menu metadata to Auth, Auth filters the menu by the logged-in user's effective privileges, and the frontend renders the returned tree.

Example UI checks:

| UI Element | Required Code |
| --- | --- |
| Person menu | `01010200106` or `01010200108` |
| Person create button | `01010200101` |
| Person edit button | `01010200102` |
| Person delete button | `01010200103` |

## Recommended Access Pattern

Use role privileges for normal access groups.

Example:

```text
ROLE_ADMIN -> all privileges
ROLE_KYC_OPERATOR -> Person view, search, create, update
ROLE_KYC_APPROVER -> Person view, search, approve, reject, send back
```

Use direct user privileges only for exceptions.

Example:

```text
User A has ROLE_KYC_OPERATOR
User A also gets direct Person Delete access for a temporary task
```

## Seeded Roles

The backend creates these roles during startup:

| Role | Purpose |
| --- | --- |
| `ROLE_ADMIN` | Full access to all module-provided privileges |
| `ROLE_KYC_OPERATOR` | Create, update, view, and search KYC operational data |
| `ROLE_KYC_APPROVER` | Review-oriented access such as view, search, reject, and send back |
| `ROLE_REPORT_VIEWER` | Report view and search access only |

The non-admin roles intentionally receive different privilege sets so access can be tested without giving every user full admin access.

## Important Rules

- Each module must own its own feature, action, menu, and submenu definitions.
- System privilege enums must stay in `systemmodule.privilege`.
- Auth should communicate with modules only through `ModulePrivilegeProvider`.
- The frontend should render menus from backend data, not hardcode module menus.
- Keep privilege codes fixed after they are used in production.
- Do not reuse a feature code for a different feature.
- Keep feature names readable because they are shown in the privilege management UI.
- Prefer role assignment over direct user assignment.
- Use direct user assignment only for exceptional access.
- Backend APIs should still validate privileges for sensitive operations; frontend checks are for UI control only.

## Default Seed

The backend currently seeds all module-provided privileges for `ROLE_ADMIN`.

Examples:

| Code | Meaning |
| --- | --- |
| `01010200101` | KYC Person Operations Person Create |
| `01010200102` | KYC Person Operations Person Update |
| `01010200103` | KYC Person Operations Person Delete |
| `01010200104` | KYC Person Operations Person Reject |
| `01010200105` | KYC Person Operations Person Send Back |
| `01010200106` | KYC Person Operations Person View |
| `01010200108` | KYC Person Operations Person Search |
