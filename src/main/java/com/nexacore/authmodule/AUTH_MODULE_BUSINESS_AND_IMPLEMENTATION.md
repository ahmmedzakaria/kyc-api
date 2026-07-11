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
- Publishing registration and authentication model configuration through application context.

Auth does not own:

- Person first name or last name.
- Person email or mobile number.
- Person contact verification state.
- KYC documents.
- Person profile photo.
- KYC approval business data.

## Configurable Registration And Authentication Models

The system must support different user registration and authentication models based on configuration.

The selected model is shared through `ApplicationContextDto` so frontend and backend can behave consistently without hardcoded assumptions.

Business goals:

- Support local username/password login.
- Support Keycloak SSO login.
- Support person-first registration where a person exists before an application user.
- Support admin-created users.
- Support invite-based user activation.
- Support approval-based person-to-user promotion.
- Support optional self-registration if enabled for a client/application.
- Allow each frontend application to render the correct registration and login UI from context.
- Allow backend APIs to enforce the same configured model.

### Model Dimensions

Registration and authentication are separate concerns.

Registration model answers:

```text
How can a person become an application user?
```

Authentication model answers:

```text
How can an existing user authenticate?
```

Recommended registration modes:

```java
public enum RegistrationMode {
    DISABLED,
    ADMIN_CREATED,
    PERSON_REQUEST,
    INVITE_ONLY,
    SELF_SERVICE,
    APPROVAL_REQUIRED
}
```

Recommended login identifier types:

```java
public enum LoginIdentifierType {
    USERNAME,
    EMAIL,
    MOBILE,
    PERSON_ID
}
```

Recommended user activation modes:

```java
public enum UserActivationMode {
    IMMEDIATE,
    EMAIL_VERIFICATION_REQUIRED,
    MOBILE_VERIFICATION_REQUIRED,
    APPROVAL_REQUIRED,
    INVITE_ACCEPTANCE_REQUIRED
}
```

### Supported Registration Models

#### `DISABLED`

No public or frontend-driven registration is allowed.

Backend behavior:

- Reject self-registration APIs.
- Allow admin/user-management APIs only when authorized.

Frontend behavior:

- Hide registration links and forms.

#### `ADMIN_CREATED`

Authorized operators create users from existing persons.

Backend behavior:

- Require authorized role/privilege.
- Require existing `KycPerson`.
- Auth verifies or generates username.
- Auth creates `AuthUser`.
- KYC sets `kyc_person.is_user = true`.

Frontend behavior:

- Show user-management screens only to privileged users.

#### `PERSON_REQUEST`

A person requests user access after being registered as a KYC person.

Backend behavior:

- Accept user-access request for an existing person.
- Route request to approval workflow.
- Create `AuthUser` only after approval.

Frontend behavior:

- Show request-access flow.
- Do not show password setup until approved.

#### `INVITE_ONLY`

User registration starts from an invitation.

Backend behavior:

- Authorized user creates invite.
- Invite links to a `KycPerson` or creates a pending person record.
- Invite token has expiry and single-use rules.
- Account activates after invite acceptance.

Frontend behavior:

- Show invite acceptance screen.
- Hide open registration.

#### `SELF_SERVICE`

Person and user onboarding can start from a public flow.

Backend behavior:

- Create `KycPerson`.
- Validate configured contact verification rules.
- Either create user immediately or route through approval based on activation mode.

Frontend behavior:

- Show public registration flow.
- Render required contact/document fields from context.

#### `APPROVAL_REQUIRED`

All user creation requires business approval.

Backend behavior:

- Create request/application record.
- Route through KYC/workflow approval.
- Create or activate `AuthUser` only after approval.

Frontend behavior:

- Show pending/rejected/approved status.
- Prevent login until account activation.

### Supported Authentication Models

### Registration And Login Model Matrix

The platform should support these configurable model pairs. A deployment or client application can enable one or more models through application context.

| Model | Registration | Login |
| --- | --- | --- |
| Email + Password | Email and password | Password |
| Mobile + Password | Mobile number and password | Password |
| Email Verification | Email confirmation | Password |
| Mobile OTP | Phone number | OTP |
| Magic Link | Email | Link |
| Social Login | OAuth provider | OAuth |
| SSO | Enterprise identity | SSO |
| MFA | Standard registration | Multiple factors |
| Biometrics | Device enrollment | Fingerprint or face |
| Passkeys | Device enrollment | Cryptographic challenge |

Implementation notes:

- `Email + Password` should create credentials only after the linked `KycPerson` is valid for the configured registration mode.
- `Mobile + Password` should create credentials only after the linked `KycPerson` is valid for the configured registration mode and has a verified or allowed `mobile_number`.
- `Email Verification` uses `kyc_person.email` and `kyc_person.email_verified`; Auth must not duplicate those fields.
- `Mobile OTP` uses `kyc_person.mobile_number` and `kyc_person.mobile_verified`; OTP delivery belongs behind reusable messaging service abstractions.
- `Magic Link` should issue short-lived, single-use login links and avoid logging full tokens.
- `Social Login` should map OAuth provider identity to an existing or newly approved `KycPerson` before creating `AuthUser`.
- `SSO` should prefer `nexacore_person_id` for mapping and use Keycloak/enterprise identity as the identity provider.
- `MFA` is an authentication policy layered on top of a primary login model.
- `Biometrics` should be treated as device-bound authentication, not as raw biometric storage in the backend.
- `Passkeys` should use a standards-based WebAuthn/FIDO2 implementation rather than custom cryptography.

Recommended additional enums:

```java
public enum RegistrationCredentialModel {
    EMAIL_PASSWORD,
    MOBILE_PASSWORD,
    EMAIL_VERIFICATION,
    MOBILE_OTP,
    MAGIC_LINK,
    SOCIAL_OAUTH,
    ENTERPRISE_SSO,
    MFA_ENROLLMENT,
    BIOMETRIC_ENROLLMENT,
    PASSKEY_ENROLLMENT
}
```

```java
public enum LoginMethod {
    PASSWORD,
    OTP,
    MAGIC_LINK,
    OAUTH,
    SSO,
    MFA,
    BIOMETRIC,
    PASSKEY
}
```

`ApplicationContextDto` should expose enabled registration credential models and enabled login methods so frontend can render only the configured flows.

#### Password Login

Backend validates username/password and issues backend JWT.

Frontend behavior:

- Show local login form.
- Use `/auth/authenticate`.

Backend behavior:

- Enable `/auth/authenticate`.
- Reject password login when `LoginMethod.PASSWORD` is not enabled.

#### SSO Login

Keycloak handles identity authentication. Backend validates Keycloak token and issues backend JWT.

Frontend behavior:

- Show SSO login.
- Redirect to Keycloak Authorization Code + PKCE.
- Use `/auth/sso/authenticate`.

Backend behavior:

- Enable SSO login only when `LoginMethod.SSO` is enabled.
- Validate Keycloak issuer, audience/client, expiry, and signature.
- Map user by `nexacore_person_id`, username, or another configured identifier.

#### Multiple Login Methods

Multiple authentication methods are enabled.

Frontend behavior:

- Render configured login options.
- Hide unavailable methods by client/application context.

Backend behavior:

- Enforce allowed methods per client/application/user type.
- Reject a login method that is not configured for the current context.

### Application Context Contract

`ApplicationContextDto` should become the shared source for frontend behavior after login and, where safe, before login through a public config endpoint.

Current context includes:

```text
clientCode
clientType
menus
privilegeCodes
enabledModules
enabledSubmodules
enabledFeatures
```

Recommended additions:

```text
registrationMode
enabledRegistrationCredentialModels
enabledLoginMethods
loginIdentifierTypes
userActivationMode
sso
registration
securityPolicy
```

Recommended DTO shape:

```java
public class ApplicationContextDto {
    private String clientCode;
    private String clientType;
    private RegistrationMode registrationMode;
    private Set<RegistrationCredentialModel> enabledRegistrationCredentialModels;
    private Set<LoginMethod> enabledLoginMethods;
    private Set<LoginIdentifierType> loginIdentifierTypes;
    private UserActivationMode userActivationMode;
    private SsoContextDto sso;
    private RegistrationContextDto registration;
    private SecurityPolicyContextDto securityPolicy;
    private List<SidebarMenuDto> menus;
    private Set<String> privilegeCodes;
    private Set<String> enabledModules;
    private Set<String> enabledSubmodules;
    private Set<String> enabledFeatures;
}
```

Recommended `SsoContextDto`:

```text
enabled
issuerUri
clientId
redirectUri
logoutRedirectUri
scopes
pkceRequired
```

Recommended `RegistrationContextDto`:

```text
enabled
mode
requiresExistingPerson
requiresApproval
requiresInvite
requiresEmailVerification
requiresMobileVerification
allowedPersonTypes
requiredFields
requiredDocuments
```

Recommended `SecurityPolicyContextDto`:

```text
passwordLoginEnabled
otpLoginEnabled
ssoLoginEnabled
sessionTimeoutSeconds
refreshTokenEnabled
maxLoginAttempts
passwordPolicyCode
```

### Configuration Source

Client-wise DB policy is the source of truth for enabled login methods and registration credential models.

Primary policy tables:

```text
auth_client_auth_policy
auth_client_registration_policy
```

Fallback properties are used only when no enabled DB policy rows exist for a client:

```properties
app.registration.mode=APPROVAL_REQUIRED
app.registration.credential-models=EMAIL_PASSWORD,MOBILE_PASSWORD,ENTERPRISE_SSO
app.auth.login-methods=SSO
app.auth.login-identifiers=USERNAME
app.registration.activation-mode=APPROVAL_REQUIRED
```

Do not use `app.auth.mode`. The old `LOCAL`/`SSO` mode is redundant because behavior is derived from `LoginMethod`:

```text
PASSWORD      -> local password login enabled
SSO           -> SSO login enabled
PASSWORD,SSO  -> both login methods enabled
```

`/auth/config` and `/auth/application-context/public` must not return legacy `authMode`. Frontend clients must read `enabledLoginMethods` instead:

```text
PASSWORD only      -> render local username/password login
SSO only           -> auto redirect guarded pages to SSO unless auto-SSO is temporarily suppressed
PASSWORD and SSO   -> route unauthenticated users to /login and render both choices
```

Resolution order:

```text
1. enabled rows in auth_client_auth_policy / auth_client_registration_policy for client_code
2. client-specific property override
3. global property fallback
4. safe system default from code
```

These policy tables belong to Auth because they control account creation and login behavior. They reference client applications by stable `client_code`; no cross-database foreign key is required.

### Backend Enforcement Rules

- Frontend context is advisory for UI behavior.
- Backend must enforce the configured registration and authentication model.
- Registration APIs must reject disabled or unavailable modes.
- Login APIs must reject unavailable authentication methods.
- SSO token bridge must reject SSO when `LoginMethod.SSO` is not enabled for the current client/application.
- Local password login must reject when `LoginMethod.PASSWORD` is not enabled for the current context.
- Person-to-user promotion must require a valid `KycPerson`.
- `AuthUser` creation must set `person_id`.

### Frontend Behavior Rules

Frontend should call application context/config before rendering login or registration.

Examples:

```text
registrationMode=DISABLED
-> hide registration

registrationMode=PERSON_REQUEST
-> show request-access flow

enabledLoginMethods=PASSWORD
-> show username/password login

enabledLoginMethods=SSO
-> show SSO login only

enabledLoginMethods=PASSWORD,SSO
-> show configured login choices
```

The frontend must not infer capabilities from routes alone. It should render based on `ApplicationContextDto` and handle backend rejection gracefully.

### Suggested API Endpoints

Public/pre-login:

```http
POST /auth/config
POST /auth/application-context/public
```

Authenticated/post-login:

```http
POST /auth/application-context
```

Registration:

```http
POST /auth/registration/request-user-access
POST /auth/registration/invite/accept
POST /auth/registration/self-service
POST /auth/users/promote-person
```

The first implementation can extend `/auth/config`; the better long-term implementation is a dedicated application context endpoint because auth config is only one part of application behavior.

### Implementation Phases

Phase 1:

- Add enums for registration mode, login identifier type, credential type, and activation mode.
- Extend `ApplicationContextDto`.
- Extend `/auth/config` or add public application context endpoint.
- Drive frontend login/register UI from returned context.

Phase 2:

- Add backend enforcement in local login and SSO login.
- Add registration-mode checks to registration/user promotion APIs.
- Add tests for disabled and unavailable modes.

Phase 3:

- Add person-to-user request workflow.
- Add invite-only activation flow.
- Add approval-required activation flow.

Phase 4:

- Move policies from properties to database-backed policy tables.
- Allow per-client/per-application policies.
- Add admin UI for registration/authentication policy management.

Phase 5:

- Add audit logging for policy changes, registration requests, account activation, login method rejection, and SSO mapping failures.

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
