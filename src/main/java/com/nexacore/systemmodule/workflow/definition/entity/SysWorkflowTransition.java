package com.nexacore.systemmodule.workflow.definition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_workflow_transitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowTransition extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_version_id", nullable = false)
    private SysWorkflowVersion workflowVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_step_id", nullable = false)
    private SysWorkflowStep fromStep;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_step_id", nullable = false)
    private SysWorkflowStep toStep;

    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    @Column(name = "action_name", nullable = false, length = 180)
    private String actionName;

    @Column(name = "required_privilege_code", length = 120)
    private String requiredPrivilegeCode;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment;

    @Column(name = "requires_attachment", nullable = false)
    private boolean requiresAttachment;

    @Column(name = "auto_assign_next_task", nullable = false)
    private boolean autoAssignNextTask;

    @Column(nullable = false)
    private boolean active;
}
