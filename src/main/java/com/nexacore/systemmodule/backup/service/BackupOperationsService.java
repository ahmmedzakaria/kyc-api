package com.nexacore.systemmodule.backup.service;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.dto.*;
import com.nexacore.systemmodule.backup.entity.*;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.exception.BackupApiException;
import com.nexacore.systemmodule.backup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class BackupOperationsService {
    private final BackupJobRepository jobs;
    private final BackupDeliveryAttemptRepository deliveries;
    private final BackupRestoreVerificationRepository verifications;
    private final BackupNotificationService notification;
    private final BackupProperties properties;

    @Transactional(transactionManager="systemTransactionManager")
    public BackupDeliveryAttemptDto retryDelivery(UUID backupId,String actor){
        SysBackupJob job=required(backupId);
        if(job.getStatus()!=BackupJobStatus.COMPLETED && job.getStatus()!=BackupJobStatus.PARTIAL)
            throw new BackupApiException(HttpStatus.CONFLICT,"DATABASE_BACKUP_DELIVERY_NOT_READY","Only completed backup artifacts can be delivered");
        SysBackupDeliveryAttempt attempt=new SysBackupDeliveryAttempt(); attempt.setBackupJob(job); attempt.setChannel("EMAIL");
        attempt.setAttemptedBy(actor==null||actor.isBlank()?"system":actor); attempt.setAttemptedAt(LocalDateTime.now());
        try { notification.deliver(job); attempt.setStatus("DELIVERED"); }
        catch(RuntimeException ex){ attempt.setStatus("FAILED"); attempt.setFailureCode("DATABASE_BACKUP_DELIVERY_FAILED"); attempt.setSanitizedFailureMessage("Backup delivery failed; inspect the configured delivery service"); }
        deliveries.save(attempt);
        return delivery(attempt);
    }
    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public List<BackupDeliveryAttemptDto> deliveryHistory(UUID id){ required(id); return deliveries.findByBackupJobIdOrderByAttemptedAtDesc(id).stream().map(this::delivery).toList(); }
    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public BackupConfigurationDto configuration(){return new BackupConfigurationDto(properties.isEnabled(),properties.getCron(),properties.getZone(),properties.getRetentionDays(),properties.getMinimumSuccessful(),properties.getTimeout().toSeconds(),"LOCAL_ENCRYPTED",!properties.getEncryptionKeyBase64().isBlank(),properties.getEncryptionKeyId(),properties.isEmailEnabled(),properties.getEmailRecipients().size(),properties.isEmailAttachmentEnabled(),properties.getEmailAttachmentMaxBytes());}
    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public List<BackupRestoreVerificationDto> verificationHistory(){return verifications.findTop100ByOrderByVerifiedAtDesc().stream().map(this::verification).toList();}
    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public BackupReadinessDto readiness(){
        var completed=jobs.findByStatusOrderByCompletedAtDesc(BackupJobStatus.COMPLETED); SysBackupJob latest=completed.isEmpty()?null:completed.getFirst();
        var verification=latest==null?Optional.<SysBackupRestoreVerification>empty():verifications.findFirstByBackupJobIdOrderByVerifiedAtDesc(latest.getId());
        boolean encrypted=!properties.getEncryptionKeyBase64().isBlank(); boolean verified=verification.map(v->"PASSED".equals(v.getStatus())).orElse(false);
        boolean replicated=verification.map(v->"REPLICATED".equals(v.getOffsiteReplicationStatus())).orElse(false);
        String warning=!properties.isEnabled()?"DATABASE_BACKUP_DISABLED":!encrypted?"DATABASE_BACKUP_ENCRYPTION_NOT_CONFIGURED":latest==null?"DATABASE_BACKUP_MISSING":!verified?"DATABASE_BACKUP_RESTORE_UNVERIFIED":!replicated?"DATABASE_BACKUP_OFFSITE_UNVERIFIED":null;
        return new BackupReadinessDto(properties.isEnabled(),encrypted,latest==null?null:latest.getId(),latest==null?null:latest.getCompletedAt(),latest==null?null:latest.getArtifactSha256(),verification.map(SysBackupRestoreVerification::getStatus).orElse("NOT_VERIFIED"),verification.map(SysBackupRestoreVerification::getVerifiedAt).orElse(null),verification.map(SysBackupRestoreVerification::getOffsiteReplicationStatus).orElse("NOT_RECORDED"),warning==null,warning);
    }
    private SysBackupJob required(UUID id){return jobs.findById(id).orElseThrow(()->new BackupApiException(HttpStatus.NOT_FOUND,"DATABASE_BACKUP_JOB_NOT_FOUND","Database backup job not found"));}
    private BackupDeliveryAttemptDto delivery(SysBackupDeliveryAttempt x){return new BackupDeliveryAttemptDto(x.getId(),x.getChannel(),x.getStatus(),x.getAttemptedBy(),x.getAttemptedAt(),x.getFailureCode(),x.getSanitizedFailureMessage());}
    private BackupRestoreVerificationDto verification(SysBackupRestoreVerification x){return new BackupRestoreVerificationDto(x.getId(),x.getBackupJob().getId(),x.getStatus(),x.getArtifactSha256(),x.getVerifiedAt(),x.getVerifierId(),x.getSanitizedFailureMessage(),x.getOffsiteReplicationStatus(),x.getOffsiteReference());}
}
