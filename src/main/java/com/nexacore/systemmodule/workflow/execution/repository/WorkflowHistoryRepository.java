package com.nexacore.systemmodule.workflow.execution.repository;

import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowHistory;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowHistoryRepository extends JpaRepository<SysWorkflowHistory, Long> {
    List<SysWorkflowHistory> findByWorkflowInstanceOrderByCreatedAtAscIdAsc(SysWorkflowInstance instance);
}
