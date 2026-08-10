package com.nexacore.systemmodule.backup.entity;

import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sys_backup_jobs")
@Getter
@Setter
@NoArgsConstructor
public class SysBackupJob {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private BackupTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupJobStatus status;

    @Column(name = "requested_by", nullable = false, length = 150)
    private String requestedBy;
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "artifact_name")
    private String artifactName;
    @Column(name = "artifact_size_bytes")
    private Long artifactSizeBytes;
    @Column(name = "artifact_sha256", length = 64)
    private String artifactSha256;
    @Column(name = "encryption_key_id", length = 120)
    private String encryptionKeyId;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "sanitized_failure_message", length = 1000)
    private String sanitizedFailureMessage;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy = 0L;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "backupJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("logicalDatabase ASC")
    private List<SysBackupDatabaseResult> databaseResults = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        id = id == null ? UUID.randomUUID() : id;
        requestedAt = requestedAt == null ? now : requestedAt;
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
