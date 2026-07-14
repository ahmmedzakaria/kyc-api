package com.nexacore.systemmodule.license.entity;

import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import com.nexacore.systemmodule.privilege.catalog.entity.SysFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubmodule;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_license_plan_entitlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLicensePlanEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plan_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLicensePlan licensePlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LicenseEntitlementType entitlementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @EqualsAndHashCode.Exclude
    private SysModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submodule_id")
    @EqualsAndHashCode.Exclude
    private SysSubmodule submodule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id")
    @EqualsAndHashCode.Exclude
    private SysFeature feature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id")
    @EqualsAndHashCode.Exclude
    private SysPrivilege privilege;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_registry_id")
    @EqualsAndHashCode.Exclude
    private SysApiRegistry apiRegistry;

    @Column(length = 80)
    private String limitCode;

    private Long limitValue;

    @Column(nullable = false)
    private boolean active;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
