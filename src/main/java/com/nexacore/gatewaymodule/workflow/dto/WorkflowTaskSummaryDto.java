package com.nexacore.gatewaymodule.workflow.dto;

import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WorkflowTaskSummaryDto(
        Long taskId,
        Long workflowInstanceId,
        String workflowCode,
        String subjectType,
        String subjectId,
        String stepCode,
        String stepName,
        WorkflowTaskStatus status,
        Long assignedUserId,
        Long assignedRoleId,
        String assignedPrivilegeCode,
        Long businessId,
        Long branchId,
        LocalDateTime createdAt,
        LocalDateTime dueAt
) {
}
