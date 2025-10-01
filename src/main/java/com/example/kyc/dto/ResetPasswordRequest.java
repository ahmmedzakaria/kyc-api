package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for resetting password using OTP")
public record ResetPasswordRequest(

    @Schema(example = "shaarifulz@gmail.com", description = "User's registered email address")
    String username,

    @Schema(example = "1234", description = "4-digit OTP sent to email")
    String otp,

    @Schema(example = "NewSecurePass@123", description = "The new password to set", minLength = 8)
    String newPassword
) {}
