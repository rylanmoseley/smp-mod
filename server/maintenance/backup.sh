#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Automated World Backup Script
# ==============================================================================
# Performs hot backups of world data, player inventories, and mod configs.
# Can be scheduled via cron (e.g. 0 */4 * * * /path/to/server/maintenance/backup.sh)
# ==============================================================================

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${SERVER_DIR}/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/smp_backup_${TIMESTAMP}.tar.gz"
RETENTION_DAYS=7

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting automated server backup..."

# Archive world, server configs, and player data
tar -czf "$BACKUP_FILE" \
    -C "$SERVER_DIR" \
    world \
    config \
    server.properties \
    2>/dev/null

if [ $? -eq 0 ]; then
    SIZE=$(du -h "$BACKUP_FILE" | awk '{print $1}')
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup successfully created: ${BACKUP_FILE} (${SIZE})"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: Backup failed!" >&2
    exit 1
fi

# Rotate backups older than RETENTION_DAYS
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Pruning backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_DIR" -name "smp_backup_*.tar.gz" -type f -mtime +${RETENTION_DAYS} -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup maintenance completed."
