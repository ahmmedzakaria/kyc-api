package com.nexacore.servicesmodule.queueingservice.service.implementations;

import com.nexacore.servicesmodule.queueingservice.config.QueueingServiceProperties;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;
import com.nexacore.servicesmodule.queueingservice.enums.QueueMessageStatus;
import com.nexacore.servicesmodule.queueingservice.provider.interfaces.QueueProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueingServiceImplTest {

    @Test
    void publishReturnsDisabledResponseWhenServiceIsDisabled() {
        QueueingServiceProperties properties = new QueueingServiceProperties();
        properties.setEnabled(false);
        properties.setProviderName("rabbitmq");

        QueueingServiceImpl service = new QueueingServiceImpl(properties, List.of(new StubQueueProvider("rabbitmq")));

        QueueMessageResponse response = service.publish(QueueMessageRequest.builder()
                .payload("payload")
                .build());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo(QueueMessageStatus.DISABLED);
        assertThat(response.getProviderName()).isEqualTo("rabbitmq");
        assertThat(response.getErrorMessage()).isEqualTo("Queueing service is disabled");
    }

    @Test
    void publishDelegatesToConfiguredProviderWhenEnabled() {
        QueueingServiceProperties properties = new QueueingServiceProperties();
        properties.setEnabled(true);
        properties.setProviderName("rabbitmq");

        StubQueueProvider rabbitProvider = new StubQueueProvider("rabbitmq");
        QueueingServiceImpl service = new QueueingServiceImpl(properties, List.of(
                new StubQueueProvider("other"),
                rabbitProvider
        ));

        QueueMessageRequest request = QueueMessageRequest.builder()
                .messageId("message-1")
                .payload("payload")
                .build();

        QueueMessageResponse response = service.publish(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo(QueueMessageStatus.PUBLISHED);
        assertThat(response.getProviderName()).isEqualTo("rabbitmq");
        assertThat(response.getMessageId()).isEqualTo("message-1");
        assertThat(rabbitProvider.lastRequest).isSameAs(request);
    }

    @Test
    void publishThrowsWhenConfiguredProviderDoesNotExist() {
        QueueingServiceProperties properties = new QueueingServiceProperties();
        properties.setEnabled(true);
        properties.setProviderName("missing");

        QueueingServiceImpl service = new QueueingServiceImpl(properties, List.of(new StubQueueProvider("rabbitmq")));

        assertThatThrownBy(() -> service.publish(QueueMessageRequest.builder().payload("payload").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No queue provider configured for: missing");
    }

    private static class StubQueueProvider implements QueueProvider {

        private final String providerName;

        private QueueMessageRequest lastRequest;

        private StubQueueProvider(String providerName) {
            this.providerName = providerName;
        }

        @Override
        public String providerName() {
            return providerName;
        }

        @Override
        public QueueMessageResponse publish(QueueMessageRequest request) {
            this.lastRequest = request;
            return QueueMessageResponse.builder()
                    .success(true)
                    .status(QueueMessageStatus.PUBLISHED)
                    .providerName(providerName)
                    .messageId(request.getMessageId())
                    .publishedAt(Instant.now())
                    .build();
        }
    }
}
