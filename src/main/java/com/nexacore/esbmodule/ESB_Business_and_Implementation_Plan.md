# ESB Module Business And Implementation Plan

## Purpose

The ESB module will provide a controlled integration layer for NexaCore when the platform needs to route requests between frontend apps, backend modules, future backend services, and external systems.

This plan is written for the current project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Proposed module: `com.nexacore.esbmodule`
- Existing modular style: Spring Modulith with open application modules
- Existing shared services: `servicesmodule`
- Existing auth and privilege: `authmodule`
- Existing audit/API logging: `logmodule`
- Current runtime wiring: `docker-compose.yml` with `nexacore-backend`, frontend apps, Keycloak, Metabase, and observability services

## Important Architecture Decision

An ESB should not be treated as the main load balancer for this project.

Use the right layer for each responsibility:

| Responsibility | Recommended owner | Reason |
| --- | --- | --- |
| HTTP load balancing across backend instances | API gateway, Nginx, cloud load balancer, Kubernetes ingress, or Docker reverse proxy | This is infrastructure traffic distribution |
| Authentication entry point, CORS, rate limiting, public route control | API gateway | These are edge concerns |
| Internal integration routing, provider selection, transformation, retry, circuit breaker, audit | ESB module | These are integration concerns |
| POS/KYC/Auth business rules | Owning business module | ESB must not contain domain business logic |
| External provider execution | `servicesmodule` provider abstractions | Keeps provider code reusable across modules |

Recommended approach:

```text
Client
  -> API Gateway / Load Balancer
  -> NexaCore backend instance
  -> esbmodule route/orchestration layer
  -> business module or shared service
  -> external provider when needed
```

## Business Goals

- Support future systems without duplicating integration code in POS, KYC, Auth, GIS, and service modules.
- Centralize external system routing, retry, timeout, transformation, and provider failover.
- Keep load-balancing decisions explicit and observable.
- Make integration behavior configurable by business, module, feature, provider, and environment.
- Support future multi-instance backend deployment while keeping the current backend runnable as a single application.
- Prepare the platform for payment gateways, SMS gateways, email providers, report services, ERP/accounting integrations, inventory integrations, and notification providers.

## Target Use Cases

### Near-Term Use Cases

- Route SMS OTP requests to the configured mobile messaging provider.
- Route email OTP and transaction notifications to the configured email provider.
- Route report generation requests to JasperReports and later other providers.
- Route payment execution requests to cash/manual/mobile wallet/payment gateway providers.
- Route barcode generation requests to a configured barcode provider.
- Route analytics/report export jobs to Metabase/JasperReports.

### Future Use Cases

- Fail over from one SMS provider to another if the primary provider is down.
- Split traffic between payment providers by business plan, country, transaction value, or provider health.
- Route requests to separate deployed backend services if modules are split out later.
- Transform internal DTOs into external API request formats.
- Apply retry, timeout, and circuit breaker policies per integration route.
- Record integration audit trails for sensitive operations.
- Publish async integration jobs through RabbitMQ.

## Non-Goals

- Do not move POS, KYC, Auth, GIS, or business rules into the ESB.
- Do not use ESB as a replacement for `authmodule` authorization checks.
- Do not use ESB as a replacement for an API gateway or reverse proxy.
- Do not route every internal Java service call through ESB. Use ESB only for integration boundaries and provider orchestration.
- Do not log sensitive request or response bodies in ESB logs.

## Recommended Module Placement

Create a new top-level module:

```text
backend/src/main/java/com/nexacore/esbmodule
├── config
├── controller
├── dto
├── entity
├── enums
├── repository
├── route
│   ├── interfaces
│   └── implementations
├── service
│   ├── interfaces
│   └── implementations
├── policy
├── transformer
├── registry
└── package-info.java
```

Reason for a top-level `esbmodule`:

- It is a platform integration module, not a simple reusable provider like `emailservice` or `reportservice`.
- It may need its own APIs, route registry, policy tables, audit records, and integration health checks.
- It should be visible as a Spring Modulith application module.

Use `servicesmodule` for concrete provider execution:

```text
esbmodule
  -> servicesmodule.emailservice
  -> servicesmodule.messagingservice
  -> servicesmodule.reportservice
  -> servicesmodule.paymentservice
  -> servicesmodule.barcodeservice
  -> servicesmodule.notificationservice
```

## Proposed Runtime Architecture

### Current Single Backend Deployment

```text
frontend / privilege-frontend
  -> nexacore-backend:9100
  -> controller
  -> business module
  -> esbmodule when integration routing is needed
  -> servicesmodule provider abstraction
  -> external provider
```

### Future Multi-Instance Deployment

```text
frontend / privilege-frontend
  -> gateway or reverse proxy
  -> nexacore-backend-1
  -> nexacore-backend-2
  -> nexacore-backend-3
```

The gateway/reverse proxy performs HTTP load balancing. The ESB module performs integration route decisions after a request reaches one backend instance.

### Future Split-Service Deployment

```text
frontend
  -> gateway
  -> auth service
  -> kyc service
  -> pos service
  -> esb/integration service
  -> external systems
```

If NexaCore is later split into microservices, the ESB module can be extracted into a separate integration service without rewriting all business modules.

## Load Balancing Plan

### Backend Instance Load Balancing

For multiple backend instances, use one of these options:

| Option | Best fit | Notes |
| --- | --- | --- |
| Nginx reverse proxy | Simple Docker/local/server deployment | Good first step for this project |
| Spring Cloud Gateway | Java/Spring route control, auth filters, rate limits | Better when gateway behavior needs to be application-managed |
| Kubernetes Ingress | Production container orchestration | Best after moving to Kubernetes |
| Cloud load balancer | Managed production infrastructure | Best for cloud deployment |

Recommended first step:

```text
Nginx or Spring Cloud Gateway
  -> nexacore-backend instance A
  -> nexacore-backend instance B
```

Keep this outside `esbmodule`.

### Provider Load Balancing

Provider load balancing belongs inside ESB only when selecting among multiple providers for the same capability.

Examples:

- SMS provider A vs SMS provider B.
- Email provider A vs email provider B.
- Payment gateway A vs payment gateway B.
- Report renderer A vs report renderer B.

Supported route strategies:

| Strategy | Use case |
| --- | --- |
| `PRIMARY_ONLY` | Use one configured provider |
| `FAILOVER` | Try primary provider, then fallback provider |
| `ROUND_ROBIN` | Spread traffic across healthy equivalent providers |
| `WEIGHTED` | Send percentage-based traffic to providers |
| `BUSINESS_RULE` | Select provider by business, country, plan, amount, or feature |

## Proposed Database Tables

Use module-prefixed table names.

Every persistent ESB table must include audit ownership columns:

- `created_by`
- `updated_by`

Apply the same rule to every future NexaCore module, submodule, service, and feature table. These columns identify which authenticated user or system actor created and last updated the record. Use a nullable value only for seed/system data where no authenticated user exists.

### `esb_route`

Stores logical integration routes.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `route_code` | varchar | Stable route key, for example `SMS_OTP_SEND` |
| `route_name` | varchar | Human-readable name |
| `source_module` | varchar | Calling module, for example `posmodule` |
| `source_feature` | varchar | Calling feature, for example `sales_receipt` |
| `target_service` | varchar | Shared service, for example `messagingservice` |
| `route_type` | varchar | `SYNC`, `ASYNC`, `SCHEDULED` |
| `allow_request_mode_override` | boolean | Allows caller to request `SYNC` or `ASYNC` mode when safe |
| `default_request_mode` | varchar | Default request mode: `SYNC` or `ASYNC` |
| `strategy` | varchar | `PRIMARY_ONLY`, `FAILOVER`, `ROUND_ROBIN`, `WEIGHTED`, `BUSINESS_RULE` |
| `enabled` | boolean | Enables or disables route |
| `created_by` | varchar | User/system actor that created the route |
| `updated_by` | varchar | User/system actor that last updated the route |
| `created_at` | timestamp | Creation time |
| `updated_at` | timestamp | Last update time |

### `esb_route_provider`

Maps a route to provider candidates.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `route_id` | bigint | FK to `esb_route` |
| `provider_code` | varchar | Provider key, for example `generic_sms`, `smtp`, `jasper` |
| `priority` | int | Lower number means higher priority |
| `weight` | int | Weighted balancing percentage/weight |
| `timeout_ms` | int | Provider timeout |
| `retry_count` | int | Retry attempts |
| `enabled` | boolean | Enables or disables provider for route |
| `created_by` | varchar | User/system actor that created the provider mapping |
| `updated_by` | varchar | User/system actor that last updated the provider mapping |
| `created_at` | timestamp | Creation time |
| `updated_at` | timestamp | Last update time |

### `esb_route_policy`

Stores route-level operational policy.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `route_id` | bigint | FK to `esb_route` |
| `rate_limit_per_minute` | int | Optional route rate limit |
| `circuit_breaker_enabled` | boolean | Enables circuit breaker |
| `failure_threshold` | int | Failure count before opening circuit |
| `open_duration_seconds` | int | How long circuit stays open |
| `audit_enabled` | boolean | Enables integration audit |
| `sensitive_payload` | boolean | Marks route as sensitive |
| `persist_request_payload` | boolean | Stores sanitized request payload for async/retry handling |
| `max_async_attempts` | int | Maximum worker attempts for async requests |
| `async_retry_delay_seconds` | int | Delay before retrying failed async request |
| `dead_letter_enabled` | boolean | Moves exhausted async request to dead-letter status |
| `created_by` | varchar | User/system actor that created the policy |
| `updated_by` | varchar | User/system actor that last updated the policy |
| `created_at` | timestamp | Creation time |
| `updated_at` | timestamp | Last update time |

### `esb_request`

Stores ESB request state for configurable synchronous and asynchronous execution.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `request_id` | varchar | Correlation/request id |
| `trace_id` | varchar | OpenTelemetry trace id when available |
| `route_code` | varchar | Requested route |
| `requested_mode` | varchar | Caller requested mode: `SYNC` or `ASYNC` |
| `effective_mode` | varchar | Final mode after route policy resolution |
| `business_id` | bigint | Business context when available |
| `branch_id` | bigint | Branch context when available |
| `idempotency_key` | varchar | Prevents duplicate sensitive execution |
| `status` | varchar | `RECEIVED`, `PROCESSING`, `SUCCESS`, `FAILED`, `RETRY_PENDING`, `DEAD_LETTER` |
| `priority` | int | Async worker priority, lower number means higher priority |
| `scheduled_at` | timestamp | When async worker may process the request |
| `attempt_count` | int | Number of processing attempts |
| `max_attempts` | int | Maximum processing attempts copied from policy |
| `payload_reference` | varchar | Reference to sanitized stored payload when needed |
| `result_reference` | varchar | Reference to stored result when needed |
| `error_code` | varchar | Safe error code |
| `error_message` | varchar | Sanitized error message |
| `created_by` | varchar | User/system actor that created the request |
| `updated_by` | varchar | User/system actor that last updated the request |
| `created_at` | timestamp | Creation time |
| `updated_at` | timestamp | Last update time |

For sensitive routes, store only a sanitized payload or a reference to an encrypted payload. Do not store raw OTP, access token, card data, password, Firebase credential, authorization header, or full PII payload.

### `esb_integration_log`

Stores safe integration execution logs.

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `request_id` | varchar | Correlation/request id |
| `trace_id` | varchar | OpenTelemetry trace id when available |
| `route_code` | varchar | Route executed |
| `provider_code` | varchar | Provider selected |
| `business_id` | bigint | Business context when available |
| `branch_id` | bigint | Branch context when available |
| `status` | varchar | `SUCCESS`, `FAILED`, `TIMEOUT`, `CIRCUIT_OPEN` |
| `http_status` | int | External HTTP status when applicable |
| `duration_ms` | bigint | Execution time |
| `error_code` | varchar | Safe error code |
| `error_message` | varchar | Sanitized error message |
| `created_by` | varchar | User/system actor that triggered the integration |
| `updated_by` | varchar | User/system actor that last updated the log row, normally same as `created_by` |
| `created_at` | timestamp | Execution time |
| `updated_at` | timestamp | Last update time |

Do not store raw OTP, token, card data, password, Firebase credential, authorization header, or full PII payload in this table.

## Suggested DTOs

```text
EsbRouteRequest
EsbRouteResponse
EsbRequestMode
EsbRequestStatus
EsbProviderCandidate
EsbExecutionContext
EsbExecutionResult
EsbAsyncRequestStatus
EsbRoutePolicyDto
EsbRouteHealthDto
```

`EsbExecutionContext` should carry safe operational context:

- `requestId`
- `traceId`
- `businessId`
- `branchId`
- `moduleCode`
- `featureCode`
- `routeCode`
- `requestedMode`
- `effectiveMode`
- `idempotencyKey`
- authenticated user id
- safe metadata map

## Suggested Interfaces

```java
public interface EsbRoutingService {
    EsbExecutionResult execute(EsbRouteRequest request);
    EsbRouteResponse submit(EsbRouteRequest request);
}
```

`execute` is for routes that must complete in the current request thread. `submit` resolves the route mode and either executes synchronously or stores the request for async processing based on route configuration.

```java
public interface EsbRouteResolver {
    EsbResolvedRoute resolve(EsbExecutionContext context, String routeCode);
}
```

```java
public interface EsbProviderSelector {
    EsbProviderCandidate select(EsbResolvedRoute route, EsbExecutionContext context);
}
```

```java
public interface EsbIntegrationLogger {
    void log(EsbExecutionResult result);
}
```

```java
public interface EsbAsyncRequestWorker {
    void processPendingRequests();
}
```

## Technical Flow

### Configurable Request Mode

Each route has a configured default execution mode.

```text
EsbRouteRequest
  -> requestedMode optional
  -> EsbRouteResolver
  -> route.default_request_mode
  -> route.allow_request_mode_override
  -> effectiveMode
```

Rules:

- If `allow_request_mode_override=false`, use `default_request_mode`.
- If `allow_request_mode_override=true`, allow the caller to request `SYNC` or `ASYNC`.
- Sensitive routes such as payment execution should require an `idempotency_key`.
- Long-running routes such as report export should default to `ASYNC`.
- User-facing OTP routes may remain `SYNC` when the caller needs an immediate send result.
- Async requests should return `202 Accepted` style API behavior with `request_id` and status.

### Synchronous Route

```text
POS sales service
  -> EsbRoutingService.submit(SEND_RECEIPT_SMS, requestedMode=SYNC)
  -> EsbRouteResolver
  -> effectiveMode=SYNC
  -> EsbProviderSelector
  -> MobileMessagingService
  -> configured SMS provider
  -> EsbIntegrationLogger
  -> POS receives normalized result
```

### Failover Route

```text
Request
  -> primary provider
  -> timeout/failure
  -> fallback provider
  -> success/failure response
  -> sanitized integration log
```

### Future Async Route

```text
Business module
  -> EsbRoutingService.submit(..., requestedMode=ASYNC)
  -> EsbRouteResolver
  -> effectiveMode=ASYNC
  -> esb_request row with status=RECEIVED
  -> response returns request_id
  -> ESB worker
  -> service/provider
  -> esb_request status update
  -> retry/dead-letter handling
```

Initial async implementation can use database polling over `esb_request`. RabbitMQ should be added later when async volume grows or when delayed retry/dead-letter queues are required.

### Future RabbitMQ Async Route

```text
Business module
  -> EsbRoutingService.submit(..., requestedMode=ASYNC)
  -> esb_request row
  -> RabbitMQ message containing request_id
  -> ESB worker
  -> load esb_request
  -> service/provider
  -> update esb_request
  -> retry/dead-letter handling
```

## Integration With Existing Modules

| Existing module | ESB relationship |
| --- | --- |
| `authmodule` | Provides authenticated user and privilege context; ESB must not bypass auth |
| `commonmodule.business` | Provides business/branch/subscription context |
| `logmodule` | Owns API access/error/audit logs; ESB may add integration-specific logs |
| `servicesmodule.messagingservice` | ESB can route SMS/mobile messages |
| `servicesmodule.emailservice` | ESB can route email notifications |
| `servicesmodule.reportservice` | ESB can route report rendering/export jobs |
| `servicesmodule.paymentservice` | ESB can route payment provider execution |
| `servicesmodule.notificationservice` | ESB can route multi-channel notification decisions |
| `posmodule` | Uses ESB for external/provider integration, not for internal POS logic |

## Security Requirements

- Require backend authorization before ESB execution.
- Enforce business and branch context before route execution.
- Mask all sensitive fields before logging.
- Store provider credentials only in environment variables, vault, or encrypted configuration.
- Do not return raw provider errors directly to frontend clients.
- Add route-level audit for payment, OTP, KYC, identity, and bulk export routes.
- Use HTTPS/TLS for external provider calls.
- Apply timeout and retry limits to prevent request pileups.
- Do not expose provider admin APIs to normal frontend apps.

## Observability Requirements

Use the existing OpenTelemetry stack.

Recommended metrics:

- `esb_route_requests_total`
- `esb_route_failures_total`
- `esb_route_duration_seconds`
- `esb_provider_failures_total`
- `esb_provider_timeout_total`
- `esb_circuit_open_total`

Recommended trace attributes:

- `route_code`
- `provider_code`
- `module_code`
- `feature_code`
- `business_id`
- `branch_id`
- `status`

Do not add request bodies, OTPs, tokens, card numbers, passwords, or full identity fields as trace attributes.

## Configuration

Suggested properties:

```properties
esb.enabled=${ESB_ENABLED:false}
esb.default-strategy=${ESB_DEFAULT_STRATEGY:PRIMARY_ONLY}
esb.default-request-mode=${ESB_DEFAULT_REQUEST_MODE:SYNC}
esb.default-timeout-ms=${ESB_DEFAULT_TIMEOUT_MS:10000}
esb.default-retry-count=${ESB_DEFAULT_RETRY_COUNT:1}
esb.audit-enabled=${ESB_AUDIT_ENABLED:true}
esb.sensitive-payload-logging-enabled=${ESB_SENSITIVE_PAYLOAD_LOGGING_ENABLED:false}
esb.provider-health-check-enabled=${ESB_PROVIDER_HEALTH_CHECK_ENABLED:true}
esb.async.enabled=${ESB_ASYNC_ENABLED:false}
esb.async.worker-enabled=${ESB_ASYNC_WORKER_ENABLED:false}
esb.async.poll-interval-ms=${ESB_ASYNC_POLL_INTERVAL_MS:5000}
esb.async.batch-size=${ESB_ASYNC_BATCH_SIZE:25}
esb.async.default-max-attempts=${ESB_ASYNC_DEFAULT_MAX_ATTEMPTS:3}
esb.async.default-retry-delay-seconds=${ESB_ASYNC_DEFAULT_RETRY_DELAY_SECONDS:60}
```

Keep ESB disabled by default until routes and provider mappings are added.

## Docker And Deployment Plan

### Current Phase

Keep one `nexacore-backend` service in Docker Compose.

Add ESB as code inside the backend only after the route model is implemented.

### Load-Balanced Backend Phase

Add a gateway/reverse proxy in Docker Compose:

```text
nexacore-gateway
  -> nexacore-backend-1:9100
  -> nexacore-backend-2:9100
```

Use this when you need multiple backend instances locally or on a server.

### Future Async Phase

Add RabbitMQ only when async integration jobs are implemented:

```text
RabbitMQ
  -> esb async workers
  -> notification/report/payment providers
```

## Implementation Phases

### Phase 1: Planning And Boundaries

- Create `esbmodule` plan and module boundary.
- Decide which routes are ESB-managed.
- Keep infrastructure load balancing outside ESB.
- Define naming for route codes and provider codes.

### Phase 2: Core ESB Model

- Add `esbmodule/package-info.java` as a Spring Modulith module.
- Add route, provider, policy, and log entities.
- Add `esb_request` entity for configurable sync/async request tracking.
- Add Flyway migration for `esb_*` tables.
- Add route repository and service interfaces.
- Add configuration properties.

### Phase 3: Provider Routing

- Implement `EsbRoutingService`.
- Implement request mode resolution: route default mode plus optional caller override.
- Implement sync request execution through `EsbRoutingService.submit`.
- Implement `PRIMARY_ONLY` and `FAILOVER` strategies first.
- Connect to existing `emailservice`, `messagingservice`, and `reportservice`.
- Add sanitized integration logging.

### Phase 4: Database-Backed Async Requests

- Implement async request persistence in `esb_request`.
- Return `request_id` immediately for async requests.
- Add request status lookup API for callers.
- Add polling worker for pending async requests.
- Add retry and dead-letter state transitions.
- Keep RabbitMQ out until async volume justifies it.

### Phase 5: Resilience

- Add timeout handling.
- Add retry policy.
- Add circuit breaker support.
- Add provider health checks.
- Add OpenTelemetry metrics and traces.

### Phase 6: Load-Balanced Runtime

- Add API gateway or Nginx reverse proxy.
- Run multiple backend containers.
- Confirm sticky-session is not required.
- Ensure JWT/resource-server auth works across all instances.
- Ensure shared state uses database/Redis, not local memory.

### Phase 7: RabbitMQ Async ESB

- Add RabbitMQ.
- Publish async request ids to queue after `esb_request` persistence.
- Add retry queue and dead-letter queue.
- Add delayed retry scheduling.
- Move high-volume async report and notification jobs from polling to RabbitMQ.

## Recommended First Routes

| Route code | Source | Target service | Default mode | Override allowed | Strategy |
| --- | --- | --- | --- | --- | --- |
| `SMS_OTP_SEND` | Auth/KYC/POS customer | `messagingservice` | `SYNC` | false | `PRIMARY_ONLY`, later `FAILOVER` |
| `EMAIL_OTP_SEND` | Auth/KYC/POS customer | `emailservice` | `SYNC` | false | `PRIMARY_ONLY` |
| `TRANSACTION_EMAIL_SEND` | POS sales/payment | `emailservice` | `ASYNC` | true | `PRIMARY_ONLY` |
| `TRANSACTION_SMS_SEND` | POS sales/payment | `messagingservice` | `ASYNC` | true | `PRIMARY_ONLY` |
| `REPORT_RENDER` | POS reporting | `reportservice` | `SYNC` | true | `PRIMARY_ONLY` |
| `REPORT_EXPORT_LARGE` | POS reporting | `reportservice` | `ASYNC` | false | `PRIMARY_ONLY` |
| `PAYMENT_EXECUTE` | POS sales | `paymentservice` | `SYNC` | false | `BUSINESS_RULE`, later `FAILOVER` |

## Security Gaps To Avoid

| Gap | Risk | Mitigation |
| --- | --- | --- |
| Treating ESB as authorization layer | Unauthorized business action may execute | Check authorization in owning module before ESB call |
| Using ESB as HTTP load balancer | Wrong abstraction and hard scaling path | Use gateway/reverse proxy/load balancer |
| Logging full provider payloads | OTP, card, token, and PII leakage | Log only safe metadata and masked references |
| Unlimited retries | Duplicate SMS, email, payment, or provider abuse | Use idempotency keys and retry limits |
| No timeout | Backend threads can block under provider outage | Set route/provider timeout |
| No idempotency for payment/notification | Duplicate charges or duplicate messages | Require idempotency key for sensitive routes |
| Provider credentials in DB/source | Credential leakage | Use env/vault/encrypted config |
| Local-memory route state only | Multi-instance deployment inconsistency | Store route config in DB/cache |

## Decision Summary

- Add `esbmodule` as a platform integration module.
- Keep HTTP load balancing outside ESB.
- Use ESB for provider routing, failover, transformation, retry, audit, and future async jobs.
- Make every ESB route configurable as `SYNC` or `ASYNC`, with optional caller override only when the route explicitly allows it.
- Start async execution with database-backed `esb_request` persistence before introducing RabbitMQ.
- Reuse existing `servicesmodule` provider abstractions instead of duplicating provider code.
- Start with synchronous `PRIMARY_ONLY` and `FAILOVER` routes.
- Add RabbitMQ only when async jobs are actually implemented.
- Add Nginx/Spring Cloud Gateway only when multiple backend instances are required.
