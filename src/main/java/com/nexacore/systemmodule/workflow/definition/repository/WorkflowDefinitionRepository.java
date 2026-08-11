package com.nexacore.systemmodule.workflow.definition.repository;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<SysWorkflowDefinition, Long> {
    Optional<SysWorkflowDefinition> findFirstByWorkflowCodeAndSubjectTypeAndTenantIdAndBusinessIdAndActiveTrue(
            String workflowCode,
            String subjectType,
            Long tenantId,
            Long businessId
    );

    List<SysWorkflowDefinition> findByActiveTrueOrderByWorkflowCodeAsc();
    List<SysWorkflowDefinition> findByTenantIdAndActiveTrueOrderByWorkflowCodeAsc(Long tenantId);
    List<SysWorkflowDefinition> findByTenantIdOrderByWorkflowCodeAsc(Long tenantId);
    Optional<SysWorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);
}
