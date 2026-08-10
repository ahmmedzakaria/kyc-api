#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

# Connection settings. Supply the password through PGPASSWORD or ~/.pgpass.
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5433}"
PGUSER="${PGUSER:-postgres}"

# Override with a space-separated list when a deployment uses different names.
BACKUP_DATABASES="${BACKUP_DATABASES:-auth_db kyc_db system_db gisdb log_db keycloak}"
BACKUP_ROOT="${BACKUP_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/runtime/database-backups}"
PG_DUMP_EXECUTABLE="${PG_DUMP_EXECUTABLE:-pg_dump}"
RETENTION_DAYS="${RETENTION_DAYS:-0}"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v "${PG_DUMP_EXECUTABLE}" >/dev/null 2>&1 \
  || fail "pg_dump executable not found: ${PG_DUMP_EXECUTABLE}"
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum is required"

configured_pgpass="${PGPASSFILE:-}"
if [[ -z "${configured_pgpass}" && -n "${HOME:-}" ]]; then
  configured_pgpass="${HOME}/.pgpass"
fi
if [[ -z "${PGPASSWORD:-}" && ( -z "${configured_pgpass}" || ! -f "${configured_pgpass}" ) ]]; then
  if [[ -t 0 ]]; then
    read -r -s -p "PostgreSQL password for ${PGUSER}@${PGHOST}:${PGPORT}: " PGPASSWORD
    printf '\n'
    [[ -n "${PGPASSWORD}" ]] || fail "PostgreSQL password cannot be empty"
    export PGPASSWORD
  else
    fail "No PostgreSQL password available; set PGPASSWORD, PGPASSFILE, or ~/.pgpass"
  fi
fi

[[ -n "${BACKUP_ROOT}" && "${BACKUP_ROOT}" != "/" ]] \
  || fail "BACKUP_ROOT must be a dedicated directory"
[[ "${PGPORT}" =~ ^[0-9]+$ ]] || fail "PGPORT must be numeric"
[[ "${RETENTION_DAYS}" =~ ^[0-9]+$ ]] || fail "RETENTION_DAYS must be zero or a positive integer"

read -r -a databases <<< "${BACKUP_DATABASES}"
(( ${#databases[@]} > 0 )) || fail "BACKUP_DATABASES is empty"

for database in "${databases[@]}"; do
  [[ "${database}" =~ ^[A-Za-z0-9_]+$ ]] \
    || fail "Unsafe database name: ${database}"
done

timestamp="$(date -u +'%Y%m%dT%H%M%SZ')"
run_name="backup-${timestamp}-$$"
partial_directory="${BACKUP_ROOT}/.${run_name}.partial"
final_directory="${BACKUP_ROOT}/${run_name}"

mkdir -p "${BACKUP_ROOT}" "${partial_directory}"

cleanup_partial() {
  if [[ -d "${partial_directory}" ]]; then
    rm -rf -- "${partial_directory}"
  fi
}
trap cleanup_partial EXIT INT TERM

printf 'created_at_utc=%s\n' "${timestamp}" > "${partial_directory}/manifest.txt"
printf 'postgres_host=%s\n' "${PGHOST}" >> "${partial_directory}/manifest.txt"
printf 'postgres_port=%s\n' "${PGPORT}" >> "${partial_directory}/manifest.txt"
printf 'postgres_user=%s\n' "${PGUSER}" >> "${partial_directory}/manifest.txt"
printf 'pg_dump_version=%s\n' "$("${PG_DUMP_EXECUTABLE}" --version)" >> "${partial_directory}/manifest.txt"
printf 'databases=%s\n' "${BACKUP_DATABASES}" >> "${partial_directory}/manifest.txt"

failed=0
for database in "${databases[@]}"; do
  output="${partial_directory}/${database}.dump"
  printf 'Backing up %s...\n' "${database}"
  if "${PG_DUMP_EXECUTABLE}" \
      --host="${PGHOST}" \
      --port="${PGPORT}" \
      --username="${PGUSER}" \
      --dbname="${database}" \
      --format=custom \
      --compress=6 \
      --no-password \
      --file="${output}"; then
    (
      cd "${partial_directory}"
      sha256sum "${database}.dump" >> SHA256SUMS
    )
    printf 'Completed %s\n' "${database}"
  else
    printf 'Failed %s\n' "${database}" >&2
    failed=1
  fi
done

if (( failed != 0 )); then
  fail "One or more databases failed; incomplete files were removed"
fi

mv -- "${partial_directory}" "${final_directory}"
trap - EXIT INT TERM

if (( RETENTION_DAYS > 0 )); then
  find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d \
    -name 'backup-*' -mtime "+${RETENTION_DAYS}" -exec rm -rf -- {} +
fi

printf 'All database backups completed: %s\n' "${final_directory}"
printf 'Verify checksums with: cd %q && sha256sum --check SHA256SUMS\n' "${final_directory}"
