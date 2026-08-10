package com.nexacore.systemmodule.backup.process;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PostgreSqlDumpExecutor {
    private final BackupProperties properties;

    public Path dump(BackupDatabaseTarget target, Path directory) throws IOException, InterruptedException {
        Path output = directory.resolve(target.logicalName() + ".dump");
        Path passFile = Files.createTempFile(directory, ".pgpass-", ".tmp");
        Path errorFile = Files.createTempFile(directory, ".pgdump-error-", ".log");
        try {
            Files.writeString(passFile, pgPassLine(target), StandardCharsets.UTF_8);
            setOwnerOnly(passFile);
            List<String> command = List.of(
                    properties.getPgDumpExecutable(),
                    "--host", target.host(),
                    "--port", Integer.toString(target.port()),
                    "--username", target.username(),
                    "--dbname", target.databaseName(),
                    "--format=custom", "--no-owner", "--no-acl",
                    "--file", output.toString()
            );
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put("PGPASSFILE", passFile.toString());
            builder.redirectError(errorFile.toFile());
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = builder.start();
            Duration timeout = properties.getTimeout();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("Database dump timed out for " + target.logicalName());
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) == 0L) {
                throw new IOException("pg_dump failed for " + target.logicalName() + ": " + sanitizedError(errorFile));
            }
            return output;
        } finally {
            Files.deleteIfExists(passFile);
            Files.deleteIfExists(errorFile);
        }
    }

    private String pgPassLine(BackupDatabaseTarget target) {
        return escape(target.host()) + ":" + target.port() + ":" + escape(target.databaseName()) + ":"
                + escape(target.username()) + ":" + escape(target.password()) + System.lineSeparator();
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace(":", "\\:"); }

    private String sanitizedError(Path file) throws IOException {
        String value = Files.readString(file, StandardCharsets.UTF_8).replaceAll("[\\r\\n]+", " ").trim();
        return value.length() <= 400 ? value : value.substring(0, 400);
    }

    private void setOwnerOnly(Path file) {
        try {
            Files.setPosixFilePermissions(file, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX development filesystem; the staging directory is still private.
        }
    }
}
