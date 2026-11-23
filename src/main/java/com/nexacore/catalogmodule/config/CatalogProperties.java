package com.nexacore.catalogmodule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(
        String defaultLocale,
        List<String> supportedLocales,
        Media media
) {
    public CatalogProperties {
        defaultLocale = defaultLocale == null || defaultLocale.isBlank() ? "en" : defaultLocale;
        supportedLocales = supportedLocales == null || supportedLocales.isEmpty()
                ? List.of("en", "bn") : List.copyOf(supportedLocales);
        media = media == null ? new Media("catalog", "/api/v1/public/media") : media;
    }

    public record Media(String namespace, String publicPath) {
    }
}
