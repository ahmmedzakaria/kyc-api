package com.nexacore.systemmodule.workflow.definition.entity;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowEngineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_workflow_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowDefinition extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_code", nullable = false, length = 100)
    private String workflowCode;

    @Column(name = "workflow_name", nullable = false, length = 180)
    private String workflowName;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "submodule_id")
    private Long submoduleId;

    @Column(name = "feature_id")
    private Long featureId;

    @Column(name = "subject_type", nullable = false, length = 100)
    private String subjectType;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "business_id")
    private Long businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 30)
    private WorkflowEngineType engineType;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    void defaults() {
        super.prePersist();
        engineType = engineType == null ? WorkflowEngineType.LOCAL : engineType;
    }
}
