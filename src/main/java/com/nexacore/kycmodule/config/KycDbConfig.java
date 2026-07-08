package com.nexacore.kycmodule.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.nexacore.kycmodule",
        entityManagerFactoryRef = "kycEntityManagerFactory",
        transactionManagerRef = "kycTransactionManager"
)
public class KycDbConfig {

    @Bean(name = "kycDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.kyc")
    public DataSource kycDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "kycEntityManagerFactory")
    @DependsOn("kycFlyway")
    public LocalContainerEntityManagerFactoryBean kycEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("kycDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.nexacore.kycmodule")
                .persistenceUnit("kyc")
                .build();
    }

    @Bean(name = "kycTransactionManager")
    public PlatformTransactionManager kycTransactionManager(
            @Qualifier("kycEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
