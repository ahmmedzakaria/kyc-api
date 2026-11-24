package com.nexacore.catalogmodule.api;

import java.util.List;

/** Complete public site contract matching the Angular CatalogSiteView. */
public record PublicSiteResponse(
        String siteKey,
        String hostname,
        String name,
        MediaView logo,
        List<String> supportedLocales,
        String defaultLocale,
        List<NavigationItemView> navigation,
        List<CategoryView> categoryNavigation,
        ContactView contact,
        List<SocialLinkView> socialLinks,
        SeoView defaultSeo
) {
    public record MediaView(String id, String kind, String url, String alternativeText,
                            String title, String mimeType, Integer width, Integer height) {}
    public record NavigationItemView(String id, String label, String path,
                                     List<NavigationItemView> children) {}
    public record LocalizedTextView(String locale, String name, String summary, String description) {}
    public record CategoryView(String id, String slug, String parentId,
                               List<LocalizedTextView> translations, MediaView image,
                               List<CategoryView> children, long productCount) {}
    public record ContactView(String email, String phone, String address, String mapUrl) {}
    public record SocialLinkView(String provider, String url, String label) {}
    public record SeoView(String title, String description, String canonicalPath,
                          MediaView image, Boolean noIndex) {}
}
