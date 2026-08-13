package com.nexacore.logmodule.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LogErrorLogDto(
        Long id,
        String method,
        String uri,
        int status,
        String traceId,
        String clientCode,
        String clientType,
        Long userId,
        Long tenantId,
        String username,
        String apiCode,
        String moduleCode,
        String moduleName,
        String submoduleCode,
        String submoduleName,
        String featureCode,
        String featureName,
        String actionCode,
        String actionName,
        String accessMode,
        Long businessId,
        Long branchId,
        String errorType,
        String message,
        String requestBody,
        String responseBody,
        LocalDateTime createdAt
) {}
