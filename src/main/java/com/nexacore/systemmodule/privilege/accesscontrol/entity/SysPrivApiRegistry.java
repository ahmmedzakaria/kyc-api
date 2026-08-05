package com.nexacore.systemmodule.privilege.accesscontrol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_priv_api_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysPrivApiRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String apiCode;

    @Column(nullable = false, length = 20)
    private String httpMethod;

    @Column(nullable = false)
    private String pathPattern;

    @Column(length = 2)
    private String moduleCode;
    private String moduleName;

    @Column(length = 2)
    private String submoduleCode;
    private String submoduleName;

    @Column(length = 2)
    private String featureTypeCode;
    private String featureTypeName;

    @Column(length = 3)
    private String featureCode;
    private String featureName;

    @Column(length = 2)
    private String actionCode;
    private String actionName;

    @Column(length = 11)
    private String requiredPrivilegeCode;

    private boolean publicApi;
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
        httpMethod = httpMethod == null ? null : httpMethod.toUpperCase();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        httpMethod = httpMethod == null ? null : httpMethod.toUpperCase();
    }
}
