package com.nexacore.systemmodule.license.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LicenseKeySummaryDto(
        Long id,
        String subscriptionCode,
        String keyPrefix,
        LocalDateTime issuedAt,
        LocalDateTime activatedAt,
        LocalDateTime lastValidatedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        boolean active
) {
}
