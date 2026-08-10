#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5433}"
PGUSER="${PGUSER:-postgres}"
TARGET_SUFFIX="${TARGET_SUFFIX:-_restore}"
RECREATE_TARGETS=false
REPLACE_ORIGINALS=false

usage() {
  cat <<'EOF'
Usage:
  restore-all-databases.sh BACKUP_DIRECTORY [options]

Options:
  --target-suffix SUFFIX  Restore database.dump into databaseSUFFIX.
                          Default: _restore
  --recreate-targets      Drop existing target databases before restoring.
  --replace-originals     Restore over the original database names. This also
                          requires --recreate-targets and typed confirmation.
  --help                  Show this help.

Authentication:
  Supply PGPASSWORD, PGPASSFILE, or ~/.pgpass. Interactive runs prompt when
  none is configured.
EOF
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

(( $# > 0 )) || { usage >&2; exit 2; }
if [[ "$1" == "--help" ]]; then
  usage
  exit 0
fi
backup_directory="$1"
shift

while (( $# > 0 )); do
  case "$1" in
    --target-suffix)
      (( $# >= 2 )) || fail "--target-suffix requires a value"
      TARGET_SUFFIX="$2"
      shift 2
      ;;
    --recreate-targets)
      RECREATE_TARGETS=true
      shift
      ;;
    --replace-originals)
      REPLACE_ORIGINALS=true
      shift
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

[[ -d "${backup_directory}" ]] || fail "Backup directory not found: ${backup_directory}"
backup_directory="$(cd "${backup_directory}" && pwd -P)"
[[ -f "${backup_directory}/SHA256SUMS" ]] || fail "SHA256SUMS is missing from the backup directory"

for command_name in pg_restore createdb dropdb psql sha256sum; do
  command -v "${command_name}" >/dev/null 2>&1 || fail "Required command not found: ${command_name}"
done

[[ "${PGPORT}" =~ ^[0-9]+$ ]] || fail "PGPORT must be numeric"
[[ "${TARGET_SUFFIX}" =~ ^[A-Za-z0-9_]*$ ]] || fail "TARGET_SUFFIX contains unsafe characters"

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

mapfile -t dump_files < <(find "${backup_directory}" -maxdepth 1 -type f -name '*.dump' -printf '%f\n' | sort)
(( ${#dump_files[@]} > 0 )) || fail "No .dump files found in ${backup_directory}"

printf 'Verifying backup checksums...\n'
(
  cd "${backup_directory}"
  sha256sum --check SHA256SUMS
)

if [[ "${REPLACE_ORIGINALS}" == true ]]; then
  [[ "${RECREATE_TARGETS}" == true ]] \
    || fail "--replace-originals also requires --recreate-targets"
  TARGET_SUFFIX=""
  if [[ ! -t 0 ]]; then
    fail "Replacing original databases requires an interactive terminal"
  fi
  printf 'WARNING: this will permanently replace the original databases.\n' >&2
  read -r -p 'Type REPLACE ORIGINAL DATABASES to continue: ' confirmation
  [[ "${confirmation}" == "REPLACE ORIGINAL DATABASES" ]] || fail "Restore cancelled"
fi

database_exists() {
  local database="$1"
  local result
  result="$(psql \
    --host="${PGHOST}" --port="${PGPORT}" --username="${PGUSER}" \
    --dbname=postgres --no-password --tuples-only --no-align \
    --set=ON_ERROR_STOP=1 \
    --command="SELECT 1 FROM pg_database WHERE datname = '${database}'")"
  [[ "${result}" == "1" ]]
}

for dump_file in "${dump_files[@]}"; do
  source_database="${dump_file%.dump}"
  [[ "${source_database}" =~ ^[A-Za-z0-9_]+$ ]] || fail "Unsafe dump filename: ${dump_file}"
  target_database="${source_database}${TARGET_SUFFIX}"
  [[ -n "${target_database}" ]] || fail "Resolved target database name is empty"

  if database_exists "${target_database}"; then
    if [[ "${RECREATE_TARGETS}" != true ]]; then
      fail "Target database ${target_database} already exists; use --recreate-targets to replace it"
    fi
    printf 'Dropping existing target %s...\n' "${target_database}"
    dropdb \
      --host="${PGHOST}" --port="${PGPORT}" --username="${PGUSER}" \
      --no-password --force "${target_database}"
  fi

  printf 'Creating target %s...\n' "${target_database}"
  createdb \
    --host="${PGHOST}" --port="${PGPORT}" --username="${PGUSER}" \
    --no-password "${target_database}"

  printf 'Restoring %s into %s...\n' "${source_database}" "${target_database}"
  if ! pg_restore \
      --host="${PGHOST}" --port="${PGPORT}" --username="${PGUSER}" \
      --dbname="${target_database}" --no-password \
      --exit-on-error --no-owner --no-privileges --verbose \
      "${backup_directory}/${dump_file}"; then
    fail "Restore failed for ${source_database}; target ${target_database} was left for inspection"
  fi
  printf 'Completed %s -> %s\n' "${source_database}" "${target_database}"
done

printf 'All database restores completed successfully.\n'
