package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ClientOriginPolicyTest {
    private final ClientOriginPolicy policy = new ClientOriginPolicy();

    @Test
    void normalizesSchemeHostDefaultPortsAndDuplicates() {
        assertThat(policy.normalizeConfiguredOrigins(
                " HTTPS://Example.COM:443/,http://example.com:80,https://example.com "))
                .isEqualTo("https://example.com,http://example.com");
    }

    @Test
    void preservesNonDefaultPortAndNormalizesInternationalHost() {
        assertThat(policy.normalizeConfiguredOrigins("https://bücher.example:8443"))
                .isEqualTo("https://xn--bcher-kva.example:8443");
    }

    @Test
    void rejectsWildcardOpaqueAndUrlShapedValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredOrigins("*"));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredOrigins("null"));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredOrigins("https://example.com/path"));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.normalizeConfiguredOrigins("https://user@example.com"));
    }

    @Test
    void comparesNormalizedRequestOriginAndFailsClosedForMalformedOrigin() {
        SysAccClientApplication client = SysAccClientApplication.builder()
                .allowedOrigins("https://example.com").build();

        assertThat(policy.isAllowed(client, "HTTPS://EXAMPLE.COM:443/")).isTrue();
        assertThat(policy.isAllowed(client, "https://example.com/path")).isFalse();
        assertThat(policy.isAllowed(null, "https://example.com")).isFalse();
    }
}
