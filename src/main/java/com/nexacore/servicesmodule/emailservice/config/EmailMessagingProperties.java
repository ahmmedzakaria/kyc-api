package com.nexacore.servicesmodule.emailservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "messaging.email")
public class EmailMessagingProperties {

    private boolean enabled = false;

    private String providerName = "smtp";

    private String fromAddress;

    private String fromName;

    private String otpSubject;

    private String otpTemplate;

    private String transactionSubject;
}
