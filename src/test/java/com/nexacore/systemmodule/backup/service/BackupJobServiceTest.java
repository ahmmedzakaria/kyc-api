package com.nexacore.systemmodule.backup.service;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import com.nexacore.systemmodule.backup.exception.BackupApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.Executor;

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
}
