package com.nexacore.authmodule.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins = List.of();
    private List<String> allowedOriginPatterns = List.of();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of(
            "Authorization", "Content-Type", "X-Silent", "X-Client-Code", "X-API-Key", "X-Trace-Id",
            "X-Step-Up-Authentication"
    );
    private List<String> exposedHeaders = List.of("X-Trace-Id");
    private boolean allowCredentials;
    private long maxAgeSeconds = 3600;
}
