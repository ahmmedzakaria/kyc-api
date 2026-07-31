package com.nexacore.systemmodule.workflow.execution.repository;

import com.nexacore.systemmodule.workflow.execution.entity.SysWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowInstanceRepository extends JpaRepository<SysWorkflowInstance, Long> {
    Optional<SysWorkflowInstance> findFirstBySubjectTypeAndSubjectIdOrderByIdDesc(String subjectType, String subjectId);
}
