package com.nexacore.gatewaymodule.license.dto;

import lombok.Builder;

@Builder
public record LicenseUsageRecordRequestDto(
        Long tenantId,
        Long businessId,
        Long clientApplicationId,
        String subscriptionCode,
        String usageCode,
        String usagePeriod,
        Long incrementBy
) {
}
