package com.nexacore.systemmodule.tenant.security;

import com.nexacore.authmodule.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StepUpAuthenticationService {
    public static final String CONFIRMATION_HEADER = "X-Step-Up-Authentication";
    private final PlatformAdministrationProperties properties;

    public void requireRecentReauthentication(HttpServletRequest request) {
        if (!properties.isEnabled()) return;
        if (!"reauthenticated".equalsIgnoreCase(request.getHeader(CONFIRMATION_HEADER))) {
            throw new AccessDeniedException("Recent reauthentication confirmation is required");
        }
        Object issuedAtValue = request.getAttribute(JwtAuthenticationFilter.JWT_ISSUED_AT_ATTRIBUTE);
        if (!(issuedAtValue instanceof Instant issuedAt)) {
            throw new AccessDeniedException("Authenticated token issue time is unavailable");
        }
        long age = Duration.between(issuedAt, Instant.now()).getSeconds();
        if (age < 0 || age > properties.getMaxAuthenticationAgeSeconds()) {
            throw new AccessDeniedException("Recent reauthentication is required");
        }
    }
}
