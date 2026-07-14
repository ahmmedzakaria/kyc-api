package com.nexacore.systemmodule.license.dto;

import com.nexacore.systemmodule.license.enums.BillingCycle;
import com.nexacore.systemmodule.license.enums.LicensePlanType;
import lombok.Builder;

@Builder
public record LicensePlanResponseDto(
        Long id,
        String planCode,
        String planName,
        LicensePlanType planType,
        BillingCycle billingCycle,
        Integer trialDays,
        String description,
        boolean active
) {
}
