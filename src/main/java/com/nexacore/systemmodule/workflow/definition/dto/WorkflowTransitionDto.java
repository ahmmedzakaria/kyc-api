package com.nexacore.systemmodule.workflow.definition.dto;

import lombok.Builder;

@Builder
public record WorkflowTransitionDto(
        Long id,
        String fromStepCode,
        String toStepCode,
        String actionCode,
        String actionName,
        String requiredPrivilegeCode,
        Boolean requiresComment,
        Boolean requiresAttachment,
        Boolean autoAssignNextTask,
        Boolean active
) {
}
