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

@Testcontainers(disabledWithoutDocker = true)
class SystemDatabaseMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration/system";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("system_migration_test")
            .withUsername("system_test")
            .withPassword("system_test");

    @BeforeEach
    void resetDatabase() throws SQLException {
        execute("DROP SCHEMA public CASCADE");
        execute("CREATE SCHEMA public");
    }

    @Test
    void migratesAnEmptySystemDatabaseToTheLatestVersion() throws SQLException {
        migrateTo(null);

        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"))
                .isEqualTo("38");
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success"))
                .isEqualTo(38);
        assertThat(regclass("sys_priv_modules")).isEqualTo("sys_priv_modules");
        assertThat(regclass("sys_acc_api_registry")).isEqualTo("sys_acc_api_registry");
        assertThat(regclass("sys_acc_client_applications")).isEqualTo("sys_acc_client_applications");
        assertThat(regclass("sys_acc_client_credentials")).isEqualTo("sys_acc_client_credentials");
        assertThat(regclass("sys_acc_client_api_permissions")).isEqualTo("sys_acc_client_api_permissions");
        assertThat(regclass("sys_acc_client_feature_permissions")).isEqualTo("sys_acc_client_feature_permissions");
        assertThat(regclass("sys_acc_client_application_tenants")).isEqualTo("sys_acc_client_application_tenants");
        assertThat(regclass("sys_priv_api_registry")).isNull();
        assertThat(regclass("sys_priv_client_applications")).isNull();
        assertThat(regclass("sys_layout_features")).isEqualTo("sys_layout_features");
        assertThat(regclass("sys_backup_jobs")).isEqualTo("sys_backup_jobs");
        assertThat(regclass("sys_backup_database_results")).isEqualTo("sys_backup_database_results");
        assertThat(regclass("sys_tenants")).isEqualTo("sys_tenants");
        assertThat(regclass("sys_tenant_domains")).isEqualTo("sys_tenant_domains");
        assertThat(regclass("sys_layout_assignment_tenant_quarantine"))
                .isEqualTo("sys_layout_assignment_tenant_quarantine");
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_name = 'sys_client_layout_profiles' "
                + "AND column_name = 'tenant_id' AND is_nullable = 'NO'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_tenants WHERE id = 1 AND status = 'ACTIVE'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_tenant_domains WHERE tenant_id = 1 AND hostname = 'localhost' "
                + "AND verification_status = 'VERIFIED' AND active")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_priv_privileges WHERE privilege_code LIKE '110601001%'")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM sys_acc_client_applications WHERE client_code IN ('WEB', 'SYSTEM_ADMIN_WEB')"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM sys_acc_client_applications "
                + "WHERE client_code IN ('WEB', 'SYSTEM_ADMIN_WEB') AND created_by IS NOT NULL AND updated_by IS NOT NULL"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM sys_acc_client_feature_permissions permission "
                + "JOIN sys_acc_client_applications client ON client.id = permission.client_application_id "
                + "JOIN sys_priv_privileges privilege ON privilege.id = permission.privilege_id "
                + "WHERE client.client_code = 'SYSTEM_ADMIN_WEB' "
                + "AND privilege.privilege_code LIKE '110601001%' AND permission.active"))
                .isEqualTo(5);
    }

    @Test
    void upgradesThePrePrefixSnapshotWithoutLosingDataOrRelationships() throws SQLException {
        migrateTo("12");
        assertThat(regclass("sys_modules")).isEqualTo("sys_modules");
        assertThat(regclass("sys_priv_modules")).isNull();

        execute("INSERT INTO sys_modules (code, name, active, created_by, updated_by, created_at, updated_at) "
                + "VALUES ('ZZ', 'Migration sentinel', true, 41, 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        execute("INSERT INTO sys_submodules (module_id, code, name, active, created_by, updated_by, created_at, updated_at) "
                + "SELECT id, '01', 'Migration child', true, 41, 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                + "FROM sys_modules WHERE code = 'ZZ'");

        migrateTo(null);

        assertThat(regclass("sys_modules")).isNull();
        assertThat(regclass("sys_priv_modules")).isEqualTo("sys_priv_modules");
        assertThat(count("SELECT count(*) FROM sys_priv_modules "
                + "WHERE code = 'ZZ' AND name = 'Migration sentinel' AND created_by = 41 AND updated_by = 42"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_priv_submodules sm "
                + "JOIN sys_priv_modules m ON m.id = sm.module_id "
                + "WHERE m.code = 'ZZ' AND sm.code = '01'"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM pg_constraint c "
                + "WHERE c.contype = 'f' "
                + "AND c.conrelid = 'sys_priv_submodules'::regclass "
                + "AND c.confrelid = 'sys_priv_modules'::regclass"))
                .isGreaterThanOrEqualTo(1);
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE tablename = 'sys_priv_submodules'"))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void upgradesADatabaseCurrentlyAtV13ToTheLatestVersion() throws SQLException {
        migrateTo("13");
        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"))
                .isEqualTo("13");

        execute("INSERT INTO sys_priv_modules (code, name, active, created_by, updated_by, created_at, updated_at) "
                + "VALUES ('ZY', 'V13 sentinel', true, 51, 52, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        migrateTo(null);

        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"))
                .isEqualTo("38");
        assertThat(count("SELECT count(*) FROM sys_priv_modules "
                + "WHERE code = 'ZY' AND created_by = 51 AND updated_by = 52"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'sys_acc_api_registry' "
                + "AND column_name IN ('client_authentication_requirement', 'user_authorization_requirement', "
                + "'data_scope', 'source', 'priority', 'last_synchronized_at')"))
                .isEqualTo(6);
        assertThat(count("SELECT count(*) FROM pg_indexes "
                + "WHERE tablename = 'sys_acc_api_registry' "
                + "AND indexname = 'uk_sys_acc_api_registry_method_path_source'"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sys_layout_features "
                + "WHERE feature_code = 'DASHBOARD' AND created_by = 0 AND updated_by = 0"))
                .isEqualTo(1);
    }

    private void migrateTo(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true);
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private long count(String sql) throws SQLException {
        return ((Number) scalar(sql)).longValue();
    }

    private String regclass(String tableName) throws SQLException {
        Object value = scalar("SELECT to_regclass('public." + tableName + "')");
        return value == null ? null : value.toString();
    }

    private Object scalar(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getObject(1);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
