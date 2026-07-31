package com.nexacore.systemmodule.workflow.definition.repository;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowDefinition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import com.nexacore.systemmodule.workflow.definition.enums.WorkflowDefinitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowVersionRepository extends JpaRepository<SysWorkflowVersion, Long> {
    Optional<SysWorkflowVersion> findFirstByWorkflowDefinitionAndStatusAndActiveTrueOrderByVersionNumberDesc(
            SysWorkflowDefinition definition,
            WorkflowDefinitionStatus status
    );

    Optional<SysWorkflowVersion> findByWorkflowDefinitionAndVersionNumber(SysWorkflowDefinition definition, Integer versionNumber);

    List<SysWorkflowVersion> findByWorkflowDefinitionOrderByVersionNumberDesc(SysWorkflowDefinition definition);
}
