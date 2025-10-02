package com.example.kyc.authmodule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user login using OTP")
public record AuthRequest(
    
    @Schema(example = "KYC_APP", description = "Registered application source")
    @NotNull(message = "source can not be null")
    @Size(min = 2, message = "source username length is 2")
	@Pattern(regexp = "\\bKYC_APP|KYC_ADMIN|KYC_WEB\\b", flags = {Flag.MULTILINE }, message = "The source is invalid.")
    String source,

    @Schema(example = "admin", description = "Registered username")
    @NotNull(message = "username can not be null")
    @Size(min = 3, message = "username min length is 3")
    String username,

    @Schema(example = "admin123", description = "User password")
    @NotNull(message = "password can not be null")
    @Size(min = 3, message = "password min length is 3")
    String password
) {}
