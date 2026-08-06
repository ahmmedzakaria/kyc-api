package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrivilegeApiAccessFilterTest {
    private static final String REQUIRED = "01010200101";

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        AuthenticatedRequestContextHolder.clear();
        ClientApplicationContextHolder.clear();
    }

    @Test
    void allowsAuthenticatedUserWithRequiredEffectivePrivilege() throws Exception {
        UserPrivilegeApiAccessFilter filter = filter(EnforcementMode.ENFORCE);
        prepareContexts(Set.of(REQUIRED));
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request(), new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(ClientApplicationContextHolder.get().orElseThrow().userDecision()).isEqualTo("ALLOWED");
    }

    @Test
    void deniesAuthenticatedUserWithoutRequiredEffectivePrivilege() throws Exception {
        UserPrivilegeApiAccessFilter filter = filter(EnforcementMode.ENFORCE);
        prepareContexts(Set.of("DIFFERENT"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(), response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("USER_PRIVILEGE_NOT_ALLOWED");
        ClientApplicationContext decision = ClientApplicationContextHolder.get().orElseThrow();
        assertThat(decision.userDecision()).isEqualTo("DENIED");
        assertThat(decision.userDenyReason()).isEqualTo("USER_PRIVILEGE_NOT_ALLOWED");
    }

    @Test
    void reportModeRecordsDenialButContinuesRequest() throws Exception {
        UserPrivilegeApiAccessFilter filter = filter(EnforcementMode.REPORT);
        prepareContexts(Set.of());
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request(), new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(ClientApplicationContextHolder.get().orElseThrow().userDecision()).isEqualTo("DENIED");
    }

    private void prepareContexts(Set<String> privileges) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("operator", "unused", "ROLE_USER"));
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                7L, "operator", 3L, "WEB", Set.of(new UserScopeAssignment(11L, null, null)),
                "trace-1", privileges));
        SysPrivApiRegistry api = SysPrivApiRegistry.builder().id(2L).apiCode("ADMIN_USERS")
                .pathPattern("/api/v1/admin/users").requiredPrivilegeCode(REQUIRED).active(true).build();
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId("trace-1").apiRegistry(api).requiredPrivilegeCode(REQUIRED)
                .clientDecision("ALLOWED").userId(7L).scopeAssignments(Set.of()).build());
    }

    private UserPrivilegeApiAccessFilter filter(EnforcementMode mode) {
        AccessControlProperties properties = new AccessControlProperties(new MockEnvironment());
        properties.setEnforcementMode(mode);
        return new UserPrivilegeApiAccessFilter(new ApiResponseJsonWriter(new ObjectMapper()),
                properties, new PublicRoutePolicy());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setServletPath("/api/v1/admin/users");
        return request;
    }
}
