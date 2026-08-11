-- Operational logs retain global/platform events, so tenant_id is nullable. Every
-- request that passed tenant resolution writes its server-derived tenant ID.

ALTER TABLE log_api_access_log
    ADD COLUMN tenant_id bigint,
    ADD COLUMN created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE log_error_log
    ADD COLUMN tenant_id bigint,
    ADD COLUMN created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE log_audit_log
    ADD COLUMN tenant_id bigint,
    ADD COLUMN created_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_by bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_log_api_access_tenant_created ON log_api_access_log(tenant_id, created_at DESC);
CREATE INDEX idx_log_error_tenant_created ON log_error_log(tenant_id, created_at DESC);
CREATE INDEX idx_log_audit_tenant_created ON log_audit_log(tenant_id, created_at DESC);

