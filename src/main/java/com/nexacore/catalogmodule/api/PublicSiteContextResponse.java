package com.nexacore.catalogmodule.api;

import java.util.List;

/** Public, presentation-safe contract. Internal tenant identifiers are deliberately excluded. */
public record PublicSiteContextResponse(
        String siteKey,
        String hostname,
        String defaultLocale,
        List<String> supportedLocales,
        String apiVersion
) {
}
