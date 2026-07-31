package com.nexacore.gatewaymodule.workflow.dto;

import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
import lombok.Builder;

import java.util.Set;

@Builder
public record WorkflowTaskSearchRequestDto(
        Long userId,
        String username,
        Set<Long> roleIds,
        Set<String> privilegeCodes,
        Long tenantId,
        Long businessId,
        Long branchId,
        Set<WorkflowTaskStatus> statuses
) {
}
