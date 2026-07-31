package com.nexacore.systemmodule.workflow.service.interfaces;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;

import java.util.List;

public interface WorkflowTaskService {
    List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request);

    WorkflowTaskDto getTask(WorkflowTaskDetailRequestDto request);
}
