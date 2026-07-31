package com.nexacore.systemmodule.workflow.definition.dto;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowStepType;
import lombok.Builder;

@Builder
public record WorkflowStepDto(
        Long id,
        String stepCode,
        String stepName,
        String displayName,
        WorkflowStepType stepType,
        Boolean terminal,
        Integer sortOrder,
        Boolean active
) {
}
