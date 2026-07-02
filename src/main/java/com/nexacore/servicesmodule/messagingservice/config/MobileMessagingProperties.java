package com.nexacore.servicesmodule.messagingservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "messaging.mobile")
public class MobileMessagingProperties {

    private boolean enabled = false;

    private String providerName = "generic";

    private String gatewayUrl;

    private String apiKey;

    private String authHeaderName = "Authorization";

    private String authHeaderPrefix = "Bearer";

    private String senderId = "NexaCore";

    private int timeoutSeconds = 10;

    private String otpTemplate = "Your NexaCore OTP is %s. It will expire in %d minutes.";
}

