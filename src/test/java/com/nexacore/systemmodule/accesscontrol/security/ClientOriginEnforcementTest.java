package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ClientOriginEnforcementTest {

    private SysPrivClientApplication resolvedClient;
    private ClientRateLimitDecision rateLimitDecision = ClientRateLimitDecision.notLimited();
    private boolean rateLimitUnavailable;
    private final ClientCredentialService credentials = new ClientCredentialService() {
        @Override
        public Optional<SysPrivClientApplication> resolveActiveClient(String clientCode) {
            return Optional.ofNullable(resolvedClient);
        }

        @Override
        public Optional<SysPrivClientApplication> validateApiKey(String clientCode, String apiKey) {
            return Optional.ofNullable(resolvedClient);
        }
    };
    private final AccessControlProperties properties = properties();
    private final ClientRateLimiter rateLimiter = (application, request) -> {
        if (rateLimitUnavailable) {
            throw new RateLimitBackendUnavailableException("unavailable", new IllegalStateException());
        }
        return rateLimitDecision;
    };
    private final ClientApplicationAuthenticationFilter filter = new ClientApplicationAuthenticationFilter(
            credentials, new ClientOriginPolicy(), new ClientIpPolicy(properties), rateLimiter, new PublicRoutePolicy(),
            new ApiResponseJsonWriter(new ObjectMapper()), properties,
            new AuthorizationEventEmitter(event -> {}, new ObjectMapper()));

    @Test
    void allowsConfiguredBrowserOrigin() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        MockHttpServletRequest request = request("POST", "https://portal.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMismatchedOrMalformedBrowserOrigin() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        MockHttpServletRequest request = request("POST", "https://attacker.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CLIENT_ORIGIN_NOT_ALLOWED");
    }

    @Test
    void absentOriginDoesNotBlockServerToServerRequest() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        MockHttpServletRequest request = request("POST", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
    }

    @Test
    void delegatesPreflightToCentralCorsConfiguration() {
        MockHttpServletRequest request = request("OPTIONS", "https://portal.example.com");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void rejectsResolvedClientOutsideItsIpAllowlist() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        resolvedClient.setAllowedIps("10.0.0.0/8");
        MockHttpServletRequest request = request("POST", "https://portal.example.com");
        request.setRemoteAddr("203.0.113.44");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CLIENT_IP_NOT_ALLOWED");
    }

    @Test
    void returns429WithSafeRetryMetadataWhenLimitIsExceeded() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        rateLimitDecision = new ClientRateLimitDecision(false, 2, 0, 37);
        MockHttpServletRequest request = request("POST", "https://portal.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("RateLimit-Limit")).isEqualTo("2");
        assertThat(response.getHeader("RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("Retry-After")).isEqualTo("37");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void failsClosedWith503WhenDistributedLimiterIsUnavailable() throws Exception {
        resolvedClient = webClient("https://portal.example.com");
        rateLimitUnavailable = true;
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "https://portal.example.com"), response,
                (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_UNAVAILABLE");
    }

    private MockHttpServletRequest request(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/person/search");
        request.setServletPath("/api/v1/person/search");
        request.addHeader(ClientApplicationAuthenticationFilter.CLIENT_CODE_HEADER, "portal");
        if (origin != null) request.addHeader("Origin", origin);
        return request;
    }

    private SysPrivClientApplication webClient(String origins) {
        return SysPrivClientApplication.builder()
                .id(3L).clientCode("portal").clientType(ClientApplicationType.WEB)
                .allowedOrigins(origins).build();
    }

    private AccessControlProperties properties() {
        AccessControlProperties properties = new AccessControlProperties(new MockEnvironment());
        properties.setEnabled(true);
        properties.setEnforcementMode(EnforcementMode.ENFORCE);
        return properties;
    }
}
