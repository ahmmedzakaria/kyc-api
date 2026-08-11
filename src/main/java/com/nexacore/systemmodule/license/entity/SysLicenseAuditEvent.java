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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_license_audit_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLicenseAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_subscription_id")
    @EqualsAndHashCode.Exclude
    private SysLicenseSubscription licenseSubscription;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String eventMessageCode;

    private Long actorUserId;
    @Column(nullable = false, updatable = false)
    private Long tenantId;
    private Long businessId;
    private Long clientApplicationId;

    @Column(columnDefinition = "TEXT")
    private String safeContextJson;

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
}
