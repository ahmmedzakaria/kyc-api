package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.validation.constraints.Size;

@Schema(description = "OTP by mobile number")
public record RequestOTPDto(

    @Schema(example = "8801969037346", description = "Mobile number with country code. 13 Digit mobile number")
    @NotNull(message = "mobile number can not be null")
    @Size(min = 13, message = "Minimum mobile number length is 13")
    @Pattern(regexp = "^8801[0-9]{9}$", message = "Mobile number must start with 8801 and contain 11 digits in total")
    String mobile,
    
    @Schema(example = "KYC_APP", description = "Registered application source")
    @NotNull(message = "source can not be null")
    @Size(min = 2, message = "source username length is 2")
	@Pattern(regexp = "\\bKYC_APP|KYC_ADMIN|KYC_WEB\\b", flags = {Flag.MULTILINE }, message = "The source is invalid.")
    String source
) {}
