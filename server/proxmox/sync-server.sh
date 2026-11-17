#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Proxmox Server Auto-Sync Script
# ==============================================================================
# Synchronizes server configurations, permissions, and mod binaries with the
# authoritative GitHub repository (https://github.com/rylanmoseley/smp-mod).
#
# Can be executed manually, triggered by a systemd timer, or hooked into
# cron / webhooks.
# ==============================================================================

set -euo pipefail

# Configuration paths (Adjust according to your Proxmox VM directory layout)
SERVER_DIR="${SERVER_DIR:-/opt/minecraft/server}"
REPO_DIR="${REPO_DIR:-/opt/minecraft/smp-mod-repo}"
LOG_FILE="${LOG_FILE:-/var/log/minecraft/sync.log}"
REPO_BRANCH="${REPO_BRANCH:-main}"
GITHUB_REPO="rylanmoseley/smp-mod"
RCON_PASS="${RCON_PASS:-}"
RCON_PORT="${RCON_PORT:-25575}"

mkdir -p "$(dirname "$LOG_FILE")"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

log "=== Starting SMP Server Configuration Sync ==="

# ------------------------------------------------------------------------------
# 1. Update Local Git Repository Mirror
# ------------------------------------------------------------------------------
if [ ! -d "$REPO_DIR/.git" ]; then
    log "Cloning repository from GitHub ($GITHUB_REPO)..."
    git clone --branch "$REPO_BRANCH" "https://github.com/${GITHUB_REPO}.git" "$REPO_DIR"
else
    log "Fetching latest changes from origin/$REPO_BRANCH..."
    cd "$REPO_DIR"
    git fetch origin "$REPO_BRANCH"
    LOCAL_HASH=$(git rev-parse HEAD)
    REMOTE_HASH=$(git rev-parse "origin/$REPO_BRANCH")

    if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
        log "New commits detected ($LOCAL_HASH -> $REMOTE_HASH). Pulling updates..."
        git pull --ff-only origin "$REPO_BRANCH"
    else
        log "Repository is already up-to-date at commit $LOCAL_HASH."
    fi
fi

# ------------------------------------------------------------------------------
# 2. Check and Sync Configuration Files
# ------------------------------------------------------------------------------
CONFIGS_CHANGED=0

sync_file() {
    local src="$1"
    local dest="$2"

    if [ ! -f "$src" ]; then
        return 0
    fi

    mkdir -p "$(dirname "$dest")"

    if [ ! -f "$dest" ]; then
        log "New file detected: $dest"
        cp "$src" "$dest"
        CONFIGS_CHANGED=1
    elif ! cmp -s "$src" "$dest"; then
        log "Change detected in: $dest"
        cp "$src" "$dest"
        CONFIGS_CHANGED=1
    fi
}

log "Syncing configuration files..."
sync_file "$REPO_DIR/server/config/smpmod-common.toml" "$SERVER_DIR/config/smpmod-common.toml"
sync_file "$REPO_DIR/server/server.properties" "$SERVER_DIR/server.properties"
sync_file "$REPO_DIR/server/permissions/luckperms-smp.txt" "$SERVER_DIR/permissions/luckperms-smp.txt"

# ------------------------------------------------------------------------------
# 3. Check for Latest Mod Release Jar (Optional)
# ------------------------------------------------------------------------------
MODS_DIR="$SERVER_DIR/mods"
mkdir -p "$MODS_DIR"

if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
    log "Checking GitHub Releases for latest smpmod jar..."
    LATEST_RELEASE=$(curl -s "https://api.github.com/repos/${GITHUB_REPO}/releases/latest" || true)
    
    if echo "$LATEST_RELEASE" | grep -q "browser_download_url"; then
        JAR_URL=$(echo "$LATEST_RELEASE" | jq -r '.assets[] | select(.name | endswith(".jar") and (contains("smpmod-") and (contains("sources") | not))) | .browser_download_url' | head -n 1)
        JAR_NAME=$(echo "$LATEST_RELEASE" | jq -r '.assets[] | select(.name | endswith(".jar") and (contains("smpmod-") and (contains("sources") | not))) | .name' | head -n 1)

        if [ -n "$JAR_NAME" ] && [ -n "$JAR_URL" ] && [ ! -f "$MODS_DIR/$JAR_NAME" ]; then
            log "Found new release jar: $JAR_NAME. Downloading..."
            # Remove older version jars
            find "$MODS_DIR" -name "smpmod-*.jar" -type f -delete
            curl -sL -o "$MODS_DIR/$JAR_NAME" "$JAR_URL"
            log "Downloaded $JAR_NAME to $MODS_DIR."
            CONFIGS_CHANGED=1
        fi
    fi
fi

# ------------------------------------------------------------------------------
# 4. Handle Server Restart if Updates Were Detected
# ------------------------------------------------------------------------------
if [ "$CONFIGS_CHANGED" -eq 1 ]; then
    log "Modifications were applied. Initiating server restart sequence..."

    # If mcrcon is installed and password configured, send countdown broadcast
    if [ -n "$RCON_PASS" ] && command -v mcrcon >/dev/null 2>&1; then
        log "Broadcasting in-game restart warning via RCON..."
        mcrcon -H 127.0.0.1 -P "$RCON_PORT" -p "$RCON_PASS" \
            "say §6[Server] §eSystem configurations updated from repository!" \
            "say §6[Server] §cServer will restart in 60 seconds to apply updates..." || true
        
        sleep 50
        
        mcrcon -H 127.0.0.1 -P "$RCON_PORT" -p "$RCON_PASS" \
            "say §6[Server] §cRestarting in 10 seconds..." \
            "save-all" || true
        
        sleep 10
    fi

    log "Restarting minecraft systemd service..."
    if command -v systemctl >/dev/null 2>&1; then
        systemctl restart minecraft
    fi
    log "Restart sequence completed."
else
    log "No changes detected. Server is completely up-to-date."
fi

log "=== Configuration Sync Finished ==="
