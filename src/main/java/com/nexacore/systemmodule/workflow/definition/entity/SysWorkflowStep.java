package com.nexacore.systemmodule.workflow.definition.entity;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowStepType;
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

@Entity
@Table(name = "sys_workflow_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowStep extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_version_id", nullable = false)
    private SysWorkflowVersion workflowVersion;

    @Column(name = "step_code", nullable = false, length = 100)
    private String stepCode;

    @Column(name = "step_name", nullable = false, length = 180)
    private String stepName;

    @Column(name = "display_name", length = 180)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private WorkflowStepType stepType;

    @Column(nullable = false)
    private boolean terminal;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    void defaults() {
        super.prePersist();
        stepType = stepType == null ? WorkflowStepType.USER_TASK : stepType;
        sortOrder = sortOrder == null ? 100 : sortOrder;
    }
}
