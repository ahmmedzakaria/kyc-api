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
@Table(name = "log_error_log")
public class LogErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String uri;
    private int status;
    private String traceId;
    private String clientCode;
    private String clientType;
    private Long userId;
    private String username;
    private String apiCode;
    private String moduleCode;
    private String moduleName;
    private String submoduleCode;
    private String submoduleName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private String accessMode;
    private Long businessId;
    private Long branchId;
    private String errorType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private LocalDateTime createdAt = LocalDateTime.now();
}
