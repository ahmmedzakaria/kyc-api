package com.nexacore.systemmodule.license.dto;

import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.license.enums.LicenseOverrideMode;
import lombok.Builder;

@Builder
public record LicenseEntitlementResponseDto(
        Long id,
        long version,
        String ownerCode,
        LicenseEntitlementType entitlementType,
        Long moduleId,
        Long submoduleId,
        Long featureId,
        Long privilegeId,
        Long apiRegistryId,
        String limitCode,
        Long limitValue,
        LicenseOverrideMode overrideMode,
        boolean active
) {
}
