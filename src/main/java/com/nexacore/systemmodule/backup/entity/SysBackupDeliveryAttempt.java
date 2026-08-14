package com.nexacore.systemmodule.backup.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="sys_backup_delivery_attempts") @Getter @Setter
public class SysBackupDeliveryAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="backup_job_id", nullable=false) private SysBackupJob backupJob;
    @Column(nullable=false,length=20) private String channel;
    @Column(nullable=false,length=20) private String status;
    @Column(name="attempted_by",nullable=false,length=150) private String attemptedBy;
    @Column(name="attempted_at",nullable=false) private LocalDateTime attemptedAt;
    @Column(name="failure_code",length=80) private String failureCode;
    @Column(name="sanitized_failure_message",length=1000) private String sanitizedFailureMessage;
    @Column(name="created_by",nullable=false) private Long createdBy=0L;
    @Column(name="updated_by",nullable=false) private Long updatedBy=0L;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now(); attemptedAt=attemptedAt==null?now:attemptedAt; createdAt=createdAt==null?now:createdAt; updatedAt=updatedAt==null?now:updatedAt;}
}
