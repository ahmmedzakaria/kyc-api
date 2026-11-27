CREATE TABLE sys_public_navigation_groups (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenants(id),
    group_code VARCHAR(80) NOT NULL,
    group_name VARCHAR(150) NOT NULL,
    surface VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_public_navigation_group_code UNIQUE (tenant_id, group_code),
    CONSTRAINT uk_public_navigation_group_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_public_navigation_group_surface CHECK
        (surface IN ('PRIMARY', 'UTILITY', 'FOOTER', 'MOBILE', 'LOGIN'))
);

CREATE TABLE sys_public_navigation_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenants(id),
    navigation_group_id BIGINT NOT NULL,
    parent_id BIGINT,
    item_code VARCHAR(80) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    label VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    aria_label VARCHAR(200),
    route_path VARCHAR(500),
    external_url VARCHAR(1000),
    target VARCHAR(20) NOT NULL DEFAULT 'SELF',
    icon VARCHAR(100),
    display_order INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_public_navigation_item_code UNIQUE (navigation_group_id, item_code),
    CONSTRAINT uk_public_navigation_item_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_public_navigation_item_scope UNIQUE (id, tenant_id, navigation_group_id),
    CONSTRAINT fk_public_navigation_item_group FOREIGN KEY (navigation_group_id, tenant_id)
        REFERENCES sys_public_navigation_groups(id, tenant_id),
    CONSTRAINT fk_public_navigation_item_parent FOREIGN KEY (parent_id, tenant_id, navigation_group_id)
        REFERENCES sys_public_navigation_items(id, tenant_id, navigation_group_id),
    CONSTRAINT ck_public_navigation_item_type CHECK
        (item_type IN ('GROUP', 'INTERNAL_ROUTE', 'EXTERNAL_URL')),
    CONSTRAINT ck_public_navigation_item_target CHECK (target IN ('SELF', 'BLANK')),
    CONSTRAINT ck_public_navigation_item_destination CHECK (
        (item_type = 'GROUP' AND route_path IS NULL AND external_url IS NULL) OR
        (item_type = 'INTERNAL_ROUTE' AND route_path IS NOT NULL AND external_url IS NULL) OR
        (item_type = 'EXTERNAL_URL' AND route_path IS NULL AND external_url IS NOT NULL)
    )
);

CREATE TABLE sys_acc_client_public_navigation_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenants(id),
    client_application_id BIGINT NOT NULL REFERENCES sys_acc_client_applications(id),
    public_navigation_item_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_client_public_navigation_item UNIQUE
        (tenant_id, client_application_id, public_navigation_item_id),
    CONSTRAINT fk_client_public_navigation_item FOREIGN KEY (public_navigation_item_id, tenant_id)
        REFERENCES sys_public_navigation_items(id, tenant_id)
);

CREATE INDEX idx_public_navigation_groups_resolution
    ON sys_public_navigation_groups (tenant_id, surface, active, display_order);
CREATE INDEX idx_public_navigation_items_resolution
    ON sys_public_navigation_items (tenant_id, navigation_group_id, active, parent_id, display_order);
CREATE INDEX idx_client_public_navigation_resolution
    ON sys_acc_client_public_navigation_items (tenant_id, client_application_id, active);
