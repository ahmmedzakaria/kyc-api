package com.nexacore.systemmodule.workflow.service.implementations;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceListRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.systemmodule.workflow.engine.interfaces.WorkflowEngine;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowRuntimeServiceImpl implements WorkflowRuntimeService {

    private final WorkflowEngine workflowEngine;

    @Override
    public WorkflowInstanceDto startWorkflow(WorkflowStartRequestDto request) {
        return workflowEngine.start(request);
    }

    @Override
    public WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request) {
        return workflowEngine.canPerformAction(request);
    }

    @Override
    public WorkflowTaskDto completeTask(WorkflowActionRequestDto request) {
        return workflowEngine.completeTask(request);
    }

    @Override
    public WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request) {
        return workflowEngine.getInstance(request);
    }

    @Override
    public List<WorkflowInstanceDto> listInstances(WorkflowInstanceListRequestDto request) {
        return workflowEngine.listInstances(request);
    }
}
