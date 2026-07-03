package com.nexacore.servicesmodule.queueingservice.service.implementations;

import com.nexacore.servicesmodule.queueingservice.config.QueueingServiceProperties;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;
import com.nexacore.servicesmodule.queueingservice.enums.QueueMessageStatus;
import com.nexacore.servicesmodule.queueingservice.provider.interfaces.QueueProvider;
import com.nexacore.servicesmodule.queueingservice.service.interfaces.QueueingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueingServiceImpl implements QueueingService {

    private final QueueingServiceProperties properties;
    private final List<QueueProvider> queueProviders;

    @Override
    public QueueMessageResponse publish(QueueMessageRequest request) {
        if (!properties.isEnabled()) {
            return QueueMessageResponse.builder()
                    .success(false)
                    .status(QueueMessageStatus.DISABLED)
                    .providerName(resolveProviderName())
                    .errorMessage("Queueing service is disabled")
                    .publishedAt(Instant.now())
                    .build();
        }

        return resolveProvider().publish(request);
    }

    private QueueProvider resolveProvider() {
        String configuredProvider = resolveProviderName();

        return queueProviders.stream()
                .filter(provider -> provider.providerName().equalsIgnoreCase(configuredProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No queue provider configured for: " + configuredProvider));
    }

    private String resolveProviderName() {
        return StringUtils.hasText(properties.getProviderName())
                ? properties.getProviderName().trim()
                : "rabbitmq";
    }
}
