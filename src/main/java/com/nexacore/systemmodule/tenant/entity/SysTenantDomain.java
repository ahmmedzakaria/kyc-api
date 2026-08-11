package com.nexacore.systemmodule.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_tenant_domains")
@Getter
@Setter
public class SysTenantDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private SysTenant tenant;
    @Column(nullable = false, length = 253)
    private String hostname;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantDomainType domainType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantDomainVerificationStatus verificationStatus = TenantDomainVerificationStatus.PENDING;
    private String verificationTokenHash;
    private LocalDateTime verificationExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastCheckedAt;
    private boolean primaryDomain;
    private boolean active = true;
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
    @PreUpdate void updateAudit() { updatedAt = LocalDateTime.now(); }
}
