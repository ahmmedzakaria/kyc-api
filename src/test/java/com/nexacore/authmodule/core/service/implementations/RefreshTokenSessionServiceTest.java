package com.nexacore.authmodule.core.service.implementations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenSessionServiceTest {

    @Test
    void rotationInvalidatesThePreviousRefreshTokenAndLogoutRevokesTheReplacement() {
        RefreshTokenSessionService service = new RefreshTokenSessionService();
        service.register("alice", "first-jti");

        assertThat(service.rotate("alice", "first-jti", "second-jti")).isTrue();
        assertThat(service.isActive("alice", "first-jti")).isFalse();
        assertThat(service.isActive("alice", "second-jti")).isTrue();
        assertThat(service.rotate("alice", "first-jti", "third-jti")).isFalse();

        service.revoke("alice");
        assertThat(service.isActive("alice", "second-jti")).isFalse();
    }
}
