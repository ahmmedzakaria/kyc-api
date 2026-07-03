# Queueing Service

`queueingservice` provides a reusable queue abstraction for ESB async requests and future module-level background jobs.

## Structure

```text
servicesmodule/queueingservice
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

## Flow

```text
ESB module or business module
  -> QueueingService
  -> QueueProvider
  -> RabbitMqQueueProvider
  -> RabbitMQ exchange/queue
```

Callers depend on `QueueingService`, not `RabbitMqQueueProvider`.

## Configuration

```properties
queueing.service.enabled=${QUEUEING_SERVICE_ENABLED:false}
queueing.service.provider-name=${QUEUEING_SERVICE_PROVIDER:rabbitmq}
queueing.service.auto-declare=${QUEUEING_SERVICE_AUTO_DECLARE:false}
queueing.service.durable-queue=${QUEUEING_SERVICE_DURABLE_QUEUE:true}
queueing.service.default-exchange=${QUEUEING_SERVICE_DEFAULT_EXCHANGE:}
queueing.service.default-queue=${QUEUEING_SERVICE_DEFAULT_QUEUE:nexacore.default}
queueing.service.default-routing-key=${QUEUEING_SERVICE_DEFAULT_ROUTING_KEY:nexacore.default}

spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
spring.rabbitmq.virtual-host=${RABBITMQ_VIRTUAL_HOST:/}
```

The service is disabled by default so local backend startup does not require RabbitMQ.

## Usage

```java
QueueMessageResponse response = queueingService.publish(
        QueueMessageRequest.builder()
                .exchangeName("nexacore.esb")
                .routingKey("esb.request.created")
                .queueName("nexacore.esb.requests")
                .correlationId(requestId)
                .payload(payload)
                .build()
);
```

## Notes

- Use queue messages for asynchronous integration work, background jobs, report exports, notifications, and retryable ESB requests.
- Do not publish raw OTP, token, card, password, authorization header, Firebase credential, or full PII payloads.
- Use `correlationId` and safe headers to connect queue messages with API logs and OpenTelemetry traces.
- Keep business decisions in the owning module. The queueing service only transports messages.
