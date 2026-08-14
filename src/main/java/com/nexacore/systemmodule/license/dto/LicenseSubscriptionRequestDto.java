package com.nexacore.systemmodule.license.dto;

import com.nexacore.systemmodule.license.enums.LicenseStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LicenseSubscriptionRequestDto(
        Long id,
        Long version,
        String subscriptionCode,
        String planCode,
        Long tenantId,
        Long businessId,
        Long clientApplicationId,
        LicenseStatus status,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime gracePeriodEndsAt,
        Boolean autoRenew,
        String metadataJson
) {
}
