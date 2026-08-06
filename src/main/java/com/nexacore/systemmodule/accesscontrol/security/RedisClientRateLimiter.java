package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.servicesmodule.cacheservice.dto.AtomicCounterResult;
import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class RedisClientRateLimiter implements ClientRateLimiter {
    private static final long WINDOW_SECONDS = 60;

    private final CacheService cacheService;
    private final AccessControlProperties properties;

    @Override
    public ClientRateLimitDecision check(SysPrivClientApplication application, HttpServletRequest request) {
        Integer configuredLimit = application == null ? null : application.getRateLimitPerMinute();
        if (!properties.isRateLimitEnabled() || configuredLimit == null) {
            return ClientRateLimitDecision.notLimited();
        }
        if (configuredLimit <= 0) {
            throw new IllegalArgumentException("Client rateLimitPerMinute must be positive");
        }
        try {
            AtomicCounterResult result = cacheService.increment(
                    cacheKey(application, request), Duration.ofSeconds(WINDOW_SECONDS));
            long count = result.value();
            long ttl = Math.max(1, result.timeToLive().toSeconds());
            return new ClientRateLimitDecision(
                    count <= configuredLimit,
                    configuredLimit,
                    Math.max(0, configuredLimit - count),
                    ttl);
        } catch (RuntimeException ex) {
            if (properties.isRateLimitFailOpen()) return ClientRateLimitDecision.notLimited();
            throw new RateLimitBackendUnavailableException("Distributed rate-limit backend is unavailable", ex);
        }
    }

    private String cacheKey(SysPrivClientApplication application, HttpServletRequest request) {
        String client = application.getId() == null ? application.getClientCode() : application.getId().toString();
        String route = request.getMethod() + ":" + request.getServletPath();
        return "rate-limit:client:" + client + ":route:" + sha256(route);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
