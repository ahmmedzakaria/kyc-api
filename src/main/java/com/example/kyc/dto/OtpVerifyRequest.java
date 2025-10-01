package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for verifying email OTP")
public record OtpVerifyRequest(
    
    @Schema(example = "shaarifulz@gmail.com", description = "The user's email address")
    String email,

    @Schema(example = "1234", description = "The 4-digit OTP sent to the user's email")
    String otp
) {}
