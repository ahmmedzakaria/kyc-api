package com.example.kyc.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
    @NotBlank String roleName
) {}