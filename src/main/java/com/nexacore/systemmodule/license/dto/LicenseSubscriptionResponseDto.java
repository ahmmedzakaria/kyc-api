package com.nexacore.systemmodule.license.dto;

import com.nexacore.systemmodule.license.enums.LicenseStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LicenseSubscriptionResponseDto(
        Long id,
        String subscriptionCode,
        String planCode,
        Long tenantId,
        Long businessId,
        Long clientApplicationId,
        LicenseStatus status,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime gracePeriodEndsAt,
        boolean autoRenew
) {
}
