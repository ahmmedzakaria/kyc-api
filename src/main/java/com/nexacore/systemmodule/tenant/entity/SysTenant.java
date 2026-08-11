package com.nexacore.systemmodule.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_tenants")
@Getter
@Setter
public class SysTenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 63)
    private String tenantCode;
    @Column(nullable = false)
    private String displayName;
    private String legalName;
    private String registrationNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.PENDING;
    @Column(nullable = false)
    private String defaultLocale = "en";
    @Column(nullable = false)
    private String defaultTimeZone = "UTC";
    private String billingEmail;
    @Version
    private long version;
    private LocalDateTime activatedAt;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    @Column(nullable = false)
    private Long createdBy;
    @Column(nullable = false)
    private Long updatedBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void createAudit() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void updateAudit() {
        updatedAt = LocalDateTime.now();
    }
}
