package com.nexacore.systemmodule.workflow.definition.dto;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowAssignmentPolicyType;
import lombok.Builder;

@Builder
public record WorkflowAssignmentPolicyDto(
        Long id,
        String stepCode,
        WorkflowAssignmentPolicyType policyType,
        Long roleId,
        Long userId,
        String privilegeCode,
        Boolean branchScoped,
        Boolean businessScoped,
        String expressionKey,
        Boolean active
) {
}
