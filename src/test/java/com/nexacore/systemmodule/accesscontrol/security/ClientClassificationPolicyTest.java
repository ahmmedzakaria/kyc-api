package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientClassificationPolicyTest {

    private final PublicRoutePolicy publicRoutePolicy = new PublicRoutePolicy();
    private final ClientOriginPolicy clientOriginPolicy = new ClientOriginPolicy();

    @Test
    void usesOnePublicRouteSetForAuthenticationAndDocumentationEndpoints() {
        assertThat(publicRoutePolicy.isPublic("/api/v1/auth/login")).isTrue();
        assertThat(publicRoutePolicy.isPublic("/api/v1/auth/refresh-token")).isTrue();
        assertThat(publicRoutePolicy.isPublic("/swagger-ui/index.html")).isTrue();
        assertThat(publicRoutePolicy.isPublic("/api/v1/system/client-app/list")).isFalse();
        assertThat(publicRoutePolicy.patterns()).doesNotContain("http://localhost:4200");
    }

    @Test
    void classifiesBrowserAndMobileClientsAsPublic() {
        assertThat(ClientApplicationType.WEB.isConfidential()).isFalse();
        assertThat(ClientApplicationType.MOBILE.isConfidential()).isFalse();
        assertThat(ClientApplicationType.POS.isConfidential()).isTrue();
        assertThat(ClientApplicationType.ERP.isConfidential()).isTrue();
        assertThat(ClientApplicationType.PARTNER_PORTAL.isConfidential()).isTrue();
        assertThat(ClientApplicationType.INTERNAL_SERVICE.isConfidential()).isTrue();
    }

    @Test
    void acceptsOnlyNormalizedConfiguredClientOrigins() {
        SysPrivClientApplication client = SysPrivClientApplication.builder()
                .allowedOrigins("https://portal.example.com, http://localhost:4200")
                .build();

        assertThat(clientOriginPolicy.isAllowed(client, "HTTPS://PORTAL.EXAMPLE.COM:443")).isTrue();
        assertThat(clientOriginPolicy.isAllowed(client, "http://localhost:4200")).isTrue();
        assertThat(clientOriginPolicy.isAllowed(client, "https://attacker.example.com")).isFalse();
        assertThat(clientOriginPolicy.isAllowed(client, "https://portal.example.com/path")).isFalse();
    }
}
