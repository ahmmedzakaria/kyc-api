package com.nexacore.systemmodule.backup.service;

import com.nexacore.servicesmodule.emailservice.dto.EmailMessageRequest;
import com.nexacore.servicesmodule.emailservice.dto.EmailAttachmentRequest;
import com.nexacore.servicesmodule.emailservice.enums.EmailMessageType;
import com.nexacore.servicesmodule.emailservice.service.interfaces.EmailMessagingService;
import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupNotificationService {
    private final BackupProperties properties;
    private final EmailMessagingService emailMessagingService;

    public void notifyResult(SysBackupJob job) {
        if (!properties.isEmailEnabled() || properties.getEmailRecipients().isEmpty()) return;
        try {
            String subject = "NexaCore database backup " + job.getStatus() + " - " + job.getId();
            String body = "Backup ID: " + job.getId() + "\n"
                    + "Status: " + job.getStatus() + "\n"
                    + "Requested by: " + job.getRequestedBy() + "\n"
                    + "Started: " + job.getStartedAt() + "\n"
                    + "Completed: " + job.getCompletedAt() + "\n"
                    + "Encrypted size: " + (job.getArtifactSizeBytes() == null ? "n/a" : job.getArtifactSizeBytes()) + " bytes\n"
                    + "Failure code: " + (job.getFailureCode() == null ? "none" : job.getFailureCode()) + "\n"
                    + "Use the authorized System Backup Administration page to download the encrypted artifact.";
            List<EmailAttachmentRequest> attachments = attachment(job);
            emailMessagingService.sendEmail(EmailMessageRequest.builder()
                    .to(properties.getEmailRecipients())
                    .subject(subject)
                    .body(body)
                    .messageType(EmailMessageType.GENERAL)
                    .referenceId(job.getId().toString())
                    .attachments(attachments)
                    .build());
        } catch (RuntimeException exception) {
            log.error("Backup notification delivery failed for job {}: {}", job.getId(), exception.getMessage());
        }
    }

    private List<EmailAttachmentRequest> attachment(SysBackupJob job) {
        if (!properties.isEmailAttachmentEnabled() || job.getArtifactName() == null
                || job.getArtifactSizeBytes() == null
                || job.getArtifactSizeBytes() > properties.getEmailAttachmentMaxBytes()) return List.of();
        Path root = properties.getLocalRoot().toAbsolutePath().normalize();
        Path artifact = root.resolve(job.getArtifactName()).normalize();
        if (!artifact.startsWith(root) || !Files.isRegularFile(artifact)) return List.of();
        return List.of(new EmailAttachmentRequest(
                "nexacore-backup-" + job.getId() + ".zip.aesgcm",
                "application/octet-stream",
                new FileSystemResource(artifact)));
    }
}
