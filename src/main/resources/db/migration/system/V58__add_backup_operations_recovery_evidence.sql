CREATE TABLE IF NOT EXISTS sys_backup_delivery_attempts (
    id BIGSERIAL PRIMARY KEY, backup_job_id UUID NOT NULL REFERENCES sys_backup_jobs(id),
    channel VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL, attempted_by VARCHAR(150) NOT NULL,
    attempted_at TIMESTAMP NOT NULL, failure_code VARCHAR(80), sanitized_failure_message VARCHAR(1000),
    created_by BIGINT NOT NULL DEFAULT 0, updated_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_backup_delivery_job_attempted ON sys_backup_delivery_attempts(backup_job_id,attempted_at DESC);

CREATE TABLE IF NOT EXISTS sys_backup_restore_verifications (
    id BIGSERIAL PRIMARY KEY, backup_job_id UUID NOT NULL REFERENCES sys_backup_jobs(id), status VARCHAR(20) NOT NULL,
    artifact_sha256 VARCHAR(64) NOT NULL, verified_at TIMESTAMP NOT NULL, verifier_id VARCHAR(150) NOT NULL,
    sanitized_failure_message VARCHAR(1000), offsite_replication_status VARCHAR(30) NOT NULL,
    offsite_reference VARCHAR(250), created_by BIGINT NOT NULL DEFAULT 0, updated_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_backup_restore_verified ON sys_backup_restore_verifications(verified_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_backup_restore_job_verified ON sys_backup_restore_verifications(backup_job_id,verified_at DESC);

INSERT INTO sys_acc_client_api_permissions(client_application_id,api_registry_id,active,created_by,updated_by,created_at,updated_at)
SELECT client.id,api.id,true,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM sys_acc_client_applications client
JOIN sys_acc_api_registry api ON api.path_pattern IN (
 '/api/v1/system/backup/delivery/retry','/api/v1/system/backup/delivery/history',
 '/api/v1/system/backup/configuration','/api/v1/system/backup/readiness',
 '/api/v1/system/backup/restore-verification/list') AND api.active
WHERE client.client_code='SYSTEM_ADMIN_WEB'
ON CONFLICT(client_application_id,api_registry_id) DO UPDATE SET active=true,updated_by=0,updated_at=CURRENT_TIMESTAMP;
