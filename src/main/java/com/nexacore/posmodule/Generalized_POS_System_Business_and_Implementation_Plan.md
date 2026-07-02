# POS Module Business And Implementation Plan

## Purpose

The POS module will extend the existing NexaCore backend with a configurable point-of-sale domain for retail, pharmacy, restaurant, service, and wholesale workflows.

This plan is written for the current project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Target module: `com.nexacore.posmodule`
- Authentication and privilege management: `authmodule`
- Shared business/tenant context: `commonmodule.business`
- Shared services: `servicesmodule`
- API response wrappers and common DTOs: `commonmodule`
- Audit and API logging: `logmodule`
- Existing frontend apps: `frontend/` and `privilege-frontend/`

## Business Goals

- Support multiple business types with one configurable POS domain.
- Keep POS features modular so the MVP can start with sales and inventory, then expand into purchases, loyalty, and analytics.
- Reuse the existing NexaCore authentication, SSO, privilege, file, cache, SMS, email, Firebase phone verification, and logging services.
- Keep database naming consistent with module-prefixed table names.

## Target Customers

- Retail shops
- Grocery and supermarkets
- Restaurants and cafes
- Pharmacies
- Fashion stores
- Electronics stores
- Salons and spas
- Service centers
- Small wholesalers

## Business Model

- Free trial per business account
- Monthly subscription by business
- Tiered plans: Starter, Professional, Enterprise
- Optional paid add-ons: SMS, email, analytics, advanced inventory, accounting export, and integrations

## Backend Module Structure

Keep POS as a business module and split it into internal submodules by domain. Shared technical capabilities such as reporting, payment gateway integration, and barcode generation should live in `servicesmodule`, not inside POS.

```text
backend/src/main/java/com/nexacore/posmodule
├── business
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── catalog
├── inventory
├── purchase
├── sales
├── customer
├── reporting
├── settings
├── shared
└── privilege
```

Follow existing module patterns:

- Controllers return `ApiResponse<T>`.
- Services expose interfaces under `service/interfaces`.
- Implementations live under `service/implementations`.
- Entities use explicit `@Table(name = "...")`.
- Repositories stay module-local.
- Privilege definitions are provided to `authmodule` through `ModulePrivilegeProvider`.

POS submodule responsibilities:

- `business`: business, branch, register, shift, cash drawer
- `catalog`: category, brand, product, variant, barcode metadata, unit, tax
- `inventory`: warehouse, stock, stock movement, adjustment
- `purchase`: supplier, purchase, purchase item
- `sales`: sale, sale item, payment record, return, receipt
- `customer`: customer, customer group, loyalty
- `reporting`: POS report query orchestration and report request creation
- `settings`: POS runtime settings
- `shared`: POS-only enums, constants, domain helpers
- `privilege`: POS privilege provider and menu/action definitions

## Shared Service Abstractions

General technical services should live under `servicesmodule` and expose provider-based abstractions. POS submodules should depend on service interfaces, not on concrete vendor/tool implementations.

Rule of thumb:

- `posmodule` owns POS business state, decisions, and database records.
- `servicesmodule` owns reusable technical execution that can be used by POS, KYC, Auth, reporting, or future modules.
- `logmodule` owns API access/error/audit persistence such as `log_api_access_log`; POS should call logging/audit APIs or publish domain events, not duplicate API logging tables.

Recommended shared service modules:

```text
backend/src/main/java/com/nexacore/servicesmodule
├── reportservice
├── paymentservice
├── barcodeservice
├── notificationservice
├── emailservice
├── messagingservice
├── fileservice
├── cacheservice
└── firebaseauthservice
```

## Reusable Component Extraction Matrix

The following POS capabilities should be separated into reusable services instead of being implemented directly inside POS submodules.

| Capability | Shared service | POS consumer | POS owns | Service owns |
| --- | --- | --- | --- | --- |
| PDF/XLSX/CSV report rendering | `servicesmodule.reportservice` | `posmodule.reporting` | report data query, filters, authorization, report request creation | JasperReports or other renderer selection, template rendering, output bytes/metadata |
| Payment execution | `servicesmodule.paymentservice` | `posmodule.sales` | sale totals, sale/payment records, business validation, refund eligibility | provider selection, authorize/capture/refund calls, gateway response normalization |
| Barcode/SKU generation | `servicesmodule.barcodeservice` | `posmodule.catalog`, `posmodule.inventory` | product barcode metadata, uniqueness rules, product assignment | barcode image/value generation, barcode format support, provider/library details |
| Notification orchestration | `servicesmodule.notificationservice` | `posmodule.sales`, `posmodule.customer`, `posmodule.inventory` | notification trigger decision and domain context | template selection, channel selection, calling email/SMS/push transports |
| Receipt delivery | `servicesmodule.emailservice`, `servicesmodule.messagingservice` | `posmodule.sales` | receipt content data, delivery trigger decision | email/SMS formatting transport, provider integration, send response |
| Receipt/product file storage | `servicesmodule.fileservice` | `posmodule.sales`, `posmodule.catalog`, `posmodule.purchase` | ownership context and business references | MinIO/filesystem storage, public URL, read/delete |
| Product/branch/promotion cache | `servicesmodule.cacheservice` | `posmodule.catalog`, `posmodule.sales`, `posmodule.settings` | cache keys and invalidation timing | Redis serialization, TTL, lookup/delete |
| Customer phone verification | `servicesmodule.firebaseauthservice` | `posmodule.customer` | customer onboarding workflow and mapping to customer record | Firebase token verification and phone claim extraction |
| API access/error logging | `logmodule` | all POS controllers/services indirectly | domain context and optional audit details | `log_api_access_log`, error log, audit persistence/filtering |

Do not create POS-specific copies of these services unless the behavior is truly domain-specific. For example, POS should have `pos_payment` records, but the gateway call belongs to `paymentservice`.

## Shared Service Implementation Pattern

Use the same implementation shape for new reusable services:

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
POS submodule -> shared service interface -> provider interface -> provider implementation
```

POS code must not depend on implementation classes such as `JasperReportProvider`, `StripePaymentProvider`, or `ZxingBarcodeProvider`.

### `reportservice`

Purpose: provide a generalized reporting abstraction for PDF, Excel, CSV, HTML, and future dashboard/export tools.

Suggested structure:

```text
servicesmodule/reportservice
├── config
├── dto
├── enums
├── provider
│   ├── interfaces
│   │   └── ReportProvider.java
│   └── implementations
│       └── JasperReportProvider.java
└── service
    ├── interfaces
    │   └── ReportService.java
    └── implementations
        └── ReportServiceImpl.java
```

POS `reporting` submodule prepares POS report data and calls `ReportService`. The shared `reportservice` selects the configured provider and hides JasperReports or future implementations from POS business code.

### `paymentservice`

Purpose: abstract payment execution across cash, manual card, mobile wallet, bank transfer, and future payment gateways.

Suggested structure:

```text
servicesmodule/paymentservice
├── config
├── dto
├── enums
├── provider
│   ├── interfaces
│   │   └── PaymentProvider.java
│   └── implementations
│       ├── CashPaymentProvider.java
│       ├── ManualCardPaymentProvider.java
│       └── MobileWalletPaymentProvider.java
└── service
    ├── interfaces
    │   └── PaymentService.java
    └── implementations
        └── PaymentServiceImpl.java
```

POS `sales` owns sale/payment records and calls `PaymentService` for payment execution. The shared `paymentservice` hides gateway details and normalizes authorize, capture, and refund responses.

### `barcodeservice`

Purpose: generate barcode values and barcode images for products, labels, receipts, and future modules.

Suggested structure:

```text
servicesmodule/barcodeservice
├── config
├── dto
├── enums
├── provider
│   ├── interfaces
│   │   └── BarcodeProvider.java
│   └── implementations
│       └── ZxingBarcodeProvider.java
└── service
    ├── interfaces
    │   └── BarcodeService.java
    └── implementations
        └── BarcodeServiceImpl.java
```

POS `catalog` stores product barcode/SKU metadata and calls `BarcodeService` for generation. The shared `barcodeservice` keeps barcode libraries and format-specific logic out of POS business code.

### `notificationservice`

Purpose: orchestrate notifications across channels such as SMS, email, and future push notifications.

Suggested structure:

```text
servicesmodule/notificationservice
├── config
├── dto
├── enums
├── template
└── service
    ├── interfaces
    │   └── NotificationService.java
    └── implementations
        └── NotificationServiceImpl.java
```

Suggested contract:

```java
public interface NotificationService {
    NotificationResult send(NotificationRequest request);
    NotificationResult sendOtp(NotificationRecipient recipient, String otp, Duration validity);
    NotificationResult sendTransactionInfo(NotificationRecipient recipient, String reference, String message);
}
```

Dependency direction:

```text
posmodule
  -> notificationservice
     -> emailservice
     -> messagingservice
     -> future pushservice
```

POS should call `NotificationService` for workflow-level events such as receipt delivery, low-stock alerts, transaction updates, and customer OTPs. `notificationservice` decides the channel and delegates transport to `emailservice`, `messagingservice`, or future providers.

### `audit/logging`

API access logging already exists in `logmodule` through entities such as `log_api_access_log`. Keep this as a cross-cutting module instead of creating POS-specific API log tables.

POS may add domain audit events for sensitive actions:

- sale voided
- refund approved
- stock adjusted
- cash drawer closed
- discount overridden

Those events should be written through a logging/audit abstraction in `logmodule`, not by duplicating logging logic inside every POS service.

## Database Naming Convention

All POS tables must use the `pos_` prefix.

Examples:

```text
pos_customer
pos_supplier
pos_category
pos_brand
pos_product
pos_product_variant
pos_inventory_item
pos_stock_movement
pos_purchase
pos_purchase_item
pos_sale
pos_sale_item
pos_payment
pos_return
pos_return_item
pos_shift
pos_cash_drawer
pos_promotion
pos_tax_rule
pos_receipt
```

Business and branch are shared across systems and should use `common_` tables, not `pos_` tables:

```text
common_business
common_branch
```

Do not prefix column names only for ownership. Keep relationship columns readable:

```text
business_id
branch_id
product_id
customer_id
sale_id
```

If any table is renamed after data exists, add a migration script. `spring.jpa.hibernate.ddl-auto=update` may create new tables but will not move data from old table names.

## Shared Business And Subscription Model

Business, branch, module enablement, feature enablement, and subscription state should be implemented under `commonmodule.business` because upcoming modules will also need the same business context.

Suggested package:

```text
backend/src/main/java/com/nexacore/commonmodule/business
├── controller
├── dto
├── entity
├── enums
├── repository
└── service
    ├── interfaces
    └── implementations
```

### Table: `common_business`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_code` | `varchar(50)` | Unique external/business code |
| `name` | `varchar(150)` | Business display name |
| `legal_name` | `varchar(200)` | Optional legal name |
| `business_type` | `varchar(50)` | Retail, pharmacy, restaurant, service, wholesale |
| `email` | `varchar(150)` | Primary contact email |
| `mobile_number` | `varchar(30)` | Primary contact mobile |
| `tax_id` | `varchar(80)` | Optional BIN/VAT/TIN/trade identifier |
| `status` | `varchar(30)` | `ACTIVE`, `SUSPENDED`, `CLOSED`, `TRIAL` |
| `timezone` | `varchar(80)` | Example: `Asia/Dhaka` |
| `default_currency` | `varchar(10)` | Example: `BDT` |
| `created_at` | `timestamp` | Audit timestamp |
| `created_by` | `bigint` | Auth user id |
| `updated_at` | `timestamp` | Audit timestamp |
| `updated_by` | `bigint` | Auth user id |

### Table: `common_branch`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_id` | `bigint` | FK to `common_business.id` |
| `branch_code` | `varchar(50)` | Unique per business |
| `name` | `varchar(150)` | Branch name |
| `branch_type` | `varchar(50)` | Store, warehouse, restaurant, virtual |
| `email` | `varchar(150)` | Branch contact email |
| `mobile_number` | `varchar(30)` | Branch contact mobile |
| `address` | `varchar(500)` | Branch address |
| `location_id` | `uuid` | Optional GIS boundary reference |
| `active` | `boolean` | Branch availability |
| `created_at` | `timestamp` | Audit timestamp |
| `created_by` | `bigint` | Auth user id |
| `updated_at` | `timestamp` | Audit timestamp |
| `updated_by` | `bigint` | Auth user id |

### Table: `common_business_module`

Tracks which high-level modules are enabled for a business.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_id` | `bigint` | FK to `common_business.id` |
| `module_code` | `varchar(50)` | Example: `POS`, `KYC`, `CRM`, `ACCOUNTING` |
| `module_name` | `varchar(150)` | Display name |
| `enabled` | `boolean` | Business-level module enablement |
| `enabled_from` | `timestamp` | Start time |
| `enabled_until` | `timestamp` | Optional expiry |
| `source` | `varchar(50)` | `SUBSCRIPTION`, `MANUAL`, `TRIAL` |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_business_sub_module`

Tracks which submodules are enabled inside a module.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_module_id` | `bigint` | FK to `common_business_module.id` |
| `sub_module_code` | `varchar(50)` | Example: `POS_SALES`, `POS_INVENTORY`, `POS_REPORTING` |
| `sub_module_name` | `varchar(150)` | Display name |
| `enabled` | `boolean` | Submodule enablement |
| `enabled_from` | `timestamp` | Start time |
| `enabled_until` | `timestamp` | Optional expiry |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_business_feature`

Tracks feature-level enablement for a business. This is separate from user privileges.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_sub_module_id` | `bigint` | FK to `common_business_sub_module.id` |
| `feature_code` | `varchar(80)` | Example: `POS_REFUND`, `POS_DISCOUNT`, `POS_REPORT_EXPORT` |
| `feature_name` | `varchar(150)` | Display name |
| `enabled` | `boolean` | Feature enablement |
| `limit_key` | `varchar(80)` | Optional limit, e.g. `MAX_BRANCHES`, `MAX_PRODUCTS` |
| `limit_value` | `numeric(19,4)` | Optional numeric entitlement |
| `enabled_from` | `timestamp` | Start time |
| `enabled_until` | `timestamp` | Optional expiry |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_subscription_plan`

Plan catalog shared across businesses.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `plan_code` | `varchar(50)` | Unique code, e.g. `STARTER`, `PRO`, `ENTERPRISE` |
| `name` | `varchar(150)` | Display name |
| `billing_cycle` | `varchar(30)` | `MONTHLY`, `YEARLY`, `TRIAL` |
| `price` | `numeric(19,4)` | Plan price |
| `currency` | `varchar(10)` | Example: `BDT` |
| `active` | `boolean` | Plan availability |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_subscription_plan_feature`

Defines module, submodule, feature, and limits included in a plan.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `plan_id` | `bigint` | FK to `common_subscription_plan.id` |
| `module_code` | `varchar(50)` | Example: `POS` |
| `sub_module_code` | `varchar(50)` | Example: `POS_SALES` |
| `feature_code` | `varchar(80)` | Example: `POS_REFUND` |
| `enabled` | `boolean` | Included or not |
| `limit_key` | `varchar(80)` | Optional limit name |
| `limit_value` | `numeric(19,4)` | Optional limit value |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_business_subscription`

Tracks the current and historical subscription state for a business.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_id` | `bigint` | FK to `common_business.id` |
| `plan_id` | `bigint` | FK to `common_subscription_plan.id` |
| `status` | `varchar(30)` | `TRIAL`, `ACTIVE`, `PAST_DUE`, `CANCELLED`, `EXPIRED` |
| `started_at` | `timestamp` | Subscription start |
| `current_period_start` | `timestamp` | Billing period start |
| `current_period_end` | `timestamp` | Billing period end |
| `trial_end_at` | `timestamp` | Optional trial expiry |
| `cancelled_at` | `timestamp` | Optional cancellation time |
| `auto_renew` | `boolean` | Renewal flag |
| `payment_reference` | `varchar(150)` | Payment gateway/manual billing reference |
| `created_at` | `timestamp` | Audit timestamp |
| `updated_at` | `timestamp` | Audit timestamp |

### Table: `common_business_subscription_event`

Optional audit/event table for subscription lifecycle.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `bigserial` | Primary key |
| `business_subscription_id` | `bigint` | FK to `common_business_subscription.id` |
| `event_type` | `varchar(50)` | `CREATED`, `RENEWED`, `UPGRADED`, `DOWNGRADED`, `CANCELLED`, `EXPIRED` |
| `event_time` | `timestamp` | Event time |
| `details` | `text` | JSON/text details |
| `created_by` | `bigint` | Auth user id |

## Subscription Maintenance Flow

Subscription state should control business module, submodule, and feature availability.

### Provisioning

1. Create or update `common_business`.
2. Create branch records in `common_branch`.
3. Assign a plan in `common_business_subscription`.
4. Read included modules/features from `common_subscription_plan_feature`.
5. Create or update:
   - `common_business_module`
   - `common_business_sub_module`
   - `common_business_feature`

### Runtime Access Check

Before executing POS actions, check three levels:

```text
1. Is the business subscription ACTIVE or TRIAL and not expired?
2. Is the business module/submodule/feature enabled?
3. Does the authenticated user have the required auth privilege?
```

Example for POS refund:

```text
business subscription active
  -> common_business_module: POS enabled
  -> common_business_sub_module: POS_RETURN enabled
  -> common_business_feature: POS_REFUND enabled
  -> auth privilege: POS_RETURN_REFUND allowed
```

### Renewal

On successful renewal:

1. Extend `current_period_start` and `current_period_end`.
2. Set status to `ACTIVE`.
3. Re-apply plan features to module/submodule/feature tables.
4. Write `common_business_subscription_event` with `RENEWED`.

### Upgrade Or Downgrade

On plan change:

1. Update `common_business_subscription.plan_id`.
2. Rebuild feature enablement from `common_subscription_plan_feature`.
3. Preserve manual overrides only when explicitly allowed.
4. Write `UPGRADED` or `DOWNGRADED` event.

### Expiry Or Failed Payment

When subscription expires or payment fails:

1. Set status to `PAST_DUE` or `EXPIRED`.
2. Disable non-grace-period features.
3. Keep read-only access if business policy requires it.
4. Block write operations such as sale creation, purchase creation, refund, and report export if the plan is expired.

### Separation From Auth Privileges

Subscription feature enablement is not the same as user permission.

```text
common_business_feature = what the business has paid for or enabled
auth_privileges = what the current user is allowed to do
```

Both must pass before the action is allowed.

## Core Domain Entities

### Business And Branch

Business and branch are shared concepts and should be owned by `commonmodule.business`, not `posmodule`.

- `Business`: tenant/business account, stored in `common_business`
- `Branch`: physical or virtual outlet, stored in `common_branch`
- `BusinessModule`: enabled modules per business, stored in `common_business_module`
- `BusinessSubModule`: enabled submodules per business, stored in `common_business_sub_module`
- `BusinessFeature`: enabled features per business, stored in `common_business_feature`
- `BusinessSubscription`: current subscription state, stored in `common_business_subscription`
- `SubscriptionPlan`: reusable plan catalog, stored in `common_subscription_plan`
- `SubscriptionPlanFeature`: feature limits/entitlements per plan, stored in `common_subscription_plan_feature`

POS should reference shared business tables:

```text
pos_sale.business_id      -> common_business.id
pos_sale.branch_id        -> common_branch.id
pos_product.business_id   -> common_business.id
pos_inventory.branch_id   -> common_branch.id
```

POS-owned branch/register operations:

- `CashDrawer`: POS cash drawer per shared branch/register
- `Shift`: cashier shift lifecycle per shared branch

### Catalog

- `Category`
- `Brand`
- `Product`
- `ProductVariant`
- `ProductBarcode`: POS-owned barcode metadata and assignment. Actual barcode generation belongs to `barcodeservice`.
- `Unit`
- `TaxRule`

### Inventory

- `InventoryItem`
- `Warehouse`
- `StockMovement`
- `StockAdjustment`
- `Purchase`
- `PurchaseItem`
- `Supplier`

### Sales

- `Sale`
- `SaleItem`
- `Payment`: POS-owned payment record. Payment execution belongs to `paymentservice`.
- `Return`
- `ReturnItem`
- `Receipt`: POS-owned receipt metadata/content source. Rendering/delivery belongs to `reportservice`, `emailservice`, and `messagingservice`.
- `Promotion`
- `Coupon`

### Customer

- `Customer`
- `CustomerGroup`
- `LoyaltyAccount`
- `LoyaltyTransaction`

## Functional Modules

1. POS setup and business settings
2. Branch and register management
3. Product catalog
4. Barcode and SKU management
5. Inventory and stock movement
6. Purchase and supplier management
7. Sales checkout
8. Payment capture
9. Returns and exchanges
10. Customer and loyalty
11. Promotions and coupons
12. Shift and cash drawer management
13. Receipt generation
14. Reporting and dashboard
15. Notifications
16. Audit logging

## Integration With Existing Modules

### `authmodule`

Use the existing authentication and privilege system.

Add `PosPrivilegeProvider` under:

```text
com.nexacore.posmodule.service.implementations
```

It should publish POS menu and action definitions through `ModulePrivilegeProvider`.

Suggested POS privilege features:

```text
POS_DASHBOARD
POS_PRODUCT
POS_INVENTORY
POS_PURCHASE
POS_SALE
POS_RETURN
POS_CUSTOMER
POS_REPORT
POS_SETTINGS
```

Suggested actions:

```text
VIEW
CREATE
UPDATE
DELETE
APPROVE
EXPORT
REFUND
DISCOUNT
```

### `servicesmodule`

Reuse existing shared services:

- `fileservice`: product images, receipt PDFs, imported purchase invoices
- `cacheservice`: product lookup, branch settings, active promotions
- `messagingservice`: SMS OTP, transaction alerts, sale/return notifications
- `emailservice`: email receipts, OTP, transaction alerts
- `notificationservice`: notification orchestration across SMS, email, and future push channels
- `firebaseauthservice`: phone verification where customer onboarding requires Firebase phone auth
- `reportservice`: report rendering abstraction, initially backed by JasperReports
- `paymentservice`: payment execution abstraction for cash, card, wallet, gateway, and refund workflows
- `barcodeservice`: barcode/SKU generation abstraction for catalog, inventory, and receipt workflows

### `logmodule`

The existing API logging filter captures API access and error logs. POS services should add domain-level audit writes for sensitive events if required, such as:

- sale voided
- refund approved
- stock adjusted
- cash drawer closed
- discount overridden

### `commonmodule`

Use shared DTO conventions:

- `ApiResponse<T>`
- `SearchDto`
- `IdRequestDto`
- `ResponseMessage`

## API Design

Use `/api/v1/pos/...` as the route prefix.

Suggested controllers:

```text
PosBusinessController      /api/v1/pos/business
PosBranchController        /api/v1/pos/branches
PosProductController       /api/v1/pos/products
PosInventoryController     /api/v1/pos/inventory
PosPurchaseController      /api/v1/pos/purchases
PosSaleController          /api/v1/pos/sales
PosReturnController        /api/v1/pos/returns
PosCustomerController      /api/v1/pos/customers
PosReportController        /api/v1/pos/reports
PosSettingsController      /api/v1/pos/settings
```

Controller expectations:

- Annotate secured APIs consistently with existing security conventions.
- Validate request DTOs with Jakarta Validation.
- Keep business logic in services, not controllers.
- Return `ResponseEntity<ApiResponse<T>>`.

## MVP Scope

### Phase 1: Foundation

- POS module package structure
- POS privilege provider
- Business and branch setup
- Product category, brand, product, variant
- Basic stock quantity
- Customer record
- Basic sale with sale items and payment
- Receipt response DTO
- Basic reports: daily sales, branch sales, product sales

### Phase 2: Inventory And Purchase

- Supplier management
- Purchase and purchase items
- Stock movement ledger
- Stock adjustment
- Low-stock report
- Barcode/SKU lookup

### Phase 3: Operational POS

- Shift opening and closing
- Cash drawer reconciliation
- Returns and refunds
- Discounts and promotions
- Receipt email/SMS
- Product image upload through `fileservice`

### Phase 4: Advanced

- Offline sync design
- Multi-warehouse
- Loyalty
- Accounting export
- Analytics and forecasting
- Marketplace/integration APIs

## Implementation Order

1. Create `posmodule` package folders.
2. Create reusable `reportservice`, `paymentservice`, `barcodeservice`, and `notificationservice` packages under `servicesmodule`.
3. Add shared service DTOs, enums, interfaces, and provider contracts.
4. Add first provider implementations: `JasperReportProvider`, cash/manual payment providers, and barcode provider.
5. Add POS enums for sale status, payment method, stock movement type, and discount type.
6. Add core POS entities with `pos_` table names.
7. Add POS repositories.
8. Add POS DTOs for create/update/search/detail responses.
9. Add POS service interfaces and implementations.
10. Add POS controllers under `/api/v1/pos`.
11. Add `PosPrivilegeProvider`.
12. Wire POS reporting through `reportservice`.
13. Wire POS payment execution through `paymentservice`.
14. Wire product barcode/SKU generation through `barcodeservice`.
15. Wire POS notification workflows through `notificationservice`.
16. Let `notificationservice` delegate transport to `messagingservice` and `emailservice`.
17. Use `fileservice` for product images, receipt files, and imported purchase documents.
18. Use `cacheservice` for product lookup, branch settings, and active promotions.
19. Use `logmodule` for API access/error logging and domain audit events.
20. Add focused tests for service calculations, provider selection, and controller validation.

## Key Business Rules

- A sale belongs to one branch.
- A sale must contain at least one sale item.
- Product stock must not go negative unless branch settings allow negative stock.
- Every stock-changing operation must create a `pos_stock_movement` record.
- Refunds must reference an original sale.
- Cash payments should affect cash drawer totals.
- Payment totals must equal sale payable amount before a sale is completed.
- Discounts must be either percentage or fixed amount and must not exceed configured limits.

## Reporting

MVP reports:

- Daily sales summary
- Sales by branch
- Sales by cashier
- Sales by product
- Payment method summary
- Stock on hand
- Low stock
- Purchase summary
- Return summary

Implementation approach:

- POS `reporting` submodule builds report data and business-specific parameters.
- Shared `reportservice` renders the output using the configured provider.
- Use JasperReports first for PDF/XLSX operational reports.
- Keep Metabase separate for dashboards and self-service analytics.

## Frontend Fit

The POS UI should be added to the existing Angular frontend unless a separate POS frontend is intentionally created.

Recommended routes:

```text
/pos/dashboard
/pos/sales
/pos/products
/pos/inventory
/pos/purchases
/pos/customers
/pos/reports
/pos/settings
```

Privilege management for POS features should be visible through the existing `privilege-frontend`.

## Security

- Use existing JWT/SSO authentication.
- Protect POS APIs with module privileges.
- Check business subscription and feature entitlement before allowing POS actions.
- Use audit logging for destructive or financial operations.
- Never trust client-side totals. Recalculate totals on the backend.
- Validate payment amount, discount amount, tax amount, and stock movement on the backend.

### Sensitive Data Security

Sensitive data security must be handled in three layers: database, API, and frontend. Frontend masking is useful for user experience, but it is not a security boundary. The backend must always enforce access rules.

Recommended reusable service:

```text
servicesmodule/dataprotectionservice
├── annotation
│   ├── SensitiveField.java
│   └── MaskedField.java
├── config
├── dto
├── enums
└── service
    ├── interfaces
    │   ├── FieldEncryptionService.java
    │   ├── SensitiveAccessAuditService.java
    │   └── SensitiveDataMaskingService.java
    └── implementations
```

Dependency direction:

```text
posmodule
  -> dataprotectionservice
  -> logmodule
```

### Sensitive Data Use Cases

| Use Case | Sensitive Data | Required Protection |
| --- | --- | --- |
| Customer profile | Phone, email, address, date of birth, identity number | Mask by default, decrypt only for privileged users, audit full-view access |
| Customer OTP | OTP value, phone number | Never store raw OTP, hash OTP, expire quickly, rate limit requests |
| Sale receipt delivery | Customer contact, receipt content, transaction reference | Send through `notificationservice`, mask contact values in response/logs |
| Refund approval | Sale reference, payment reference, approver identity | Require privilege, audit approval, block expired subscription access |
| Report export | Sales totals, customer details, payment references | Require export privilege, audit export, apply field masking based on report type |
| Subscription and billing | Business tax id, billing reference, plan status | Restrict to business owner/admin, audit plan changes |
| API access logs | IP address, user agent, request path, user id | Sanitize request/response body, do not log tokens or OTP |
| Payment workflow | Payment reference, gateway response, refund id | Do not store card data, tokenize provider references, mask gateway payloads |

### Database Controls

- Classify sensitive columns before creating tables.
- Encrypt high-risk fields at application level before persistence.
- Store searchable/display-safe values separately when needed.
- Never store raw OTP, password, token, card number, CVV, or Firebase service credentials.
- Store OTP as a hash with expiry time and attempt counter.
- Keep audit columns on sensitive tables: `created_at`, `created_by`, `updated_at`, `updated_by`.
- Add sensitive access audit for full data view/export: `viewed_by`, `viewed_at`, `reason`, `ip_address`.
- Use a least-privilege application database user. Do not run the app with a superuser DB account.

Example sensitive columns:

```text
common_business.tax_id
pos_customer.email
pos_customer.mobile_number
pos_customer.address
pos_payment.payment_reference
pos_refund.approval_reference
```

### API Controls

- Never expose JPA entities directly from POS APIs.
- Use separate DTOs for masked and privileged responses.
- Enforce authorization in the service layer as well as the controller.
- Validate business ownership, branch access, subscription status, feature entitlement, and user privilege.
- Mask sensitive response fields by default:
  - phone: `017******89`
  - email: `z***@mail.com`
  - identity number: show last 4 characters only
  - payment reference: show only provider and last 4 characters
- Do not log request/response bodies that contain OTP, password, token, service-account JSON, card data, or identity data.
- Add rate limiting for OTP, login, report export, and refund APIs.

Recommended API decision flow:

```text
JWT/SSO authenticated
  -> business ownership valid
  -> subscription ACTIVE or TRIAL
  -> module/submodule/feature enabled
  -> user privilege allowed
  -> sensitive data policy applied
  -> masked DTO returned
  -> sensitive access audited if full data was viewed/exported
```

### Frontend Controls

- Do not store OTP, password, Firebase tokens, service credentials, full customer identity, or payment data in `localStorage`.
- Avoid putting sensitive data in URL query parameters.
- Mask customer and payment data by default.
- Show full sensitive data only after backend confirms privilege.
- Require a reason or confirmation before viewing/exporting highly sensitive data.
- Do not print sensitive API responses in browser console logs.
- Disable autocomplete for OTP, password, identity, and payment fields where appropriate.
- Treat Angular route guards as UI protection only. Backend APIs must still enforce every permission.

### Security Gaps To Avoid

| Gap | Risk | Mitigation |
| --- | --- | --- |
| Only hiding buttons in Angular | User can call API directly | Enforce permission and subscription checks in backend service layer |
| Returning entities directly | Passwords, tokens, internal ids, or full PII may leak | Use explicit response DTOs |
| Logging full request bodies | OTP, token, identity, or payment data may enter logs | Sanitize logs and block sensitive keys |
| Storing raw OTP | OTP can be reused if DB leaks | Store OTP hash, expiry, and attempt count |
| Storing service account JSON in source tree | Credential leakage | Use ignored file or environment path and never commit credentials |
| Depending on `ddl-auto=update` for sensitive schema changes | Missing constraints, indexes, or migrations | Use Flyway for controlled schema changes |
| No report export audit | Bulk data leak cannot be traced | Audit export user, time, filters, business, and branch |
| No tenant/business validation | One business may access another business data | Always filter by `business_id` and branch access |
| Unmasked report output | PII leaks through PDF/XLSX/CSV | Apply report-level field policy before rendering |

## Configuration

Use environment-driven properties for POS behavior.

Suggested properties:

```properties
pos.negative-stock-allowed=${POS_NEGATIVE_STOCK_ALLOWED:false}
pos.default-currency=${POS_DEFAULT_CURRENCY:BDT}
pos.receipt-prefix=${POS_RECEIPT_PREFIX:POS}
pos.tax-inclusive-pricing=${POS_TAX_INCLUSIVE_PRICING:false}
pos.low-stock-threshold=${POS_LOW_STOCK_THRESHOLD:5}
```

## Suggested Platform Additions

Adopt new tools in phases so the POS module improves reliability and reporting without adding too much operational complexity at once.

### Now

- Flyway
- Testcontainers
- JasperReports
- `reportservice` abstraction with `JasperReportProvider`
- `paymentservice` abstraction with cash/manual payment providers
- `barcodeservice` abstraction with a barcode-generation provider
- `notificationservice` abstraction over email/SMS notification workflows

### Soon

- Metabase
- Spring Modulith
- additional payment gateway providers
- report export scheduling
- notification templates and recipient preferences

### Later

- OpenTelemetry + Prometheus/Grafana
- RabbitMQ for async jobs
- asynchronous report generation jobs
- asynchronous receipt email/SMS jobs
- push notification provider

## Verification Expectations

For POS backend changes:

```bash
cd backend
mvn test
```

For frontend POS changes:

```bash
cd frontend
npm run build
```

For Docker/config changes:

```bash
docker compose config
```

## Migration Notes

Because this project currently uses `spring.jpa.hibernate.ddl-auto=update`, new POS tables can be created automatically in development. Production should use explicit migrations before enabling POS in a real environment.

Required migration principles:

- Use `pos_` prefix for all POS tables.
- Keep foreign-key names readable and stable.
- Seed initial POS privileges before exposing POS routes in the UI.
- Add indexes for tenant/business, branch, product, sale date, and barcode lookup fields.
