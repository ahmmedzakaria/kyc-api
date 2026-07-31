package com.nexacore.systemmodule.workflow.service.implementations;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDetailRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSearchRequestDto;
import com.nexacore.gatewaymodule.workflow.dto.WorkflowTaskSummaryDto;
import com.nexacore.systemmodule.workflow.engine.interfaces.WorkflowEngine;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    private final WorkflowEngine workflowEngine;

    @Override
    public List<WorkflowTaskSummaryDto> findUserTasks(WorkflowTaskSearchRequestDto request) {
        return workflowEngine.findUserTasks(request);
    }

    @Override
    public WorkflowTaskDto getTask(WorkflowTaskDetailRequestDto request) {
        return workflowEngine.getTask(request);
    }
}
