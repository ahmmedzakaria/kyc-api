package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LayoutCodeGenerationServiceImpl implements LayoutCodeGenerationService {

    @Override
    public String normalizeBusinessCode(String value, String fallbackPrefix) {
        String source = value == null || value.isBlank() ? fallbackPrefix : value;
        String normalized = source.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Layout code is required");
        }
        return normalized;
    }

    @Override
    public String normalizeTCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    @Override
    public void validateTCode(String value) {
        String normalized = normalizeTCode(value);
        if (normalized == null) {
            return;
        }
        if (normalized.length() > 30 || !normalized.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("T-code must be uppercase alphanumeric and at most 30 characters");
        }
        String suffix = normalized.replaceAll("^.*?(\\d{3})$", "$1");
        if (suffix.matches("\\d{3}") && Integer.parseInt(suffix) <= 100) {
            throw new IllegalArgumentException("T-code numeric values 001-100 are reserved for system UI quick navigation");
        }
    }
}
