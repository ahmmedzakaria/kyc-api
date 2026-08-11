package com.nexacore.systemmodule.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_platform_admin_audit_events")
@Getter
@Setter
public class SysPlatformAdminAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long actorUserId;
    private Long actorTenantId;
    private Long targetTenantId;
    @Column(nullable = false, length = 100) private String actionCode;
    @Column(nullable = false, length = 30) private String outcome;
    @Column(length = 500) private String reason;
    @Column(length = 100) private String traceId;
    @Column(nullable = false) private Long createdBy;
    @Column(nullable = false) private Long updatedBy;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
