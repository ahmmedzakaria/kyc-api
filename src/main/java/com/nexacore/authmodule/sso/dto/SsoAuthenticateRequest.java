package com.nexacore.authmodule.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;

public record SsoAuthenticateRequest(
        @Schema(example = "NEXACORE_APP", description = "Registered application source")
        @NotNull(message = "source can not be null")
        @Pattern(regexp = "\\b(NEXACORE_APP|NEXACORE_ADMIN|NEXACORE_WEB|KYC_APP|KYC_ADMIN|KYC_WEB)\\b", flags = {Flag.MULTILINE}, message = "The source is invalid.")
        String source,

        @NotBlank(message = "accessToken can not be blank")
        String accessToken
) {
}
