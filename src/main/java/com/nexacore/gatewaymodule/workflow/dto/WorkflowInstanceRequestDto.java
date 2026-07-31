package com.nexacore.gatewaymodule.workflow.dto;

import lombok.Builder;

@Builder
public record WorkflowInstanceRequestDto(
        Long workflowInstanceId,
        String subjectType,
        String subjectId
) {
}
