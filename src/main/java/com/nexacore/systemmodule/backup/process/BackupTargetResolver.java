package com.nexacore.systemmodule.backup.process;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BackupTargetResolver {
    private static final List<String> LOGICAL_TARGETS = List.of("auth", "system", "kyc", "gis", "log");
    private final Environment environment;

    public List<BackupDatabaseTarget> resolve() {
        return LOGICAL_TARGETS.stream().map(this::resolve).toList();
    }

    private BackupDatabaseTarget resolve(String logicalName) {
        String prefix = "spring.datasource." + logicalName + ".";
        String jdbcUrl = required(prefix + "jdbc-url");
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String databaseName = uri.getPath().replaceFirst("^/", "");
        if (databaseName.isBlank()) throw new IllegalStateException("Backup database name is missing for " + logicalName);
        return new BackupDatabaseTarget(
                logicalName,
                uri.getHost(),
                uri.getPort() < 0 ? 5432 : uri.getPort(),
                databaseName,
                required(prefix + "username"),
                environment.getProperty(prefix + "password", "")
        );
    }

    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Required backup configuration is missing: " + key);
        return value;
    }
}
