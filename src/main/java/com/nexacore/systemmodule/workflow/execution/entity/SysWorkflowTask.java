package com.nexacore.systemmodule.workflow.execution.entity;

import com.nexacore.systemmodule.workflow.definition.entity.SysWorkflowStep;
import com.nexacore.systemmodule.workflow.definition.entity.WorkflowAuditInfo;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowTaskStatus;
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
@Table(name = "sys_workflow_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowTask extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private SysWorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "step_id", nullable = false)
    private SysWorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowTaskStatus status;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "assigned_role_id")
    private Long assignedRoleId;

    @Column(name = "assigned_privilege_code", length = 120)
    private String assignedPrivilegeCode;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "claimed_by_user_id")
    private Long claimedByUserId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "engine_task_id", length = 120)
    private String engineTaskId;

    @PrePersist
    void defaults() {
        super.prePersist();
        status = status == null ? WorkflowTaskStatus.OPEN : status;
    }
}
