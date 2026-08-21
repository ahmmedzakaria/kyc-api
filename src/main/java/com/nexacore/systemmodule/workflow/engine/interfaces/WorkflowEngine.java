package com.nexacore.systemmodule.workflow.engine.interfaces;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowActionRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowDecisionResponseDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceListRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowInstanceRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowStartRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;

import java.util.List;

public interface WorkflowEngine {
    WorkflowInstanceDto start(WorkflowStartRequestDto request);

    WorkflowDecisionResponseDto canPerformAction(WorkflowActionRequestDto request);

    WorkflowTaskDto completeTask(WorkflowActionRequestDto request);

    List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request);

    WorkflowTaskDto getTask(WorkflowTaskDetailRequestDto request);

    WorkflowInstanceDto getInstance(WorkflowInstanceRequestDto request);

    List<WorkflowInstanceDto> listInstances(WorkflowInstanceListRequestDto request);
}
