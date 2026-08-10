package com.nexacore.systemmodule.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.crypto.BackupEncryptor;
import com.nexacore.systemmodule.backup.entity.SysBackupDatabaseResult;
import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.process.BackupDatabaseTarget;
import com.nexacore.systemmodule.backup.process.BackupTargetResolver;
import com.nexacore.systemmodule.backup.process.PostgreSqlDumpExecutor;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import com.nexacore.systemmodule.backup.repository.BackupDatabaseResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.EnumSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupWorker {
    private static final long ADVISORY_LOCK_ID = 0x4E43424BL;
    private final BackupJobRepository repository;
    private final BackupDatabaseResultRepository databaseResultRepository;
    private final BackupTargetResolver targetResolver;
    private final PostgreSqlDumpExecutor dumpExecutor;
    private final BackupEncryptor encryptor;
    private final BackupProperties properties;
    private final ObjectMapper objectMapper;
    private final BackupNotificationService notificationService;
    @Qualifier("systemDataSource")
    private final DataSource systemDataSource;

    public void execute(UUID jobId) {
        try (Connection lockConnection = systemDataSource.getConnection()) {
            if (!tryLock(lockConnection)) {
                fail(jobId, "BACKUP_ALREADY_RUNNING", "Another backup is already running");
                return;
            }
            try {
                executeLocked(jobId);
            } finally {
                unlock(lockConnection);
            }
        } catch (Exception exception) {
            log.error("Backup job {} failed", jobId, exception);
            fail(jobId, "BACKUP_EXECUTION_FAILED", safeMessage(exception));
        }
    }

    private void executeLocked(UUID jobId) throws Exception {
        SysBackupJob job = requiredJob(jobId);
        job.setStatus(BackupJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        repository.save(job);

        Path root = validatedRoot();
        Path staging = Files.createTempDirectory(root.resolve("staging"), "backup-" + jobId + "-");
        setOwnerOnly(staging);
        List<Path> successfulDumps = new ArrayList<>();
        try {
            for (BackupDatabaseTarget target : targetResolver.resolve()) {
                SysBackupDatabaseResult result = startResult(job, target);
                try {
                    Path dump = dumpExecutor.dump(target, staging);
                    successfulDumps.add(dump);
                    result.setSizeBytes(Files.size(dump));
                    result.setSha256(BackupFileSupport.sha256(dump));
                    result.setStatus(BackupJobStatus.COMPLETED);
                } catch (Exception exception) {
                    result.setStatus(BackupJobStatus.FAILED);
                    result.setFailureCode("DATABASE_DUMP_FAILED");
                    result.setSanitizedFailureMessage(safeMessage(exception));
                    log.error("Backup job {} database {} failed: {}", jobId, target.logicalName(), safeMessage(exception));
                }
                result.setCompletedAt(LocalDateTime.now());
                databaseResultRepository.save(result);
            }

            boolean allSucceeded = successfulDumps.size() == job.getDatabaseResults().size();
            if (!successfulDumps.isEmpty()) {
                Path artifact = artifactPath(root, jobId);
                Files.createDirectories(artifact.getParent());
                packageEncrypted(job, successfulDumps, artifact);
                job.setArtifactName(root.relativize(artifact).toString());
                job.setArtifactSizeBytes(Files.size(artifact));
                job.setArtifactSha256(BackupFileSupport.sha256(artifact));
                job.setEncryptionKeyId(properties.getEncryptionKeyId());
                job.setExpiresAt(LocalDateTime.now().plusDays(properties.getRetentionDays()));
            }
            job.setStatus(allSucceeded ? BackupJobStatus.COMPLETED : successfulDumps.isEmpty() ? BackupJobStatus.FAILED : BackupJobStatus.PARTIAL);
            if (!allSucceeded) {
                job.setFailureCode("ONE_OR_MORE_DATABASES_FAILED");
                job.setSanitizedFailureMessage("One or more configured databases could not be backed up");
            }
            job.setCompletedAt(LocalDateTime.now());
            repository.save(job);
            notificationService.notifyResult(job);
        } finally {
            deleteTree(staging);
        }
    }

    private SysBackupDatabaseResult startResult(SysBackupJob job, BackupDatabaseTarget target) {
        SysBackupDatabaseResult result = new SysBackupDatabaseResult();
        result.setBackupJob(job);
        result.setLogicalDatabase(target.logicalName());
        result.setDatabaseName(target.databaseName());
        result.setStatus(BackupJobStatus.RUNNING);
        result.setStartedAt(LocalDateTime.now());
        job.getDatabaseResults().add(result);
        databaseResultRepository.save(result);
        return result;
    }

    private void packageEncrypted(SysBackupJob job, List<Path> dumps, Path artifact) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", 1);
        manifest.put("backupId", job.getId());
        manifest.put("trigger", job.getTriggerType());
        manifest.put("requestedBy", job.getRequestedBy());
        manifest.put("startedAt", job.getStartedAt());
        manifest.put("consistency", "INDEPENDENT_DATABASE_SNAPSHOTS");
        manifest.put("databases", job.getDatabaseResults().stream().map(result -> Map.of(
                "logicalName", result.getLogicalDatabase(),
                "databaseName", result.getDatabaseName(),
                "status", result.getStatus().name(),
                "sizeBytes", result.getSizeBytes() == null ? 0L : result.getSizeBytes(),
                "sha256", result.getSha256() == null ? "" : result.getSha256()
        )).toList());
        try (OutputStream encrypted = encryptor.encryptedStream(artifact);
             ZipOutputStream zip = new ZipOutputStream(encrypted)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            objectMapper.writeValue(zip, manifest);
            zip.closeEntry();
            for (Path dump : dumps) {
                zip.putNextEntry(new ZipEntry(dump.getFileName().toString()));
                Files.copy(dump, zip);
                zip.closeEntry();
            }
        }
    }

    private Path validatedRoot() throws IOException {
        Path root = properties.getLocalRoot().toAbsolutePath().normalize();
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path working = Path.of("").toAbsolutePath().normalize();
        if (root.getParent() == null || root.equals(home) || root.equals(working)) {
            throw new IllegalStateException("Unsafe backup root: choose a dedicated subdirectory");
        }
        Files.createDirectories(root.resolve("staging"));
        return root;
    }

    private Path artifactPath(Path root, UUID jobId) {
        LocalDate date = LocalDate.now();
        return root.resolve(Integer.toString(date.getYear()))
                .resolve(String.format("%02d", date.getMonthValue()))
                .resolve(String.format("%02d", date.getDayOfMonth()))
                .resolve("backup-" + jobId + ".zip.aesgcm");
    }

    private boolean tryLock(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT pg_try_advisory_lock(" + ADVISORY_LOCK_ID + ")")) {
            return result.next() && result.getBoolean(1);
        }
    }

    private void unlock(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_unlock(" + ADVISORY_LOCK_ID + ")");
        } catch (Exception exception) {
            log.warn("Could not release database backup advisory lock: {}", exception.getMessage());
        }
    }

    private SysBackupJob requiredJob(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Backup job not found"));
    }

    private void fail(UUID jobId, String code, String message) {
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(BackupJobStatus.FAILED);
            job.setFailureCode(code);
            job.setSanitizedFailureMessage(message);
            job.setCompletedAt(LocalDateTime.now());
            repository.save(job);
            notificationService.notifyResult(job);
        });
    }

    private String safeMessage(Exception exception) {
        String value = exception.getMessage();
        if (value == null || value.isBlank()) return "Backup operation failed";
        value = value.replaceAll("[\\r\\n]+", " ");
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException exception) { log.warn("Could not delete backup staging path {}", path.getFileName()); }
            });
        } catch (IOException exception) {
            log.warn("Could not clean backup staging directory: {}", exception.getMessage());
        }
    }

    private void setOwnerOnly(Path directory) {
        try {
            Files.setPosixFilePermissions(directory, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX development filesystem.
        }
    }
}
