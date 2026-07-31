package com.nexacore.systemmodule.workflow.service.interfaces;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowHistoryDto;

import java.util.List;

public interface WorkflowHistoryService {
    List<WorkflowHistoryDto> getHistory(Long workflowInstanceId);
}
