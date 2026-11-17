#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Modrinth v2 API Asset Synchronizer
# Syncs generated icon and gallery assets to Modrinth listing.
# Default mode is --dry-run. Pass --apply to perform actual HTTP mutations.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GEN_DIR="$REPO_ROOT/docs/assets/generated"

MODRINTH_API="https://api.modrinth.com/v2"
USER_AGENT="rylanmoseley/smp-mod-graphics-pipeline/1.0.0"

MODE="dry-run"
TARGET_TYPE="mod" # mod | modpack

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --dry-run          Preview API requests without sending (default)"
    echo "  --apply            Perform live API requests to Modrinth"
    echo "  --target [mod|pack] Target project listing (default: mod)"
    echo "  --help             Show this help message"
    echo ""
    echo "Required Environment Variables for --apply:"
    echo "  MODRINTH_TOKEN       Modrinth API authorization token"
    echo "  MODRINTH_PROJECT_ID  Project slug or ID (default: 'smpmod-rdiamondcreeper')"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --apply)
            MODE="apply"
            shift
            ;;
        --dry-run)
            MODE="dry-run"
            shift
            ;;
        --target)
            TARGET_TYPE="$2"
            shift 2
            ;;
        --help|-h)
            usage
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            ;;
    esac
done

PROJECT_ID="${MODRINTH_PROJECT_ID:-smpmod-rdiamondcreeper}"

echo "=============================================================================="
echo " Modrinth Asset Sync (Mode: $MODE | Target: $TARGET_TYPE | Project: $PROJECT_ID)"
echo "=============================================================================="

if [ "$MODE" = "apply" ]; then
    if [ -z "${MODRINTH_TOKEN:-}" ]; then
        echo "ERROR: MODRINTH_TOKEN environment variable is required for --apply mode." >&2
        exit 1
    fi
fi

# Select icon asset
if [ "$TARGET_TYPE" = "pack" ]; then
    ICON_FILE="$GEN_DIR/icon-modpack.png"
else
    ICON_FILE="$GEN_DIR/icon-mod.png"
fi

# 1. Sync Project Icon
echo "--- 1. Syncing Project Icon ($ICON_FILE) ---"
if [ "$MODE" = "dry-run" ]; then
    echo "[DRY-RUN] PATCH $MODRINTH_API/project/$PROJECT_ID/icon?ext=png"
    echo "          Payload: $ICON_FILE ($(wc -c < "$ICON_FILE" | tr -d ' ') bytes)"
else
    echo "Uploading icon: $ICON_FILE..."
    curl -f -X PATCH "$MODRINTH_API/project/$PROJECT_ID/icon?ext=png" \
        -H "Authorization: $MODRINTH_TOKEN" \
        -H "User-Agent: $USER_AGENT" \
        -H "Content-Type: image/png" \
        --data-binary "@$ICON_FILE"
    echo "Icon upload complete."
fi

# 2. Sync Gallery Cards
echo "--- 2. Syncing Gallery Cards ---"
declare -a GALLERY_ITEMS=(
    "01-overview.png:true:SMP Mod Suite - Complete Architecture Overview:Featured architectural showcase across all 6 subsystems"
    "02-settlements-economy.png:false:Contiguous Settlements & Digital Commerce:Contiguous chunk BFS cluster claims and non-material digital economy"
    "03-emergent-roles.png:false:Emergent Combat Roles & Playstyle XP:Classless DPS, Tank, Support, and Scout role profiles"
    "04-death-recovery-lore.png:false:UUID Death Recovery & Item Provenance:UUID buyback ledgers, fee decay, and persistent historical lore"
)

for item in "${GALLERY_ITEMS[@]}"; do
    IFS=":" read -r filename featured title description <<< "$item"
    file_path="$GEN_DIR/gallery/$filename"

    if [ ! -f "$file_path" ]; then
        echo "WARNING: Gallery file $file_path not found, skipping." >&2
        continue
    fi

    size=$(wc -c < "$file_path" | tr -d ' ')

    if [ "$MODE" = "dry-run" ]; then
        echo "[DRY-RUN] POST $MODRINTH_API/project/$PROJECT_ID/gallery?ext=png&featured=$featured&title=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$title'))")"
        echo "          File: $filename ($size bytes) | Featured: $featured"
    else
        echo "Uploading gallery image: $filename..."
        encoded_title=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$title'''))")
        encoded_desc=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$description'''))")
        curl -f -X POST "$MODRINTH_API/project/$PROJECT_ID/gallery?ext=png&featured=$featured&title=$encoded_title&description=$encoded_desc" \
            -H "Authorization: $MODRINTH_TOKEN" \
            -H "User-Agent: $USER_AGENT" \
            -H "Content-Type: image/png" \
            --data-binary "@$file_path"
        echo "Gallery upload complete: $filename"
    fi
done

echo "=============================================================================="
echo " Sync operation completed ($MODE mode)."
echo "=============================================================================="
