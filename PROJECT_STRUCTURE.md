# Project Structure

This backend is organized into core modules with clear responsibilities and dependency boundaries.

## High-Level Modules

1. `core module`
2. `appconfigmodule`
3. `authmodule`
4. `commonmodule`
5. `kycmodule` (independent module)
6. `gismodule` (independent module)

## Module Responsibilities

### `core module`
- Contains application bootstrap and top-level entry wiring.
- Main class: `NexaCoreApplication`.

### `appconfigmodule`
- Holds cross-cutting technical configuration.
- Includes:
  - Security and JWT filter configuration
  - Swagger/OpenAPI setup
  - Multi-database datasource/entity-manager/transaction-manager configs
  - Global exception handling and app lifecycle hooks

### `authmodule`
- Owns authentication and authorization domain logic.
- Includes:
  - Auth APIs (`/auth/*`)
  - User/Role entities and repositories
  - Authentication services and startup seed data

### `commonmodule`
- Shared/common components used by multiple modules.
- Includes:
  - Standard API response wrapper (`ApiResponse`)
  - Shared DTOs (`SearchDto`, `IdRequest`, etc.)
  - Shared constants
  - Shared service interfaces and implementations (for example storage service)

### `kycmodule` (Independent)
- Owns KYC business domain and APIs.
- Independent from `gismodule`.
- Uses `commonmodule` for shared DTOs/response patterns/services.

### `gismodule` (Independent)
- Owns GIS/location search domain and APIs.
- Independent from `kycmodule`.
- Uses `commonmodule` for shared DTOs/response patterns.

## Dependency Rules

1. `kycmodule` and `gismodule` are independent modules.
2. Both `kycmodule` and `gismodule` depend on `commonmodule`.
3. `authmodule` can also consume shared utilities from `commonmodule`.
4. `appconfigmodule` provides global configuration used across modules.
5. Cross-module direct coupling should be minimized; shared concerns belong in `commonmodule`.

## Conceptual Dependency View

```text
                  +------------------+
                  |   core module    |
                  +------------------+
                            |
                            v
                  +------------------+
                  | appconfigmodule  |
                  +------------------+
                       /    |     \
                      v     v      v
               +---------+ +---------+ +---------+
               |authmodule| |kycmodule| |gismodule|
               +---------+ +---------+ +---------+
                      \       |       /
                       \      |      /
                        v     v     v
                    +------------------+
                    |   commonmodule   |
                    +------------------+
```

## Current Package Mapping

- `com.nexacore` -> core module
- `com.nexacore.appconfigmodule` -> appconfigmodule
- `com.nexacore.authmodule` -> authmodule
- `com.nexacore.commonmodule` -> commonmodule
- `com.nexacore.kycmodule` -> independent KYC module
- `com.nexacore.gismodule` -> independent GIS module
