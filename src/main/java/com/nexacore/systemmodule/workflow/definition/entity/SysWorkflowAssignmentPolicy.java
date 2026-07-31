package com.nexacore.systemmodule.workflow.definition.entity;

import com.nexacore.systemmodule.workflow.definition.enums.WorkflowAssignmentPolicyType;
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
@Table(name = "sys_workflow_assignment_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysWorkflowAssignmentPolicy extends WorkflowAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_version_id", nullable = false)
    private SysWorkflowVersion workflowVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "step_id", nullable = false)
    private SysWorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 40)
    private WorkflowAssignmentPolicyType policyType;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "privilege_code", length = 120)
    private String privilegeCode;

    @Column(name = "branch_scoped", nullable = false)
    private boolean branchScoped;

    @Column(name = "business_scoped", nullable = false)
    private boolean businessScoped;

    @Column(name = "expression_key", length = 120)
    private String expressionKey;

    @Column(nullable = false)
    private boolean active;
}
