package com.nexacore.authmodule.core.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class UsernameNormalizer {
    public static final int MAX_LENGTH = 150;

    public String normalize(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username is required");
        }

        String normalized = Normalizer.normalize(username.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("normalized username must not exceed " + MAX_LENGTH + " characters");
        }
        return normalized;
    }
}
