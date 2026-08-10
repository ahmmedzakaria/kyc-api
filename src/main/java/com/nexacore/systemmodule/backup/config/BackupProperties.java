package com.nexacore.systemmodule.backup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "nexacore.backup")
public class BackupProperties {
    private boolean enabled;
    private String cron = "0 0 2 * * *";
    private String zone = "UTC";
    private Path localRoot = Path.of("./runtime/backups");
    private int retentionDays = 30;
    private int minimumSuccessful = 7;
    private Duration timeout = Duration.ofMinutes(30);
    private String pgDumpExecutable = "pg_dump";
    private String encryptionKeyBase64 = "";
    private String encryptionKeyId = "local-v1";
    private boolean emailEnabled = true;
    private List<String> emailRecipients = new ArrayList<>();
    private boolean emailAttachmentEnabled;
    private long emailAttachmentMaxBytes = 15_000_000L;
}
