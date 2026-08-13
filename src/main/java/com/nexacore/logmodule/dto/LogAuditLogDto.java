package com.nexacore.logmodule.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LogAuditLogDto(
        Long id,
        String traceId,
        String clientCode,
        String clientType,
        Long userId,
        Long tenantId,
        String username,
        String moduleCode,
        String moduleName,
        String submoduleCode,
        String submoduleName,
        String featureCode,
        String featureName,
        String actionCode,
        String actionName,
        String accessMode,
        String action,
        String entityName,
        String entityId,
        Long businessId,
        Long branchId,
        String details,
        LocalDateTime createdAt
) {}
