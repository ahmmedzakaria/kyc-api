package com.nexacore.systemmodule.backup.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="sys_backup_restore_verifications") @Getter @Setter
public class SysBackupRestoreVerification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="backup_job_id",nullable=false) private SysBackupJob backupJob;
    @Column(nullable=false,length=20) private String status;
    @Column(name="artifact_sha256",nullable=false,length=64) private String artifactSha256;
    @Column(name="verified_at",nullable=false) private LocalDateTime verifiedAt;
    @Column(name="verifier_id",nullable=false,length=150) private String verifierId;
    @Column(name="sanitized_failure_message",length=1000) private String sanitizedFailureMessage;
    @Column(name="offsite_replication_status",nullable=false,length=30) private String offsiteReplicationStatus;
    @Column(name="offsite_reference",length=250) private String offsiteReference;
    @Column(name="created_by",nullable=false) private Long createdBy=0L;
    @Column(name="updated_by",nullable=false) private Long updatedBy=0L;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now(); verifiedAt=verifiedAt==null?now:verifiedAt; createdAt=createdAt==null?now:createdAt; updatedAt=updatedAt==null?now:updatedAt;}
}
