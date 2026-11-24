-- Bootstrap the first published catalog site for tenant 7 (arsv / abd.localhost).
-- Tenant ownership is an application-level reference to system_db.sys_tenants.

INSERT INTO catalog_sites (
    tenant_id,
    site_key,
    default_locale,
    publication_state,
    published_at,
    published_by,
    version,
    created_by,
    updated_by
)
VALUES (
    7,
    'arsv-public-site',
    'en',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    'migration:v2',
    1,
    'migration:v2',
    'migration:v2'
)
ON CONFLICT (tenant_id, site_key) DO NOTHING;

INSERT INTO catalog_site_translations (
    tenant_id,
    site_id,
    locale,
    name,
    contact_email,
    address,
    seo_title,
    seo_description,
    created_by,
    updated_by
)
SELECT
    site.tenant_id,
    site.id,
    'en',
    'Rohman & Shuvo',
    'abd@gami.com',
    'Dhaka, Bangladesh',
    'Rohman & Shuvo',
    'Official product catalog and corporate website for Rohman & Shuvo.',
    'migration:v2',
    'migration:v2'
FROM catalog_sites site
WHERE site.tenant_id = 7
  AND site.site_key = 'arsv-public-site'
ON CONFLICT (tenant_id, site_id, locale) DO NOTHING;
