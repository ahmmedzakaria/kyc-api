package com.nexacore.gatewaymodule.workflow.dto;

import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record WorkflowTaskDto(
        Long taskId,
        Long workflowInstanceId,
        String stepCode,
        String stepName,
        WorkflowTaskStatus status,
        Long assignedUserId,
        Long assignedRoleId,
        String assignedPrivilegeCode,
        Long claimedByUserId,
        LocalDateTime claimedAt,
        LocalDateTime completedAt,
        List<String> availableActionCodes
) {
}
