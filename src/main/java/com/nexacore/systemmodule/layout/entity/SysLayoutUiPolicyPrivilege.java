package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_ui_policy_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutUiPolicyPrivilege extends LayoutAuditInfo {
    @EmbeddedId
    private SysLayoutUiPolicyPrivilegeId id;

    @MapsId("uiPolicyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ui_policy_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLayoutUiPolicy uiPolicy;

    @MapsId("privilegeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPrivPrivilege privilege;

    @Column(nullable = false)
    private boolean active;
}
