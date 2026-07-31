package com.nexacore.systemmodule.workflow.definition.repository;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowAssignmentPolicy;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowAssignmentPolicyRepository extends JpaRepository<SysWorkflowAssignmentPolicy, Long> {
    List<SysWorkflowAssignmentPolicy> findByWorkflowVersionAndStepAndActiveTrue(SysWorkflowVersion version, SysWorkflowStep step);

    void deleteByWorkflowVersion(SysWorkflowVersion version);
}
