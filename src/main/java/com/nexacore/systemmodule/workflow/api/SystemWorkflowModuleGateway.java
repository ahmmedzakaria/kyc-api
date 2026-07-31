package com.nexacore.systemmodule.workflow.api;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;
import com.nexacore.gatewaymodule.workflow.service.interfaces.WorkflowModuleGateway;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowRuntimeService;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SystemWorkflowModuleGateway implements WorkflowModuleGateway {

    private final WorkflowRuntimeService workflowRuntimeService;
    private final WorkflowTaskService workflowTaskService;

    @Override
    public WorkflowInstanceDto startWorkflow(WorkflowStartRequestDto request) {
        return workflowRuntimeService.startWorkflow(request);
    }

    @Override
    public WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request) {
        return workflowRuntimeService.canPerformAction(request);
    }

    @Override
    public WorkflowTaskDto completeTask(WorkflowActionRequestDto request) {
        return workflowRuntimeService.completeTask(request);
    }

    @Override
    public List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request) {
        return workflowTaskService.findUserTasks(request);
    }

    @Override
    public WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request) {
        return workflowRuntimeService.getInstance(request);
    }
}
