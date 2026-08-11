package com.nexacore.systemmodule.license.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sys_license_usage_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sys_license_usage_subscription_period_code",
                columnNames = {"license_subscription_id", "usage_period", "usage_code"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLicenseUsageSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_subscription_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLicenseSubscription licenseSubscription;

    @Column(nullable = false, updatable = false)
    private Long tenantId;
    private Long businessId;

    @Column(nullable = false, length = 20)
    private String usagePeriod;

    @Column(nullable = false, length = 80)
    private String usageCode;

    @Column(nullable = false)
    private Long usageValue;

    private LocalDateTime measuredAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        measuredAt = measuredAt == null ? now : measuredAt;
        usageValue = usageValue == null ? 0L : usageValue;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        measuredAt = measuredAt == null ? LocalDateTime.now() : measuredAt;
    }
}
