package com.nexacore.catalogmodule.api;

import com.nexacore.gatewaymodule.layout.dto.PublicLayoutContextDto;
import com.nexacore.gatewaymodule.navigation.dto.PublicNavigationContextDto;

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
        SeoView defaultSeo,
        PublicLayoutContextDto layout,
        List<PublicNavigationContextDto.Region> navigationRegions
) {
    public PublicSiteResponse withLayout(PublicLayoutContextDto resolvedLayout) {
        return new PublicSiteResponse(siteKey, hostname, name, logo, supportedLocales, defaultLocale,
                navigation, categoryNavigation, contact, socialLinks, defaultSeo, resolvedLayout, navigationRegions);
    }

    public PublicSiteResponse withSystemContext(PublicLayoutContextDto resolvedLayout,
                                                PublicNavigationContextDto resolvedNavigation) {
        List<NavigationItemView> primary = resolvedNavigation.regions().stream()
                .filter(region -> "PRIMARY".equals(region.surface()))
                .flatMap(region -> region.groups().stream())
                .flatMap(group -> group.items().stream())
                .map(PublicSiteResponse::navigationItem)
                .toList();
        return new PublicSiteResponse(siteKey, hostname, name, logo, supportedLocales, defaultLocale,
                primary, categoryNavigation, contact, socialLinks, defaultSeo, resolvedLayout,
                resolvedNavigation.regions());
    }

    private static NavigationItemView navigationItem(PublicNavigationContextDto.Item item) {
        String path = "INTERNAL_ROUTE".equals(item.type()) ? item.routePath() : item.externalUrl();
        return new NavigationItemView(item.code(), item.label(), path,
                item.children().stream().map(PublicSiteResponse::navigationItem).toList());
    }

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
