package com.nexacore.systemmodule.license.api;

import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;
import com.nexacore.gatewaymodule.license.dto.LicenseUsageRecordRequestDto;
import com.nexacore.gatewaymodule.license.service.interfaces.LicenseModuleGateway;
import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.license.service.interfaces.LicenseDecisionService;
import com.nexacore.systemmodule.license.service.interfaces.LicenseUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SystemLicenseModuleGateway implements LicenseModuleGateway {

    private final LicenseDecisionService licenseDecisionService;
    private final LicenseUsageService licenseUsageService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LicenseDecisionResponseDto checkFeatureAccess(LicenseDecisionRequestDto request) {
        return licenseDecisionService.check(withType(request, LicenseEntitlementType.FEATURE));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LicenseDecisionResponseDto checkApiAccess(LicenseDecisionRequestDto request) {
        return licenseDecisionService.check(withType(request, LicenseEntitlementType.API));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LicenseDecisionResponseDto checkLimit(LicenseDecisionRequestDto request) {
        return licenseDecisionService.check(withType(request, LicenseEntitlementType.LIMIT));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void recordUsage(LicenseUsageRecordRequestDto request) {
        licenseUsageService.recordUsage(request);
    }

    private LicenseDecisionRequestDto withType(LicenseDecisionRequestDto request, LicenseEntitlementType entitlementType) {
        return LicenseDecisionRequestDto.builder()
                .tenantId(request.tenantId())
                .businessId(request.businessId())
                .clientApplicationId(request.clientApplicationId())
                .entitlementType(entitlementType.name())
                .moduleId(request.moduleId())
                .submoduleId(request.submoduleId())
                .featureId(request.featureId())
                .privilegeId(request.privilegeId())
                .apiRegistryId(request.apiRegistryId())
                .moduleCode(request.moduleCode())
                .submoduleCode(request.submoduleCode())
                .featureTypeCode(request.featureTypeCode())
                .featureCode(request.featureCode())
                .privilegeCode(request.privilegeCode())
                .apiCode(request.apiCode())
                .limitCode(request.limitCode())
                .requestedUsage(request.requestedUsage())
                .build();
    }
}
