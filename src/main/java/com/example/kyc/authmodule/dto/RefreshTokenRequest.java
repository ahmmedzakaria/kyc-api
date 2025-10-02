package com.example.kyc.authmodule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for refreshing JWT access token")
public record RefreshTokenRequest(
    @Schema(description = "Refresh token obtained during login", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotNull(message = "refreshToken can not be null")
    @Size(min = 10, message = "Minimum username length is 10")
    String refreshToken
) {}
