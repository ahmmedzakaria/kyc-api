package com.nexacore.systemmodule.layout.service.implementations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LayoutCodeGenerationServiceImplTest {

    private final LayoutCodeGenerationServiceImpl service = new LayoutCodeGenerationServiceImpl();

    @Test
    void normalizesBusinessCodeToUppercaseSnakeCase() {
        assertThat(service.normalizeBusinessCode("Customer Onboarding", null))
                .isEqualTo("CUSTOMER_ONBOARDING");
    }

    @Test
    void normalizesTCodeForQuickNavigation() {
        assertThat(service.normalizeTCode(" kyc-101 "))
                .isEqualTo("KYC101");
    }

    @Test
    void rejectsReservedSystemUiTCodeRangeForAdminGeneratedCodes() {
        assertThatThrownBy(() -> service.validateTCode("KYC001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("001-100");
    }

    @Test
    void allowsClientBusinessTCodeRangeFrom101() {
        service.validateTCode("KYC101");
    }
}
