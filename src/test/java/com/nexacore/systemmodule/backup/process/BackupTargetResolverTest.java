package com.nexacore.systemmodule.backup.process;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class BackupTargetResolverTest {
    @Test
    void includesCatalogDatabaseInPlatformBackupSet() {
        MockEnvironment environment = new MockEnvironment();
        for (String name : new String[]{"auth", "system", "kyc", "gis", "log", "catalog"}) {
            environment.setProperty("spring.datasource." + name + ".jdbc-url",
                    "jdbc:postgresql://database:5432/" + name + "_db");
            environment.setProperty("spring.datasource." + name + ".username", "postgres");
            environment.setProperty("spring.datasource." + name + ".password", "secret");
        }

        var targets = new BackupTargetResolver(environment).resolve();

        assertThat(targets).extracting(BackupDatabaseTarget::logicalName)
                .containsExactly("auth", "system", "kyc", "gis", "log", "catalog");
        assertThat(targets.getLast().databaseName()).isEqualTo("catalog_db");
    }
}
