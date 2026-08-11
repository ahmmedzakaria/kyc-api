package com.nexacore.systemmodule.tenant.dto;

import com.nexacore.systemmodule.tenant.entity.TenantDomainType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank @Size(max = 63) String tenantCode,
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 255) String legalName,
        @Size(max = 100) String registrationNumber,
        @Email @Size(max = 320) String billingEmail,
        @NotBlank @Size(max = 253) String hostname,
        TenantDomainType domainType,
        @Size(max = 20) String defaultLocale,
        @Size(max = 64) String defaultTimeZone
) {}
