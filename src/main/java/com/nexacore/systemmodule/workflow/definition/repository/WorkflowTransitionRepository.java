package com.nexacore.systemmodule.workflow.definition.repository;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowTransition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<SysWorkflowTransition, Long> {
    Optional<SysWorkflowTransition> findByWorkflowVersionAndFromStepAndActionCodeAndActiveTrue(
            SysWorkflowVersion version,
            SysWorkflowStep fromStep,
            String actionCode
    );

    List<SysWorkflowTransition> findByWorkflowVersionAndFromStepAndActiveTrue(SysWorkflowVersion version, SysWorkflowStep fromStep);

    void deleteByWorkflowVersion(SysWorkflowVersion version);
}
