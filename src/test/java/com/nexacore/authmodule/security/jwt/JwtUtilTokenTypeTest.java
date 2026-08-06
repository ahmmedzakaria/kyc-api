package com.nexacore.authmodule.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTokenTypeTest {

    private final JwtUtil jwtUtil = new JwtUtil();
    private final UserDetails user = User.withUsername("alice")
            .password("not-used")
            .authorities("ROLE_USER")
            .build();

    @BeforeEach
    void configureJwt() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "allowedClockSkewSeconds", 0L);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 120_000L);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "test-issuer");
        ReflectionTestUtils.setField(jwtUtil, "audience", "test-audience");
        jwtUtil.init();
    }

    @Test
    void generatesPurposeBoundTokensWithStandardClaims() {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        assertThat(jwtUtil.extractTokenType(accessToken)).isEqualTo(JwtTokenType.ACCESS);
        assertThat(jwtUtil.extractTokenType(refreshToken)).isEqualTo(JwtTokenType.REFRESH);
        assertThat(jwtUtil.extractRoles(accessToken)).containsExactly("ROLE_USER");
        assertThat(jwtUtil.extractJwtId(accessToken)).isNotBlank().isNotEqualTo(jwtUtil.extractJwtId(refreshToken));
        assertThat(jwtUtil.extractClaim(accessToken, Claims::getIssuer)).isEqualTo("test-issuer");
        assertThat(jwtUtil.extractClaim(accessToken, Claims::getAudience)).isEqualTo("test-audience");
    }

    @Test
    void rejectsCrossPurposeTokenUseAndRolesMissingFromRefreshToken() {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        assertThatThrownBy(() -> jwtUtil.requireTokenType(accessToken, JwtTokenType.REFRESH))
                .isInstanceOf(InvalidTokenTypeException.class);
        assertThatThrownBy(() -> jwtUtil.requireTokenType(refreshToken, JwtTokenType.ACCESS))
                .isInstanceOf(InvalidTokenTypeException.class);
        assertThatThrownBy(() -> jwtUtil.extractRoles(refreshToken))
                .isInstanceOf(JwtException.class);
    }
}
