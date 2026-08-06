package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;

@Component
public class ClientOriginPolicy {
    public boolean isAllowed(SysPrivClientApplication application, String requestOrigin) {
        if (!StringUtils.hasText(requestOrigin)) {
            return true;
        }
        if (application == null || !StringUtils.hasText(application.getAllowedOrigins())) {
            return false;
        }
        try {
            String normalizedRequestOrigin = normalize(requestOrigin);
            return Arrays.stream(application.getAllowedOrigins().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(this::normalize)
                    .anyMatch(normalizedRequestOrigin::equals);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String normalize(String origin) {
        URI uri = URI.create(origin.trim());
        if (uri.getScheme() == null || uri.getHost() == null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("Invalid origin");
        }
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || "http".equalsIgnoreCase(uri.getScheme()) && port == 80
                || "https".equalsIgnoreCase(uri.getScheme()) && port == 443;
        return uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase()
                + (defaultPort ? "" : ":" + port);
    }
}
