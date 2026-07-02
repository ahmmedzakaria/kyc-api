# Mobile Messaging Service

## Purpose

The mobile messaging service centralizes outbound mobile messages for NexaCore workflows. It is designed for OTP, transaction notifications, and general mobile messages while keeping the backend independent from a specific SMS gateway vendor.

The current implementation sends JSON over HTTP POST to a configurable provider endpoint. In local development, messaging is disabled by default and calls return a skipped response instead of contacting a provider.

## Package Structure

```text
com.nexacore.servicesmodule.messagingservice
├── config
│   └── MobileMessagingProperties.java
├── dto
│   ├── MobileMessageRequest.java
│   └── MobileMessageResponse.java
├── enums
│   └── MobileMessageType.java
└── service
    ├── implementations
    │   └── MobileMessagingServiceImpl.java
    └── interfaces
        └── MobileMessagingService.java
```

## Component Responsibilities

### `MobileMessagingService`

Service contract used by other modules. It exposes:

- `sendMessage(MobileMessageRequest request)`: sends any supported mobile message.
- `sendOtp(String mobileNumber, String otp, Duration validity)`: formats and sends an OTP message.
- `sendTransactionInfo(String mobileNumber, String transactionReference, String message)`: sends transaction-related information.

### `MobileMessagingServiceImpl`

Default Spring `@Service` implementation. It:

- validates required request fields.
- skips sending when `messaging.mobile.enabled=false`.
- builds the provider payload.
- adds optional authorization headers.
- sends a JSON HTTP POST request to the configured gateway.
- maps provider status/body into `MobileMessageResponse`.
- masks mobile numbers in logs.

### `MobileMessagingProperties`

Spring configuration holder bound from `messaging.mobile.*` properties. It controls provider URL, auth, sender ID, timeout, and OTP text template.

### DTOs

`MobileMessageRequest` carries outbound message data:

- `mobileNumber`
- `message`
- `messageType`
- `referenceId`
- `metadata`

`MobileMessageResponse` returns provider and delivery attempt data:

- `success`
- `skipped`
- `providerName`
- `mobileNumber`
- `messageType`
- `referenceId`
- `statusCode`
- `providerResponse`
- `errorMessage`

### `MobileMessageType`

Supported message categories:

- `OTP`
- `TRANSACTION_INFO`
- `GENERAL`

## Technical Flow

### General Message Flow

```text
Caller
  |
  | injects MobileMessagingService
  v
sendMessage(request)
  |
  | validate request, mobile number, message, message type
  v
Check messaging.mobile.enabled
  |
  | false
  v
Return skipped MobileMessageResponse

  |
  | true
  v
Check gateway URL
  |
  v
Build JSON payload
  |
  v
Build optional auth header
  |
  v
HTTP POST to messaging.mobile.gateway-url
  |
  v
Return MobileMessageResponse from provider status/body
```

### OTP Flow

```text
sendOtp(mobileNumber, otp, validity)
  |
  | validate OTP
  v
Resolve validity duration, default 5 minutes
  |
  v
Format message with messaging.mobile.otp-template
  |
  v
Build MobileMessageRequest with type OTP
  |
  v
Delegate to sendMessage(...)
```

### Transaction Info Flow

```text
sendTransactionInfo(mobileNumber, transactionReference, message)
  |
  v
Build MobileMessageRequest with type TRANSACTION_INFO
  |
  v
Delegate to sendMessage(...)
```

## Provider Payload

The implementation sends this JSON shape to the configured gateway:

```json
{
  "mobileNumber": "017XXXXXXXX",
  "message": "Your NexaCore OTP is 123456. It will expire in 5 minutes.",
  "messageType": "OTP",
  "referenceId": "OTP",
  "senderId": "NexaCore",
  "metadata": {
    "validityMinutes": 5
  }
}
```

If the provider expects a different payload shape, update `buildGatewayPayload(...)` in `MobileMessagingServiceImpl` or add a provider-specific adapter.

## Configuration

Configuration is defined in `backend/src/main/resources/application.properties`:

```properties
messaging.mobile.enabled=${MOBILE_MESSAGING_ENABLED:false}
messaging.mobile.provider-name=${MOBILE_MESSAGING_PROVIDER:generic}
messaging.mobile.gateway-url=${MOBILE_MESSAGING_GATEWAY_URL:}
messaging.mobile.api-key=${MOBILE_MESSAGING_API_KEY:}
messaging.mobile.auth-header-name=${MOBILE_MESSAGING_AUTH_HEADER_NAME:Authorization}
messaging.mobile.auth-header-prefix=${MOBILE_MESSAGING_AUTH_HEADER_PREFIX:Bearer}
messaging.mobile.sender-id=${MOBILE_MESSAGING_SENDER_ID:NexaCore}
messaging.mobile.timeout-seconds=${MOBILE_MESSAGING_TIMEOUT_SECONDS:10}
messaging.mobile.otp-template=${MOBILE_MESSAGING_OTP_TEMPLATE:Your NexaCore OTP is %s. It will expire in %d minutes.}
```

Docker Compose passes the same environment variables to the backend service.

## Usage Example

```java
@Service
public class ExampleService {

    private final MobileMessagingService mobileMessagingService;

    public ExampleService(MobileMessagingService mobileMessagingService) {
        this.mobileMessagingService = mobileMessagingService;
    }

    public void sendLoginOtp(String mobileNumber, String otp) {
        mobileMessagingService.sendOtp(mobileNumber, otp, Duration.ofMinutes(5));
    }

    public void sendTransactionMessage(String mobileNumber, String transactionId) {
        mobileMessagingService.sendTransactionInfo(
                mobileNumber,
                transactionId,
                "Your transaction " + transactionId + " has been processed."
        );
    }
}
```

## Runtime Behavior

When messaging is disabled:

- no external provider call is made.
- a log entry is written with the masked mobile number.
- response has `success=false`, `skipped=true`, and `statusCode=0`.

When messaging is enabled:

- `messaging.mobile.gateway-url` must be configured.
- `Content-Type: application/json` is always sent.
- if `api-key` and `auth-header-name` are configured, the auth header is added.
- HTTP 2xx provider responses are treated as success.
- non-2xx provider responses return `success=false` and keep the provider response body for diagnostics.

## Extension Notes

- Add provider-specific request mapping by replacing or extending `buildGatewayPayload(...)`.
- Add provider-specific response parsing by enriching `MobileMessageResponse`.
- Add persistence or audit logging around `sendMessage(...)` if delivery attempts must be tracked.
- Add retry/rate-limit behavior outside this service or through a dedicated provider adapter if SMS delivery becomes business-critical.

