package com.nexacore.systemmodule.accesscontrol.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class ClientRedirectUriPolicy {
    private final ClientOriginPolicy originPolicy;

    public ClientRedirectUriPolicy(ClientOriginPolicy originPolicy) {
        this.originPolicy = originPolicy;
    }

    public String normalizeConfiguredUris(String configuredUris) {
        if (!StringUtils.hasText(configuredUris)) {
            return null;
        }
        Set<String> normalized = parse(configuredUris);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    public String resolveRegisteredUri(String configuredUris, String requestOrigin) {
        Set<String> registered = parse(configuredUris);
        if (registered.isEmpty()) {
            throw new IllegalArgumentException("No redirect URI is registered");
        }
        if (!StringUtils.hasText(requestOrigin)) {
            return registered.iterator().next();
        }
        String normalizedOrigin = originPolicy.normalizeOrigin(requestOrigin);
        return registered.stream()
                .filter(uri -> normalizedOrigin.equals(originOf(uri)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Origin has no registered redirect URI"));
    }

    private Set<String> parse(String configuredUris) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(configuredUris)) {
            return result;
        }
        for (String value : configuredUris.split(",")) {
            if (StringUtils.hasText(value)) {
                URI uri = URI.create(value.trim());
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null || uri.getUserInfo() != null
                        || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                    throw new IllegalArgumentException("Invalid redirect URI");
                }
                originOf(uri.toString());
                result.add(uri.toString());
            }
        }
        return result;
    }

    private String originOf(String value) {
        URI uri = URI.create(value);
        String authority = uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
        return originPolicy.normalizeOrigin(uri.getScheme() + "://" + authority);
    }
}
