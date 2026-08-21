package com.nexacore.gatewaymodule.workflow.dto;

import com.nexacore.systemmodule.workflow.execution.enums.WorkflowInstanceStatus;
import lombok.Builder;

@Builder
public record WorkflowInstanceListRequestDto(
        String workflowCode,
        String subjectType,
        WorkflowInstanceStatus status,
        Long businessId,
        Long branchId
) {
}
