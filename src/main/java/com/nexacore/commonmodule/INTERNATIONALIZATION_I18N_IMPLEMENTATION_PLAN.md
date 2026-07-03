# Internationalization (i18n) Business And Implementation Plan

## Purpose

Internationalization will make NexaCore support multiple languages consistently across backend APIs, frontend applications, notifications, reports, validation messages, ESB errors, and future modules.

This plan is written for the current project structure:

- Backend: Spring Boot 3.5, Java 21, Maven project under `backend/`
- Package root: `com.nexacore`
- Shared module: `commonmodule`
- API response wrapper: `commonmodule.dto.ApiResponse`
- Global exception handling: `appconfigmodule.HttpExceptionHandler`
- Existing frontend apps: `frontend/` and `privilege-frontend/`
- Shared services: `servicesmodule`
- Planned ESB module: `esbmodule`

## Business Goals

- Support English and Bangla first.
- Allow future languages without changing business logic.
- Keep translations consistent across backend, frontend, SMS, email, reports, and ESB.
- Avoid hard-coded user-facing text in services, controllers, and validators.
- Return stable message codes from backend so frontend can display localized text reliably.
- Support business-specific language preference later.

## Recommended First Languages

| Language | Locale | Priority |
| --- | --- | --- |
| English | `en` | Default |
| Bangla | `bn` | First localized language |

Future locales can include `ar`, `hi`, or customer-specific country locales when business needs exist.

## Architecture Decision

Use a hybrid model:

| Layer | Responsibility |
| --- | --- |
| Backend | Resolve validation/errors, return message codes and safe fallback messages |
| Frontend | Render UI labels, menus, buttons, static pages, and client-side validation text |
| Notification services | Resolve SMS/email template language before sending |
| Report service | Render report title/labels by selected locale |
| ESB | Route localized messages by passing locale context, not by owning translations |

Backend should not be the only translation source for frontend UI. The frontend should own screen labels and UX text, while backend owns API/business/validation/error messages.

## Locale Resolution

Recommended locale resolution order:

1. Explicit request parameter, for internal/admin use: `?lang=bn`
2. `Accept-Language` HTTP header
3. Authenticated user preference
4. Business/branch default language
5. Application default: `en`

Suggested request examples:

```http
Accept-Language: bn
```

```http
Accept-Language: en-US,en;q=0.9,bn;q=0.8
```

## Backend Module Structure

Add shared i18n components under `commonmodule`:

```text
backend/src/main/java/com/nexacore/commonmodule/i18n
├── config
│   └── I18nConfig.java
├── dto
│   └── LocalizedMessage.java
├── service
│   ├── interfaces
│   │   └── MessageLocalizationService.java
│   └── implementations
│       └── MessageLocalizationServiceImpl.java
└── util
    └── MessageCodeUtil.java
```

Add message bundles under resources:

```text
backend/src/main/resources/i18n
├── messages.properties
├── messages_en.properties
├── messages_bn.properties
├── validation.properties
├── validation_en.properties
└── validation_bn.properties
```

Use `messages.properties` as default fallback.

## Message Code Convention

Use stable message codes. Do not use raw English text as the source of truth.

Format:

```text
{module}.{submodule}.{feature}.{event}
```

Examples:

```text
common.success
common.error.unexpected
auth.login.success
auth.login.invalid_credentials
kyc.person.created
pos.sale.created
esb.route.disabled
queueing.publish.failed
validation.required
validation.invalid_phone
```

Rules:

- Codes must be lowercase.
- Use dots for hierarchy.
- Keep code names stable after frontend/reports start using them.
- Add module prefix for module-specific messages.
- Use `common.*` only for truly shared messages.

## API Response Plan

Current `ApiResponse` returns a list of `ResponseMessage` with text. Keep compatibility, but add message-code support gradually.

Recommended future response shape:

```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": [
    {
      "type": "SUCCESS",
      "code": "kyc.person.created",
      "text": "Person created successfully"
    }
  ],
  "data": {}
}
```

Implementation approach:

1. Add `code` field to `ResponseMessage`.
2. Add helper methods to build responses from message codes.
3. Keep existing string-based helpers during migration.
4. Update controllers/services gradually to use codes instead of hard-coded text.

## Backend Technical Flow

```text
HTTP request
  -> LocaleResolver
  -> controller/service
  -> message code
  -> MessageLocalizationService
  -> MessageSource
  -> ApiResponse with code + localized text
```

For validation:

```text
DTO validation annotation
  -> message code
  -> MethodArgumentNotValidException
  -> HttpExceptionHandler
  -> localized validation message
  -> ApiResponse
```

## Spring Configuration Plan

Suggested backend configuration:

```properties
i18n.default-locale=${I18N_DEFAULT_LOCALE:en}
i18n.supported-locales=${I18N_SUPPORTED_LOCALES:en,bn}
i18n.basename=${I18N_BASENAME:i18n/messages,i18n/validation}
i18n.encoding=${I18N_ENCODING:UTF-8}
i18n.fallback-to-system-locale=${I18N_FALLBACK_TO_SYSTEM_LOCALE:false}
```

Suggested Spring beans:

- `MessageSource`
- `LocaleResolver` or `AcceptHeaderLocaleResolver`
- `LocaleChangeInterceptor` if query parameter language switching is needed
- `MessageLocalizationService`

## Validation Message Plan

Use message codes in DTO validation annotations.

Example:

```java
@NotBlank(message = "{validation.required}")
private String name;

@Pattern(regexp = "...", message = "{validation.invalid_phone}")
private String phoneNumber;
```

Then define:

```properties
validation.required=This field is required
validation.invalid_phone=Invalid phone number
```

```properties
validation.required=এই তথ্যটি আবশ্যক
validation.invalid_phone=ফোন নম্বর সঠিক নয়
```

## Module Responsibilities

| Module | i18n responsibility |
| --- | --- |
| `commonmodule` | Message resolution infrastructure and shared message codes |
| `appconfigmodule` | Locale resolver, interceptors, exception localization wiring |
| `authmodule` | Auth message codes and auth validation messages |
| `kycmodule` | KYC business and validation message codes |
| `gismodule` | GIS/search messages |
| `posmodule` | POS business, receipt, invoice, report, and validation messages |
| `servicesmodule.emailservice` | Localized email subjects/templates |
| `servicesmodule.messagingservice` | Localized SMS templates |
| `servicesmodule.reportservice` | Localized report labels/titles |
| `servicesmodule.queueingservice` | Queue publish/error message codes |
| `esbmodule` | Pass locale context and return localized route/provider errors |

## Notification i18n

SMS and email services should accept locale.

Future DTO additions:

```text
locale
templateCode
templateParameters
```

Example:

```text
templateCode = otp.sms
locale = bn
templateParameters = {otp, expiryMinutes}
```

The service resolves the template:

```text
otp.sms=Your {0} OTP is {1}. It will expire in {2} minutes.
```

Do not build notification text manually inside POS/KYC/Auth services.

## Report i18n

`reportservice` should support locale-aware parameters:

```text
REPORT_LOCALE
REPORT_TITLE
LABEL_TOTAL
LABEL_DATE
LABEL_CUSTOMER
```

POS reporting should prepare data; `reportservice` should resolve report labels or receive localized labels as parameters.

## ESB i18n

ESB should carry locale context in `EsbExecutionContext`.

Add:

```text
locale
messageCode
localizedMessage
```

ESB should not own module translations. It should use `MessageLocalizationService` for ESB-owned messages only, such as:

```text
esb.route.disabled
esb.provider.missing
esb.request.accepted
esb.request.failed
```

When ESB routes to another service, pass locale as safe metadata.

## Frontend Plan

Frontend apps should own UI translations:

```text
frontend
  -> labels, menus, buttons, table headers, forms

privilege-frontend
  -> admin labels, privilege menus, role/user screens
```

Recommended approach:

- Use Angular i18n or a runtime library such as Transloco/ngx-translate.
- Store selected language in user preference and local storage.
- Send `Accept-Language` header on every API request.
- Prefer backend message `code` for API result/error display.
- Use backend `text` as fallback when frontend does not know the code.

Frontend display rule:

```text
if frontend has translation for response.message.code:
  show frontend translation
else:
  show backend localized text
```

## Database Plan

Most translations should live in files first. Add database-backed translations only when admins need runtime-editable text.

Future table:

### `common_i18n_message`

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | bigint | Primary key |
| `message_code` | varchar | Stable message code |
| `locale` | varchar | Locale, for example `en`, `bn` |
| `message_text` | text | Translated message |
| `module_code` | varchar | Owning module |
| `enabled` | boolean | Enables/disables runtime message |
| `created_by` | varchar | User/system actor that created the message |
| `updated_by` | varchar | User/system actor that last updated the message |
| `created_at` | timestamp | Creation time |
| `updated_at` | timestamp | Last update time |

Start with file-based bundles. Move to database-backed overrides only after the translation workflow is clear.

## Security And Privacy

- Do not include secrets, OTPs, full identity values, tokens, card data, passwords, or authorization headers inside message files.
- Do not expose internal exception details through localized messages.
- Keep frontend-facing messages user-safe.
- Log message codes and safe context, not full sensitive text.
- For API errors, return stable codes and sanitized text.

## Implementation Phases

### Phase 1: Backend Foundation

- Add `commonmodule.i18n` package.
- Add `MessageSource` configuration.
- Add `MessageLocalizationService`.
- Add `messages_en.properties` and `messages_bn.properties`.
- Add default `common.*`, `validation.*`, `auth.*`, `kyc.*`, `esb.*`, and `queueing.*` codes.
- Add locale resolution from `Accept-Language`.
- Add unit tests for locale resolution and message lookup.

### Phase 2: API Response Migration

- Add `code` to `ResponseMessage`.
- Add `ApiResponse` helper methods for message codes.
- Keep existing string-message helpers for backward compatibility.
- Update `HttpExceptionHandler` to localize validation and common errors.
- Add unit tests for localized exception responses.

### Phase 3: Module Adoption

- Replace hard-coded backend response messages gradually.
- Start with `authmodule`, `kycmodule`, and `servicesmodule.queueingservice`.
- Add module-specific message keys.
- Add unit tests for each converted service/controller path.

### Phase 4: Notification And Report i18n

- Add locale to SMS/email request DTOs.
- Add template code support.
- Add localized OTP and transaction templates.
- Add report label localization.
- Add unit tests for notification template resolution and report label parameters.

### Phase 5: Frontend i18n

- Add frontend translation library or Angular i18n setup.
- Add English and Bangla JSON/message files.
- Add language switcher.
- Add API interceptor for `Accept-Language`.
- Display backend message codes with frontend fallback.

### Phase 6: Runtime Translation Management

- Add `common_i18n_message` only when runtime-editable messages are required.
- Add admin UI for message override.
- Add cache invalidation for message changes.
- Audit translation changes.

## Unit Test Expectations

Add unit tests for:

- default locale fallback.
- `Accept-Language` locale resolution.
- message lookup success.
- missing message fallback.
- parameterized messages.
- validation message localization.
- `ApiResponse` code + text behavior.
- notification template localization.
- report label localization.
- ESB localized route/provider errors.
- queueing localized publish failure messages.

## Suggested First Message Keys

```properties
common.success=Success
common.error.unexpected=Unexpected error occurred
common.error.validation=Validation failed
common.error.not_found=Resource not found
common.error.method_not_allowed=Method not allowed

validation.required=This field is required
validation.invalid_phone=Invalid phone number
validation.invalid_email=Invalid email address

auth.login.success=Login successful
auth.login.invalid_credentials=Invalid username or password

kyc.person.created=Person created successfully
kyc.person.not_found=Person not found

esb.route.disabled=Integration route is disabled
esb.provider.missing=Integration provider is not configured
esb.request.accepted=Integration request accepted

queueing.publish.success=Message published successfully
queueing.publish.failed=Message publish failed
queueing.service.disabled=Queueing service is disabled
```

Bangla versions should be added in `messages_bn.properties`.

## Decision Summary

- Put i18n infrastructure in `commonmodule`.
- Use `Accept-Language` as the primary request locale mechanism.
- Use backend message codes plus localized fallback text.
- Let frontend own UI text and use backend codes for API results/errors.
- Start with file-based message bundles.
- Add database-backed translations later only if runtime editing is required.
- Include i18n tests as part of every converted module or service.
