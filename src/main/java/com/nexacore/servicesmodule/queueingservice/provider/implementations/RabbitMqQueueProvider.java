package com.nexacore.servicesmodule.queueingservice.provider.implementations;

import com.nexacore.servicesmodule.queueingservice.config.QueueingServiceProperties;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;
import com.nexacore.servicesmodule.queueingservice.enums.QueueMessageStatus;
import com.nexacore.servicesmodule.queueingservice.provider.interfaces.QueueProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RabbitMqQueueProvider implements QueueProvider {

    private static final String PROVIDER_NAME = "rabbitmq";

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final QueueingServiceProperties properties;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public QueueMessageResponse publish(QueueMessageRequest request) {
        QueueMessageRequest resolvedRequest = resolveRequest(request);

        try {
            if (properties.isAutoDeclare()) {
                declareQueueResources(resolvedRequest);
            }

            rabbitTemplate.convertAndSend(
                    resolvedRequest.getExchangeName(),
                    resolvedRequest.getRoutingKey(),
                    resolvedRequest.getPayload(),
                    createMessagePostProcessor(resolvedRequest)
            );

            return QueueMessageResponse.builder()
                    .success(true)
                    .status(QueueMessageStatus.PUBLISHED)
                    .providerName(providerName())
                    .messageId(resolvedRequest.getMessageId())
                    .correlationId(resolvedRequest.getCorrelationId())
                    .queueName(resolvedRequest.getQueueName())
                    .exchangeName(resolvedRequest.getExchangeName())
                    .routingKey(resolvedRequest.getRoutingKey())
                    .publishedAt(Instant.now())
                    .build();
        } catch (RuntimeException exception) {
            return QueueMessageResponse.builder()
                    .success(false)
                    .status(QueueMessageStatus.FAILED)
                    .providerName(providerName())
                    .messageId(resolvedRequest.getMessageId())
                    .correlationId(resolvedRequest.getCorrelationId())
                    .queueName(resolvedRequest.getQueueName())
                    .exchangeName(resolvedRequest.getExchangeName())
                    .routingKey(resolvedRequest.getRoutingKey())
                    .errorMessage(exception.getMessage())
                    .publishedAt(Instant.now())
                    .build();
        }
    }

    private QueueMessageRequest resolveRequest(QueueMessageRequest request) {
        QueueMessageRequest resolvedRequest = request == null ? new QueueMessageRequest() : request;

        if (!StringUtils.hasText(resolvedRequest.getQueueName())) {
            resolvedRequest.setQueueName(properties.getDefaultQueue());
        }
        if (!StringUtils.hasText(resolvedRequest.getExchangeName())) {
            resolvedRequest.setExchangeName(properties.getDefaultExchange());
        }
        if (!StringUtils.hasText(resolvedRequest.getRoutingKey())) {
            resolvedRequest.setRoutingKey(StringUtils.hasText(properties.getDefaultRoutingKey())
                    ? properties.getDefaultRoutingKey()
                    : resolvedRequest.getQueueName());
        }
        if (!StringUtils.hasText(resolvedRequest.getMessageId())) {
            resolvedRequest.setMessageId(UUID.randomUUID().toString());
        }
        if (!StringUtils.hasText(resolvedRequest.getCorrelationId())) {
            resolvedRequest.setCorrelationId(resolvedRequest.getMessageId());
        }
        return resolvedRequest;
    }

    private void declareQueueResources(QueueMessageRequest request) {
        Queue queue = request.isDurable()
                ? QueueBuilder.durable(request.getQueueName()).build()
                : QueueBuilder.nonDurable(request.getQueueName()).build();

        amqpAdmin.declareQueue(queue);

        if (StringUtils.hasText(request.getExchangeName())) {
            amqpAdmin.declareExchange(new DirectExchange(request.getExchangeName(), true, false));
            amqpAdmin.declareBinding(org.springframework.amqp.core.BindingBuilder
                    .bind(queue)
                    .to(new DirectExchange(request.getExchangeName(), true, false))
                    .with(request.getRoutingKey()));
        }
    }

    private MessagePostProcessor createMessagePostProcessor(QueueMessageRequest request) {
        return (Message message) -> {
            message.getMessageProperties().setMessageId(request.getMessageId());
            message.getMessageProperties().setCorrelationId(request.getCorrelationId());
            message.getMessageProperties().setDeliveryMode(request.isDurable()
                    ? MessageDeliveryMode.PERSISTENT
                    : MessageDeliveryMode.NON_PERSISTENT);

            for (Map.Entry<String, Object> header : request.getHeaders().entrySet()) {
                if (header.getValue() != null) {
                    message.getMessageProperties().setHeader(header.getKey(), header.getValue());
                }
            }
            return message;
        };
    }
}
