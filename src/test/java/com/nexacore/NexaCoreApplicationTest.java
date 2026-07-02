package com.nexacore;

import com.nexacore.support.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NexaCoreApplicationTest extends PostgresTestContainers {

    private final JdbcTemplate authJdbcTemplate;
    private final JdbcTemplate kycJdbcTemplate;
    private final JdbcTemplate gisJdbcTemplate;
    private final JdbcTemplate logJdbcTemplate;

    @Autowired
    NexaCoreApplicationTest(
            @Qualifier("authDataSource") DataSource authDataSource,
            @Qualifier("kycDataSource") DataSource kycDataSource,
            @Qualifier("gisDataSource") DataSource gisDataSource,
            @Qualifier("logDataSource") DataSource logDataSource
    ) {
        this.authJdbcTemplate = new JdbcTemplate(authDataSource);
        this.kycJdbcTemplate = new JdbcTemplate(kycDataSource);
        this.gisJdbcTemplate = new JdbcTemplate(gisDataSource);
        this.logJdbcTemplate = new JdbcTemplate(logDataSource);
    }

    @Test
    void contextLoadsWithPostgresContainersAndFlywayHistory() {
        assertFlywayHistoryExists(authJdbcTemplate);
        assertFlywayHistoryExists(kycJdbcTemplate);
        assertFlywayHistoryExists(gisJdbcTemplate);
        assertFlywayHistoryExists(logJdbcTemplate);
    }

    private void assertFlywayHistoryExists(JdbcTemplate jdbcTemplate) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name = 'flyway_schema_history'
                        """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }
}
