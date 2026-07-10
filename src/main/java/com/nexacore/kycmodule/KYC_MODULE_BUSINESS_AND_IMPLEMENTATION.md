# KYC Module Business And Implementation Plan

## Purpose

The KYC module owns person identity, person profile, person contact information, KYC records, and person documents.

The module is the source of truth for:

- Person identity
- Email and email verification
- Mobile number and mobile verification
- First name and last name
- KYC profile details
- KYC documents and profile photo metadata
- Whether a person has been promoted to an application user

## Core Domain Rules

- Every `AuthUser` is a `KycPerson`.
- Not every `KycPerson` is an `AuthUser`.
- A `KycPerson` can later become an `AuthUser` after approval/business verification.
- `kyc_person.is_user` identifies whether the person currently has an application user account.
- `kyc_person.username` is assigned only when the person becomes a user.
- KYC person creation must not generate usernames for ordinary non-user persons.
- Auth is responsible for username verification/generation before promoting a person to user.

## Business Responsibilities

The KYC module should support these business capabilities:

- Register a person before they become a user.
- Maintain person contact and profile data.
- Store and expose profile photo metadata.
- Store and expose person documents such as NID and supporting files.
- Maintain detailed person information such as family, education, and address details.
- Publish person registration events for audit/logging.
- Expose a module gateway so Auth can resolve and promote a person without depending on KYC repositories directly.

## Ownership Boundary

KYC owns:

- `kyc_person`
- `kyc_person_details`
- `kyc_person_document`
- Person APIs under `/api/v1/person`
- Person/KYC service rules
- KYC privilege definitions
- Person profile/contact fields

KYC does not own:

- Passwords
- Login sessions
- Application JWT issuance
- Auth roles
- User credential validation
- Keycloak token validation

## Tables

### `kyc_person`

Primary table for person identity and contact data.

Important columns:

```text
id
username
mobile_number
email
first_name
last_name
date_of_birth
gender
national_id
photo_url
blood_grop
email_verified
mobile_verified
is_user
```

Rules:

- `email` is unique.
- `mobile_number` is unique.
- `username` is unique when present.
- `username` can be null for non-user persons.
- `is_user` defaults to `false`.
- When Auth promotes a person to user, KYC sets `is_user = true` and stores the assigned username.

### `kyc_person_details`

Stores secondary person details:

```text
person_id
father_name
mother_name
emergency_contact_*
education_*
current_location_*
permanent_location_*
created_by
updated_by
created_at
updated_at
```

### `kyc_person_document`

Stores document metadata and file references.

Rules:

- Actual file storage is delegated to the shared file service.
- The KYC module stores document type, storage path, content type, original filename, size, and timestamps.
- Single-document types such as profile photo and NID should upsert the latest file.

## Current Implementation

Current package:

```text
com.nexacore.kycmodule
├── config
└── person
    ├── api
    ├── controller
    ├── dto
    ├── entity
    ├── repository
    ├── service
    ├── kyc
    └── privilege
```

Important classes:

- `KycPerson`
- `KycPersonDetails`
- `KycPersonDocument`
- `PersonService`
- `PersonController`
- `PersonRepository`
- `KycPersonModuleGateway`
- `KycPrivilegeProvider`

## Gateway Contract

KYC exposes `PersonModuleGateway` through `KycPersonModuleGateway`.

Auth should use this gateway for:

- Finding a person by id.
- Finding a person by username.
- Finding a person by email.
- Finding a person by mobile number.
- Ensuring a person exists for a seeded/default user.
- Promoting an existing person to user.
- Checking whether a person exists.
- Checking username assignment at the person level.

Auth must not directly use KYC repositories.

## Person Creation Flow

For ordinary KYC person registration:

1. Request arrives at `PersonController`.
2. `PersonService` maps request into `KycPerson`.
3. Person profile/contact data is saved in `kyc_person`.
4. Optional details are saved in `kyc_person_details`.
5. Optional files are stored through `FileManagementService`.
6. Document metadata is saved in `kyc_person_document`.
7. `PersonRegisteredEvent` is published.

Important rule:

```text
Do not generate username during ordinary person creation.
```

## Person To User Promotion Flow

When a person is approved to become a user:

1. Auth verifies the business flow has approved the promotion.
2. Auth generates or validates a unique username.
3. Auth calls `PersonModuleGateway.promotePersonToUser(...)`.
4. KYC validates that the username is not assigned to another person.
5. KYC stores the username on `kyc_person`.
6. KYC sets `is_user = true`.
7. Auth creates or updates the linked `auth_users` row with `person_id`.

## Data Source

KYC uses:

```properties
spring.datasource.kyc.jdbc-url=jdbc:postgresql://localhost:5433/kyc_db
```

Flyway migrations are under:

```text
src/main/resources/db/migration/kyc
```

## Migration Notes

Current planned schema changes include:

- Add `kyc_person.is_user`.
- Make `kyc_person.username` nullable.
- Add a unique index for non-null usernames.

Migration:

```text
db/migration/kyc/V2__add_person_user_flag_and_username_constraint.sql
```

## Validation Rules

Recommended validation:

- `email` is required for a person.
- `mobile_number` is required for a person.
- `email` must be unique.
- `mobile_number` must be unique.
- `username` must be unique when present.
- `username` should only be set by Auth/user promotion flow.

## Security Rules

- Do not expose document storage paths directly when a controlled download endpoint is required.
- Do not return unnecessary PII in broad search responses.
- Apply authorization checks to person create/update/delete/read operations.
- Keep KYC document access protected.

## Integration With Keycloak

Keycloak does not own person profile data.

The Keycloak User Storage SPI reads:

- `auth_db.auth_users` for account credentials and enabled status.
- `kyc_db.kyc_person` for email, mobile, first name, last name, and verification flags.

KYC remains the source of truth for profile/contact data even when Keycloak shows profile information.

## Implementation Backlog

- Add focused tests for `KycPersonModuleGateway`.
- Add tests proving ordinary person creation does not generate username.
- Add tests proving promotion sets `is_user = true`.
- Add tests proving username conflict is rejected during promotion.
- Add API/business flow for person-to-user approval if not already present.
- Add audit logging for person promotion to user.
