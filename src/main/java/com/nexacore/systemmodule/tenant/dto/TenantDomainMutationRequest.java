package com.nexacore.systemmodule.tenant.dto;

import com.nexacore.systemmodule.tenant.entity.TenantDomainType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TenantDomainMutationRequest(@NotNull Long tenantId, Long domainId,
                                          @Size(max = 253) String hostname,
                                          TenantDomainType domainType) {}
