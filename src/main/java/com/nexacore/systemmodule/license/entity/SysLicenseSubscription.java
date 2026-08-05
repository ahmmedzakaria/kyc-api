package com.nexacore.systemmodule.license.entity;

import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;
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
@Table(name = "sys_license_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLicenseSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String subscriptionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plan_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLicensePlan licensePlan;

    private Long tenantId;
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_application_id")
    @EqualsAndHashCode.Exclude
    private SysPrivClientApplication clientApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LicenseStatus status;

    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime gracePeriodEndsAt;
    private boolean autoRenew;
    private LocalDateTime cancelledAt;
    private LocalDateTime suspendedAt;

    @Column(columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        status = status == null ? LicenseStatus.DRAFT : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
