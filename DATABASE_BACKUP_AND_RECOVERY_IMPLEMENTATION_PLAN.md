# Database Backup and Recovery Utility Implementation Plan

## Status

Proposed implementation plan. Not yet implemented.

This plan adds scheduled and manually requested PostgreSQL backups for the NexaCore
application databases, stores encrypted backup bundles locally, and sends backup
notifications or encrypted backup delivery to approved email recipients. It also adds
an authorized Backup Administration page to `system-frontend-21`.

Backups are useful only when they can be restored. Restore verification, retention,
encryption, authorization, and operational alerting are therefore part of the feature,
not optional follow-up work.

## Scope

Initial application-database scope:

| Logical module | Database |
| --- | --- |
| Authentication | `auth_db` |
| System, privilege, layout, license, workflow | `system_db` |
| KYC | `kyc_db` |
| GIS | `gisdb` in current configuration; confirm whether production uses `gis_db` |
| Logs | `log_db` |

The Keycloak and Metabase databases are separate operational systems. They should be
added as explicitly configured optional targets after ownership and restore
responsibility are confirmed; do not silently include them using application database
credentials.

Uploaded files and MinIO objects are not PostgreSQL data. A complete disaster-recovery
plan must back them up separately. This plan records that dependency but initially
implements PostgreSQL backup only.

## Goals

- Run backups automatically at a configurable interval or cron schedule.
- Allow an authorized system administrator to request a backup from the frontend.
- Back up all configured application databases into one manifest-driven backup set.
- Encrypt backup files before long-term local storage or email delivery.
- Retain backups locally according to a configurable policy.
- Notify a dedicated, allowlisted email address about success or failure.
- Optionally attach a small encrypted backup bundle to email.
- Provide authorized list, status, download, retry-delivery, and verification actions.
- Record immutable audit metadata without logging credentials or plaintext data.
- Regularly prove that backups can be restored.

## Non-goals

- Do not allow every authenticated user to back up the complete platform.
- Do not expose database passwords, dump command lines, or server filesystem paths.
- Do not accept arbitrary email recipients from an on-demand request.
- Do not restore a production database directly from the frontend.
- Do not treat email as the only backup storage location.
- Do not store unencrypted dumps in the Git repository, uploads folder, or frontend.
- Do not claim that sequential logical dumps provide one atomic transaction across five
  physical databases.

## Security decision

The phrase "any user can take the backup" must mean any user holding an explicit
platform backup privilege. A full database backup contains credentials, personal data,
KYC evidence metadata, access-control configuration, workflow history, and logs. A
normal tenant user or tenant administrator must never receive a platform-wide dump.

Create separate privileges:

| Privilege | Purpose |
| --- | --- |
| `DATABASE_BACKUP_VIEW` | List backup jobs and sanitized metadata |
| `DATABASE_BACKUP_EXECUTE` | Request a new backup |
| `DATABASE_BACKUP_DOWNLOAD` | Download an encrypted completed backup |
| `DATABASE_BACKUP_DELIVER` | Retry delivery to configured recipients |
| `DATABASE_BACKUP_MANAGE` | Update schedule/retention settings if runtime editing is enabled |
| `DATABASE_BACKUP_VERIFY` | Request an isolated restore verification |

Register these through `ModulePrivilegeProvider`, seed them for the platform/system
administrator only, and use the same codes in backend `@PrivilegeApi`/
`@PreAuthorize`, frontend UI policies, route policies, and navigation linkage.

A tenant data export is a different feature. If required later, design a tenant-scoped
export that applies tenant/business/branch predicates and excludes credentials and
platform data. Do not implement it by giving tenant users access to this utility.

## Delivery decision

### Recommended default

```text
encrypted local backup
    + email success/failure notification
    + authenticated frontend download
```

Email should normally contain metadata and a short-lived authenticated link, not the
database dump itself. Mail systems commonly impose attachment size limits, replicate
messages across servers, scan attachments, and retain mail longer than intended.

### Optional attachment mode

Support attachment delivery only when all conditions hold:

- `BACKUP_EMAIL_ATTACHMENT_ENABLED=true`;
- the final encrypted bundle is below `BACKUP_EMAIL_ATTACHMENT_MAX_BYTES`;
- the recipient is in the server-side allowlist;
- encryption completed successfully;
- the encryption key is not contained in the same email;
- the mail provider accepts the encrypted file type.

When the file is too large, send a notification and secure download link instead. Do
not split sensitive archives across multiple emails in version 1.

## Backup format and consistency

Use the PostgreSQL client utility `pg_dump`, not ORM/JPA serialization.

Recommended per-database format:

```text
pg_dump --format=custom --compress=<configured> --no-owner --no-acl
```

The custom format supports `pg_restore`, selective inspection, and parallel restore.
Create one dump per physical database, then package the encrypted backup set with a
manifest.

Each `pg_dump` is transactionally consistent for its own database. Because the five
module databases are physically separate and the application has cross-database
application-level references, sequential dumps are not one atomic platform-wide
snapshot. The manifest must record the start and completion time of every dump and the
overall consistency limitation.

If the recovery-point requirement later demands a coordinated cluster-level snapshot,
evaluate PostgreSQL physical backup/WAL archiving or infrastructure volume snapshots.
That is a separate operational design from this logical backup utility.

## Backup set layout

Local storage must be outside the repository and configurable:

```text
BACKUP_LOCAL_ROOT=/var/lib/nexacore/backups
```

Illustrative logical layout:

```text
<root>/
  2026/
    08/
      11/
        backup-<job-id>/
          manifest.json
          auth_db.dump
          system_db.dump
          kyc_db.dump
          gisdb.dump
          log_db.dump
        backup-<job-id>.tar.zst.age
```

Plaintext staging files must live in a private temporary directory created with owner
permissions only. Delete staging files after encryption in a `finally` path. The
retained artifact should be encrypted; the manifest may also remain only inside the
encrypted archive if it contains sensitive operational details.

Do not expose absolute paths through APIs.

## Manifest

Every backup set includes a versioned manifest:

```json
{
  "schemaVersion": 1,
  "backupId": "uuid",
  "trigger": "SCHEDULED",
  "requestedBy": "system",
  "startedAt": "2026-08-11T02:00:00Z",
  "completedAt": "2026-08-11T02:04:20Z",
  "applicationVersion": "...",
  "postgresClientVersion": "...",
  "consistency": "INDEPENDENT_DATABASE_SNAPSHOTS",
  "databases": [
    {
      "logicalName": "auth",
      "databaseName": "auth_db",
      "startedAt": "...",
      "completedAt": "...",
      "sizeBytes": 12345,
      "sha256": "...",
      "status": "COMPLETED"
    }
  ],
  "encryptedArtifactSha256": "...",
  "encryptionKeyId": "..."
}
```

Never include passwords, JDBC URLs containing credentials, raw environment values, or
encryption secrets.

## Configuration

Use typed Spring configuration properties backed by environment variables:

```properties
nexacore.backup.enabled=${BACKUP_ENABLED:false}
nexacore.backup.schedule.cron=${BACKUP_CRON:0 0 2 * * *}
nexacore.backup.schedule.zone=${BACKUP_TIME_ZONE:UTC}
nexacore.backup.local-root=${BACKUP_LOCAL_ROOT:/var/lib/nexacore/backups}
nexacore.backup.retention.days=${BACKUP_RETENTION_DAYS:30}
nexacore.backup.retention.minimum-successful=${BACKUP_MIN_SUCCESSFUL:7}
nexacore.backup.timeout=${BACKUP_TIMEOUT:30m}
nexacore.backup.compression-level=${BACKUP_COMPRESSION_LEVEL:6}
nexacore.backup.email.enabled=${BACKUP_EMAIL_ENABLED:true}
nexacore.backup.email.recipients=${BACKUP_EMAIL_RECIPIENTS:}
nexacore.backup.email.attachment-enabled=${BACKUP_EMAIL_ATTACHMENT_ENABLED:false}
nexacore.backup.email.attachment-max-bytes=${BACKUP_EMAIL_ATTACHMENT_MAX_BYTES:15000000}
nexacore.backup.download-link-ttl=${BACKUP_DOWNLOAD_LINK_TTL:15m}
nexacore.backup.verification.enabled=${BACKUP_VERIFICATION_ENABLED:false}
nexacore.backup.verification.cron=${BACKUP_VERIFICATION_CRON:0 0 4 * * SUN}
```

Database targets should be derived from the configured application datasource URLs,
but backup credentials should be separately configurable and limited to the database
permissions required by `pg_dump`. Avoid using a PostgreSQL superuser.

Do not pass passwords in command-line arguments. Use a short-lived private
`PGPASSFILE` or equivalent protected process environment, ensure its mode is `0600`,
and remove it after the process completes. Command/error logging must redact host,
username, paths, and secrets as appropriate.

## Encryption and key management

Encrypt the packaged bundle before retention or delivery. Use an established tool or
library supporting authenticated encryption and streaming large files. Suitable
deployment choices include an operations-managed `age` public key or envelope
encryption using a KMS-managed key.

Required rules:

- encryption recipient/key ID is configuration, not a request parameter;
- private decryption keys are never stored with backups or sent by email;
- support key rotation while retaining old decryption capability for retained backups;
- record only the non-secret key ID in metadata;
- compute checksums before and after packaging/encryption;
- fail the job if encryption fails;
- never fall back to storing or emailing plaintext.

For local development, allow an explicitly configured development key. Do not add a
hard-coded production key or passphrase to source control.

## Backend module structure

Create a new System submodule, for example:

```text
com.nexacore.systemmodule.backup
  config/
    BackupProperties.java
  controller/
    DatabaseBackupController.java
  dto/
    BackupCreateRequestDto.java
    BackupJobDto.java
    BackupListRequestDto.java
    BackupDeliveryRequestDto.java
    BackupVerificationRequestDto.java
  entity/
    SysBackupJob.java
    SysBackupDatabaseResult.java
    SysBackupDelivery.java
    SysBackupVerification.java
  enums/
    BackupJobStatus.java
    BackupTrigger.java
    BackupDeliveryStatus.java
    BackupVerificationStatus.java
  repository/
  scheduler/
    DatabaseBackupScheduler.java
    BackupRetentionScheduler.java
    BackupVerificationScheduler.java
  service/interfaces/
  service/implementations/
  process/
    PostgreSqlDumpExecutor.java
    PostgreSqlRestoreVerifier.java
  storage/
    BackupStorage.java
    LocalEncryptedBackupStorage.java
  crypto/
    BackupEncryptor.java
  notification/
    BackupNotificationService.java
```

Keep process execution, storage, encryption, email delivery, metadata persistence, and
orchestration behind separate interfaces so they can be unit-tested and replaced.

## Metadata schema

Add new `system_db` tables with the required audit columns.

### `sys_backup_jobs`

```text
id UUID primary key
trigger_type SCHEDULED | MANUAL
status QUEUED | RUNNING | COMPLETED | PARTIAL | FAILED | DELETING | DELETED
requested_by
requested_at
started_at
completed_at
artifact_name
artifact_size_bytes
artifact_sha256
encryption_key_id
failure_code
sanitized_failure_message
expires_at
created_by
updated_by
created_at
updated_at
```

Do not store the database password, encryption secret, email message body, or absolute
filesystem path.

### `sys_backup_database_results`

```text
id
backup_job_id FK
logical_database
database_name
status
started_at
completed_at
size_bytes
sha256
failure_code
sanitized_failure_message
audit columns
```

### `sys_backup_deliveries`

```text
id
backup_job_id FK
recipient_hash_or_masked_value
delivery_mode NOTIFICATION | ATTACHMENT | DOWNLOAD_LINK
status
attempt_count
last_attempt_at
provider_message_id
sanitized_failure_message
audit columns
```

### `sys_backup_verifications`

```text
id
backup_job_id FK
status
started_at
completed_at
verified_databases
failure_code
sanitized_failure_message
audit columns
```

The job table lives in `system_db`, which is itself backed up. A backup job should
persist its initial metadata before dumping `system_db`; its final status will appear
only in later backups. This is acceptable and should be documented in the manifest.

## Job orchestration

### Scheduled job

Use Spring scheduling only to enqueue a backup request. Perform dump work on a bounded
dedicated executor, not the scheduler thread or HTTP request thread.

In a multi-instance backend deployment, acquire a distributed lock before starting a
scheduled backup. Use a system-database advisory lock or a reviewed scheduling-lock
library. If the lock cannot be obtained, record/metric the skipped run without starting
a duplicate backup.

### Manual job

The manual endpoint creates a `QUEUED` job and returns HTTP `202 Accepted` immediately.
The user polls job status or the frontend refreshes the list. Never keep the HTTP
request open while `pg_dump`, packaging, encryption, and email run.

### Concurrency

Default to one active full-platform backup at a time. If another request arrives:

- return the existing active job, or
- create a rejected/coalesced request with a stable conflict response.

Do not run concurrent dumps merely because several users click the button.

### Failure semantics

- `COMPLETED`: every configured database dumped, packaged, encrypted, and retained.
- `PARTIAL`: at least one database dump failed; do not present the bundle as a valid
  full backup.
- `FAILED`: no usable encrypted set was produced or encryption/storage failed.

Email failure does not invalidate a safely retained backup. Record backup status as
completed and delivery status as failed, then allow delivery retry.

## API contract

All endpoints follow the project's POST convention under:

```text
/api/v1/system/backup
```

### Request backup

```http
POST /api/v1/system/backup/run
```

Request body should normally be empty or contain only a non-sensitive operator note.
Database names, recipients, local paths, command switches, and encryption keys are
server configuration, not caller-controlled fields.

Required privilege: `DATABASE_BACKUP_EXECUTE`.

Response: queued `BackupJobDto` with no filesystem path.

### List jobs

```http
POST /api/v1/system/backup/list
```

Filters:

```text
status
trigger
requestedFrom
requestedTo
page
pageSize
```

Required privilege: `DATABASE_BACKUP_VIEW`.

Use server-side pagination; do not return an unlimited history.

### Job detail

```http
POST /api/v1/system/backup/detail
```

Returns sanitized job, per-database, delivery, and verification status.

### Download encrypted artifact

```http
POST /api/v1/system/backup/download
```

Required privilege: `DATABASE_BACKUP_DOWNLOAD` plus recent/step-up authentication if
available. Prefer returning a single-use short-lived download token and a separate
streaming download request rather than loading the entire archive into JVM memory.

Required response controls:

```text
Cache-Control: no-store
Content-Disposition: attachment
X-Content-Type-Options: nosniff
```

Audit successful and failed download attempts.

### Retry email delivery

```http
POST /api/v1/system/backup/delivery/retry
```

Required privilege: `DATABASE_BACKUP_DELIVER`. Recipients remain server-configured;
the request contains only the backup job ID and approved delivery mode.

### Request verification

```http
POST /api/v1/system/backup/verify
```

Required privilege: `DATABASE_BACKUP_VERIFY`. Verification runs asynchronously in an
isolated environment and never restores over a live application database.

### Configuration summary

```http
POST /api/v1/system/backup/configuration
```

Return only safe information such as enabled state, schedule, timezone, retention,
attachment size limit, and masked recipients. Never return credentials, paths,
passwords, or encryption material.

## Email integration

Reuse the existing JavaMail infrastructure through a backup-specific notification
adapter. Extend the existing email service only through a reviewed attachment/stream
contract; do not read large backup files fully into a byte array.

Success email should include:

- backup ID;
- trigger and requesting actor;
- start/completion time;
- per-database status;
- encrypted artifact size and checksum;
- retention expiry;
- verification state;
- secure download action when attachment is disabled or too large.

Failure email should include a stable failure code and sanitized operator guidance,
not raw process output that may reveal credentials or internal paths.

Email addresses must come from configuration or an administrator-managed allowlist.
Changing recipients is a high-risk configuration operation and must be audited.

## Local retention

Retention runs only against records/files known to the backup metadata store. Never
delete by broad glob or an unresolved path.

Deletion rules:

- retain backups for the configured number of days;
- always retain at least the configured minimum number of successful backups;
- do not count failed/partial jobs toward the successful minimum;
- optionally retain verified backups longer than unverified backups;
- prevent deletion while a download, delivery, or verification is active;
- mark metadata `DELETING`, delete the exact validated artifact, then mark `DELETED`;
- audit deletion and whether recovery remains possible.

Reject a backup root that resolves to `/`, a home directory, the repository root, or
another unsafe broad directory.

## Restore verification

A checksum proves file integrity but not restorability. At a configured interval,
select a retained encrypted backup and:

1. Decrypt it in an isolated private workspace.
2. Verify manifest and checksums.
3. Create isolated disposable PostgreSQL databases or a temporary PostgreSQL
   container.
4. Restore every dump with `pg_restore`.
5. Run schema and smoke checks, including Flyway history and key table counts.
6. Verify cross-database person/user/profile and role/privilege reconciliation where
   applicable.
7. Destroy the isolated databases and plaintext files.
8. Record and notify the result.

Restore remains an operations-runbook action in version 1. The system frontend may
request or display verification, but it must not restore production databases.

## Frontend design

Add route:

```text
/backup
```

Suggested page:

```text
pages/backup/
  backup-list.component.ts
  backup-list.component.html
  backup-list.component.scss
  backup.model.ts
```

Page sections:

### Overview

- last successful backup;
- next scheduled run;
- latest verification result;
- retained backup count and total encrypted size;
- schedule, timezone, and masked notification recipients.

### Backup jobs

Use `DynamicListComponent` with:

```text
backup ID
trigger
requested by
status badge
started/completed time
duration
encrypted size
delivery status
verification status
expiry
```

### Actions

- `Run backup now` for `DATABASE_BACKUP_EXECUTE`;
- `Download encrypted backup` for `DATABASE_BACKUP_DOWNLOAD`;
- `Retry delivery` for `DATABASE_BACKUP_DELIVER`;
- `Verify restore` for `DATABASE_BACKUP_VERIFY`;
- view sanitized failure detail for `DATABASE_BACKUP_VIEW`.

Use the shared authorized UI directive or existing privilege-context mechanism to hide
unavailable actions. Backend authorization remains mandatory.

Show an explicit warning that the downloaded file is encrypted and requires the
operations-managed private key. Never show or download the decryption key.

## Navigation and privilege registration

- Add a Backup Administration feature through the System privilege provider.
- Add a Flyway migration using the next free `system` version at implementation time.
- Seed `/backup` navigation for `SYSTEM_ADMIN_WEB`.
- Link navigation visibility to `DATABASE_BACKUP_VIEW`.
- Add route and UI policies for every sensitive action.
- Add `ApiEndpoints` entries and a `backup.service.ts` in
  `system-frontend-21/src/app/core/services`.
- Document endpoints and operational limitations in `system-frontend-21/MODULES.md`.

## Process execution safety

`PostgreSqlDumpExecutor` must:

- use `ProcessBuilder` with an argument list, never a shell-concatenated command;
- validate the configured executable path and database target allowlist;
- prohibit caller-controlled command options;
- set a hard timeout and terminate the exact child process on timeout;
- capture bounded stdout/stderr without leaking it to clients;
- redact sensitive output before logging or persistence;
- verify a zero exit code and non-empty output;
- calculate checksums using streaming I/O;
- avoid loading dumps into JVM memory;
- record PostgreSQL client/server compatibility information;
- expose health/readiness diagnostics when `pg_dump` is unavailable.

Container deployment must install a PostgreSQL client version compatible with the
production server and mount a dedicated backup volume. Do not write backups into the
container's ephemeral filesystem.

## Multi-tenant behavior

This utility produces a platform-level backup of shared-schema databases. It must run
outside tenant-row filtering under an explicit system backup execution context—not by
accidentally omitting tenant context.

Rules:

- only platform backup privileges authorize execution and download;
- the backup runner uses dedicated database credentials and does not reuse a user's
  tenant-filtered ORM session;
- every manual job records the authenticated platform actor and client application;
- tenant administrators cannot request, list, download, or receive full backups;
- future tenant exports must be separate, tenant-scoped, redacted APIs.

## Audit and observability

Record structured events for:

- scheduled/manual request;
- job start/completion/failure;
- each database dump result;
- encryption and retention result;
- email delivery and retry;
- download token creation and artifact download;
- verification request/result;
- retention deletion;
- configuration changes.

Metrics:

```text
backup_last_success_timestamp
backup_duration_seconds
backup_artifact_bytes
backup_failures_total
backup_partial_total
backup_delivery_failures_total
backup_verification_last_success_timestamp
backup_retained_successful_count
backup_oldest_successful_age_seconds
```

Alert when:

- no successful backup exists inside the expected recovery-point window;
- repeated scheduled runs fail or are skipped;
- restore verification fails;
- local storage approaches capacity;
- email delivery repeatedly fails;
- a download is attempted without authorization;
- retained successful backups fall below the minimum.

## Phased implementation

### Phase 0 — operational decisions

- Confirm the five application database names in every environment, especially
  `gisdb` versus `gis_db`.
- Define recovery point objective and recovery time objective.
- Confirm whether Keycloak, Metabase, and MinIO are separate backup responsibilities.
- Choose encryption/key-management mechanism.
- Choose notification-only versus optional encrypted attachment delivery.
- Choose local retention capacity and filesystem ownership.
- Approve platform backup privileges and recipient allowlist.

### Phase 1 — backup core

- Add typed configuration and startup validation.
- Implement target discovery from allowlisted datasource configuration.
- Implement safe `pg_dump` execution and manifest generation.
- Implement packaging, encryption, checksum, and local encrypted storage.
- Add unit tests for command construction, redaction, timeout, and failure handling.
- Provide a command-line or service-level manual test before exposing an API.

### Phase 2 — metadata and asynchronous orchestration

- Add `sys_backup_*` migrations and entities.
- Implement queued jobs, bounded executor, concurrency lock, and status transitions.
- Add scheduled enqueueing with distributed locking.
- Add safe retention processing.
- Recover stale `RUNNING` jobs after application restart as failed/interrupted.

### Phase 3 — notifications

- Add success/failure email templates.
- Add notification-only delivery first.
- Add retry with bounded exponential backoff.
- Add optional streaming attachment delivery behind size and configuration gates.
- Verify that secrets and raw process output never enter email.

### Phase 4 — secured backend APIs

- Register backup privilege definitions.
- Add controllers, DTO validation, pagination, and stable error codes.
- Add short-lived single-use download tokens and streaming responses.
- Add audit events, rate limits, and recent-authentication checks where available.
- Add authorization tests for every endpoint and direct-ID access.

### Phase 5 — system frontend

- Add `/backup`, service, models, list, status, and authorized actions.
- Add navigation and route/UI policies.
- Handle queued/running/partial/failed/expired/deleted states.
- Add confirmation dialogs for expensive actions.
- Never expose paths, keys, raw stderr, or recipient addresses.

### Phase 6 — restore verification

- Implement isolated decrypt/restore verification.
- Add scheduled verification and manual authorized request.
- Add schema, Flyway, row-count, and cross-database reconciliation checks.
- Publish and test the production restore runbook.

### Phase 7 — disaster-recovery completeness

- Add independently managed MinIO/upload backup.
- Decide and add Keycloak/Metabase backup if owned by this platform.
- Add off-host/object-storage replication; local-only backup does not protect against
  host loss, disk failure, theft, or ransomware.
- Test a complete environment recovery using database and file backups.

## Testing requirements

### Unit tests

- configuration validation;
- database allowlisting;
- command argument construction without shell injection;
- password and output redaction;
- status transitions;
- attachment size fallback;
- retention selection without unsafe path deletion;
- role/privilege authorization decisions.

### Integration tests

- dump and restore each database into disposable PostgreSQL databases;
- failed database produces `PARTIAL` and no misleading full-success state;
- encryption failure retains no plaintext artifact;
- duplicate scheduled/manual requests do not run concurrently;
- application restart handles stale jobs;
- email failure permits retry without rerunning the dump;
- expired download tokens fail;
- unauthorized and tenant users cannot list, run, or download backups;
- direct-ID requests cannot enumerate backup metadata.

### Frontend tests

- actions render only for matching privileges;
- manual request displays queued/running progress;
- partial/failure detail is sanitized;
- download uses the authorized flow and handles expiry;
- empty history and disabled backup configuration render safely.

### Operational acceptance test

1. Seed representative data in all five databases.
2. Run a manual backup through the frontend.
3. Confirm one encrypted retained artifact and correct metadata.
4. Confirm notification or size-gated attachment delivery.
5. Restore every database in an isolated environment.
6. Start the backend against restored databases.
7. Run authentication, person/profile, privilege, layout, license, workflow, GIS, and
   log smoke checks.
8. Record actual backup duration, size, restore duration, and recovery gaps.

## Rollback and failure recovery

- The feature is disabled by default with `BACKUP_ENABLED=false`.
- Disabling scheduling must not delete retained backups.
- Metadata migrations are additive.
- A failed deployment can disable APIs/scheduling while operations retain direct access
  to encrypted artifacts.
- Do not automatically delete partial staging data until the failure handler has
  recorded diagnostics; then remove plaintext securely from the private staging area.
- If metadata and filesystem state diverge, run a reconciliation command that reports
  orphan metadata/artifacts before any deletion.

## Completion criteria

Version 1 is complete only when:

- all configured application databases are included in every successful backup set;
- backups are encrypted before retention or delivery;
- no password or encryption secret appears in process arguments, logs, APIs, or email;
- scheduled jobs cannot duplicate across application instances;
- manual jobs are asynchronous and privilege-protected;
- recipients are server-controlled and allowlisted;
- encrypted artifacts can be downloaded only by authorized platform users;
- retention preserves the configured minimum number of successful backups;
- a full isolated restore has succeeded and is documented;
- failure and stale-backup alerts are active;
- tenant users cannot access platform backup functionality;
- local backup is explicitly documented as insufficient for host-level disaster
  recovery until off-host replication is implemented.

