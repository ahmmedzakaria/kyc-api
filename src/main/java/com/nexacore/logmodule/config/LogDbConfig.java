package com.nexacore.logmodule.config;

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
        basePackages = "com.nexacore.logmodule.repository",
        entityManagerFactoryRef = "logEntityManagerFactory",
        transactionManagerRef = "logTransactionManager"
)
public class LogDbConfig {

    @Bean(name = "logDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.log")
    public DataSource logDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "logEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean logEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("logDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.nexacore.logmodule.entity")
                .persistenceUnit("log")
                .build();
    }

    @Bean(name = "logTransactionManager")
    public PlatformTransactionManager logTransactionManager(
            @Qualifier("logEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
