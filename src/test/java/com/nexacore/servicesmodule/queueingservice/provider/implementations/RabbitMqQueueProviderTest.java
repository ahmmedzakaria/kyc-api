package com.nexacore.servicesmodule.queueingservice.provider.implementations;

import com.nexacore.servicesmodule.queueingservice.config.QueueingServiceProperties;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;
import com.nexacore.servicesmodule.queueingservice.enums.QueueMessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RabbitMqQueueProviderTest {

    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private QueueingServiceProperties properties;
    private RabbitMqQueueProvider provider;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        amqpAdmin = mock(AmqpAdmin.class);
        properties = new QueueingServiceProperties();
        properties.setAutoDeclare(false);
        properties.setDefaultExchange("");
        properties.setDefaultQueue("nexacore.default");
        properties.setDefaultRoutingKey("nexacore.default");
        provider = new RabbitMqQueueProvider(rabbitTemplate, amqpAdmin, properties);
    }

    @Test
    void publishUsesDefaultsAndSendsMessage() {
        QueueMessageResponse response = provider.publish(QueueMessageRequest.builder()
                .payload(Map.of("requestId", "request-1"))
                .build());

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo(QueueMessageStatus.PUBLISHED);
        assertThat(response.getProviderName()).isEqualTo("rabbitmq");
        assertThat(response.getQueueName()).isEqualTo("nexacore.default");
        assertThat(response.getExchangeName()).isEmpty();
        assertThat(response.getRoutingKey()).isEqualTo("nexacore.default");
        assertThat(response.getMessageId()).isNotBlank();
        assertThat(response.getCorrelationId()).isEqualTo(response.getMessageId());
        verify(rabbitTemplate).convertAndSend(eq(""), eq("nexacore.default"), eq(Map.of("requestId", "request-1")),
                any(MessagePostProcessor.class));
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void publishPreservesMessageMetadataAndHeaders() throws Exception {
        QueueMessageRequest request = QueueMessageRequest.builder()
                .exchangeName("nexacore.esb")
                .routingKey("esb.request.created")
                .queueName("nexacore.esb.requests")
                .messageId("message-1")
                .correlationId("correlation-1")
                .headers(Map.of("routeCode", "REPORT_EXPORT_LARGE"))
                .payload("payload")
                .durable(true)
                .build();

        QueueMessageResponse response = provider.publish(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("message-1");
        assertThat(response.getCorrelationId()).isEqualTo("correlation-1");
        assertThat(response.getExchangeName()).isEqualTo("nexacore.esb");
        assertThat(response.getRoutingKey()).isEqualTo("esb.request.created");

        MessagePostProcessor postProcessor = capturePostProcessor(request);
        Message message = new Message("payload".getBytes(StandardCharsets.UTF_8));
        postProcessor.postProcessMessage(message);

        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("message-1");
        assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo("correlation-1");
        assertThat(message.getMessageProperties().getHeaders()).containsEntry("routeCode", "REPORT_EXPORT_LARGE");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    void publishAutoDeclaresQueueResourcesWhenEnabled() {
        properties.setAutoDeclare(true);

        QueueMessageResponse response = provider.publish(QueueMessageRequest.builder()
                .exchangeName("nexacore.esb")
                .routingKey("esb.request.created")
                .queueName("nexacore.esb.requests")
                .payload("payload")
                .build());

        assertThat(response.isSuccess()).isTrue();
        verify(amqpAdmin).declareQueue(any());
        verify(amqpAdmin).declareExchange(any());
        verify(amqpAdmin).declareBinding(any());
    }

    @Test
    void publishReturnsFailedResponseWhenRabbitTemplateFails() {
        doThrow(new IllegalStateException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(eq(""), eq("nexacore.default"), eq("payload"), any(MessagePostProcessor.class));

        QueueMessageResponse response = provider.publish(QueueMessageRequest.builder()
                .payload("payload")
                .build());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo(QueueMessageStatus.FAILED);
        assertThat(response.getErrorMessage()).isEqualTo("broker unavailable");
    }

    private MessagePostProcessor capturePostProcessor(QueueMessageRequest request) {
        final MessagePostProcessor[] captured = new MessagePostProcessor[1];

        org.mockito.Mockito.doAnswer(invocation -> {
            captured[0] = invocation.getArgument(3);
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(request.getExchangeName()),
                eq(request.getRoutingKey()),
                eq(request.getPayload()),
                any(MessagePostProcessor.class)
        );

        provider.publish(request);
        return captured[0];
    }
}
