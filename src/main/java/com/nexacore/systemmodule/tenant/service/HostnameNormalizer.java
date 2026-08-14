package com.nexacore.systemmodule.tenant.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;

@Component
public class HostnameNormalizer {
    public String normalize(String rawHost) {
        if (!StringUtils.hasText(rawHost)) {
            throw new IllegalArgumentException("A tenant hostname is required");
        }
        String host = rawHost.trim();
        if (host.contains("://")) {
            try {
                URI uri = URI.create(host);
                if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                        || uri.getRawFragment() != null) throw new IllegalArgumentException("Invalid hostname");
                host = uri.getHost();
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid hostname", exception);
            }
        }
        if (host.startsWith("[")) {
            int closingBracket = host.indexOf(']');
            if (closingBracket < 0) throw new IllegalArgumentException("Invalid hostname");
            host = host.substring(1, closingBracket);
        } else {
            int colon = host.lastIndexOf(':');
            if (colon > 0 && host.indexOf(':') == colon) host = host.substring(0, colon);
        }
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        String normalized = host.contains(":")
                ? host.toLowerCase(Locale.ROOT)
                : IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.length() > 253) {
            throw new IllegalArgumentException("Invalid hostname");
        }
        return normalized;
    }
}
