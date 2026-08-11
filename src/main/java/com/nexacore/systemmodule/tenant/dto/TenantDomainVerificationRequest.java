package com.nexacore.systemmodule.tenant.dto;

import jakarta.validation.constraints.NotNull;

public record TenantDomainVerificationRequest(@NotNull Long domainId) {}
