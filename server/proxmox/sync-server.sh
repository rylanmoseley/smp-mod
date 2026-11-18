#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Turnkey Proxmox Server Provisioner & Auto-Sync Suite
# ==============================================================================
# Performs end-to-end server provisioning and updates:
# 1. Clones/pulls the authoritative Git repository.
# 2. Automatically installs NeoForge 1.21.1 Dedicated Server loader if missing.
# 3. Synchronizes all companion mods (Tectonic, YUNG's Suite, Curios, Rylan's Items,
#    OPAC, Voice Chat, Squaremap, Spark, Chunky, LuckPerms, etc.) via Packwiz bootstrap.
# 4. Downloads the latest smpmod-*.jar core mod binary from GitHub Releases.
# 5. Synchronizes server configs, properties, and datapacks.
# 6. Restarts the service with in-game countdown alerts if changes were made.
# ==============================================================================

set -euo pipefail

# Configuration paths (Customizable via environment variables)
SERVER_DIR="${SERVER_DIR:-/opt/minecraft/server}"
REPO_DIR="${REPO_DIR:-/opt/minecraft/smp-mod-repo}"
LOG_FILE="${LOG_FILE:-/var/log/minecraft/sync.log}"
REPO_BRANCH="${REPO_BRANCH:-main}"
GITHUB_REPO="rylanmoseley/smp-mod"
NEOFORGE_VERSION="21.1.234"
RCON_PASS="${RCON_PASS:-}"
RCON_PORT="${RCON_PORT:-25575}"

mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$SERVER_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

log "========================================================"
log " Starting SMP Server Provisioning & Update Sequence      "
log " Target: $SERVER_DIR                                    "
log "========================================================"

UPDATES_APPLIED=0

# ------------------------------------------------------------------------------
# 1. Update or Clone Local Git Repository
# ------------------------------------------------------------------------------
if [ ! -d "$REPO_DIR/.git" ]; then
    log "[Git] Cloning repository from GitHub ($GITHUB_REPO, branch $REPO_BRANCH)..."
    git clone --branch "$REPO_BRANCH" "https://github.com/${GITHUB_REPO}.git" "$REPO_DIR"
    UPDATES_APPLIED=1
else
    log "[Git] Fetching latest changes from origin/$REPO_BRANCH..."
    cd "$REPO_DIR"
    git fetch origin "$REPO_BRANCH"
    LOCAL_HASH=$(git rev-parse HEAD)
    REMOTE_HASH=$(git rev-parse "origin/$REPO_BRANCH")

    if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
        log "[Git] New commits found ($LOCAL_HASH -> $REMOTE_HASH). Pulling updates..."
        git pull --ff-only origin "$REPO_BRANCH"
        UPDATES_APPLIED=1
    else
        log "[Git] Repository is already up-to-date at commit $LOCAL_HASH."
    fi
fi

# ------------------------------------------------------------------------------
# 2. Provision NeoForge Dedicated Server Loader (If Missing)
# ------------------------------------------------------------------------------
UNIX_ARGS="$SERVER_DIR/libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/unix_args.txt"

if [ ! -f "$UNIX_ARGS" ]; then
    log "[NeoForge] NeoForge $NEOFORGE_VERSION loader not found. Installing..."
    INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar"
    INSTALLER_JAR="/tmp/neoforge-${NEOFORGE_VERSION}-installer.jar"

    log "[NeoForge] Downloading installer from $INSTALLER_URL..."
    curl -sL -o "$INSTALLER_JAR" "$INSTALLER_URL"

    log "[NeoForge] Running headless server installation into $SERVER_DIR..."
    cd "$SERVER_DIR"
    java -jar "$INSTALLER_JAR" --installServer

    rm -f "$INSTALLER_JAR"
    log "[NeoForge] Server loader successfully installed!"
    UPDATES_APPLIED=1
fi

# ------------------------------------------------------------------------------
# 3. Provision & Sync Companion Mods via Packwiz Bootstrap
# ------------------------------------------------------------------------------
BOOTSTRAP_JAR="$SERVER_DIR/packwiz-installer-bootstrap.jar"
BOOTSTRAP_URL="https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar"

if [ ! -f "$BOOTSTRAP_JAR" ]; then
    log "[Packwiz] Downloading packwiz-installer-bootstrap.jar..."
    curl -sL -o "$BOOTSTRAP_JAR" "$BOOTSTRAP_URL"
fi

if [ -f "$REPO_DIR/pack/pack.toml" ]; then
    log "[Packwiz] Synchronizing companion mods (side: server)..."
    cd "$SERVER_DIR"
    # Run packwiz-installer-bootstrap in headless server mode (-g disables GUI dialogs)
    BOOTSTRAP_ARGS=("-g" "--side" "server")
    if [ -f "$SERVER_DIR/packwiz-installer.jar" ]; then
        BOOTSTRAP_ARGS+=("--bootstrap-no-update")
    fi
    java -jar "$BOOTSTRAP_JAR" "${BOOTSTRAP_ARGS[@]}" "file://${REPO_DIR}/pack/pack.toml" || {
        log "[Packwiz] WARNING: Local pack.toml sync failed, trying raw GitHub URL..."
        java -jar "$BOOTSTRAP_JAR" "${BOOTSTRAP_ARGS[@]}" "https://raw.githubusercontent.com/${GITHUB_REPO}/${REPO_BRANCH}/pack/pack.toml" || true
    }
fi

# ------------------------------------------------------------------------------
# 4. Download / Sync Core SMP Mod Jar (smpmod)
# ------------------------------------------------------------------------------
MODS_DIR="$SERVER_DIR/mods"
mkdir -p "$MODS_DIR"

if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
    log "[Mod] Checking GitHub Releases for latest smpmod jar..."
    LATEST_RELEASE=$(curl -s "https://api.github.com/repos/${GITHUB_REPO}/releases/latest" || true)

    if echo "$LATEST_RELEASE" | grep -q "browser_download_url"; then
        JAR_URL=$(echo "$LATEST_RELEASE" | jq -r '.assets[] | select(.name | endswith(".jar") and (contains("smpmod-") and (contains("sources") | not))) | .browser_download_url' | head -n 1)
        JAR_NAME=$(echo "$LATEST_RELEASE" | jq -r '.assets[] | select(.name | endswith(".jar") and (contains("smpmod-") and (contains("sources") | not))) | .name' | head -n 1)

        if [ -n "$JAR_NAME" ] && [ -n "$JAR_URL" ] && [ ! -f "$MODS_DIR/$JAR_NAME" ]; then
            log "[Mod] Downloading updated core mod: $JAR_NAME..."
            find "$MODS_DIR" -name "smpmod-*.jar" -type f -delete
            curl -sL -o "$MODS_DIR/$JAR_NAME" "$JAR_URL"
            log "[Mod] Successfully updated $JAR_NAME."
            UPDATES_APPLIED=1
        fi
    fi
fi

# ------------------------------------------------------------------------------
# 5. Synchronize Server Configurations, Properties, and Permissions
# ------------------------------------------------------------------------------
sync_file() {
    local src="$1"
    local dest="$2"

    if [ ! -f "$src" ]; then
        return 0
    fi

    mkdir -p "$(dirname "$dest")"

    if [ ! -f "$dest" ]; then
        log "[Config] Installing: $dest"
        cp "$src" "$dest"
        UPDATES_APPLIED=1
    elif ! cmp -s "$src" "$dest"; then
        log "[Config] Updating: $dest"
        cp "$src" "$dest"
        UPDATES_APPLIED=1
    fi
}

log "[Config] Syncing server configuration profiles..."
sync_file "$REPO_DIR/server/config/smpmod-common.toml" "$SERVER_DIR/config/smpmod-common.toml"
sync_file "$REPO_DIR/server/server.properties" "$SERVER_DIR/server.properties"
sync_file "$REPO_DIR/server/permissions/luckperms-smp.txt" "$SERVER_DIR/permissions/luckperms-smp.txt"
sync_file "$REPO_DIR/server/run.sh" "$SERVER_DIR/run.sh"

# Ensure run.sh is executable
if [ -f "$SERVER_DIR/run.sh" ]; then
    chmod +x "$SERVER_DIR/run.sh"
fi

# Ensure EULA is accepted
echo "eula=true" > "$SERVER_DIR/eula.txt"

# ------------------------------------------------------------------------------
# 6. Synchronize World Datapacks
# ------------------------------------------------------------------------------
DATAPACKS_SRC="$REPO_DIR/server/datapacks"
DATAPACKS_DEST="$SERVER_DIR/world/datapacks"

if [ -d "$DATAPACKS_SRC" ]; then
    log "[Datapacks] Synchronizing world datapacks..."
    mkdir -p "$DATAPACKS_DEST"
    rsync -av --update --exclude="README.md" "$DATAPACKS_SRC/" "$DATAPACKS_DEST/" || true
fi

# ------------------------------------------------------------------------------
# 7. Safe Server Restart Handling (If Running)
# ------------------------------------------------------------------------------
if [ "$UPDATES_APPLIED" -eq 1 ]; then
    log "[Service] Changes detected. Checking if Minecraft service is running..."

    # Create rolling pre-update world snapshot if world directory exists
    if [ -d "$SERVER_DIR/world" ]; then
        BACKUP_DIR="$SERVER_DIR/backups/pre-update"
        mkdir -p "$BACKUP_DIR"
        BACKUP_FILE="$BACKUP_DIR/world-$(date +%Y%m%d-%H%M%S).tar.gz"
        log "[Backup] Creating pre-update world snapshot at $BACKUP_FILE..."
        tar --exclude="world/session.lock" -czf "$BACKUP_FILE" -C "$SERVER_DIR" world || log "[Backup] Warning: Snapshot completed with non-fatal warnings."
        # Retain last 5 rolling snapshots to prevent disk exhaustion
        ls -dt "$BACKUP_DIR/"*.tar.gz 2>/dev/null | tail -n +6 | xargs -r rm -f || true
        log "[Backup] Pre-update snapshot saved successfully."
    fi

    if command -v systemctl >/dev/null 2>&1 && systemctl is-active --quiet minecraft; then
        log "[Service] Active server detected. Initiating countdown..."

        # RCON in-game warning broadcast
        if [ -n "$RCON_PASS" ] && command -v mcrcon >/dev/null 2>&1; then
            mcrcon -H 127.0.0.1 -P "$RCON_PORT" -p "$RCON_PASS" \
                "say §6[Server] §eSystem updates detected from repository!" \
                "say §6[Server] §cServer will restart in 60 seconds to apply updates..." || true

            sleep 50

            mcrcon -H 127.0.0.1 -P "$RCON_PORT" -p "$RCON_PASS" \
                "say §6[Server] §cRestarting in 10 seconds..." \
                "save-all" || true

            sleep 10
        fi

        log "[Service] Restarting minecraft.service..."
        systemctl restart minecraft
        log "[Service] Restart completed."
    else
        log "[Service] Minecraft service is not active. Ready for next startup."
    fi
else
    log "[Status] Server is already 100% synchronized and up-to-date."
fi

log "=== Provisioning and Update Sequence Finished ==="
