#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Deterministic Graphics Build Script
# Compiles all documentation, README, and Modrinth branding assets via Typst.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

OUT_DIR="$REPO_ROOT/docs/assets/generated"
mkdir -p "$OUT_DIR/gallery" "$OUT_DIR/headers" "$OUT_DIR/diagrams"

# Verify Typst binary
if ! command -v typst >/dev/null 2>&1; then
    echo "ERROR: 'typst' CLI not found in PATH." >&2
    echo "Please install Typst >= 0.15.0 or ensure it is accessible in PATH." >&2
    exit 1
fi

TYPST_VER=$(typst --version)
echo "Using $TYPST_VER"
echo "Root directory: $REPO_ROOT"
echo "Output directory: $OUT_DIR"
echo "Fonts path: $SCRIPT_DIR/fonts"
echo "------------------------------------------------------------------------------"

COMMON_ARGS=(
    --root "$REPO_ROOT"
    --ignore-system-fonts
    --font-path "$SCRIPT_DIR/fonts"
    --format png
    --ppi 72
)

compile_asset() {
    local template="$1"
    local output="$2"
    shift 2
    local extra_args=("$@")

    echo -n "Compiling $(basename "$output")... "
    typst compile "${COMMON_ARGS[@]}" "${extra_args[@]}" "$template" "$output"
    local size
    size=$(wc -c < "$output" | tr -d ' ')
    echo "DONE (${size} bytes)"
}

# 1. Project Icons (512x512)
compile_asset "$SCRIPT_DIR/templates/project-icon.typ" "$OUT_DIR/icon-mod.png" --input variant=mod
compile_asset "$SCRIPT_DIR/templates/project-icon.typ" "$OUT_DIR/icon-modpack.png" --input variant=modpack

# 2. Hero Banner (1280x360)
compile_asset "$SCRIPT_DIR/templates/hero-banner.typ" "$OUT_DIR/hero-readme.png"

# 3. Modrinth Gallery Cards (1280x720, 16:9)
compile_asset "$SCRIPT_DIR/templates/gallery-card.typ" "$OUT_DIR/gallery/01-overview.png" --input card=overview
compile_asset "$SCRIPT_DIR/templates/gallery-card.typ" "$OUT_DIR/gallery/02-settlements-economy.png" --input card=settlements
compile_asset "$SCRIPT_DIR/templates/gallery-card.typ" "$OUT_DIR/gallery/03-emergent-roles.png" --input card=roles
compile_asset "$SCRIPT_DIR/templates/gallery-card.typ" "$OUT_DIR/gallery/04-death-recovery-lore.png" --input card=death-lore

# 4. Subsystem Section Headers (1200x160)
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/economy.png" --input subsystem=economy
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/death-recovery.png" --input subsystem=death-recovery
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/land-claims.png" --input subsystem=land-claims
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/world-navigation.png" --input subsystem=world-navigation
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/emergent-roles.png" --input subsystem=emergent-roles
compile_asset "$SCRIPT_DIR/templates/section-header.typ" "$OUT_DIR/headers/server-utilities.png" --input subsystem=server-utilities

# 5. Emergent Roles Diagram (1200x420)
compile_asset "$SCRIPT_DIR/templates/roles-diagram.typ" "$OUT_DIR/diagrams/emergent-roles-quadrant.png"

echo "------------------------------------------------------------------------------"
echo "All 14 graphics targets compiled successfully!"
