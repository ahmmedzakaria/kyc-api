package com.nexacore.systemmodule.license.service;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LicenseEntitlementValidatorTest {
    private final LicenseEntitlementValidator validator = new LicenseEntitlementValidator();

    @Test void acceptsExactlyOneCompatibleTarget() {
        var request = LicenseEntitlementRequestDto.builder().entitlementType(LicenseEntitlementType.API).apiRegistryId(9L).build();
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }
    @Test void rejectsMultipleTargets() {
        var request = LicenseEntitlementRequestDto.builder().entitlementType(LicenseEntitlementType.API).apiRegistryId(9L).featureId(2L).build();
        assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Exactly one");
    }
    @Test void rejectsTargetThatDoesNotMatchType() {
        var request = LicenseEntitlementRequestDto.builder().entitlementType(LicenseEntitlementType.MODULE).featureId(2L).build();
        assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("incompatible");
    }
}
