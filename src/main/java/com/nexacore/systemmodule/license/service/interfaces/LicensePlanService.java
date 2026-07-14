package com.nexacore.systemmodule.license.service.interfaces;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.dto.LicensePlanRequestDto;
import com.nexacore.systemmodule.license.dto.LicensePlanResponseDto;

import java.util.List;

public interface LicensePlanService {
    LicensePlanResponseDto savePlan(LicensePlanRequestDto request);

    void savePlanEntitlement(LicenseEntitlementRequestDto request);

    List<LicensePlanResponseDto> listPlans();
}
