package com.nexacore.gatewaymodule.layout.dto;

public record PublicLayoutBrandDto(
        String displayName,
        String shortName,
        String logoUrl,
        String logoDarkUrl,
        String faviconUrl,
        String supportUrl
) {}
