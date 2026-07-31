package com.nexacore.systemmodule.workflow.definition.dto;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowDefinitionStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record WorkflowVersionDto(
        Long id,
        Integer versionNumber,
        WorkflowDefinitionStatus status,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        LocalDateTime publishedAt,
        Boolean active,
        List<WorkflowStepDto> steps,
        List<WorkflowTransitionDto> transitions,
        List<WorkflowAssignmentPolicyDto> assignmentPolicies
) {
}
