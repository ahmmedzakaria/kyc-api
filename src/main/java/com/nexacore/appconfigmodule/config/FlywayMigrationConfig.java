package com.nexacore.appconfigmodule.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayMigrationConfig {

    @Value("${nexacore.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${nexacore.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;

    @Bean(name = "authFlyway", initMethod = "migrate")
    public Flyway authFlyway(@Qualifier("authDataSource") DataSource dataSource) {
        return createFlyway(dataSource, "classpath:db/migration/auth");
    }

    @Bean(name = "systemFlyway", initMethod = "migrate")
    public Flyway systemFlyway(@Qualifier("systemDataSource") DataSource dataSource) {
        return createFlyway(dataSource, "classpath:db/migration/system");
    }

    @Bean(name = "kycFlyway", initMethod = "migrate")
    public Flyway kycFlyway(@Qualifier("kycDataSource") DataSource dataSource) {
        return createFlyway(dataSource, "classpath:db/migration/kyc");
    }

    @Bean(name = "gisFlyway", initMethod = "migrate")
    public Flyway gisFlyway(@Qualifier("gisDataSource") DataSource dataSource) {
        return createFlyway(dataSource, "classpath:db/migration/gis");
    }

    @Bean(name = "logFlyway", initMethod = "migrate")
    public Flyway logFlyway(@Qualifier("logDataSource") DataSource dataSource) {
        return createFlyway(dataSource, "classpath:db/migration/log");
    }

    private Flyway createFlyway(DataSource dataSource, String location) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion("0")
                .validateOnMigrate(validateOnMigrate)
                .load();
    }
}
