package com.nexacore.gatewaymodule.workflow.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record WorkflowActionRequestDto(
        Long workflowInstanceId,
        Long workflowTaskId,
        String actionCode,
        Long actorUserId,
        String username,
        String comment,
        Set<Long> roleIds,
        Set<String> privilegeCodes,
        String safeContextJson
) {
}
