ALTER TABLE sys_acc_client_applications
    ADD COLUMN IF NOT EXISTS oauth_client_id VARCHAR(150),
    ADD COLUMN IF NOT EXISTS allowed_redirect_uris TEXT,
    ADD COLUMN IF NOT EXISTS allowed_logout_redirect_uris TEXT;

UPDATE sys_acc_client_applications
SET oauth_client_id = COALESCE(oauth_client_id, 'nexacore-client'),
    allowed_origins = COALESCE(NULLIF(allowed_origins, ''), 'http://localhost:5300'),
    allowed_redirect_uris = COALESCE(allowed_redirect_uris, 'http://localhost:5300/sso/callback'),
    allowed_logout_redirect_uris = COALESCE(allowed_logout_redirect_uris, 'http://localhost:5300/login'),
    updated_by = COALESCE(updated_by, 0),
    updated_at = CURRENT_TIMESTAMP
WHERE client_code = 'WEB';

UPDATE sys_acc_client_applications
SET oauth_client_id = COALESCE(oauth_client_id, 'nexacore-client'),
    allowed_origins = COALESCE(NULLIF(allowed_origins, ''), 'http://localhost:5301'),
    allowed_redirect_uris = COALESCE(allowed_redirect_uris, 'http://localhost:5301/sso/callback'),
    allowed_logout_redirect_uris = COALESCE(allowed_logout_redirect_uris, 'http://localhost:5301/login'),
    updated_by = COALESCE(updated_by, 0),
    updated_at = CURRENT_TIMESTAMP
WHERE client_code = 'SYSTEM_ADMIN_WEB';

UPDATE sys_acc_client_applications
SET oauth_client_id = COALESCE(oauth_client_id, 'nexacore-client'),
    allowed_origins = COALESCE(NULLIF(allowed_origins, ''), 'http://localhost:5302'),
    allowed_redirect_uris = COALESCE(allowed_redirect_uris, 'http://localhost:5302/sso/callback'),
    allowed_logout_redirect_uris = COALESCE(allowed_logout_redirect_uris, 'http://localhost:5302/login'),
    updated_by = COALESCE(updated_by, 0),
    updated_at = CURRENT_TIMESTAMP
WHERE client_code = 'LOG_ADMIN_WEB';
