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
        execute("INSERT INTO auth_users (username, person_id, enabled) VALUES "
                + "('No.Scope', 102, true), ('Ambiguous.User', 103, true)");
        execute("INSERT INTO auth_user_scope_assignments "
                + "(user_id, tenant_id, active, created_by, updated_by) "
                + "SELECT id, 10, true, 0, 0 FROM auth_users WHERE username='Legacy.User'");
        execute("INSERT INTO auth_user_scope_assignments "
                + "(user_id, tenant_id, active, created_by, updated_by) "
                + "SELECT id, tenant_id, true, 0, 0 FROM auth_users "
                + "CROSS JOIN (VALUES (20), (30)) candidates(tenant_id) "
                + "WHERE username='Ambiguous.User'");
        execute("INSERT INTO auth_roles (name) VALUES ('ROLE_LEGACY')");

        migrateTo("14");

        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success "
                + "ORDER BY installed_rank DESC LIMIT 1")).isEqualTo("14");
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(14);
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
                + "AND person_id=101 AND tenant_id=10 AND normalized_username='legacy.user' AND NOT locked"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM auth_roles WHERE name='ROLE_LEGACY' "
                + "AND tenant_id IS NULL AND role_code='ROLE_LEGACY' AND active"))
                .isEqualTo(1);
        assertThat(regclass("auth_user_backfill_quarantine"))
                .isEqualTo("auth_user_backfill_quarantine");
        assertThat(count("SELECT count(*) FROM auth_user_backfill_quarantine "
                + "WHERE reason='NO_ACTIVE_SCOPE' AND NOT resolved")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM auth_user_backfill_quarantine "
                + "WHERE reason='AMBIGUOUS_ACTIVE_TENANT' "
                + "AND candidate_tenant_ids=ARRAY[20,30]::bigint[] AND NOT resolved")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM auth_users "
                + "WHERE normalized_username IS NULL")).isZero();
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('ux_auth_users_person_id','ux_auth_users_username')"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('ux_auth_users_tenant_person_phase1',"
                + "'ux_auth_users_tenant_username_phase1','ux_auth_users_tenant_external_identity_phase1',"
                + "'ux_auth_roles_global_name_phase1','ux_auth_roles_tenant_name_phase1',"
                + "'ux_auth_roles_global_code_phase1','ux_auth_roles_tenant_code_phase1')"))
                .isEqualTo(7);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> migrateTo(null))
                .isInstanceOf(org.flywaydb.core.api.FlywayException.class)
                .hasMessageContaining("without a trusted tenant");

        // Simulate explicit operator review. No request header or arbitrary profile is
        // used to choose these tenant dispositions.
        execute("UPDATE auth_users SET tenant_id=40 WHERE username='No.Scope'");
        execute("INSERT INTO auth_user_scope_assignments "
                + "(user_id, tenant_id, active, created_by, updated_by) "
                + "SELECT id, 40, true, 0, 0 FROM auth_users WHERE username='No.Scope'");
        execute("DELETE FROM auth_user_scope_assignments WHERE user_id=(SELECT id FROM auth_users "
                + "WHERE username='Ambiguous.User') AND tenant_id=30");
        execute("UPDATE auth_users SET tenant_id=20 WHERE username='Ambiguous.User'");
        execute("UPDATE auth_user_backfill_quarantine SET resolved=true, resolved_by=900, "
                + "resolved_at=CURRENT_TIMESTAMP, updated_by=900 WHERE NOT resolved");

        migrateTo(null);

        assertThat(scalar("SELECT version FROM flyway_schema_history WHERE success "
                + "ORDER BY installed_rank DESC LIMIT 1")).isEqualTo("15");
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(15);
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_name='auth_users' "
                + "AND column_name IN ('tenant_id','normalized_username') AND is_nullable='NO'"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN "
                + "('ux_auth_users_username','ux_auth_users_person_id')")).isZero();
        assertThat(count("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN "
                + "('ux_auth_users_tenant_person','ux_auth_users_tenant_username',"
                + "'ux_auth_users_tenant_external_identity')")).isEqualTo(3);

        execute("INSERT INTO auth_users (username, normalized_username, person_id, tenant_id, enabled) "
                + "VALUES ('Legacy.User', 'legacy.user', 101, 20, true)");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execute(
                        "INSERT INTO auth_users (username, normalized_username, person_id, tenant_id, enabled) "
                                + "VALUES ('LEGACY.USER', 'legacy.user', 999, 10, true)"))
                .isInstanceOf(SQLException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execute(
                        "INSERT INTO auth_users (username, normalized_username, person_id, tenant_id, enabled) "
                                + "VALUES ('another', 'another', 101, 10, true)"))
                .isInstanceOf(SQLException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execute(
                        "INSERT INTO auth_user_scope_assignments "
                                + "(user_id, tenant_id, active, created_by, updated_by) "
                                + "SELECT id, 99, true, 0, 0 FROM auth_users "
                                + "WHERE tenant_id=20 AND normalized_username='legacy.user'"))
                .isInstanceOf(SQLException.class);

        execute("INSERT INTO auth_roles (name, tenant_id, role_code) VALUES "
                + "('ROLE_GLOBAL_PHASE2', NULL, 'ROLE_GLOBAL_PHASE2'),"
                + "('ROLE_TENANT_10', 10, 'ROLE_TENANT_10'),"
                + "('ROLE_TENANT_20', 20, 'ROLE_TENANT_20')");
        execute("INSERT INTO auth_user_roles (user_id, role_id) "
                + "SELECT u.id, r.id FROM auth_users u CROSS JOIN auth_roles r "
                + "WHERE u.username='Legacy.User' AND r.name IN ('ROLE_GLOBAL_PHASE2','ROLE_TENANT_10')");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execute(
                        "INSERT INTO auth_user_roles (user_id, role_id) "
                                + "SELECT u.id, r.id FROM auth_users u CROSS JOIN auth_roles r "
                                + "WHERE u.username='Legacy.User' AND r.name='ROLE_TENANT_20'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Tenant role cannot be assigned");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execute(
                        "UPDATE auth_roles SET tenant_id=11 WHERE name='ROLE_TENANT_10'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Assigned role tenant ownership cannot be changed");

        execute("INSERT INTO auth_roles (name, tenant_id, role_code) VALUES "
                + "('ROLE_REVIEWER', 10, 'ROLE_REVIEWER'),"
                + "('ROLE_REVIEWER', 20, 'ROLE_REVIEWER')");
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
