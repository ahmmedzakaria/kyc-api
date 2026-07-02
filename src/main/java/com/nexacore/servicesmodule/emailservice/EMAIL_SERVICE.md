# Email Messaging Service

## Purpose

The email messaging service centralizes outbound email notifications for NexaCore workflows. It supports OTP emails, transaction notifications, and general emails through Spring Mail.

Email sending is disabled by default. When disabled, service calls return a skipped response and do not contact SMTP.

## Package Structure

```text
com.nexacore.servicesmodule.emailservice
├── config
│   └── EmailMessagingProperties.java
├── dto
│   ├── EmailMessageRequest.java
│   └── EmailMessageResponse.java
├── enums
│   └── EmailMessageType.java
└── service
    ├── implementations
    │   └── EmailMessagingServiceImpl.java
    └── interfaces
        └── EmailMessagingService.java
```

## Component Responsibilities

### `EmailMessagingService`

Service contract used by other modules. It exposes:

- `sendEmail(EmailMessageRequest request)`: sends any supported email.
- `sendOtp(String emailAddress, String otp, Duration validity)`: formats and sends an OTP email.
- `sendTransactionInfo(String emailAddress, String transactionReference, String message)`: sends transaction-related information.

### `EmailMessagingServiceImpl`

Default Spring `@Service` implementation. It:

- validates recipients, subject, and body.
- skips sending when `messaging.email.enabled=false`.
- uses `JavaMailSender` when email sending is enabled.
- supports `to`, `cc`, `bcc`, text body, and HTML body.
- maps send attempts into `EmailMessageResponse`.

### `EmailMessagingProperties`

Spring configuration holder bound from `messaging.email.*` properties. It controls provider name, sender identity, OTP subject/template, and transaction subject.

## Technical Flow

```text
Caller
  |
  | injects EmailMessagingService
  v
sendEmail(request)
  |
  v
Validate recipients, subject, body, type
  |
  v
Check messaging.email.enabled
  |
  | false
  v
Return skipped EmailMessageResponse

  |
  | true
  v
Resolve JavaMailSender
  |
  v
Build MimeMessage
  |
  v
Send through SMTP
  |
  v
Return EmailMessageResponse
```

## Configuration

Configuration is defined in `backend/src/main/resources/application.properties`:

```properties
messaging.email.enabled=${EMAIL_MESSAGING_ENABLED:false}
messaging.email.provider-name=${EMAIL_MESSAGING_PROVIDER:smtp}
messaging.email.from-address=${EMAIL_FROM_ADDRESS:no-reply@${app.slug}.local}
messaging.email.from-name=${EMAIL_FROM_NAME:${app.name}}
messaging.email.otp-subject=${EMAIL_OTP_SUBJECT:Your ${app.name} OTP}
messaging.email.otp-template=${EMAIL_OTP_TEMPLATE:Your ${app.name} OTP is %s. It will expire in %d minutes.}
messaging.email.transaction-subject=${EMAIL_TRANSACTION_SUBJECT:${app.name} Transaction Update}

spring.mail.host=${SMTP_HOST:}
spring.mail.port=${SMTP_PORT:587}
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${SMTP_STARTTLS_ENABLE:true}
spring.mail.properties.mail.smtp.starttls.required=${SMTP_STARTTLS_REQUIRED:false}
spring.mail.properties.mail.smtp.connectiontimeout=${SMTP_CONNECTION_TIMEOUT:10000}
spring.mail.properties.mail.smtp.timeout=${SMTP_TIMEOUT:10000}
spring.mail.properties.mail.smtp.writetimeout=${SMTP_WRITE_TIMEOUT:10000}
```

Minimum SMTP runtime values:

```properties
EMAIL_MESSAGING_ENABLED=true
EMAIL_FROM_ADDRESS=no-reply@example.com
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=smtp-user
SMTP_PASSWORD=smtp-password
```

## Usage Example

```java
@Service
public class ExampleEmailWorkflow {

    private final EmailMessagingService emailMessagingService;

    public ExampleEmailWorkflow(EmailMessagingService emailMessagingService) {
        this.emailMessagingService = emailMessagingService;
    }

    public void sendLoginOtp(String emailAddress, String otp) {
        emailMessagingService.sendOtp(emailAddress, otp, Duration.ofMinutes(5));
    }

    public void sendTransactionMessage(String emailAddress, String transactionId) {
        emailMessagingService.sendTransactionInfo(
                emailAddress,
                transactionId,
                "Your transaction " + transactionId + " has been processed."
        );
    }
}
```

## Runtime Behavior

When email messaging is disabled:

- no SMTP connection is made.
- response has `success=false` and `skipped=true`.

When email messaging is enabled:

- `JavaMailSender` must be configured through `spring.mail.*`.
- text and HTML bodies are supported.
- failed SMTP delivery raises an `IllegalStateException`.
