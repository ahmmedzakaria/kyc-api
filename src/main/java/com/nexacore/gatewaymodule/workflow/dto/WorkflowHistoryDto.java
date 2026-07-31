package com.nexacore.gatewaymodule.workflow.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WorkflowHistoryDto(
        Long id,
        Long workflowInstanceId,
        Long workflowTaskId,
        String eventType,
        String actionCode,
        String fromStepCode,
        String toStepCode,
        Long actorUserId,
        String actorUsername,
        String commentText,
        String messageCode,
        String safeContextJson,
        LocalDateTime createdAt
) {
}
