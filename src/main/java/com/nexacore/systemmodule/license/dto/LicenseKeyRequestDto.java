package com.nexacore.systemmodule.license.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LicenseKeyRequestDto(
        String subscriptionCode,
        String rawLicenseKey,
        String activationFingerprint,
        LocalDateTime expiresAt
) {
}
