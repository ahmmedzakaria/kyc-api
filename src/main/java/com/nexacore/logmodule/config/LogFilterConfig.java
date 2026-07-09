package com.nexacore.logmodule.config;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.logmodule.filter.ApiLoggingFilter;
import com.nexacore.logmodule.service.LogService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LogFilterConfig {
    @Bean
    public FilterRegistrationBean<ApiLoggingFilter> apiLoggingFilterRegistration(LogService logService,
                                                                                 AuthModuleGateway authModuleGateway) {
        FilterRegistrationBean<ApiLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiLoggingFilter(logService, authModuleGateway));
        registration.setOrder(Ordered.LOWEST_PRECEDENCE); // run last
        return registration;
    }
}
