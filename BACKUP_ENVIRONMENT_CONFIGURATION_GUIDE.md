# Database Backup Environment Configuration Guide

## Purpose

NexaCore database backup is disabled by default. A deployment must explicitly enable it and provide a valid 256-bit AES encryption key before manual or scheduled backups can produce an artifact.

The application backs up the configured Auth, KYC, System, GIS, and Log PostgreSQL databases. Each successful run produces an AES-GCM encrypted archive in the configured local backup directory.

## Required environment variables

| Variable | Required | Example | Description |
|---|---:|---|---|
| `BACKUP_ENABLED` | Yes | `true` | Enables manual and scheduled database backup execution. |
| `BACKUP_ENCRYPTION_KEY_BASE64` | Yes | Base64 value | A Base64-encoded key that must decode to exactly 32 bytes. |
| `BACKUP_LOCAL_ROOT` | Recommended | `/var/lib/nexacore/backups` | Dedicated directory where encrypted artifacts are stored. |
| `BACKUP_PG_DUMP_EXECUTABLE` | Recommended | `/usr/bin/pg_dump` | Absolute path or command name for `pg_dump`. |

Do not commit the encryption key to Git, `.env.example`, application properties, Compose files, or documentation.

## Generate the encryption key

Generate one 256-bit key:

```bash
openssl rand -base64 32 | tr -d '\n'
```

Store the output in a password manager, secret manager, or another protected recovery location. Do not regenerate this key during routine restarts. Losing it makes existing encrypted backups unrecoverable.

Set the key in the current terminal:

```bash
export BACKUP_ENCRYPTION_KEY_BASE64="$(openssl rand -base64 32 | tr -d '\n')"
```

Verify the decoded size without printing the key:

```bash
printf '%s' "$BACKUP_ENCRYPTION_KEY_BASE64" | base64 --decode | wc -c
```

The result must be:

```text
32
```

## Complete local-development configuration

From the backend directory:

```bash
cd /home/zahmmed/volume2/kyc-project-Copy/backend

export BACKUP_ENABLED=true
export BACKUP_LOCAL_ROOT="$PWD/runtime/backups"
export BACKUP_PG_DUMP_EXECUTABLE=/usr/bin/pg_dump
export BACKUP_ENCRYPTION_KEY_BASE64='<stored-44-character-base64-value>'
export BACKUP_ENCRYPTION_KEY_ID=local-v1
export BACKUP_RETENTION_DAYS=30
export BACKUP_MIN_SUCCESSFUL=7
export BACKUP_TIMEOUT=30m
export BACKUP_CRON='0 0 2 * * *'
export BACKUP_TIME_ZONE=Asia/Dhaka
export BACKUP_EMAIL_ENABLED=false

mvn spring-boot:run
```

Replace the placeholder with the real Base64 value. Do not include angle brackets in the actual value.

Environment variables only affect processes started from the same environment. Exporting variables in a terminal does not update an already running backend or an IntelliJ run configuration.

## IntelliJ configuration

Open **Run → Edit Configurations**, select the Spring Boot backend configuration, and add these environment variables:

```text
BACKUP_ENABLED=true
BACKUP_LOCAL_ROOT=/home/zahmmed/volume2/kyc-project-Copy/backend/runtime/backups
BACKUP_PG_DUMP_EXECUTABLE=/usr/bin/pg_dump
BACKUP_ENCRYPTION_KEY_BASE64=<actual-base64-key>
BACKUP_ENCRYPTION_KEY_ID=local-v1
BACKUP_EMAIL_ENABLED=false
```

Stop the existing backend process completely and start it again from that configuration. Log in again after restart because the current local login-session registry is held in memory.

## Docker Compose configuration

Create or update the root `.env` file:

```dotenv
BACKUP_ENABLED=true
BACKUP_ENCRYPTION_KEY_BASE64=<actual-base64-key>
BACKUP_ENCRYPTION_KEY_ID=production-v1
BACKUP_CRON=0 0 2 * * *
BACKUP_TIME_ZONE=Asia/Dhaka
BACKUP_RETENTION_DAYS=30
BACKUP_MIN_SUCCESSFUL=7
BACKUP_EMAIL_ENABLED=false
```

The Compose service sets `BACKUP_LOCAL_ROOT=/opt/nexacore/backups` and stores that directory in the `nexacore-backups` Docker volume.

Rebuild and restart the backend:

```bash
docker compose up -d --build nexacore-backend
```

Production deployments should inject `BACKUP_ENCRYPTION_KEY_BASE64` from the platform's secret manager instead of keeping it in a plaintext `.env` file.

## Optional scheduling and retention variables

| Variable | Default | Description |
|---|---|---|
| `BACKUP_CRON` | `0 0 2 * * *` | Spring cron expression for scheduled backup. Default is 02:00:00 daily. |
| `BACKUP_TIME_ZONE` | `UTC` | Time zone used to evaluate the cron expression. |
| `BACKUP_RETENTION_DAYS` | `30` | Artifact expiration and retention period in days. |
| `BACKUP_MIN_SUCCESSFUL` | `7` | Minimum number of successful backups retained by cleanup. |
| `BACKUP_TIMEOUT` | `30m` | Maximum execution time allowed for each `pg_dump` process. |
| `BACKUP_ENCRYPTION_KEY_ID` | `local-v1` | Non-secret identifier recorded with the artifact to identify the required key. |

Changing `BACKUP_ENCRYPTION_KEY_ID` does not rotate the encryption key. When intentionally rotating keys, retain every old key together with its key ID until all corresponding backups have expired.

## Optional email delivery variables

| Variable | Default | Description |
|---|---|---|
| `BACKUP_EMAIL_ENABLED` | `true` | Enables backup completion notifications. Set to `false` until email is configured. |
| `BACKUP_EMAIL_RECIPIENTS` | Empty | Comma-separated dedicated backup-recipient addresses. |
| `BACKUP_EMAIL_ATTACHMENT_ENABLED` | `false` | Attaches the encrypted artifact when it is within the configured size limit. |
| `BACKUP_EMAIL_ATTACHMENT_MAX_BYTES` | `15000000` | Maximum encrypted artifact size allowed as an attachment. |

Email delivery also requires the application's general email settings, including `EMAIL_MESSAGING_ENABLED`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD`.

Example:

```bash
export BACKUP_EMAIL_ENABLED=true
export BACKUP_EMAIL_RECIPIENTS='backup-operator@example.com'
export BACKUP_EMAIL_ATTACHMENT_ENABLED=false

export EMAIL_MESSAGING_ENABLED=true
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_USERNAME='<smtp-user>'
export SMTP_PASSWORD='<smtp-password>'
export SMTP_AUTH=true
export SMTP_STARTTLS_ENABLE=true
```

Prefer sending a completion notification without an attachment when artifacts are large or email retention rules are unsuitable for database backups.

## PostgreSQL connectivity

The application derives each backup target from its configured JDBC datasource. Confirm that `pg_dump` can reach PostgreSQL using TCP:

```bash
PGPASSWORD='<database-password>' pg_dump \
  -h localhost -p 5433 -U postgres -d kyc_db \
  -F c -f /tmp/kyc-db-connection-test.dump
```

For local command-line tools, `~/.pgpass` can avoid passwords in shell history:

```text
localhost:5433:*:postgres:<database-password>
```

Secure it with:

```bash
chmod 600 ~/.pgpass
```

The backend backup worker creates a temporary PostgreSQL password file for each dump operation and does not add database passwords to the command line.

## Verify the running process configuration

Find the process listening on port 9100:

```bash
BACKEND_PID="$(lsof -t -iTCP:9100 -sTCP:LISTEN)"
```

Verify non-secret values inherited by that process:

```bash
tr '\0' '\n' < "/proc/$BACKEND_PID/environ" | \
  grep -E '^BACKUP_(ENABLED|LOCAL_ROOT|PG_DUMP_EXECUTABLE|TIME_ZONE|EMAIL_ENABLED)='
```

Do not print or log `BACKUP_ENCRYPTION_KEY_BASE64`.

## Expected API behavior

When backup is disabled, `POST /api/v1/system/backup/run` returns:

```json
{
  "status": "ERROR",
  "statusCode": 503,
  "message": [
    {
      "type": "ERROR",
      "code": "DATABASE_BACKUP_DISABLED",
      "message": "Database backup is disabled by system configuration"
    }
  ],
  "data": null
}
```

When enabled and correctly configured, the endpoint returns HTTP `202 Accepted` with a queued backup job. Execution continues asynchronously; use the list or detail endpoint to inspect the final job and per-database results.

## Troubleshooting

### `DATABASE_BACKUP_DISABLED`

The running JVM did not receive `BACKUP_ENABLED=true`. Restart the backend from the environment where the variable is configured.

### `BACKUP_ENCRYPTION_KEY_BASE64 must decode to 32 bytes`

The configured value is not a Base64-encoded 256-bit key. Generate it with `openssl rand -base64 32`, verify that it decodes to 32 bytes, and restart the backend.

### `pg_dump: no password supplied`

Verify the datasource password configuration. For standalone shell scripts, configure `PGPASSWORD`, `PGPASSFILE`, or `~/.pgpass`.

### `Peer authentication failed for user postgres`

The command is using a Unix socket. Supply `-h localhost` to force TCP password authentication.

### `pg_dump executable not found`

Install PostgreSQL client tools and set `BACKUP_PG_DUMP_EXECUTABLE` to the absolute executable path, commonly `/usr/bin/pg_dump`.

### Backup job is `FAILED` but database rows are `COMPLETED`

The database dump stage succeeded and packaging, encryption, filesystem, or notification processing failed afterward. Inspect the job-level `failureCode` and `failureMessage` through the backup detail endpoint.

## Security checklist

- Keep backup disabled until storage, encryption, and recovery procedures are configured.
- Store encryption keys outside the application repository and backup artifacts.
- Restrict the backup directory to the backend operating-system account.
- Never use world-writable permissions such as `chmod 777`.
- Keep database and SMTP passwords out of command history and source control.
- Test restoration regularly using isolated `_restore` databases.
- Retain old encryption keys for as long as backups encrypted with them exist.
- Monitor failed and partial backup jobs.
- Keep at least one protected off-host copy for disaster recovery.
