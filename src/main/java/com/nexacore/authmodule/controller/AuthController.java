package com.nexacore.authmodule.controller;


import com.nexacore.authmodule.dto.AuthRequest;
import com.nexacore.authmodule.dto.AuthResponse;
import com.nexacore.authmodule.dto.RefreshTokenRequest;
import com.nexacore.authmodule.service.implementations.AuthServiceImpl;
import com.nexacore.authmodule.service.interfaces.AuthService;
import com.nexacore.commonmodule.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Refresh Token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest requestDto) {
        return authService.refreshToken(requestDto);
    }

}