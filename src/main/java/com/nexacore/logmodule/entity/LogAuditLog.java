package com.nexacore.logmodule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "log_audit_log")
public class LogAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String traceId;
    private String clientCode;
    private String clientType;
    private Long userId;
    private Long tenantId;
    private String username;
    private String moduleCode;
    private String moduleName;
    private String submoduleCode;
    private String submoduleName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private String accessMode;
    private String action;
    private String entityName;
    private String entityId;
    private Long businessId;
    private Long branchId;

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime createdAt = LocalDateTime.now();
    private Long createdBy = 0L;
    private Long updatedBy = 0L;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
