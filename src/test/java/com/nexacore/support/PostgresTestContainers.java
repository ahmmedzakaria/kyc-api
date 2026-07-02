package com.nexacore.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresTestContainers {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    protected static final PostgreSQLContainer<?> AUTH_DB = createPostgres("auth_db");

    @Container
    protected static final PostgreSQLContainer<?> KYC_DB = createPostgres("kyc_db");

    @Container
    protected static final PostgreSQLContainer<?> GIS_DB = createPostgres("gisdb");

    @Container
    protected static final PostgreSQLContainer<?> LOG_DB = createPostgres("log_db");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registerDatasource(registry, "spring.datasource.auth", AUTH_DB);
        registerDatasource(registry, "spring.datasource.kyc", KYC_DB);
        registerDatasource(registry, "spring.datasource.gis", GIS_DB);
        registerDatasource(registry, "spring.datasource.log", LOG_DB);
    }

    private static PostgreSQLContainer<?> createPostgres(String databaseName) {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName(databaseName)
                .withUsername("postgres")
                .withPassword("postgres");
    }

    private static void registerDatasource(
            DynamicPropertyRegistry registry,
            String prefix,
            PostgreSQLContainer<?> container
    ) {
        registry.add(prefix + ".jdbc-url", container::getJdbcUrl);
        registry.add(prefix + ".username", container::getUsername);
        registry.add(prefix + ".password", container::getPassword);
        registry.add(prefix + ".driver-class-name", container::getDriverClassName);
    }
}
