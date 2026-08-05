package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
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
@Table(name = "sys_layout_feature_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutFeaturePrivilege extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_feature_id", nullable = false)
    private SysLayoutFeature layoutFeature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id", nullable = false)
    private SysPrivPrivilege privilege;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false, length = 20)
    private PrivilegeMatchMode matchMode;

    @Column(nullable = false)
    private boolean active;
}
