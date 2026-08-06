package com.nexacore.systemmodule.accesscontrol.security;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class PublicRoutePolicy {
    private static final List<String> PATTERNS = List.of(
            "/api/v1/auth/login", "/api/v1/auth/authenticate", "/api/v1/auth/config",
            "/api/v1/auth/application-context/public", "/api/v1/auth/sso/authenticate",
            "/api/v1/auth/login-status", "/api/v1/auth/refresh-token", "/oauth2/**",
            "/users/register", "/api/v1/auth/test", "/v3/api-docs/**",
            "/swagger-ui/**", "/swagger-ui.html"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean isPublic(String path) {
        return path != null && PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    public String[] patterns() {
        return PATTERNS.toArray(String[]::new);
    }
}
