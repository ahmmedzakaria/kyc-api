package com.nexacore.systemmodule.backup.service;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import com.nexacore.systemmodule.backup.exception.BackupApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.Executor;
import java.util.UUID;
import java.util.Optional;
import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupJobServiceTest {

    @Test
    void reportsDisabledBackupAsServiceUnavailable() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(false);
        Executor directExecutor = Runnable::run;
        BackupJobService service = new BackupJobService(
                null, null, properties, directExecutor);

        assertThatThrownBy(() -> service.enqueue(BackupTrigger.MANUAL, "system_admin"))
                .isInstanceOfSatisfying(BackupApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getCode()).isEqualTo("DATABASE_BACKUP_DISABLED");
                    assertThat(exception.getMessage()).isEqualTo("Database backup is disabled by system configuration");
                });
    }

    @Test
    void reportsMissingArtifactWithStableCode() {
        BackupProperties properties = new BackupProperties();
        properties.setLocalRoot(java.nio.file.Path.of("target/nonexistent-backups"));
        BackupJobRepository repository = mock(BackupJobRepository.class);
        UUID id = UUID.randomUUID();
        SysBackupJob job = new SysBackupJob(); job.setId(id); job.setStatus(BackupJobStatus.COMPLETED); job.setArtifactName("missing.aesgcm");
        when(repository.findById(id)).thenReturn(Optional.of(job));
        BackupJobService service = new BackupJobService(repository, null, properties, Runnable::run);

        assertThatThrownBy(() -> service.artifact(id)).isInstanceOfSatisfying(BackupApiException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getCode()).isEqualTo("DATABASE_BACKUP_ARTIFACT_MISSING");
        });
    }
}
