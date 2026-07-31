package com.nexacore.systemmodule.workflow.definition.service.interfaces;

import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;
import com.nexacore.systemmodule.workflow.definition.dto.WorkflowPublishRequestDto;

import java.util.List;

public interface WorkflowDefinitionService {
    WorkflowDefinitionDto save(WorkflowDefinitionDto request, String username);

    List<WorkflowDefinitionDto> list(boolean activeOnly);

    WorkflowDefinitionDto publish(WorkflowPublishRequestDto request, String username);

    WorkflowDefinitionDto retire(WorkflowPublishRequestDto request, String username);
}
