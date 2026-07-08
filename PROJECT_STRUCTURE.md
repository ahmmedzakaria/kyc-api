# Project Structure

This backend is organized as a modular monolith. Top-level packages under `com.nexacore` are Spring Modulith application modules; inside those modules, features are organized as `module > submodule`.

## High-Level Modules

1. `appconfigmodule`
2. `authmodule`
3. `commonmodule`
4. `systemmodule`
5. `kycmodule`
6. `gismodule`
7. `logmodule`
8. `servicesmodule`
9. `posmodule`
10. planned `esbmodule`

## Module Responsibilities

### `appconfigmodule`
- Cross-cutting technical configuration.
- Includes Swagger/OpenAPI, Flyway orchestration, global exception handling, app config, and shared web/security configuration.

### `authmodule`
- Owns authentication, user, role, token, SSO, and auth persistence.
- Privilege persistence and privilege-management APIs currently live here because they use the auth datasource.
- Reusable privilege definition contracts are provided by `systemmodule.privilege`.

### `commonmodule`
- Shared DTOs, constants, i18n, and common context used by multiple modules.

### `systemmodule`
- System-level capabilities and contracts that coordinate access across modules and frontend applications.
- Contains privilege definition contracts under:

```text
systemmodule/privilege
├── dto
├── enums
└── service/interfaces
```

These contracts describe module/submodule/frontend access definitions. Business modules publish their access definitions through `ModulePrivilegeProvider`.

### `kycmodule`
- Owns person and KYC business behavior.
- Uses `module > submodule` layout.
- KYC records are nested under the person submodule because KYC will be treated as person-related behavior.

Current structure:

```text
kycmodule
├── config
└── person
    ├── controller
    ├── dto
    ├── entity
    ├── repository
    ├── service
    │   └── implementations
    ├── kyc
    │   ├── controller
    │   ├── dto
    │   │   └── documentation
    │   ├── entity
    │   ├── repository
    │   └── service
    │       ├── interfaces
    │       └── implementations
    └── privilege
```

### `gismodule`
- Owns GIS/location search domain and APIs.
- Should not depend directly on KYC internals except through explicit contracts where needed.

### `logmodule`
- Owns API access logs, error logs, audit logs, cleanup, and log persistence.

### `servicesmodule`
- Reusable technical services.
- Existing submodules include file, report, queueing, cache, email, messaging, and Firebase auth services.

### `posmodule`
- POS module planning and future POS code.
- Database import helper scripts belong under `src/main/resources/db/manual/pos`.

## Dependency Rules

1. Top-level modules remain the Spring Modulith boundaries.
2. Submodules own their controllers, DTOs, entities, repositories, and services.
3. Shared request/response DTOs and i18n infrastructure belong in `commonmodule`.
4. Business modules publish frontend access definitions through `systemmodule.privilege.service.interfaces.ModulePrivilegeProvider`.
5. Auth owns privilege persistence for now; `systemmodule` owns reusable privilege definition contracts.
6. Reusable technical capabilities belong in `servicesmodule`.
7. Cross-module direct coupling should be minimized; prefer shared contracts or service interfaces.

## Current Package Mapping

- `com.nexacore` -> application bootstrap
- `com.nexacore.appconfigmodule` -> cross-cutting configuration
- `com.nexacore.authmodule` -> auth, user, role, token, SSO, privilege persistence/API
- `com.nexacore.commonmodule` -> shared DTOs, i18n, and common context
- `com.nexacore.systemmodule` -> system-level shared capabilities and contracts
- `com.nexacore.systemmodule.privilege` -> system-level privilege definition contracts
- `com.nexacore.kycmodule.person` -> person submodule
- `com.nexacore.kycmodule.person.kyc` -> person-scoped KYC submodule
- `com.nexacore.kycmodule.person.privilege` -> KYC/person privilege provider
- `com.nexacore.gismodule` -> GIS module
- `com.nexacore.logmodule` -> logging module
- `com.nexacore.servicesmodule` -> reusable technical service submodules
- `com.nexacore.posmodule` -> POS planning/future module
