package com.nexacore.systemmodule.tenant.dto;

import com.nexacore.systemmodule.tenant.entity.TenantDomainType;
import com.nexacore.systemmodule.tenant.entity.TenantDomainVerificationStatus;

import java.time.LocalDateTime;

public record TenantDomainDto(Long id, String hostname, TenantDomainType domainType,
                              TenantDomainVerificationStatus verificationStatus,
                              boolean primaryDomain, boolean active, LocalDateTime verifiedAt,
                              LocalDateTime lastCheckedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}
