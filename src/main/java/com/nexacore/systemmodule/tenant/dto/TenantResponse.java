package com.nexacore.systemmodule.tenant.dto;

import com.nexacore.systemmodule.tenant.entity.TenantStatus;

import java.time.LocalDateTime;

public record TenantResponse(Long id, String tenantCode, String displayName,
                             TenantStatus status, String hostname,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {}
