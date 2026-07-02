package com.nexacore.servicesmodule.messagingservice.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.servicesmodule.messagingservice.config.MobileMessagingProperties;
import com.nexacore.servicesmodule.messagingservice.dto.MobileMessageRequest;
import com.nexacore.servicesmodule.messagingservice.dto.MobileMessageResponse;
import com.nexacore.servicesmodule.messagingservice.enums.MobileMessageType;
import com.nexacore.servicesmodule.messagingservice.service.interfaces.MobileMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMessagingServiceImpl implements MobileMessagingService {

    private final MobileMessagingProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public MobileMessageResponse sendMessage(MobileMessageRequest request) {
        validateRequest(request);

        if (!properties.isEnabled()) {
            log.info(
                    "Mobile messaging is disabled. Skipping {} message to {}",
                    request.getMessageType(),
                    maskMobileNumber(request.getMobileNumber())
            );
            return buildSkippedResponse(request);
        }

        if (!StringUtils.hasText(properties.getGatewayUrl())) {
            throw new IllegalStateException("Mobile messaging gateway URL is not configured");
        }

        try {
            String payload = objectMapper.writeValueAsString(buildGatewayPayload(request));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getGatewayUrl()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            buildAuthorizationHeader().ifPresent(header ->
                    requestBuilder.header(header.name(), header.value())
            );

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            if (!success) {
                log.warn(
                        "Mobile message provider {} returned status {} for {} message to {}",
                        properties.getProviderName(),
                        response.statusCode(),
                        request.getMessageType(),
                        maskMobileNumber(request.getMobileNumber())
                );
            }

            return MobileMessageResponse.builder()
                    .success(success)
                    .skipped(false)
                    .providerName(properties.getProviderName())
                    .mobileNumber(request.getMobileNumber())
                    .messageType(request.getMessageType())
                    .referenceId(request.getReferenceId())
                    .statusCode(response.statusCode())
                    .providerResponse(response.body())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mobile message sending was interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send mobile message", e);
        }
    }

    @Override
    public MobileMessageResponse sendOtp(String mobileNumber, String otp, Duration validity) {
        if (!StringUtils.hasText(otp)) {
            throw new IllegalArgumentException("OTP must not be blank");
        }

        long validityMinutes = Optional.ofNullable(validity)
                .filter(value -> !value.isZero() && !value.isNegative())
                .orElse(Duration.ofMinutes(5))
                .toMinutes();

        String message = String.format(properties.getOtpTemplate(), otp.trim(), validityMinutes);

        return sendMessage(MobileMessageRequest.builder()
                .mobileNumber(mobileNumber)
                .message(message)
                .messageType(MobileMessageType.OTP)
                .referenceId("OTP")
                .metadata(Map.of("validityMinutes", validityMinutes))
                .build());
    }

    @Override
    public MobileMessageResponse sendTransactionInfo(String mobileNumber, String transactionReference, String message) {
        return sendMessage(MobileMessageRequest.builder()
                .mobileNumber(mobileNumber)
                .message(message)
                .messageType(MobileMessageType.TRANSACTION_INFO)
                .referenceId(transactionReference)
                .build());
    }

    private Map<String, Object> buildGatewayPayload(MobileMessageRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobileNumber", request.getMobileNumber());
        payload.put("message", request.getMessage());
        payload.put("messageType", request.getMessageType());
        payload.put("referenceId", request.getReferenceId());
        payload.put("senderId", properties.getSenderId());
        payload.put("metadata", request.getMetadata());
        return payload;
    }

    private Optional<AuthHeader> buildAuthorizationHeader() {
        if (!StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(properties.getAuthHeaderName())) {
            return Optional.empty();
        }

        String value = properties.getApiKey().trim();
        if (StringUtils.hasText(properties.getAuthHeaderPrefix())) {
            value = properties.getAuthHeaderPrefix().trim() + " " + value;
        }
        return Optional.of(new AuthHeader(properties.getAuthHeaderName().trim(), value));
    }

    private void validateRequest(MobileMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Mobile message request must not be null");
        }
        if (!StringUtils.hasText(request.getMobileNumber())) {
            throw new IllegalArgumentException("Mobile number must not be blank");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("Mobile message must not be blank");
        }
        if (request.getMessageType() == null) {
            request.setMessageType(MobileMessageType.GENERAL);
        }
    }

    private MobileMessageResponse buildSkippedResponse(MobileMessageRequest request) {
        return MobileMessageResponse.builder()
                .success(false)
                .skipped(true)
                .providerName(properties.getProviderName())
                .mobileNumber(request.getMobileNumber())
                .messageType(request.getMessageType())
                .referenceId(request.getReferenceId())
                .statusCode(0)
                .providerResponse("Mobile messaging is disabled")
                .build();
    }

    private String maskMobileNumber(String mobileNumber) {
        if (!StringUtils.hasText(mobileNumber)) {
            return "";
        }
        String normalized = mobileNumber.trim();
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private record AuthHeader(String name, String value) {
    }
}

