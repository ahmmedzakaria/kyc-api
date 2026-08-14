package com.nexacore.systemmodule.accesscontrol.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicAuthenticationRouteCharacterizationTest {
    private final PublicRoutePolicy policy = new PublicRoutePolicy();

    @Test
    void preAuthenticationEndpointsCurrentlyBypassNormalClientAndJwtEnforcement() {
        assertThat(new String[]{
                "/api/v1/auth/login",
                "/api/v1/auth/authenticate",
                "/api/v1/auth/config",
                "/api/v1/auth/application-context/public",
                "/api/v1/auth/sso/authenticate",
                "/api/v1/auth/login-status",
                "/api/v1/auth/refresh-token",
                "/oauth2/authorization/nexacore"
        }).allMatch(policy::isPublic);
    }

    @Test
    void sessionAdministrationEndpointsRemainProtected() {
        assertThat(new String[]{
                "/api/v1/auth/logout",
                "/api/v1/auth/session-status",
                "/api/v1/auth/application-context"
        }).noneMatch(policy::isPublic);
    }
}
