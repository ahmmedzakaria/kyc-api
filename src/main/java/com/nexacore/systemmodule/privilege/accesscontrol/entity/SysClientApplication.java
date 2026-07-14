package com.nexacore.systemmodule.privilege.accesscontrol.entity;

import com.nexacore.systemmodule.privilege.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.privilege.accesscontrol.enums.ClientApplicationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sys_client_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysClientApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String clientCode;

    @Column(nullable = false)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClientApplicationType clientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClientApplicationStatus status;

    @Column(columnDefinition = "TEXT")
    private String allowedOrigins;

    @Column(columnDefinition = "TEXT")
    private String allowedIps;

    private Integer rateLimitPerMinute;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        status = status == null ? ClientApplicationStatus.ACTIVE : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
