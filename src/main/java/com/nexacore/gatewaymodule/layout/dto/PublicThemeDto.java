package com.nexacore.gatewaymodule.layout.dto;

public record PublicThemeDto(
        String id,
        String label,
        String base,
        String swatch,
        PublicThemePrimariesDto primaries,
        PublicThemeChromeDto chromeOverrides
) {}
