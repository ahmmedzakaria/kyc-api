package com.nexacore.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class CatalogDatabaseMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_migration_test")
            .withUsername("catalog_test")
            .withPassword("catalog_test");

    @BeforeEach
    void resetDatabase() throws SQLException {
        execute("DROP SCHEMA public CASCADE");
        execute("CREATE SCHEMA public");
    }

    @Test
    void migratesCleanDatabaseAndRepeatMigrationIsIdempotent() throws SQLException {
        Flyway flyway = flyway();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        assertThat(count("SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='public' AND table_name LIKE 'catalog_%' "
                + "AND table_name <> 'flyway_schema_history'")).isEqualTo(25);
        assertThat(count("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema='public' AND table_name LIKE 'catalog_%' "
                + "AND column_name IN ('created_at','updated_at','created_by','updated_by')")).isEqualTo(100);
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM catalog_sites WHERE tenant_id=7 "
                + "AND site_key='arsv-public-site' AND publication_state='PUBLISHED' "
                + "AND created_by='migration:v2' AND updated_by='migration:v2'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM catalog_site_translations translation "
                + "JOIN catalog_sites site ON site.tenant_id=translation.tenant_id AND site.id=translation.site_id "
                + "WHERE site.tenant_id=7 AND site.site_key='arsv-public-site' "
                + "AND translation.locale='en' AND translation.created_by='migration:v2' "
                + "AND translation.updated_by='migration:v2'")).isEqualTo(1);
    }

    @Test
    void rejectsCrossTenantRelationships() throws SQLException {
        flyway().migrate();
        execute("INSERT INTO catalog_categories "
                + "(tenant_id,slug,created_by,updated_by) VALUES (1,'one','test','test')");
        long categoryId = count("SELECT id FROM catalog_categories WHERE tenant_id=1 AND slug='one'");

        assertThatThrownBy(() -> execute("INSERT INTO catalog_products "
                + "(tenant_id,category_id,slug,created_by,updated_by) VALUES "
                + "(2," + categoryId + ",'invalid','test','test')"))
                .isInstanceOf(SQLException.class);
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/catalog")
                .baselineOnMigrate(true).baselineVersion("0").validateOnMigrate(true)
                .load();
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            return ((Number) resultSet.getObject(1)).longValue();
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
