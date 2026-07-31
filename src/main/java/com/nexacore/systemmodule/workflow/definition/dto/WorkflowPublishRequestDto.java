package com.nexacore.systemmodule.workflow.definition.dto;

public record WorkflowPublishRequestDto(
        String workflowCode,
        String subjectType,
        Long tenantId,
        Long businessId,
        Integer versionNumber
) {
}
