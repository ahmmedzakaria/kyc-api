package com.nexacore.gatewaymodule.layout.dto;

public record PublicFontConfigDto(
        String bodyFamily,
        String headingFamily,
        String monoFamily,
        String fontSource,
        String fontUrl,
        String fallbackStack
) {}
