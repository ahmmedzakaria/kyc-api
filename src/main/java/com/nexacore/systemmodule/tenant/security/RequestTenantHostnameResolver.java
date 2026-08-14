package com.nexacore.systemmodule.tenant.security;

import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;

/** Resolves the tenant-facing hostname for direct and centralized-API requests. */
@Component
@RequiredArgsConstructor
public class RequestTenantHostnameResolver {
    private final HostnameNormalizer hostnameNormalizer;

    public String resolve(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (!StringUtils.hasText(origin)) return hostnameNormalizer.normalize(request.getServerName());
        URI uri;
        try {
            uri = URI.create(origin.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid request origin", exception);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath())) {
            throw new IllegalArgumentException("Invalid request origin");
        }
        return hostnameNormalizer.normalize(uri.getHost());
    }
}
