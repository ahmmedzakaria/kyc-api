package com.nexacore.systemmodule.accesscontrol.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class AccessControlPropertiesTest {

    @Test
    void defaultsToFailClosedEnforcement() {
        AccessControlProperties properties = new AccessControlProperties(new MockEnvironment());

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEnforcementMode()).isEqualTo(EnforcementMode.ENFORCE);
        assertThat(properties.isDecisionEvaluationEnabled()).isTrue();
    }

    @Test
    void productionRejectsReportMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        AccessControlProperties properties = new AccessControlProperties(environment);
        properties.setEnforcementMode(EnforcementMode.REPORT);

        assertThatIllegalStateException()
                .isThrownBy(properties::validateProductionMode)
                .withMessageContaining("ENFORCE");
    }
}
