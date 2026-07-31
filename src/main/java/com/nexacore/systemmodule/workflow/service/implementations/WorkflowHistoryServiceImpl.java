package com.nexacore.systemmodule.workflow.service.implementations;

import com.nexacore.gatewaymodule.workflow.dto.WorkflowHistoryDto;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowHistory;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowInstance;
import com.nexacore.systemmodule.workflow.execution.repository.WorkflowHistoryRepository;
import com.nexacore.systemmodule.workflow.execution.repository.WorkflowInstanceRepository;
import com.nexacore.systemmodule.workflow.service.interfaces.WorkflowHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowHistoryServiceImpl implements WorkflowHistoryService {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowHistoryRepository historyRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<WorkflowHistoryDto> getHistory(Long workflowInstanceId) {
        SysWorkflowInstance instance = instanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowInstanceId));
        return historyRepository.findByWorkflowInstanceOrderByCreatedAtAscIdAsc(instance).stream()
                .map(this::toDto)
                .toList();
    }

    private WorkflowHistoryDto toDto(SysWorkflowHistory history) {
        return WorkflowHistoryDto.builder()
                .id(history.getId())
                .workflowInstanceId(history.getWorkflowInstance().getId())
                .workflowTaskId(history.getWorkflowTask() == null ? null : history.getWorkflowTask().getId())
                .eventType(history.getEventType().name())
                .actionCode(history.getActionCode())
                .fromStepCode(history.getFromStepCode())
                .toStepCode(history.getToStepCode())
                .actorUserId(history.getActorUserId())
                .actorUsername(history.getActorUsername())
                .commentText(history.getCommentText())
                .messageCode(history.getMessageCode())
                .safeContextJson(history.getSafeContextJson())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
