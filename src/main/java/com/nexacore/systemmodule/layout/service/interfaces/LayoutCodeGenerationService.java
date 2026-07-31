package com.nexacore.systemmodule.layout.service.interfaces;

public interface LayoutCodeGenerationService {
    String normalizeBusinessCode(String value, String fallbackPrefix);
    String normalizeTCode(String value);
    void validateTCode(String value);
}
