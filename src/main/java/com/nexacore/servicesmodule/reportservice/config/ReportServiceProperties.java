package com.nexacore.servicesmodule.reportservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "report.service")
public class ReportServiceProperties {

    private String providerName = "jasper";

    private String templatesBasePath = "reports/templates";

    private String defaultFileName = "report";
}
