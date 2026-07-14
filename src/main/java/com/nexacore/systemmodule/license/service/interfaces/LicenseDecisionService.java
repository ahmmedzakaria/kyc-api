package com.nexacore.systemmodule.license.service.interfaces;

import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;

public interface LicenseDecisionService {
    LicenseDecisionResponseDto check(LicenseDecisionRequestDto request);
}
