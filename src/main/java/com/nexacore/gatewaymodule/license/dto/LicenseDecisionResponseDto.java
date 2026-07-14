package com.nexacore.gatewaymodule.license.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LicenseDecisionResponseDto(
        boolean allowed,
        String decisionCode,
        String messageCode,
        String fallbackMessage,
        String licenseStatus,
        String subscriptionCode,
        String planCode,
        String deniedReason,
        LocalDateTime expiresAt,
        Long remainingUsage
) {
}
