package com.nexacore.authmodule.sso.controller;

import com.nexacore.authmodule.dto.AuthResponse;
import com.nexacore.authmodule.sso.dto.SsoAuthenticateRequest;
import com.nexacore.authmodule.sso.service.SsoAuthService;
import com.nexacore.commonmodule.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SSO Authentication", description = "Handles SSO authentication APIs")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/sso")
public class SsoController {

    private final SsoAuthService ssoAuthService;

    @Operation(summary = "Authenticate user by Keycloak access token", security = {})
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody @Valid SsoAuthenticateRequest requestDto) {
        return ssoAuthService.authenticate(requestDto);
    }
}
