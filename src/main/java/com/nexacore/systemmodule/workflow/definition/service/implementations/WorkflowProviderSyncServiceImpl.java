package com.nexacore.systemmodule.workflow.definition.service.implementations;

import com.nexacore.systemmodule.workflow.definition.dto.WorkflowDefinitionDto;
import com.nexacore.systemmodule.workflow.definition.service.interfaces.WorkflowDefinitionService;
import com.nexacore.systemmodule.workflow.definition.service.interfaces.WorkflowProviderSyncService;
import com.nexacore.systemmodule.workflow.provider.ModuleWorkflowProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowProviderSyncServiceImpl implements WorkflowProviderSyncService {

    private final List<ModuleWorkflowProvider> providers;
    private final WorkflowDefinitionService workflowDefinitionService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public List<WorkflowDefinitionDto> sync(String username) {
        return providers.stream()
                .flatMap(provider -> provider.getWorkflowDefinitions().stream())
                .map(definition -> workflowDefinitionService.save(definition, username))
                .toList();
    }
}
