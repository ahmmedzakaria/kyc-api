# Backend Agent Guide

## Scope

This guide applies to the NexaCore backend Maven project under `backend/`.

## Project Shape

- Java 21
- Spring Boot 3.5
- Maven build file: `pom.xml`
- Package root: `com.nexacore`
- Modular monolith structure verified with Spring Modulith
- Existing top-level modules include:
  - `appconfigmodule`
  - `authmodule`
  - `commonmodule`
  - `gatewaymodule`
  - `gismodule`
  - `kycmodule`
  - `logmodule`
  - `posmodule`
  - `servicesmodule`
  - planned `esbmodule`

## Module Rules

- Keep package and ownership boundaries under `com.nexacore`.
- Business modules own domain rules, persistence, controllers, and module-specific services.
- Reusable technical capabilities belong in `servicesmodule`.
- Cross-cutting configuration belongs in `appconfigmodule`.
- API access logs, error logs, and audit logs belong in `logmodule`.
- Shared DTOs, constants, and common context belong in `commonmodule`.
- Do not duplicate provider implementations inside business modules when a reusable service abstraction is appropriate.

## Person/User Domain Rules

- Every `AuthUser` is a `KycPerson`.
- Not every `KycPerson` is an `AuthUser`.
- A `KycPerson` can later become an `AuthUser` after approval/business verification.
- Keep person identity/profile fields such as first name and last name in `kyc_db.kyc_person`.
- Do not duplicate person profile fields in `auth_db.auth_users`; store only the application-level reference `auth_users.person_id`.
- Because `auth_db` and `kyc_db` are separate databases, treat `auth_users.person_id -> kyc_person.id` as an application-level reference, not a physical cross-database foreign key.

## Database Rules

- Use explicit `@Table(name = "...")` mappings for persistent entities.
- Prefix every table with the owning module code, for example:
  - `auth_`
  - `kyc_`
  - `gis_`
  - `log_`
  - `pos_`
  - `esb_`
- Prefix join tables too, for example `auth_user_roles`.
- Do not prefix column names only for module ownership. Use readable names such as `user_id`, `role_id`, `business_id`, and `branch_id`.
- Every persistent table in every module, submodule, service, and feature must include:
  - `created_by`
  - `updated_by`
  - `created_at`
  - `updated_at`
- `created_by` and `updated_by` should store the authenticated user or system actor responsible for the change.
- Use a clear system actor value for seed data, scheduled jobs, migrations, and automated integrations.
- When renaming existing tables, add or document a migration path. `ddl-auto=update` can create new prefixed tables in development but will not move existing data.

## Migration Rules

- Use Flyway for controlled schema changes.
- Keep migration locations aligned with existing datasource-specific migration folders under `src/main/resources/db/migration`.
- Do not rely on `spring.jpa.hibernate.ddl-auto=update` for production schema evolution.
- Keep SQL clients, reports, documentation, and Keycloak SPI queries aligned with entity table names.

## Service Abstraction Rules

Use this structure for reusable technical services:

```text
servicesmodule/{service-name}
├── config
├── dto
├── enums
├── provider
│   ├── interfaces
│   └── implementations
└── service
    ├── interfaces
    └── implementations
```

Provider-based services should follow this dependency direction:

```text
business module -> shared service interface -> provider interface -> provider implementation
```

Business modules must not depend on concrete provider classes such as `JasperReportProvider`, SMTP implementations, SMS gateway clients, or payment gateway clients.

## ESB Rules

- `esbmodule` is for integration routing, provider selection, transformation, retry, circuit breaker, failover, and integration audit.
- Do not use ESB as the main HTTP load balancer.
- Use an API gateway, Nginx, Kubernetes ingress, cloud load balancer, or reverse proxy for backend instance load balancing.
- ESB must not contain POS, KYC, Auth, GIS, or subscription business rules.
- ESB routes must avoid logging sensitive request and response payloads.

## Security Rules

- Backend APIs must enforce authorization even when frontend route guards hide UI actions.
- Do not return entities directly from controllers when sensitive fields may exist.
- Use explicit response DTOs.
- Do not log raw OTP, token, password, card, Firebase credential, authorization header, or full PII payloads.
- Store credentials through environment variables, ignored local files, vaults, or encrypted configuration.
- Apply business and branch filtering for multi-business data access.

## Internationalization Rules

- Avoid hard-coded user-facing response, validation, notification, report, and ESB error text in new backend code.
- Use stable message codes such as `auth.login.success`, `kyc.person.created`, `esb.route.disabled`, or `queueing.publish.failed`.
- Keep shared i18n infrastructure in `commonmodule`.
- Backend should resolve API/business/validation messages and return both message code and localized fallback text when response DTOs support it.
- Frontend apps should own UI labels and send `Accept-Language` on API requests.
- Add or update message bundle entries when introducing new user-facing messages.

## Documentation Rules

- Keep implementation plans aligned with the current project structure.
- When adding planned tables to documents, include module-prefixed table names and the required audit columns.
- When proposing new infrastructure, clarify whether it belongs in code, Docker Compose, gateway/reverse proxy, or future deployment infrastructure.

## Unit Test Rules

- Add unit tests for every new backend module, submodule, reusable service, provider, and feature implementation.
- Place tests in the matching package under `src/test/java`.
- Test service behavior through public interfaces where practical.
- Mock external infrastructure in unit tests, including RabbitMQ, SMTP, SMS gateways, payment gateways, file storage providers, and HTTP clients.
- Use Testcontainers only for integration tests that explicitly need real infrastructure.
- Cover disabled configuration paths, missing provider/configuration failures, success responses, failure responses, validation rules, and sensitive-data masking.
- For provider-based services, test both the service resolver and the provider implementation.
- Run `mvn test` before completing backend implementation work.

## Verification

Run the narrowest meaningful checks for the files touched:

```bash
mvn test
```

For Docker/config changes, validate from the repository root when possible:

```bash
docker compose config
```

If a command cannot be run because Docker, local databases, network access, or dependencies are unavailable, report that clearly.
