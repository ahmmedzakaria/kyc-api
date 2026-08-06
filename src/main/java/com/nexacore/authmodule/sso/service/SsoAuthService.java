package com.nexacore.authmodule.sso.service;

import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.service.implementations.AuthClientPolicyService;
import com.nexacore.authmodule.security.jwt.JwtUtil;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.core.dto.AuthResponse;
import com.nexacore.authmodule.core.service.implementations.LogoutSessionService;
import com.nexacore.authmodule.core.service.implementations.RefreshTokenSessionService;
import com.nexacore.authmodule.sso.dto.SsoAuthenticateRequest;
import com.nexacore.commonmodule.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsoAuthService {

    private final AuthenticationProperties authenticationProperties;
    private final KeycloakSsoService keycloakSsoService;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final LogoutSessionService logoutSessionService;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final AuthClientPolicyService authClientPolicyService;

    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(SsoAuthenticateRequest request, String clientCode) {
        if (!authClientPolicyService.isLoginMethodEnabled(LoginMethod.SSO, clientCode)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "SSO_LOGIN_DISABLED"));
        }

        try {
            var profile = keycloakSsoService.verifyToken(request.accessToken());
            var user = keycloakSsoService.syncUser(profile);
            var userDetails = userDetailsService.loadUserByUsername(user.getUsername());

            logoutSessionService.login(userDetails.getUsername());
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            refreshTokenSessionService.register(userDetails.getUsername(), jwtUtil.extractJwtId(refreshToken));

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            log.info("SSO login successful for username={}, keycloakSubject={}", user.getUsername(), profile.subject());
            return ResponseEntity.ok(ApiResponse.success(response, "SSO authentication successful. Token generated"));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), List.of(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurred: " + e.getLocalizedMessage())));
        }
    }
}
