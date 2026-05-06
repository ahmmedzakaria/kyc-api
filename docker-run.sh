#!/bin/bash
set -e

APP_NAME="nexacore-backend"
IMAGE_NAME="nexacore-backend"
CONTAINER_NAME="nexacore-backend"
UPLOAD_DIR="$(pwd)/uploads"
PORT="9100"

echo "🚀 Building ${APP_NAME} Docker image..."
docker build -t ${IMAGE_NAME} .

# Ensure uploads directory exists
mkdir -p "$UPLOAD_DIR"

# Stop and remove old container if exists
if [ "$(docker ps -aq -f name=${CONTAINER_NAME})" ]; then
  echo "🧹 Removing old container..."
  docker stop ${CONTAINER_NAME} >/dev/null 2>&1 || true
  docker rm ${CONTAINER_NAME} >/dev/null 2>&1 || true
fi

echo "🏗️ Starting ${APP_NAME} container..."

docker run -d \
  --name ${CONTAINER_NAME} \
  -p ${PORT}:${PORT} \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/kyc_db" \
  -e SPRING_DATASOURCE_USERNAME="postgres" \
  -e SPRING_DATASOURCE_PASSWORD="123456" \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
  -e SPRING_PROFILES_ACTIVE="docker" \
  -e STORAGE_FILESYSTEM_BASE_DIR="/opt/nexacore/uploads" \
  --add-host=host.docker.internal:host-gateway \
  -v "$UPLOAD_DIR":/opt/nexacore/uploads \
  ${IMAGE_NAME}

echo "✅ ${APP_NAME} is up and running on http://localhost:${PORT}"
echo "📂 Uploads directory: $UPLOAD_DIR"
echo "📜 Logs: docker logs -f ${CONTAINER_NAME}"
