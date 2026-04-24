#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

INSTALL_ROOT="${MINIO_INSTALL_ROOT:-${PROJECT_ROOT}/tools/minio}"
BIN_DIR="${INSTALL_ROOT}/bin"
DATA_DIR="${MINIO_DATA_DIR:-${INSTALL_ROOT}/data}"
CONFIG_DIR="${INSTALL_ROOT}/config"
MINIO_VERSION="${MINIO_VERSION:-latest}"
MINIO_DOWNLOAD_URL="${MINIO_DOWNLOAD_URL:-https://dl.min.io/server/minio/release/linux-amd64/minio}"
MC_DOWNLOAD_URL="${MC_DOWNLOAD_URL:-https://dl.min.io/client/mc/release/linux-amd64/mc}"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://127.0.0.1:9000}"
MINIO_CONSOLE_ADDRESS="${MINIO_CONSOLE_ADDRESS:-:9001}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-kyc-files}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command curl
require_command chmod
require_command mkdir

mkdir -p "${BIN_DIR}" "${DATA_DIR}" "${CONFIG_DIR}"

download_if_missing() {
  local url="$1"
  local target="$2"
  if [[ -f "${target}" ]]; then
    echo "Using existing binary: ${target}"
    return
  fi
  echo "Downloading ${url}"
  curl -fsSL "${url}" -o "${target}"
  chmod +x "${target}"
}

download_if_missing "${MINIO_DOWNLOAD_URL}" "${BIN_DIR}/minio"
download_if_missing "${MC_DOWNLOAD_URL}" "${BIN_DIR}/mc"

cat > "${CONFIG_DIR}/minio.env" <<EOF
export MINIO_ROOT_USER=${MINIO_ROOT_USER}
export MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
export MINIO_VOLUMES=${DATA_DIR}
export MINIO_OPTS="--console-address ${MINIO_CONSOLE_ADDRESS}"
export MINIO_ENDPOINT=${MINIO_ENDPOINT}
export MINIO_BUCKET=${MINIO_BUCKET}
EOF

cat > "${BIN_DIR}/start-minio.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${INSTALL_ROOT}/config/minio.env"
exec "${SCRIPT_DIR}/minio" server "${MINIO_VOLUMES}" ${MINIO_OPTS}
EOF
chmod +x "${BIN_DIR}/start-minio.sh"

cat > "${BIN_DIR}/create-bucket.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${INSTALL_ROOT}/config/minio.env"
"${SCRIPT_DIR}/mc" alias set local "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"
"${SCRIPT_DIR}/mc" mb --ignore-existing "local/${MINIO_BUCKET}"
"${SCRIPT_DIR}/mc" anonymous set download "local/${MINIO_BUCKET}"
EOF
chmod +x "${BIN_DIR}/create-bucket.sh"

cat <<EOF
MinIO binaries installed under: ${INSTALL_ROOT}

Next steps:
1. Start MinIO:
   ${BIN_DIR}/start-minio.sh
2. Create and expose the bucket:
   ${BIN_DIR}/create-bucket.sh
3. Use these backend env vars:
   STORAGE_TYPE=minio
   MINIO_ENDPOINT=${MINIO_ENDPOINT}
   MINIO_PUBLIC_BASE_URL=${MINIO_ENDPOINT}
   MINIO_ACCESS_KEY=${MINIO_ROOT_USER}
   MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD}
   MINIO_BUCKET=${MINIO_BUCKET}
EOF
