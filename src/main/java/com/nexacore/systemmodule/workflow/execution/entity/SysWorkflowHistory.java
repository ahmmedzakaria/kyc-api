package com.nexacore.systemmodule.workflow.execution.entity;

import com.nexacore.systemmodule.workflow.definition.entity.WorkflowAuditInfo;
import com.nexacore.systemmodule.workflow.execution.enums.WorkflowHistoryEventType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_workflow_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowHistory extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private SysWorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_task_id")
    private SysWorkflowTask workflowTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private WorkflowHistoryEventType eventType;

    @Column(name = "action_code", length = 100)
    private String actionCode;

    @Column(name = "from_step_code", length = 100)
    private String fromStepCode;

    @Column(name = "to_step_code", length = 100)
    private String toStepCode;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 120)
    private String actorUsername;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "message_code", length = 120)
    private String messageCode;

    @Column(name = "safe_context_json", columnDefinition = "TEXT")
    private String safeContextJson;
}
