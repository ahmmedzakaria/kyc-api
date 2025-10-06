package com.example.kyc.appconfigmodule.dbconfig;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.kyc.gismodule.repository",
        entityManagerFactoryRef = "gisEntityManagerFactory",
        transactionManagerRef = "gisTransactionManager"
)
public class GisDbConfig {

    @Bean(name = "gisDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.gis")
    public DataSource gisDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "gisEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean gisEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("gisDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.example.kyc.gismodule.entity") // ✅ include all audit entities
                .persistenceUnit("gis")
                .build();
    }

    @Bean(name = "gisTransactionManager")
    public PlatformTransactionManager gisTransactionManager(
            @Qualifier("gisEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}

