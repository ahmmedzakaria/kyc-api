package com.nexacore.authmodule.controller;


import com.nexacore.authmodule.dto.AuthRequest;
import com.nexacore.authmodule.dto.AuthResponse;
import com.nexacore.authmodule.dto.AuthConfigResponse;
import com.nexacore.authmodule.dto.LoginStatusRequest;
import com.nexacore.authmodule.dto.LoginStatusResponse;
import com.nexacore.authmodule.dto.RefreshTokenRequest;
import com.nexacore.authmodule.service.interfaces.AuthService;
import com.nexacore.commonmodule.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Handles registration, login, and password management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Login user by OTP", security = {}) // security = {} make disables JWT for this method (for using swagger UI)
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody @Valid AuthRequest requestDto) {
        return authService.authenticate(requestDto);
    }

    @Operation(summary = "Get authentication mode and SSO client config", security = {})
    @PostMapping("/config")
    public ResponseEntity<ApiResponse<AuthConfigResponse>> getAuthConfig(@RequestHeader(value = "Origin", required = false) String origin) {
        return authService.getAuthConfig(origin);
    }

    @Operation(summary = "Logout user from shared application session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        return authService.logout(username);
    }

    @Operation(summary = "Check tracked login status for a username", security = {})
    @PostMapping("/login-status")
    public ResponseEntity<ApiResponse<LoginStatusResponse>> loginStatus(@RequestBody LoginStatusRequest requestDto) {
        return authService.loginStatus(requestDto);
    }

    @Operation(summary = "Check current application session")
    @PostMapping("/session-status")
    public ResponseEntity<ApiResponse<Void>> sessionStatus() {
        return authService.sessionStatus();
    }

    @Operation(summary = "Refresh Token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest requestDto) {
        return authService.refreshToken(requestDto);
    }

}
