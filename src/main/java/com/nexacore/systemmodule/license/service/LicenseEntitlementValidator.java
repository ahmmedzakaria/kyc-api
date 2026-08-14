package com.nexacore.systemmodule.license.service;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import org.springframework.stereotype.Component;

@Component
public class LicenseEntitlementValidator {
    public void validate(LicenseEntitlementRequestDto request) {
        if (request.entitlementType() == null) throw new IllegalArgumentException("Entitlement type is required");
        int targets = count(request.moduleId()) + count(request.submoduleId()) + count(request.featureId())
                + count(request.privilegeId()) + count(request.apiRegistryId()) + count(text(request.limitCode()));
        if (targets != 1) throw new IllegalArgumentException("Exactly one entitlement target is required");
        boolean compatible = switch (request.entitlementType()) {
            case MODULE -> request.moduleId() != null;
            case SUBMODULE -> request.submoduleId() != null;
            case FEATURE, ADD_ON -> request.featureId() != null;
            case ACTION -> request.privilegeId() != null;
            case API -> request.apiRegistryId() != null;
            case LIMIT -> text(request.limitCode()) != null && request.limitValue() != null && request.limitValue() >= 0;
        };
        if (!compatible) throw new IllegalArgumentException("Entitlement target is incompatible with type " + request.entitlementType());
    }
    private int count(Object value) { return value == null ? 0 : 1; }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
