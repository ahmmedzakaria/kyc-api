package com.nexacore.systemmodule.tenant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TenantLifecycleRequest(@NotNull Long tenantId, @Size(max = 500) String reason) {}
