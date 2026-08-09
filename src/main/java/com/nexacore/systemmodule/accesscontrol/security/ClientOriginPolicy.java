package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.IDN;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ClientOriginPolicy {
    public boolean isAllowed(SysAccClientApplication application, String requestOrigin) {
        if (!StringUtils.hasText(requestOrigin)) {
            return true;
        }
        if (application == null || !StringUtils.hasText(application.getAllowedOrigins())) {
            return false;
        }
        try {
            String normalizedRequestOrigin = normalize(requestOrigin);
            return parse(application.getAllowedOrigins()).contains(normalizedRequestOrigin);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public String normalizeConfiguredOrigins(String configuredOrigins) {
        if (!StringUtils.hasText(configuredOrigins)) {
            return null;
        }
        Set<String> normalized = parse(configuredOrigins);
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(",", normalized);
    }

    private Set<String> parse(String configuredOrigins) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String configuredOrigin : configuredOrigins.split(",")) {
            if (StringUtils.hasText(configuredOrigin)) {
                normalized.add(normalize(configuredOrigin));
            }
        }
        return normalized;
    }

    private String normalize(String origin) {
        if (!StringUtils.hasText(origin) || "null".equalsIgnoreCase(origin.trim()) || "*".equals(origin.trim())) {
            throw new IllegalArgumentException("Opaque and wildcard origins are not allowed");
        }
        URI uri = URI.create(origin.trim());
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        String parsedHost = uri.getHost();
        int port = uri.getPort();
        if (parsedHost == null && uri.getRawAuthority() != null && !uri.getRawAuthority().contains("@")) {
            String authority = uri.getRawAuthority();
            int portSeparator = authority.lastIndexOf(':');
            if (portSeparator > -1 && authority.indexOf(':') == portSeparator) {
                try {
                    port = Integer.parseInt(authority.substring(portSeparator + 1));
                    parsedHost = authority.substring(0, portSeparator);
                } catch (NumberFormatException ignored) {
                    parsedHost = authority;
                }
            } else {
                parsedHost = authority;
            }
        }
        if (!("http".equals(scheme) || "https".equals(scheme)) || parsedHost == null
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath())) {
            throw new IllegalArgumentException("Invalid origin");
        }
        String host = parsedHost.toLowerCase(Locale.ROOT);
        if (!host.contains(":")) {
            host = IDN.toASCII(host);
        } else {
            host = "[" + host + "]";
        }
        if (port < -1 || port > 65535) {
            throw new IllegalArgumentException("Invalid origin port");
        }
        boolean defaultPort = port == -1
                || "http".equals(scheme) && port == 80
                || "https".equals(scheme) && port == 443;
        return scheme + "://" + host
                + (defaultPort ? "" : ":" + port);
    }
}
