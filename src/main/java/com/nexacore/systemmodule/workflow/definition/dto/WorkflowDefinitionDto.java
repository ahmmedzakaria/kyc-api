package com.nexacore.systemmodule.workflow.definition.dto;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowEngineType;
import lombok.Builder;

import java.util.List;

@Builder
public record WorkflowDefinitionDto(
        Long id,
        String workflowCode,
        String workflowName,
        Long moduleId,
        Long submoduleId,
        Long featureId,
        String subjectType,
        Long tenantId,
        Long businessId,
        WorkflowEngineType engineType,
        Boolean active,
        List<WorkflowVersionDto> versions
) {
}
