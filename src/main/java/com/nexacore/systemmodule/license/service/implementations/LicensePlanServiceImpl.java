package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseEntitlementResponseDto;
import com.nexacore.systemmodule.license.dto.LicensePlanRequestDto;
import com.nexacore.systemmodule.license.dto.LicensePlanResponseDto;
import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import com.nexacore.systemmodule.license.entity.SysLicensePlanEntitlement;
import com.nexacore.systemmodule.license.enums.BillingCycle;
import com.nexacore.systemmodule.license.enums.LicensePlanType;
import com.nexacore.systemmodule.license.repository.LicensePlanEntitlementRepository;
import com.nexacore.systemmodule.license.repository.LicensePlanRepository;
import com.nexacore.systemmodule.license.service.interfaces.LicensePlanService;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import com.nexacore.systemmodule.license.service.LicenseEntitlementValidator;

@Service
@RequiredArgsConstructor
public class LicensePlanServiceImpl implements LicensePlanService {

    private final LicensePlanRepository licensePlanRepository;
    private final LicensePlanEntitlementRepository entitlementRepository;
    private final LicenseEntitlementValidator entitlementValidator;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicensePlanResponseDto savePlan(LicensePlanRequestDto request) {
        SysLicensePlan plan = request.id() == null
                ? licensePlanRepository.findByPlanCode(request.planCode()).orElseGet(SysLicensePlan::new)
                : licensePlanRepository.findById(request.id()).orElseGet(SysLicensePlan::new);

        plan.setPlanCode(request.planCode());
        plan.setPlanName(request.planName());
        plan.setPlanType(request.planType() == null ? LicensePlanType.STANDARD : request.planType());
        plan.setBillingCycle(request.billingCycle() == null ? BillingCycle.NONE : request.billingCycle());
        plan.setTrialDays(request.trialDays());
        plan.setDescription(request.description());
        plan.setActive(request.active() == null || request.active());

        return toResponse(licensePlanRepository.save(plan));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void savePlanEntitlement(LicenseEntitlementRequestDto request) {
        entitlementValidator.validate(request);
        SysLicensePlan plan = licensePlanRepository.findByPlanCode(request.planCode())
                .orElseThrow(() -> new IllegalArgumentException("License plan not found: " + request.planCode()));

        SysLicensePlanEntitlement entitlement = request.id() == null ? new SysLicensePlanEntitlement()
                : entitlementRepository.findByIdAndLicensePlanPlanCode(request.id(), request.planCode())
                .orElseThrow(() -> new IllegalArgumentException("License plan entitlement not found"));
        if (request.id() != null && (request.version() == null || entitlement.getVersion() != request.version()))
            throw new OptimisticLockingFailureException("License entitlement was modified by another administrator");
        entitlement.setLicensePlan(plan);
        entitlement.setEntitlementType(request.entitlementType());
        entitlement.setModule(referenceModule(request.moduleId()));
        entitlement.setSubmodule(referenceSubmodule(request.submoduleId()));
        entitlement.setFeature(referenceFeature(request.featureId()));
        entitlement.setPrivilege(referencePrivilege(request.privilegeId()));
        entitlement.setApiRegistry(referenceApi(request.apiRegistryId()));
        entitlement.setLimitCode(normalizeCode(request.limitCode()));
        entitlement.setLimitValue(request.limitValue());
        entitlement.setActive(request.active() == null || request.active());
        entitlementRepository.save(entitlement);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LicenseEntitlementResponseDto> listPlanEntitlements(String planCode) {
        return entitlementRepository.findByLicensePlan_PlanCodeAndActiveTrue(planCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private LicenseEntitlementResponseDto toResponse(SysLicensePlanEntitlement entitlement) {
        return LicenseEntitlementResponseDto.builder()
                .id(entitlement.getId())
                .version(entitlement.getVersion())
                .ownerCode(entitlement.getLicensePlan().getPlanCode())
                .entitlementType(entitlement.getEntitlementType())
                .moduleId(entitlement.getModule() == null ? null : entitlement.getModule().getId())
                .submoduleId(entitlement.getSubmodule() == null ? null : entitlement.getSubmodule().getId())
                .featureId(entitlement.getFeature() == null ? null : entitlement.getFeature().getId())
                .privilegeId(entitlement.getPrivilege() == null ? null : entitlement.getPrivilege().getId())
                .apiRegistryId(entitlement.getApiRegistry() == null ? null : entitlement.getApiRegistry().getId())
                .limitCode(entitlement.getLimitCode())
                .limitValue(entitlement.getLimitValue())
                .active(entitlement.isActive())
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LicensePlanResponseDto> listPlans() {
        return licensePlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private LicensePlanResponseDto toResponse(SysLicensePlan plan) {
        return LicensePlanResponseDto.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .planType(plan.getPlanType())
                .billingCycle(plan.getBillingCycle())
                .trialDays(plan.getTrialDays())
                .description(plan.getDescription())
                .active(plan.isActive())
                .build();
    }

    private SysPrivModule referenceModule(Long id) {
        return id == null ? null : SysPrivModule.builder().id(id).build();
    }

    private SysPrivSubmodule referenceSubmodule(Long id) {
        return id == null ? null : SysPrivSubmodule.builder().id(id).build();
    }

    private SysPrivFeature referenceFeature(Long id) {
        return id == null ? null : SysPrivFeature.builder().id(id).build();
    }

    private SysPrivPrivilege referencePrivilege(Long id) {
        return id == null ? null : SysPrivPrivilege.builder().id(id).build();
    }

    private SysAccApiRegistry referenceApi(Long id) {
        return id == null ? null : SysAccApiRegistry.builder().id(id).build();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
