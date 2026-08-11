-- The Auth identity reset invalidates stored user IDs and usernames. Remove all
-- user-linked operational logs so new identity IDs cannot be confused with old actors.

TRUNCATE TABLE
    log_api_access_log,
    log_audit_log,
    log_error_log
RESTART IDENTITY;
