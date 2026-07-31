package com.nexacore.systemmodule.workflow.definition.repository;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<SysWorkflowStep, Long> {
    Optional<SysWorkflowStep> findByWorkflowVersionAndStepCode(SysWorkflowVersion version, String stepCode);

    List<SysWorkflowStep> findByWorkflowVersionAndActiveTrueOrderBySortOrderAscIdAsc(SysWorkflowVersion version);
}
