package com.nexacore.systemmodule.workflow.service.interfaces;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;

public interface WorkflowRuntimeService {
    WorkflowInstanceDto startWorkflow(WorkflowStartRequestDto request);

    WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request);

    WorkflowTaskDto completeTask(WorkflowActionRequestDto request);

    WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request);
}
