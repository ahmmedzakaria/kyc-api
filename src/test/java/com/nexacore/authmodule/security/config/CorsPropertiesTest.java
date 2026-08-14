package com.nexacore.authmodule.security.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {
    @Test void permitsTheStepUpHeaderRequiredBySensitiveBrowserOperations() {
        assertThat(new CorsProperties().getAllowedHeaders()).contains("X-Step-Up-Authentication");
    }
}
