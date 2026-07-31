package com.nexacore.systemmodule.workflow.provider;

import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;

import java.util.List;

public interface ModuleWorkflowProvider {
    List<WorkflowDefinitionDto> getWorkflowDefinitions();
}
