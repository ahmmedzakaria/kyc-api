package com.nexacore.systemmodule.workflow.execution.entity;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowDefinition;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowVersion;
import com.nexacore.systemmodule.workflow.definition.entity.WorkflowAuditInfo;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowInstanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_workflow_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowInstance extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private SysWorkflowDefinition workflowDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_version_id", nullable = false)
    private SysWorkflowVersion workflowVersion;

    @Column(name = "workflow_code", nullable = false, length = 100)
    private String workflowCode;

    @Column(name = "subject_type", nullable = false, length = 100)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 120)
    private String subjectId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id")
    private SysWorkflowStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowInstanceStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "engine_instance_id", length = 120)
    private String engineInstanceId;

    @PrePersist
    void defaults() {
        super.prePersist();
        status = status == null ? WorkflowInstanceStatus.RUNNING : status;
        startedAt = startedAt == null ? LocalDateTime.now() : startedAt;
    }
}
