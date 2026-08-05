# License Submodule Business And Implementation Plan

## Purpose

The license submodule will extend `systemmodule` with platform-owned license management for clients, tenants, and businesses. It will define subscription plans, license keys, entitlements, activation state, limits, renewal dates, and enforcement decisions that other modules can consume.

This plan is written for the current backend project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Target module: `com.nexacore.systemmodule.license`
- Existing client access package: `com.nexacore.systemmodule.accesscontrol`
- Existing privilege package: `com.nexacore.systemmodule.privilege`
- API response wrappers and common DTOs: `commonmodule`
- API access/error/audit logging: `logmodule`
- Flyway migration location: `src/main/resources/db/migration/system`

## Business Goals

- Provide centralized license management for SaaS clients, tenants, businesses, and frontend client applications.
- Support trial, paid, suspended, expired, and enterprise contract licenses.
- Control module, submodule, feature, API, branch, user, device, and usage entitlements from backend configuration.
- Integrate license checks with existing client access and user privilege checks.
- Allow commercial plans to evolve without duplicating license logic inside POS, KYC, Auth, GIS, or future modules.
- Keep sensitive license keys and activation credentials hashed and out of logs.

## Ownership Boundary

License management belongs in `systemmodule` because it is platform governance, not business-domain behavior.

```text
systemmodule/license owns:
- license plans
- license subscriptions
- license keys and activation state
- tenant/business entitlement records
- license limits and usage snapshots
- license validation and enforcement decisions
- license audit events
```

Business modules consume license decisions but do not own license rules.

```text
business module -> LicenseModuleGateway -> systemmodule/license/api/SystemLicenseModuleGateway -> LicenseDecisionService -> license repository/domain
accesscontrol filter -> LicenseDecisionService -> license repository/domain
privilege context -> license entitlements -> visible modules/features
```

Business modules must communicate with license through a module gateway. The gateway contract should live in `gatewaymodule`, and the implementation should live in `systemmodule.license.api`. `LicenseDecisionService` remains an internal license service used by the gateway implementation, filters, and license-owned controllers.

Recommended gateway structure:

```text
gatewaymodule/license/service/interfaces/LicenseModuleGateway.java
gatewaymodule/license/dto/LicenseDecisionRequestDto.java
gatewaymodule/license/dto/LicenseDecisionResponseDto.java

systemmodule/license/api/SystemLicenseModuleGateway.java
systemmodule/license/service/interfaces/LicenseDecisionService.java
systemmodule/license/service/interfaces/LicenseUsageService.java
```

Dependency direction:

```text
posmodule / kycmodule / gismodule
  -> gatewaymodule.license.service.interfaces.LicenseModuleGateway
  -> systemmodule.license.api.SystemLicenseModuleGateway
  -> LicenseDecisionService
  -> sys_license_* tables
```

Gateway methods should be use-case specific and DTO-based. Do not expose entities or repositories through the gateway.

Suggested gateway methods:

```java
LicenseDecisionResponseDto checkFeatureAccess(LicenseDecisionRequestDto request);
LicenseDecisionResponseDto checkApiAccess(LicenseDecisionRequestDto request);
LicenseDecisionResponseDto checkLimit(LicenseDecisionRequestDto request);
void recordUsage(LicenseUsageRecordRequestDto request);
```

Business modules still own their domain workflows. They decide when a license decision is needed, pass the correct tenant/business/client/module/feature/limit context to `LicenseModuleGateway`, and enforce the returned decision before changing domain state.

Business modules should:

- Keep their own domain entities, repositories, services, controllers, and validation rules.
- Publish their privilege/menu metadata through `ModulePrivilegeProvider`.
- Check license decisions through `LicenseModuleGateway` before licensed actions, premium features, writes, exports, or limit-consuming operations.
- Record domain records only after the license decision allows the action.
- Pass safe usage increments through the license gateway when an operation consumes a licensed quota.
- Return explicit business errors using message codes from the license decision when access is denied.

Business modules should not:

- Store license plans, license subscriptions, license keys, entitlement rules, or commercial plan limits in module-owned tables.
- Rebuild module/submodule/feature enablement tables inside the business module.
- Decide expiry, grace-period, suspension, renewal, or plan-upgrade rules locally.
- Depend directly on `LicenseDecisionService`, `LicenseUsageService`, concrete license implementations, or license persistence classes.

### Business Module Example: POS Sale Creation

POS owns the sale workflow, but the license submodule owns the commercial decision.

```text
POS request: create sale
  -> POS validates business_id, branch_id, cashier shift, cart lines, stock, tax, payment input
  -> POS asks LicenseModuleGateway:Create BPMN
       tenant_id / business_id
       module: POS
       feature: Sales
       action: Create
       limit: MONTHLY_INVOICES
  -> LicenseModuleGateway returns allow/deny with status, message code, and remaining usage
  -> POS denies the request if license is expired, suspended, unlicensed, or invoice quota is exceeded
  -> POS creates pos_sale, pos_sale_item, pos_payment only when allowed
  -> POS records invoice usage through LicenseModuleGateway
```

POS still owns POS-specific rules:

- product exists and belongs to the business
- branch has stock
- cashier shift is open
- discounts and taxes are valid
- payment total matches sale total
- receipt data is generated from sale records

The license submodule owns license-specific rules:

- POS module is included in the plan
- Sales feature is entitled
- subscription is active, trial, or grace-period allowed
- business has not exceeded invoice, branch, user, device, or export limits
- suspended or revoked licenses block writes

The same pattern applies to other business modules:

| Business module | Owns | License check examples |
| --- | --- | --- |
| `posmodule` | sales, inventory, purchases, returns, cash drawer, POS reports | POS module, return feature, branch limit, invoice quota, report export quota |
| `kycmodule` | person profile, KYC documents, verification workflow | KYC module, verification feature, monthly KYC case limit, document storage limit |
| `gismodule` | locations, boundaries, GIS lookups | GIS module, map layer feature, geocode/API quota |
| Future modules | module-specific domain data and workflows | module entitlement, premium features, usage limits |

## Relationship With Client Access

The existing `systemmodule.accesscontrol` package controls whether a client application can call a backend API. The new license submodule should control whether the tenant, business, or client is commercially entitled to use that API or feature.

Request enforcement should use this order:

1. Validate public API status.
2. Validate frontend or integration client identity through `privilege.accesscontrol`.
3. Resolve tenant and business context.
4. Validate license status and effective dates.
5. Validate licensed module, feature, API, and usage limits.
6. Validate authenticated user JWT where required.
7. Validate user privilege.
8. Apply tenant, business, and branch filtering.

Client access and license access must stay separate:

| Concern | Owner | Example |
| --- | --- | --- |
| Client identity | `systemmodule.accesscontrol` | `WEB`, `POS`, `MOBILE`, partner API key |
| Commercial entitlement | `systemmodule.license` | tenant has POS Professional until 2027-01-31 |
| User authorization | `systemmodule.privilege` and `authmodule` | cashier can create sale |

## Proposed Package Structure

Add a new package under `systemmodule`:

```text
com.nexacore.systemmodule.license
├── controller
├── dto
├── entity
├── enums
├── repository
├── service
│   ├── interfaces
│   └── implementations
└── scheduler
```

Recommended class responsibilities:

| Package | Responsibility |
| --- | --- |
| `controller` | Admin APIs for plans, license assignment, activation, suspension, renewal, and usage lookup |
| `dto` | Request and response DTOs only; controllers must not return entities |
| `entity` | `sys_` prefixed license tables |
| `enums` | License status, plan type, billing cycle, limit type, entitlement type |
| `repository` | System database repositories |
| `service/interfaces` | Public license contracts consumed by filters, privilege context, and business modules |
| `service/implementations` | License validation, entitlement resolution, lifecycle updates |
| `scheduler` | Expiry checks, grace-period transitions, usage snapshot rollups |

## Core Concepts

### Plan

A plan is a reusable commercial package such as Trial, Starter, Professional, Enterprise, POS Starter, or KYC Enterprise.

Plan examples:

```textnextBytes
TRIAL_14_DAYSnextBytes
POS_STARTER_MONTHLY
POS_PROFESSIONAL_MONTHLY
KYC_ENTERPRISE_YEARLY
PLATFORM_ENTERPRISE_CUSTOM
```

### Subscription

A subscription assigns a plan to a tenant, business, or client application for a time window. One tenant may have a platform subscription, and one business may have an additional POS-specific subscription.

### Entitlement

An entitlement enables a module, submodule, feature, API, or limit.

Examples:

```textPOS_SALES
MODULE: POS
SUBMODULE: POS_SALES
FEATURE: POS_SALES_RETURN
API: POST /pos/sales/create
LIMIT: MAX_BRANCHES = 5
LIMIT: MAX_USERS = 25
LIMIT: MONTHLY_INVOICES = 1000
```

### License Key

A license key is an activation credential for a tenant, business, client application, or offline installation. Store only the hash of the key. Return the raw key only once when generated.

### Usage

Usage records track consumed limits such as active users, branches, devices, invoices, API calls, storage, SMS quota, or report exports.

## License Lifecycle

Supported license statuses:

```text
DRAFT
ACTIVE
TRIAL
GRACE_PERIOD
EXPIRED
SUSPENDED
CANCELLED
REVOKED
```

Lifecycle rules:

- `DRAFT`: plan or subscription prepared but not active.
- `TRIAL`: temporary access with fixed start and end dates.
- `ACTIVE`: paid or approved access within effective dates.
- `GRACE_PERIOD`: access after expiry with limited duration and optional feature restrictions.
- `EXPIRED`: effective end date has passed and grace period is over.
- `SUSPENDED`: manually blocked for billing, abuse, compliance, or admin decision.
- `CANCELLED`: customer or admin ended the subscription.
- `REVOKED`: license key or subscription is invalidated and must not be accepted again.

## Database Design

Use the `sys_` table prefix because license management is system-level platform configuration.

All persistent tables must include:

- `created_by`
- `updated_by`
- `created_at`
- `updated_at`

### `sys_license_plans`

Stores reusable commercial plans.

```text
id
plan_code
plan_name
plan_type
billing_cycle
trial_days
description
active
created_by
updated_by
created_at
updated_at
```

Suggested `plan_type` values:

```text
TRIAL
STANDARD
ENTERPRISE
CUSTOM
INTERNAL
```

Suggested `billing_cycle` values:

```text
NONE
MONTHLY
QUARTERLY
YEARLY
CUSTOM
```

### `sys_license_plan_entitlements`

Stores default entitlements for a plan.

```text
id
license_plan_id
entitlement_type
module_id
submodule_id
feature_id
privilege_id
api_registry_id
limit_code
limit_value
active
created_by
updated_by
created_at
updated_at
```

Suggested `entitlement_type` values:

```text
MODULE
SUBMODULE
FEATURE
ACTION
API
LIMIT
ADD_ON
```

When the entitlement maps to an API, use `sys_priv_api_registry.id`. When it maps to module, submodule, feature, or action access, use the normalized privilege catalog IDs instead of copying code/name values into license tables.

Use the normalized privilege catalog instead of duplicating module, submodule, and feature names:

| Entitlement type | Reference |
| --- | --- |
| `MODULE` | `sys_priv_modules.id` |
| `SUBMODULE` | `sys_priv_submodules.id` |
| `FEATURE` | `sys_priv_features.id` |
| `ACTION` | `sys_priv_privileges.id` |
| `API` | `sys_priv_api_registry.id` |
| `LIMIT` | `limit_code` and `limit_value` |

### `sys_license_subscriptions`

Assigns a license plan to a tenant, business, or client application.

```text
id
subscription_code
license_plan_id
tenant_id
business_id
client_application_id
status
starts_at
expires_at
grace_period_ends_at
auto_renew
cancelled_at
suspended_at
suspension_reason
metadata_json
created_by
updated_by
created_at
updated_at
```

Rules:

- At least one of `tenant_id`, `business_id`, or `client_application_id` must be present.
- A business-level license can override or extend tenant-level entitlements.
- `client_application_id` should reference `sys_priv_client_applications.id` when licensing a specific frontend or integration client.

### `sys_license_keys`

Stores license activation credentials.

```text
id
license_subscription_id
license_key_hash
key_prefix
activation_fingerprint_hash
issued_at
activated_at
last_validated_at
expires_at
revoked_at
active
created_by
updated_by
created_at
updated_at
```

Security rules:

- Never store raw license keys.
- Never log raw license keys, activation fingerprints, machine identifiers, tokens, headers, or full request payloads.
- Show raw generated license keys only once in the response DTO.
- Use a short non-sensitive `key_prefix` for support lookup.

### `sys_license_entitlement_overrides`

Stores subscription-specific additions, removals, or limit changes.

```text
id
license_subscription_id
entitlement_type
module_id
submodule_id
feature_id
privilege_id
api_registry_id
limit_code
limit_value
override_mode
active
created_by
updated_by
created_at
updated_at
```

Suggested `override_mode` values:

```text
ALLOW
DENY
LIMIT_OVERRIDE
```

### `sys_license_usage_snapshots`

Stores measured license usage for limit checks and reporting.

```text
id
license_subscription_id
tenant_id
business_id
usage_period
usage_code
usage_value
measured_at
created_by
updated_by
created_at
updated_at
```

Suggested `usage_code` values:

```text
ACTIVE_USERS
ACTIVE_BRANCHES
REGISTERED_DEVICES
MONTHLY_INVOICES
MONTHLY_API_CALLS
STORAGE_MB
SMS_SENT
REPORT_EXPORTS
```

### `sys_license_audit_events`

Stores safe license lifecycle events.

```text
id
license_subscription_id
event_type
event_message_code
actor_user_id
tenant_id
business_id
client_application_id
safe_context_json
created_by
updated_by
created_at
updated_at
```

Do not store sensitive payloads in `safe_context_json`.

## Service Contracts

Expose license behavior through interfaces under `service/interfaces`.

### `LicensePlanService`

Responsibilities:

- Create and update plans.
- Configure plan entitlements.
- Enable or disable plans.
- Return plan DTOs for admin screens.

### `LicenseSubscriptionService`

Responsibilities:

- Assign a plan to a tenant, business, or client application.
- Start trials.
- Renew, cancel, suspend, reactivate, and expire subscriptions.
- Apply entitlement overrides.

### `LicenseKeyService`

Responsibilities:

- Generate license keys.
- Hash and store license keys.
- Activate license keys.
- Validate license keys without exposing raw values.
- Revoke compromised or inactive keys.

### `LicenseDecisionService`

Responsibilities:

- Validate whether a tenant, business, client application, module, feature, action, or API is licensed.
- Return a structured decision DTO with allow/deny status, message code, and safe fallback message.
- Resolve plan entitlements plus subscription overrides.
- Apply effective date, status, and usage limit checks.

Suggested decision DTO fields:

```text
allowed
decisionCode
messageCode
fallbackMessage
licenseStatus
subscriptionCode
planCode
deniedReason
expiresAt
remainingUsage
```

### `LicenseUsageService`

Responsibilities:

- Record usage increments.
- Recalculate usage snapshots.
- Validate hard and soft limits.
- Provide admin usage reports.

## API Contract

All APIs should use explicit request and response DTOs and return `ApiResponse<T>`.

Suggested admin endpoints:

```text
POST /system/license/plan/save
POST /system/license/plan/list
POST /system/license/plan/details
POST /system/license/plan/entitlement/save
POST /system/license/subscription/assign
POST /system/license/subscription/list
POST /system/license/subscription/details
POST /system/license/subscription/renew
POST /system/license/subscription/suspend
POST /system/license/subscription/reactivate
POST /system/license/subscription/cancel
POST /system/license/key/generate
POST /system/license/key/activate
POST /system/license/key/validate
POST /system/license/usage/list
POST /system/license/decision/check
```

Do not make business modules call admin controllers or internal license services. Business modules should depend on `LicenseModuleGateway`; the gateway implementation can delegate to `LicenseDecisionService` inside `systemmodule.license`.

## Privilege Model

Add system privilege metadata for license administration.

Recommended module registry update:

| Module | Code | Note |
| --- | --- | --- |
| System | `05` | Platform governance features such as privilege, client access, and license |

Recommended submodule:

| Module | Submodule | Code |
| --- | --- | --- |
| System | License | `03` |

Recommended features:

| Feature Type | Feature | Code |
| --- | --- | --- |
| Setup | License Plan | `001` |
| Setup | Plan Entitlement | `002` |
| Operations | License Subscription | `001` |
| Operations | License Key | `002` |
| Operations | License Activation | `003` |Create BPMN
| Reports | License Usage | `001` |
| Reports | License Audit | `002` |

Use existing actions such as Create, Update, Delete, View, Search, and Approve where appropriate. Add new actions only if the existing action catalog cannot express the workflow.

## BPMN
BPMN (Business Process Model and Notation) is an international standard (maintained by the Object Management Group (OMG)) for visually modeling business processes. It provides a common language that business analysts, developers, and stakeholders can all understand.

Instead of writing workflow logic entirely in code, you draw a process diagram, and workflow engines like Camunda, Flowable, and Activiti can execute it.

## Enforcement Points

### Client API Filter

Extend `ClientApiAccessFilter` or the client access decision flow so licensed APIs are checked after client identity is validated and before user privilege validation.

The filter should deny:

- missing tenant or business context for licensed APIs
- expired license
- suspended license
- unlicensed module or API
- exceeded hard usage limit

### Privilege Context

`PrivilegeServiceImpl#getApplicationContext` should hide or mark unavailable menu entries when the client or tenant license does not include the feature. This keeps frontend menus aligned with commercial entitlements.

### Business Services

Business modules should call `LicenseModuleGateway` for domain-specific limits that cannot be reliably enforced at the filter layer.

Examples:

- POS branch creation checks `MAX_BRANCHES`.
- Auth user creation checks `MAX_USERS`.
- POS invoice creation checks `MONTHLY_INVOICES`.
- Report export checks `REPORT_EXPORTS`.Create BPMN

## Internationalization

Avoid hard-coded user-facing response and validation text.

Suggested message codes:

```text
system.license.plan.created
system.license.subscription.assigned
system.license.subscription.expired
system.license.subscription.suspended
system.license.key.generated
system.license.key.activated
system.license.key.invalid
system.license.entitlement.denied
system.license.limit.exceeded
```

Add or update message bundle entries when implementation introduces user-facing messages.

## Security And Logging Rules

- Do not log raw license keys.
- Do not log authorization headers, API keys, client secrets, activation fingerprints, or full PII payloads.
- Hash license keys before persistence.
- Prefer short key prefixes for admin lookup.
- Enforce backend authorization even if frontend hides unlicensed menus.
- Return explicit DTOs from controllers.
- Store safe license lifecycle events in `sys_license_audit_events`.
- Send API access logs and error logs through `logmodule`; do not duplicate API logging tables in license.

## Migration Plan

Create a Flyway migration under:

```text
src/main/resources/db/migration/system
```

Suggested next migration:

```text
V4__add_license_management.sql
```

Migration responsibilities:

- Create all `sys_license_*` tables.
- Add indexes for subscription lookup by `tenant_id`, `business_id`, `client_application_id`, `status`, and `expires_at`.
- Add uniqueness constraints for plan code and subscription code.
- Add foreign keys to `sys_priv_client_applications`, `sys_priv_api_registry`, and `sys_priv_privileges` where applicable.
- Seed default Trial, Starter, Professional, and Enterprise plans only if the business wants bootstrap data in migration.

Do not rely on `spring.jpa.hibernate.ddl-auto=update` for production schema evolution.

## Implementation Phases

### Phase 1: Domain And Schema

- Add `systemmodule/license` package structure.
- Add enums for status, plan type, billing cycle, entitlement type, override mode, and usage code.
- Add JPA entities with explicit `@Table(name = "...")`.
- Add repositories.
- Add Flyway migration for `sys_license_*` tables.

### Phase 2: Plan And Subscription Management

- Implement `LicensePlanService`.
- Implement `LicenseSubscriptionService`.
- Add admin controllers for plan and subscription workflows.
- Add DTO validation and explicit response DTOs.
- Add system privilege definitions for license administration.

### Phase 3: License Key And Activation

- Implement `LicenseKeyService`.
- Generate one-time raw license keys.
- Store only hashes and safe key prefixes.
- Add activation and validation APIs.
- Add audit events for generation, activation, validation failure, and revocation.

### Phase 4: Enforcement Integration

- Implement `LicenseDecisionService`.
- Integrate license decisions into client access checks.
- Integrate license filtering into application context/menu response.
- Add business-service limit checks for user, branch, invoice, and report export limits.

### Phase 5: Usage Tracking And Automation

- Implement `LicenseUsageService`.
- Add scheduled expiry transitions.
- Add scheduled usage snapshot recalculation.
- Add warning events for licenses approaching expiry or usage thresholds.
- Prepare notification integration through reusable notification/email/messaging services when available.

## Testing Plan

Add unit tests under the matching package in `src/test/java`.

Required test coverage:

- Plan creation and validation.
- Entitlement resolution from plan defaults.
- Subscription overrides for allow, deny, and limit override.
- Trial activation and expiry.
- Grace-period transitions.
- Suspended, expired, cancelled, and revoked access denial.
- License key generation stores only hashed values.
- License key activation rejects invalid or revoked keys.
- Decision service allows licensed APIs and denies unlicensed APIs.
- Usage limit checks for hard limits.
- Client access integration denies requests before user privilege checks when license is invalid.
- Sensitive values are not included in audit or log DTOs.

Run the narrowest meaningful checks after implementation:

```bash
mvn test
```

## Open Decisions

- Confirm whether tenant identity is a first-class table or still represented by business/client context only.
- Confirm whether licenses are sold at tenant level, business level, client application level, or a combination.
- Confirm whether offline license activation is required for on-premise deployments.
- Confirm whether billing and invoices belong in this license submodule or a future billing submodule.
- Confirm exact module and submodule codes before seeding privileges.
- Confirm whether POS limits such as branches, registers, products, invoices, and devices should be included in default bootstrap plans.
