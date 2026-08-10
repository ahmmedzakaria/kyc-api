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
class AuthDatabaseMigrationTest {
    private static final String MIGRATION_LOCATION = "classpath:db/migration/auth";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_migration_test")
            .withUsername("auth_test")
            .withPassword("auth_test");

    @BeforeEach
    void resetLegacySchema() throws SQLException {
        execute("DROP SCHEMA public CASCADE");
        execute("CREATE SCHEMA public");
        execute("CREATE TABLE auth_users ("
                + "id bigserial PRIMARY KEY, username varchar(255) UNIQUE, password varchar(255), "
                + "enabled boolean NOT NULL DEFAULT true, external_provider varchar(255), "
                + "external_subject varchar(255), last_login_at timestamp, "
                + "created_at timestamp, updated_at timestamp)");
        execute("CREATE TABLE auth_roles (id bigserial PRIMARY KEY, name varchar(255) NOT NULL UNIQUE)");
        execute("CREATE TABLE auth_user_roles (user_id bigint NOT NULL REFERENCES auth_users(id), "
                + "role_id bigint NOT NULL REFERENCES auth_roles(id), PRIMARY KEY (user_id, role_id))");
    }

    @Test
    void upgradesLegacyAuthSchemaToAdditiveTenantAccountFoundation() throws SQLException {
        migrateTo("10");
        execute("INSERT INTO auth_users (username, person_id, enabled) VALUES ('Legacy.User', 101, true)");
        execute("INSERT INTO auth_roles (name) VALUES ('ROLE_LEGACY')");

        migrateTo(null);

        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success "
                + "ORDER BY installed_rank DESC LIMIT 1")).isEqualTo("12");
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(12);
        assertThat(regclass("auth_persons")).isEqualTo("auth_persons");
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' "
                + "AND table_name='auth_users' AND column_name IN "
                + "('tenant_id','normalized_username','locked','password_changed_at',"
                + "'credentials_expire_at','created_by','updated_by')")).isEqualTo(7);
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' "
                + "AND table_name='auth_roles' AND column_name IN "
                + "('tenant_id','role_code','description','active','created_by','updated_by','created_at','updated_at')"))
                .isEqualTo(8);
        assertThat(count("SELECT count(*) FROM auth_users WHERE username='Legacy.User' "
                + "AND person_id=101 AND tenant_id IS NULL AND normalized_username IS NULL AND NOT locked"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM auth_roles WHERE name='ROLE_LEGACY' "
                + "AND tenant_id IS NULL AND role_code IS NULL AND active"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('ux_auth_users_person_id','ux_auth_users_username')"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('ux_auth_users_tenant_person_phase1',"
                + "'ux_auth_users_tenant_username_phase1','ux_auth_users_tenant_external_identity_phase1',"
                + "'ux_auth_roles_global_name_phase1','ux_auth_roles_tenant_name_phase1',"
                + "'ux_auth_roles_global_code_phase1','ux_auth_roles_tenant_code_phase1')"))
                .isEqualTo(7);
    }

    private void migrateTo(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true);
        if (target != null) configuration.target(target);
        configuration.load().migrate();
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
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
