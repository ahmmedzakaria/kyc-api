# Auth Privilege System

The privilege system controls access to modules, menus, features, and actions through a fixed-width privilege code. It lives in the Auth module because privileges are part of authentication and authorization, not part of any single business module.

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
moduleCode(2) + featureTypeCode(2) + featureCode(3) + actionCode(2)
```

Example:

```text
010200101
```

Segment breakdown:

```text
01 02 001 01
|  |  |   |
|  |  |   action code
|  |  feature/object code
|  feature type code
module code
```

Meaning:

| Segment | Length | Value | Meaning |
| --- | ---: | --- | --- |
| Module | 2 | `01` | KYC module |
| Feature type | 2 | `02` | Operations |
| Feature/object | 3 | `001` | Person |
| Action | 2 | `01` | Create |

So `010200101` means:

```text
KYC -> Operations -> Person -> Create
```

## Current Code Standards

### Modules

| Module | Code |
| --- | --- |
| KYC | `01` |
| Auth | `02` |
| GIS | `03` |
| Services | `04` |

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

Feature codes are module-owned, three-digit values.

Example:

| Module | Feature Type | Feature | Code |
| --- | --- | --- | --- |
| KYC | Operations | Person | `001` |

New feature codes should be assigned deliberately and should not be reused for a different feature inside the same module/type.

## Backend Model

The privilege system uses these core tables:

| Table | Purpose |
| --- | --- |
| `privileges` | Catalog of all available privileges |
| `role_privileges` | Privileges assigned to roles |
| `user_privileges` | Direct privileges assigned to users |
| `roles` | Existing role table |
| `users` | Existing user table |

Effective user access is calculated as:

```text
role privileges + direct user privileges
```

This means a user can receive access through a role, and exceptional access can also be assigned directly to the user.

## Backend Classes

Main implementation files:

| File | Responsibility |
| --- | --- |
| `Privilege.java` | Privilege catalog entity |
| `Role.java` | Role to privilege mapping |
| `User.java` | User to privilege mapping |
| `PrivilegeRepository.java` | Privilege persistence |
| `PrivilegeService.java` | Service contract |
| `PrivilegeServiceImpl.java` | Code generation, assignment, and check logic |
| `PrivilegeController.java` | HTTP APIs |
| `DataSeeder.java` | Default admin privilege seed |

Enums:

| Enum | Purpose |
| --- | --- |
| `ApplicationModule` | Module code registry |
| `FeatureType` | Setup, Operations, Reports |
| `PrivilegeAction` | Action code registry |

## API Contract

All APIs use `POST`.

### Save Privilege

Endpoint:

```text
POST /auth/privilege/save
```

Request:

```json
{
  "moduleCode": "01",
  "moduleName": "KYC",
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
010200101
```

If the code already exists, the catalog row is updated.

### List Privileges

Endpoint:

```text
POST /auth/privilege/list
```

Returns all privilege catalog records.

### Check Privilege

Endpoint:

```text
POST /auth/privilege/check
```

Option 1, check by full code:

```json
{
  "username": "admin",
  "privilegeCode": "010200101"
}
```

Option 2, check by code parts:

```json
{
  "username": "admin",
  "moduleCode": "01",
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
  "privilegeCode": "010200101",
  "allowed": true
}
```

### Current User Privileges

Endpoint:

```text
POST /auth/privilege/my-codes
```

Returns the effective privilege codes for the logged-in user.

### Assign Privileges To Role

Endpoint:

```text
POST /auth/privilege/assign-role
```

Request:

```json
{
  "roleId": 1,
  "privilegeCodes": [
    "010200101",
    "010200102",
    "010200106"
  ]
}
```

This replaces the role's privilege set.

### Assign Privileges To User

Endpoint:

```text
POST /auth/privilege/assign-user
```

Request:

```json
{
  "userId": 1,
  "privilegeCodes": [
    "010200101"
  ]
}
```

This replaces the user's direct privilege set.

## Login Response

Login and refresh responses include `privilegeCodes`.

Example:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "privilegeCodes": [
    "010200101",
    "010200102",
    "010200106"
  ]
}
```

The frontend can use these codes to show or hide modules, menus, buttons, and page actions.

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

Example UI checks:

| UI Element | Required Code |
| --- | --- |
| Person menu | `010200106` or `010200108` |
| Person create button | `010200101` |
| Person edit button | `010200102` |
| Person delete button | `010200103` |

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

## Important Rules

- Keep privilege codes fixed after they are used in production.
- Do not reuse a feature code for a different feature.
- Keep feature names readable because they are shown in the privilege management UI.
- Prefer role assignment over direct user assignment.
- Use direct user assignment only for exceptional access.
- Backend APIs should still validate privileges for sensitive operations; frontend checks are for UI control only.

## Default Seed

The backend currently seeds KYC Person Operations privileges for `ROLE_ADMIN`.

Examples:

| Code | Meaning |
| --- | --- |
| `010200101` | KYC Operations Person Create |
| `010200102` | KYC Operations Person Update |
| `010200103` | KYC Operations Person Delete |
| `010200104` | KYC Operations Person Reject |
| `010200105` | KYC Operations Person Send Back |
| `010200106` | KYC Operations Person View |
| `010200108` | KYC Operations Person Search |
