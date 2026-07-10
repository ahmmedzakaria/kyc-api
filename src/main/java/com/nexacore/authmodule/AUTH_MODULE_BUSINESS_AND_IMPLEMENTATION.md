# Auth Module Business And Implementation Plan

## Purpose

The Auth module owns application accounts, credentials, roles, sessions, JWT issuance, local login, SSO token bridge, and authorization integration.

The module does not own person profile/contact data. Those fields belong to the KYC module.

## Core Domain Rules

- Every `AuthUser` must reference a `KycPerson`.
- Not every `KycPerson` is an `AuthUser`.
- A person becomes an application user only after approval/business verification.
- `auth_users.person_id` links an application user to `kyc_person.id`.
- `auth_users.person_id` is an application-level cross-database reference, not a physical foreign key.
- Email, email verification, mobile number, and mobile verification are not stored in `auth_users`.
- Auth owns username verification/generation for application users.

## Business Responsibilities

Auth owns:

- Local username/password authentication.
- Keycloak SSO token bridge.
- Backend JWT and refresh token issuance.
- Role assignment.
- Auth user persistence.
- Password hash storage.
- Enabled/disabled account state.
- Login/logout session tracking.
- Mapping a verified person to an application user.
- Publishing auth and privilege access information to other modules through gateway interfaces.

Auth does not own:

- Person first name or last name.
- Person email or mobile number.
- Person contact verification state.
- KYC documents.
- Person profile photo.
- KYC approval business data.

## Tables

### `auth_users`

Application account table.

Important columns:

```text
id
username
person_id
password
enabled
external_provider
external_subject
last_login_at
created_by
updated_by
created_at
updated_at
```

Rules:

- `username` is unique.
- `person_id` is unique when present.
- `person_id` points to `kyc_db.kyc_person.id`.
- There is no physical cross-database foreign key.
- `email`, `email_verified`, `mobile_number`, and `mobile_verified` must not be stored in `auth_users`.

### `auth_roles`

Role table.

Example roles:

```text
ROLE_ADMIN
ROLE_KYC_OPERATOR
ROLE_KYC_APPROVER
ROLE_REPORT_VIEWER
```

### `auth_user_roles`

Join table between users and roles.

Rules:

- Table is owned by Auth.
- Join columns are `user_id` and `role_id`.

## Current Implementation

Current package:

```text
com.nexacore.authmodule
├── api
├── core
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
├── security
├── sso
└── startup
```

Important classes:

- `AuthUser`
- `AuthRole`
- `UserRepository`
- `RoleRepository`
- `AuthServiceImpl`
- `KeycloakSsoService`
- `SsoAuthService`
- `JwtUtil`
- `JwtAuthenticationFilter`
- `MyUserDetailsService`
- `DataSeeder`
- `AuthModuleGatewayImpl`

## Local Login Flow

1. Frontend calls `/auth/authenticate`.
2. `AuthServiceImpl` authenticates through Spring Security.
3. `MyUserDetailsService` loads the `AuthUser` by username.
4. Password is validated with the configured `PasswordEncoder`.
5. Auth issues backend JWT and refresh token.
6. Frontend uses backend JWT for protected APIs.

Local login should be enabled only when configured authentication mode permits it.

## SSO Login Flow

Recommended SSO flow:

1. Frontend starts Keycloak Authorization Code + PKCE.
2. Keycloak authenticates the user.
3. Frontend receives Keycloak access token.
4. Frontend sends token to backend `/auth/sso/authenticate`.
5. `KeycloakSsoService` validates issuer, audience/client, expiry, and signature.
6. Auth resolves the linked `KycPerson`.
7. Auth creates or updates `AuthUser`.
8. Auth issues the existing backend JWT and refresh token.
9. Existing backend authorization continues unchanged.

## Person-To-User Promotion Flow

Auth owns username assignment.

Flow:

1. Business process approves a `KycPerson` to become a user.
2. Auth normalizes or generates a username.
3. Auth checks username uniqueness in `auth_users`.
4. Auth also checks person username availability through `PersonModuleGateway`.
5. Auth calls `PersonModuleGateway.promotePersonToUser(...)`.
6. KYC stores username and sets `kyc_person.is_user = true`.
7. Auth creates or updates `auth_users`.
8. Auth assigns roles in `auth_user_roles`.

## Username Rules

Recommended username rules:

- Username is required for every `AuthUser`.
- Username must be unique in `auth_users`.
- Username must be unique on `kyc_person` when present.
- Username normalization should happen in Auth.
- KYC should not generate usernames during ordinary person creation.
- If a requested username is already taken, Auth may append a numeric suffix.

## Keycloak User Storage SPI Integration

The Keycloak SPI reads:

```text
auth_db.auth_users
auth_db.auth_roles
auth_db.auth_user_roles
kyc_db.kyc_person
```

Auth-owned values from `auth_users`:

```text
id
person_id
username
password
enabled
roles
```

KYC-owned values from `kyc_person`:

```text
email
email_verified
mobile_number
mobile_verified
first_name
last_name
```

Keycloak attributes exposed by SPI:

```text
nexacore_user_id
nexacore_person_id
nexacore_mobile_number
nexacore_mobile_verified
nexacore_roles
```

Backend SSO should prefer `nexacore_person_id` when mapping a Keycloak login back to a local application user.

## Data Source

Auth uses:

```properties
spring.datasource.auth.jdbc-url=jdbc:postgresql://localhost:5433/auth_db
```

Flyway migrations are under:

```text
src/main/resources/db/migration/auth
```

## Migration Notes

Current planned schema changes include:

- Add `auth_users.person_id`.
- Add a unique index on `auth_users.person_id`.
- Add a unique index on `auth_users.username`.
- Remove contact/profile fields from `auth_users`.

Migrations:

```text
db/migration/auth/V5__add_auth_user_person_reference.sql
db/migration/auth/V6__remove_contact_fields_from_auth_users.sql
```

## Security Rules

- Never log raw passwords.
- Never log raw JWTs or refresh tokens.
- Never log Keycloak access tokens.
- Never store contact/profile data redundantly in Auth.
- Enforce backend authorization on APIs even when frontend route guards exist.
- Use DTOs for API responses; do not return sensitive entities directly.
- Keep roles and privilege decisions server-side.

## Gateway Contracts

Auth exposes `AuthModuleGateway` for other modules.

Auth consumes `PersonModuleGateway` for KYC person lookup and promotion.

Auth must not directly use KYC repositories.

## Implementation Backlog

- Add tests for username generation and conflict handling.
- Add tests for SSO mapping by `nexacore_person_id`.
- Add tests proving AuthUser cannot be created without a valid person reference.
- Add tests proving removed contact fields are not used by Auth.
- Add a formal person-to-user approval API if not already implemented.
- Add audit logging for user creation, role assignment, SSO login, and account disablement.
