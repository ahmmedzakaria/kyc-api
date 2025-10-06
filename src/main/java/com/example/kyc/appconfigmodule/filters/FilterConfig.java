package com.example.kyc.appconfigmodule.filters;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

//@Configuration
public class FilterConfig {
//    @Bean
    public FilterRegistrationBean<AuditLoggingFilter> auditFilter(AuditLoggingFilter filter) {
        FilterRegistrationBean<AuditLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE); // run last
        return registration;
    }


}

