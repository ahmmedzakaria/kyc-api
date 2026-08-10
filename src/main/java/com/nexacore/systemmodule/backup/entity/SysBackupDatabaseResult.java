package com.nexacore.systemmodule.backup.entity;

import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_backup_database_results")
@Getter
@Setter
@NoArgsConstructor
public class SysBackupDatabaseResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "backup_job_id", nullable = false)
    private SysBackupJob backupJob;

    @Column(name = "logical_database", nullable = false, length = 40)
    private String logicalDatabase;
    @Column(name = "database_name", nullable = false, length = 120)
    private String databaseName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupJobStatus status;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "size_bytes")
    private Long sizeBytes;
    @Column(length = 64)
    private String sha256;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "sanitized_failure_message", length = 1000)
    private String sanitizedFailureMessage;
    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy = 0L;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
