package com.nexacore.gatewaymodule.workflow.service.interfaces;

import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;

import java.util.List;

public interface WorkflowModuleGateway extends ModuleGateway {
    WorkflowInstanceDto startWorkflow(WorkflowStartRequestDto request);

    WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request);

    WorkflowTaskDto completeTask(WorkflowActionRequestDto request);

    List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request);

    WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request);
}
