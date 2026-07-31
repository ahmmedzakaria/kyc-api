package com.nexacore.systemmodule.workflow.definition.service.interfaces;

import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;

import java.util.List;

public interface WorkflowProviderSyncService {
    List<WorkflowDefinitionDto> sync(String username);
}
