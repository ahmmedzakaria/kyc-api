package com.nexacore.gatewaymodule.workflow.dto;

import com.nexacore.systemmodule.workflow.execution.enums.WorkflowInstanceStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WorkflowInstanceDto(
        Long id,
        String workflowCode,
        String subjectType,
        String subjectId,
        Long tenantId,
        Long businessId,
        Long branchId,
        Long requesterUserId,
        String currentStepCode,
        String currentStepName,
        WorkflowInstanceStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
