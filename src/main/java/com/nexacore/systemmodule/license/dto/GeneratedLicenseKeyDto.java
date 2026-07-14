package com.nexacore.systemmodule.license.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GeneratedLicenseKeyDto(
        Long id,
        String subscriptionCode,
        String rawLicenseKey,
        String keyPrefix,
        LocalDateTime expiresAt
) {
}
