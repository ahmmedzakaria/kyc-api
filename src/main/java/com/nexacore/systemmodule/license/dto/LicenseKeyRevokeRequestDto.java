package com.nexacore.systemmodule.license.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record LicenseKeyRevokeRequestDto(@NotNull Long keyId, @NotBlank String reason) {}
