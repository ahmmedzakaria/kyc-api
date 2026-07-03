package com.nexacore.servicesmodule.queueingservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "queueing.service")
public class QueueingServiceProperties {

    private boolean enabled = false;

    private String providerName = "rabbitmq";

    private boolean autoDeclare = false;

    private boolean durableQueue = true;

    private String defaultExchange = "";

    private String defaultQueue = "nexacore.default";

    private String defaultRoutingKey = "nexacore.default";
}
