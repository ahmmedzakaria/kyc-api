package com.nexacore.systemmodule.tenant.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nexacore.platform-administration.step-up")
@Getter
@Setter
public class PlatformAdministrationProperties {
    private boolean enabled = true;
    private long maxAuthenticationAgeSeconds = 300;
}
