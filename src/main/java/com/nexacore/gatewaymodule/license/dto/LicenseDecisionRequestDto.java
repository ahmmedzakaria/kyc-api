package com.nexacore.gatewaymodule.license.dto;

import lombok.Builder;

@Builder
public record LicenseDecisionRequestDto(
        Long tenantId,
        Long businessId,
        Long clientApplicationId,
        String entitlementType,
        Long moduleId,
        Long submoduleId,
        Long featureId,
        Long privilegeId,
        Long apiRegistryId,
        String moduleCode,
        String submoduleCode,
        String featureTypeCode,
        String featureCode,
        String privilegeCode,
        String apiCode,
        String limitCode,
        Long requestedUsage
) {
}
