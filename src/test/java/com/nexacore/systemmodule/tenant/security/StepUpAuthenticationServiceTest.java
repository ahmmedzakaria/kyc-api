package com.nexacore.systemmodule.tenant.security;

import com.nexacore.authmodule.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepUpAuthenticationServiceTest {
    private final PlatformAdministrationProperties properties = new PlatformAdministrationProperties();
    private final StepUpAuthenticationService service = new StepUpAuthenticationService(properties);

    @Test
    void acceptsExplicitConfirmationWithFreshToken() {
        MockHttpServletRequest request = request(Instant.now());
        assertThatCode(() -> service.requireRecentReauthentication(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingConfirmationOrStaleToken() {
        MockHttpServletRequest missing = new MockHttpServletRequest();
        missing.setAttribute(JwtAuthenticationFilter.JWT_ISSUED_AT_ATTRIBUTE, Instant.now());
        assertThatThrownBy(() -> service.requireRecentReauthentication(missing))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> service.requireRecentReauthentication(request(Instant.now().minusSeconds(301))))
                .isInstanceOf(AccessDeniedException.class);
    }

    private MockHttpServletRequest request(Instant issuedAt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(StepUpAuthenticationService.CONFIRMATION_HEADER, "reauthenticated");
        request.setAttribute(JwtAuthenticationFilter.JWT_ISSUED_AT_ATTRIBUTE, issuedAt);
        return request;
    }
}
