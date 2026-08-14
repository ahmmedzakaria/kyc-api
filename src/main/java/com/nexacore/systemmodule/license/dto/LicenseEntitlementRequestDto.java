package com.nexacore.systemmodule.license.dto;

import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.license.enums.LicenseOverrideMode;
import lombok.Builder;

@Builder
public record LicenseEntitlementRequestDto(
        Long id,
        Long version,
        String planCode,
        String subscriptionCode,
        LicenseEntitlementType entitlementType,
        Long moduleId,
        Long submoduleId,
        Long featureId,
        Long privilegeId,
        Long apiRegistryId,
        String limitCode,
        Long limitValue,
        LicenseOverrideMode overrideMode,
        Boolean active
) {
}
