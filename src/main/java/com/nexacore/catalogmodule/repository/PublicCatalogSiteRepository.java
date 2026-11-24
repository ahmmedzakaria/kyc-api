package com.nexacore.catalogmodule.repository;

import com.nexacore.catalogmodule.api.PublicSiteResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PublicCatalogSiteRepository {
    private final JdbcTemplate jdbc;

    public PublicCatalogSiteRepository(@Qualifier("catalogDataSource") DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public Optional<PublicSiteResponse> findPublished(long tenantId, String hostname,
                                                       String locale, List<String> supportedLocales,
                                                       String mediaPublicPath) {
        List<SiteRow> sites = jdbc.query("""
                SELECT s.id, s.site_key, s.default_locale,
                       COALESCE(requested.name, english.name) AS name,
                       COALESCE(requested.contact_email, english.contact_email, '') AS email,
                       COALESCE(requested.contact_phone, english.contact_phone, '') AS phone,
                       COALESCE(requested.address, english.address, '') AS address,
                       COALESCE(requested.seo_title, english.seo_title, requested.name, english.name) AS seo_title,
                       COALESCE(requested.seo_description, english.seo_description, '') AS seo_description
                FROM catalog_sites s
                JOIN catalog_site_translations english ON english.tenant_id=s.tenant_id
                     AND english.site_id=s.id AND english.locale='en'
                LEFT JOIN catalog_site_translations requested ON requested.tenant_id=s.tenant_id
                     AND requested.site_id=s.id AND requested.locale=?
                WHERE s.tenant_id=? AND s.publication_state='PUBLISHED'
                ORDER BY s.id LIMIT 1
                """, (rs, row) -> new SiteRow(rs.getLong("id"), rs.getString("site_key"),
                rs.getString("default_locale"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("address"), rs.getString("seo_title"),
                rs.getString("seo_description")), locale, tenantId);
        if (sites.isEmpty()) return Optional.empty();
        SiteRow site = sites.getFirst();
        var logo = new PublicSiteResponse.MediaView("site-logo", "IMAGE",
                "/assets/catalog/placeholder.svg", site.name(), site.name(), "image/svg+xml", null, null);
        return Optional.of(new PublicSiteResponse(site.siteKey(), hostname, site.name(), logo,
                List.copyOf(supportedLocales), site.defaultLocale(), navigation(tenantId, site.id(), locale),
                categories(tenantId, locale, mediaPublicPath),
                new PublicSiteResponse.ContactView(site.email(), site.phone(), site.address(), null),
                List.of(), new PublicSiteResponse.SeoView(site.seoTitle(), site.seoDescription(), "/", logo, false)));
    }

    private List<PublicSiteResponse.NavigationItemView> navigation(long tenantId, long siteId, String locale) {
        List<NavigationRow> rows = jdbc.query("""
                SELECT n.id,n.parent_id,COALESCE(t.label,en.label) label,n.path
                FROM catalog_navigation_items n
                JOIN catalog_navigation_item_translations en ON en.tenant_id=n.tenant_id AND en.navigation_item_id=n.id AND en.locale='en'
                LEFT JOIN catalog_navigation_item_translations t ON t.tenant_id=n.tenant_id AND t.navigation_item_id=n.id AND t.locale=?
                WHERE n.tenant_id=? AND n.site_id=? AND n.publication_state='PUBLISHED'
                ORDER BY n.display_order,n.id
                """, (rs, i) -> new NavigationRow(rs.getLong("id"), (Long) rs.getObject("parent_id"),
                rs.getString("label"), rs.getString("path")), locale, tenantId, siteId);
        Map<Long, List<NavigationRow>> children = new LinkedHashMap<>();
        rows.forEach(row -> children.computeIfAbsent(row.parentId(), ignored -> new ArrayList<>()).add(row));
        return navChildren(null, children);
    }

    private List<PublicSiteResponse.NavigationItemView> navChildren(Long parent, Map<Long, List<NavigationRow>> rows) {
        return rows.getOrDefault(parent, List.of()).stream()
                .map(value -> new PublicSiteResponse.NavigationItemView(Long.toString(value.id()),
                        value.label(), value.path(), navChildren(value.id(), rows)))
                .toList();
    }

    private List<PublicSiteResponse.CategoryView> categories(long tenantId, String locale, String mediaPath) {
        List<CategoryRow> values = jdbc.query("""
                SELECT c.id,c.slug,c.parent_id,CASE WHEN t.id IS NULL THEN 'en' ELSE ? END translation_locale,COALESCE(t.name,en.name) name,
                       COALESCE(t.summary,en.summary) summary,COALESCE(t.description,en.description) description,
                       c.image_media_id,(SELECT count(*) FROM catalog_products p WHERE p.tenant_id=c.tenant_id AND p.category_id=c.id AND p.publication_state='PUBLISHED') product_count
                FROM catalog_categories c
                JOIN catalog_category_translations en ON en.tenant_id=c.tenant_id AND en.category_id=c.id AND en.locale='en'
                LEFT JOIN catalog_category_translations t ON t.tenant_id=c.tenant_id AND t.category_id=c.id AND t.locale=?
                WHERE c.tenant_id=? AND c.publication_state='PUBLISHED'
                ORDER BY c.display_order,c.id
                """, (rs, i) -> {
            Long mediaId = (Long) rs.getObject("image_media_id");
            return new CategoryRow(rs.getLong("id"), (Long) rs.getObject("parent_id"), rs.getString("slug"),
                    rs.getString("translation_locale"), rs.getString("name"), rs.getString("summary"),
                    rs.getString("description"), mediaId, rs.getLong("product_count"));
        }, locale, locale, tenantId);
        Map<Long, List<CategoryRow>> children = new LinkedHashMap<>();
        values.forEach(value -> children.computeIfAbsent(value.parentId(), ignored -> new ArrayList<>()).add(value));
        return categoryChildren(null, children, mediaPath);
    }

    private List<PublicSiteResponse.CategoryView> categoryChildren(Long parent,
            Map<Long, List<CategoryRow>> rows, String mediaPath) {
        return rows.getOrDefault(parent, List.of()).stream().map(value -> {
            var translation = new PublicSiteResponse.LocalizedTextView(value.locale(), value.name(), value.summary(), value.description());
            var image = value.mediaId() == null ? null : new PublicSiteResponse.MediaView(value.mediaId().toString(), "IMAGE",
                    mediaPath + "/" + value.mediaId(), value.name(), null, null, null, null);
            return new PublicSiteResponse.CategoryView(Long.toString(value.id()), value.slug(),
                    value.parentId() == null ? null : value.parentId().toString(), List.of(translation), image,
                    categoryChildren(value.id(), rows, mediaPath), value.productCount());
        }).toList();
    }

    private record SiteRow(long id, String siteKey, String defaultLocale, String name,
                           String email, String phone, String address, String seoTitle, String seoDescription) {}
    private record NavigationRow(long id, Long parentId, String label, String path) {}
    private record CategoryRow(long id, Long parentId, String slug, String locale, String name,
                               String summary, String description, Long mediaId, long productCount) {}
}
