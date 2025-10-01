package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user login using password")
public record LoginRequestWithPassword(

    @Schema(example = "shaarifulz@gmail.com", description = "Registered user email address")
    @NotNull(message = "username can not be null")
    @Size(min = 2, message = "Minimum username length is 2")
    String username,
    
    @Schema(example = "KYC_APP", description = "Registered application source")
    @NotNull(message = "source can not be null")
    @Size(min = 2, message = "source username length is 2")
	@Pattern(regexp = "\\bKYC_APP|KYC_ADMIN|KYC_WEB\\b", flags = {Flag.MULTILINE }, message = "The source is invalid.")
    String source,

    @Schema(example = "12345678", description = "User's password or OTP")
    @NotNull(message = "password can not be null")
    @Size(min = 4, message = "password username length is 4")
    String password
) {}
