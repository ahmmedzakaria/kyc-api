-- Tenant/client assignments may override branding from their global layout template.
-- NULL means inherit the corresponding global profile branding value.

ALTER TABLE sys_client_layout_profiles
    ADD COLUMN brand_display_name varchar(180),
    ADD COLUMN brand_short_name varchar(80),
    ADD COLUMN brand_logo_url varchar(500),
    ADD COLUMN brand_logo_dark_url varchar(500),
    ADD COLUMN brand_favicon_url varchar(500),
    ADD COLUMN brand_support_url varchar(500);

CREATE INDEX idx_sys_client_layout_profiles_tenant_default
    ON sys_client_layout_profiles(tenant_id, client_application_id, default_profile, active);
