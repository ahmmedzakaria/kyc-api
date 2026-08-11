package com.nexacore.systemmodule.tenant.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TenantCodeNormalizer {
    private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");
    public String normalize(String value) {
        String code = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!VALID.matcher(code).matches()) {
            throw new IllegalArgumentException("Tenant code must be 3-63 lowercase letters, digits, or hyphens");
        }
        return code;
    }
}
