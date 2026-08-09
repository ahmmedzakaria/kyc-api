package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.layout.enums.AssignmentScope;
import com.nexacore.systemmodule.layout.enums.DeviceTarget;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
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
@Table(name = "sys_client_layout_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysClientLayoutProfile extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_application_id", nullable = false)
    private SysAccClientApplication clientApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_id", nullable = false)
    private SysLayoutProfile layoutProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_scope", nullable = false, length = 30)
    private AssignmentScope assignmentScope;

    @Column(name = "role_code", length = 80)
    private String roleCode;

    @Column(name = "privilege_code", length = 80)
    private String privilegeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_target", nullable = false, length = 30)
    private DeviceTarget deviceTarget;

    @Column(name = "module_code", length = 80)
    private String moduleCode;

    @Column(name = "default_profile", nullable = false)
    private boolean defaultProfile;

    @Column(nullable = false)
    private boolean selectable;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active;
}
