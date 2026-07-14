package com.nexacore.gatewaymodule.license.service.interfaces;

import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;
import com.nexacore.gatewaymodule.license.dto.LicenseUsageRecordRequestDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

public interface LicenseModuleGateway extends ModuleGateway {
    LicenseDecisionResponseDto checkFeatureAccess(LicenseDecisionRequestDto request);

    LicenseDecisionResponseDto checkApiAccess(LicenseDecisionRequestDto request);

    LicenseDecisionResponseDto checkLimit(LicenseDecisionRequestDto request);

    void recordUsage(LicenseUsageRecordRequestDto request);
}
