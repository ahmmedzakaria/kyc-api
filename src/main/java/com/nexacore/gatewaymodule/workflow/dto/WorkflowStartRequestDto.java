package com.nexacore.gatewaymodule.workflow.dto;

import lombok.Builder;

@Builder
public record WorkflowStartRequestDto(
        String workflowCode,
        String subjectType,
        String subjectId,
        Long tenantId,
        Long businessId,
        Long branchId,
        Long requesterUserId,
        String username,
        String safeContextJson
) {
}
