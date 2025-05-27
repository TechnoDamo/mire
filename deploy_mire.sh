#!/bin/bash

set -e # Exit immediately if a command exits with a non-zero status.

# --- Configuration --- Hardcoded values for non-interactive deployment ---
INSTALL_DIR="/opt/miregame"
SERVICE_USER="mireuser"      # User the service will run as
SERVICE_GROUP="miregroup"    # Group for the service user
MIRE_PORT="3333"             # Port for Mire server (Note: Port 23 requires root privileges or equivalent)
PROJECT_NAME="mire"        # Used for JAR name and service name
# --- End Configuration ---

# Check if running as root
if [ "$(id -u)" -ne 0 ]; then
  echo "This script needs to be run with sudo or as root." >&2
  exit 1
fi

# Get project version from project.clj to name the JAR correctly
PROJECT_VERSION=$(grep -oP '(?<=defproject mire ").*?(?=")' project.clj)
if [ -z "$PROJECT_VERSION" ]; then
    echo "Could not determine project version from project.clj. Using generic JAR name."
    JAR_NAME="${PROJECT_NAME}-standalone.jar"
else
    JAR_NAME="${PROJECT_NAME}-${PROJECT_VERSION}-standalone.jar"
fi

SERVICE_FILE_NAME="${PROJECT_NAME}.service"
SERVICE_FILE_PATH="/etc/systemd/system/${SERVICE_FILE_NAME}"

echo "Mire Non-Interactive Deployment"
echo "---------------------------------"
echo "  Installation Dir:   ${INSTALL_DIR}"
echo "  Service User:       ${SERVICE_USER}"
echo "  Service Group:      ${SERVICE_GROUP}"
echo "  Mire Port:          ${MIRE_PORT} (Ensure privileges for this port if <1024)"
echo "  Uberjar Name:       ${JAR_NAME}"
echo "  Service File:       ${SERVICE_FILE_PATH}"
echo "---------------------------------"

# 1. Build the Uberjar
echo "
Building Uberjar..."
if [ -f "./lein" ]; then
    ./lein uberjar
elif command -v lein &> /dev/null; then
    lein uberjar
else
    echo "Leiningen command (lein or ./lein) not found. Please ensure Leiningen is installed and in your PATH or the script is in the project root." >&2
    exit 1
fi

UBERJAR_PATH="target/${JAR_NAME}"
if [ ! -f "$UBERJAR_PATH" ]; then
    echo "Uberjar not found at ${UBERJAR_PATH} after build. Please check the build process." >&2
    exit 1
fi
echo "Uberjar built successfully: ${UBERJAR_PATH}"

# 2. Create User and Group (if they don't exist)
echo "
Setting up user '${SERVICE_USER}' and group '${SERVICE_GROUP}'..."
if ! getent group "$SERVICE_GROUP" > /dev/null; then
  echo "Creating group ${SERVICE_GROUP}..."
  groupadd -r "$SERVICE_GROUP"
else
  echo "Group ${SERVICE_GROUP} already exists."
fi

if ! getent passwd "$SERVICE_USER" > /dev/null; then
  echo "Creating user ${SERVICE_USER}..."
  useradd -r -g "$SERVICE_GROUP" -d "$INSTALL_DIR" -s /sbin/nologin "$SERVICE_USER"
else
  echo "User ${SERVICE_USER} already exists."
fi

# 3. Set up Deployment Directory
echo "
Setting up deployment directory ${INSTALL_DIR}..."
# Remove existing directory to ensure a clean state, as it's non-interactive
if [ -d "$INSTALL_DIR" ]; then
    echo "Removing existing installation directory ${INSTALL_DIR}..."
    rm -rf "$INSTALL_DIR"
fi
mkdir -p "$INSTALL_DIR"

echo "Copying Uberjar to ${INSTALL_DIR}/${JAR_NAME}..."
cp "$UBERJAR_PATH" "${INSTALL_DIR}/${JAR_NAME}"
echo "Copying resources directory to ${INSTALL_DIR}/resources..."
cp -r resources "${INSTALL_DIR}/"

chown -R "${SERVICE_USER}:${SERVICE_GROUP}" "$INSTALL_DIR"
chmod -R 750 "$INSTALL_DIR" # Read/execute for user/group
chmod 640 "${INSTALL_DIR}/${JAR_NAME}" # Read for user/group, write for user (owner)

# 4. Create systemd Service File
echo "
Creating systemd service file at ${SERVICE_FILE_PATH}..."

# Using a heredoc for the service file content for clarity
cat > "$SERVICE_FILE_PATH" << EOF
[Unit]
Description=Mire MUD Game Server (${PROJECT_NAME})
After=network.target

[Service]
User=${SERVICE_USER}
Group=${SERVICE_GROUP}
WorkingDirectory=${INSTALL_DIR}
ExecStart=/usr/bin/java -jar ${INSTALL_DIR}/${JAR_NAME} ${MIRE_PORT} ${INSTALL_DIR}/resources

# If MIRE_PORT is < 1024 (e.g., 23), the User=${SERVICE_USER} will need permissions.
# This can be achieved by:
# 1. Running the service as root (Not recommended: User=root, Group=root).
# 2. Using authbind: ExecStart=/usr/bin/authbind --deep /usr/bin/java ... (requires authbind setup for ${SERVICE_USER} and port ${MIRE_PORT})
# 3. Using capabilities on the java executable or specific to the service.
# 4. Port redirection (e.g., iptables) to forward port 23 to a higher port that Mire listens on (Mire itself runs on >1023).
# This script assumes the chosen user can bind to the specified port or measures have been taken.

SuccessExitStatus=143
Restart=on-failure
RestartSec=5

# Optional: Add resource limits for security and stability
# CPUQuota=50%
# MemoryMax=512M

[Install]
WantedBy=multi-user.target
EOF

echo "Systemd service file created."

# 5. Manage the Service
echo "
Reloading systemd daemon, enabling and starting ${SERVICE_FILE_NAME}..."
systemctl daemon-reload
systemctl enable "${SERVICE_FILE_NAME}"
systemctl restart "${SERVICE_FILE_NAME}" # Use restart to ensure it picks up changes if service was already running

echo "
Deployment complete!"
echo "To check the status, run: sudo systemctl status ${SERVICE_FILE_NAME}"
echo "To view logs, run: sudo journalctl -u ${SERVICE_FILE_NAME} -f"

exit 0 