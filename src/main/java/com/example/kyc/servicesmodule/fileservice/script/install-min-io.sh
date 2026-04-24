#!/bin/bash

set -e

echo "🔹 Installing MinIO..."

# Variables (change if needed)
MINIO_USER="minio-user"
MINIO_DIR="/opt/minio"
DATA_DIR="/opt/minio/data"
BIN_PATH="/usr/local/bin/minio"

# 1. Create system user
if ! id "$MINIO_USER" &>/dev/null; then
  echo "🔹 Creating user: $MINIO_USER"
  sudo useradd -r $MINIO_USER -s /sbin/nologin
fi

# 2. Create directories
echo "🔹 Creating directories..."
sudo mkdir -p $MINIO_DIR
sudo mkdir -p $DATA_DIR

# 3. Download MinIO binary
echo "🔹 Downloading MinIO..."
wget -q https://dl.min.io/server/minio/release/linux-amd64/minio -O minio

# 4. Install binary
echo "🔹 Installing binary..."
chmod +x minio
sudo mv minio $BIN_PATH

# 5. Set permissions
echo "🔹 Setting permissions..."
sudo chown -R $MINIO_USER:$MINIO_USER $MINIO_DIR
sudo chown -R $MINIO_USER:$MINIO_USER $DATA_DIR

# 6. Create environment file
echo "🔹 Creating config..."
sudo tee /etc/default/minio > /dev/null <<EOF
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
MINIO_VOLUMES="$DATA_DIR"
MINIO_OPTS="--console-address :9001"
EOF

# 7. Create systemd service
echo "🔹 Creating systemd service..."
sudo tee /etc/systemd/system/minio.service > /dev/null <<EOF
[Unit]
Description=MinIO
After=network.target

[Service]
User=$MINIO_USER
Group=$MINIO_USER
EnvironmentFile=/etc/default/minio
ExecStart=$BIN_PATH server \$MINIO_VOLUMES \$MINIO_OPTS
Restart=always
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF

# 8. Reload systemd & start service
echo "🔹 Starting MinIO service..."
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl enable minio
sudo systemctl start minio

# 9. Status check
echo "🔹 Checking service status..."
sudo systemctl status minio --no-pager

echo ""
echo "✅ MinIO installed successfully!"
echo "🌐 Console: http://localhost:9001"
echo "🔑 Username: minioadmin"
echo "🔑 Password: minioadmin"