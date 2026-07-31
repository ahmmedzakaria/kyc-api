package com.nexacore.systemmodule.workflow.execution.repository;

import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowInstance;
import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowTask;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends JpaRepository<SysWorkflowTask, Long> {
    Optional<SysWorkflowTask> findFirstByWorkflowInstanceAndStatusInOrderByIdDesc(
            SysWorkflowInstance instance,
            Collection<WorkflowTaskStatus> statuses
    );

    List<SysWorkflowTask> findByStatusInOrderByCreatedAtDesc(Collection<WorkflowTaskStatus> statuses);
}
