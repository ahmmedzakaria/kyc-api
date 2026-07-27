package com.nexacore.authmodule.core.controller;


import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.AuthRequest;
import com.nexacore.authmodule.core.dto.AuthResponse;
import com.nexacore.authmodule.core.dto.AuthConfigResponse;
import com.nexacore.authmodule.core.dto.LoginStatusRequest;
import com.nexacore.authmodule.core.dto.LoginStatusResponse;
import com.nexacore.authmodule.core.dto.RefreshTokenRequest;
import com.nexacore.authmodule.core.service.interfaces.AuthService;
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
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Login user by OTP", security = {}) // security = {} make disables JWT for this method (for using swagger UI)
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody @Valid AuthRequest requestDto,
                                                                  @RequestHeader(value = "X-Client-Code", required = false) String clientCode) {
        return authService.authenticate(requestDto, clientCode);
    }

    @Operation(summary = "Get authentication mode and SSO client config", security = {})
    @PostMapping("/config")
    public ResponseEntity<ApiResponse<AuthConfigResponse>> getAuthConfig(@RequestHeader(value = "Origin", required = false) String origin,
                                                                         @RequestHeader(value = "X-Client-Code", required = false) String clientCode) {
        return authService.getAuthConfig(origin, clientCode);
    }

    @Operation(summary = "Get public application context for login and registration", security = {})
    @PostMapping("/application-context/public")
    public ResponseEntity<ApiResponse<ApplicationContextDto>> getPublicApplicationContext(@RequestHeader(value = "Origin", required = false) String origin,
                                                                                         @RequestHeader(value = "X-Client-Code", required = false) String clientCode) {
        return authService.getPublicApplicationContext(origin, clientCode);
    }

    @Operation(summary = "Get authenticated application context")
    @PostMapping("/application-context")
    public ResponseEntity<ApiResponse<ApplicationContextDto>> getApplicationContext(@RequestHeader(value = "Origin", required = false) String origin,
                                                                                   @RequestHeader(value = "X-Client-Code", required = false) String clientCode) {
        return authService.getApplicationContext(origin, clientCode);
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
