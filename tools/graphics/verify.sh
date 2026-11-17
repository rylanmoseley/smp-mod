#!/usr/bin/env bash
# ==============================================================================
# SMP Mod Suite - Deterministic Graphics Verification Suite
# Asserts file existence, PNG IHDR pixel dimensions, byte budgets, and determinism.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GEN_DIR="$REPO_ROOT/docs/assets/generated"

echo "=============================================================================="
echo " Starting Graphics Pipeline Verification"
echo " Target Directory: $GEN_DIR"
echo "=============================================================================="

# Array of expected targets: "path:expected_width:expected_height:max_bytes:label"
TARGETS=(
    "icon-mod.png:512:512:262144:Mod Icon (512x512)"
    "icon-modpack.png:512:512:262144:Modpack Icon (512x512)"
    "hero-readme.png:1280:360:1048576:Hero Banner (1280x360)"
    "gallery/01-overview.png:1280:720:5242880:Gallery 01 Overview (1280x720)"
    "gallery/02-settlements-economy.png:1280:720:5242880:Gallery 02 Settlements (1280x720)"
    "gallery/03-emergent-roles.png:1280:720:5242880:Gallery 03 Roles (1280x720)"
    "gallery/04-death-recovery-lore.png:1280:720:5242880:Gallery 04 Death Lore (1280x720)"
    "headers/economy.png:1200:160:524288:Header Economy (1200x160)"
    "headers/death-recovery.png:1200:160:524288:Header Death Recovery (1200x160)"
    "headers/land-claims.png:1200:160:524288:Header Land Claims (1200x160)"
    "headers/world-navigation.png:1200:160:524288:Header World Navigation (1200x160)"
    "headers/emergent-roles.png:1200:160:524288:Header Emergent Roles (1200x160)"
    "headers/server-utilities.png:1200:160:524288:Header Server Utilities (1200x160)"
    "diagrams/emergent-roles-quadrant.png:1200:420:1048576:Diagram Roles Quadrant (1200x420)"
)

# ------------------------------------------------------------------------------
# 1. Existence, Dimensions & Byte Budget Verification
# ------------------------------------------------------------------------------
echo "--- 1. Verifying Dimensions & Byte Budgets ---"
ALL_PASSED=1

for entry in "${TARGETS[@]}"; do
    IFS=":" read -r rel_path exp_w exp_h max_b label <<< "$entry"
    full_path="$GEN_DIR/$rel_path"

    if [ ! -f "$full_path" ]; then
        echo "❌ MISSING: $rel_path" >&2
        ALL_PASSED=0
        continue
    fi

    # Read IHDR dimensions and size via inline python stdlib
    read -r actual_w actual_h actual_size < <(python3 -c "
import struct, os, sys
with open('$full_path', 'rb') as f:
    header = f.read(24)
    if header[:8] != b'\x89PNG\r\n\x1a\n':
        sys.exit(1)
    w, h = struct.unpack('>II', header[16:24])
s = os.path.getsize('$full_path')
print(f'{w} {h} {s}')
")

    # Assert dimensions
    if [ "$actual_w" -ne "$exp_w" ] || [ "$actual_h" -ne "$exp_h" ]; then
        echo "❌ DIMENSION MISMATCH: $rel_path (expected ${exp_w}x${exp_h}, got ${actual_w}x${actual_h})" >&2
        ALL_PASSED=0
        continue
    fi

    # Assert budget
    if [ "$actual_size" -gt "$max_b" ]; then
        echo "❌ BUDGET EXCEEDED: $rel_path (size: ${actual_size} bytes, limit: ${max_b} bytes)" >&2
        ALL_PASSED=0
        continue
    fi

    printf "  ✓ %-35s %dx%-4d  %7d B  (max %7d B)\n" "$rel_path" "$actual_w" "$actual_h" "$actual_size" "$max_b"
done

if [ "$ALL_PASSED" -ne 1 ]; then
    echo "ERROR: Dimension or budget verification failed." >&2
    exit 1
fi

# ------------------------------------------------------------------------------
# 2. Back-to-Back Determinism Verification (Byte-for-Byte SHA-256 Match)
# ------------------------------------------------------------------------------
echo "--- 2. Verifying Determinism (Byte-for-Byte SHA-256 Assertion) ---"
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

TMP_GEN="$TMP_DIR/docs/assets/generated"
mkdir -p "$TMP_GEN/gallery" "$TMP_GEN/headers" "$TMP_GEN/diagrams"

COMMON_ARGS=(
    --root "$REPO_ROOT"
    --ignore-system-fonts
    --font-path "$SCRIPT_DIR/fonts"
    --format png
    --ppi 72
)

# Recompile all 14 assets into the temporary directory
typst compile "${COMMON_ARGS[@]}" --input variant=mod "$SCRIPT_DIR/templates/project-icon.typ" "$TMP_GEN/icon-mod.png"
typst compile "${COMMON_ARGS[@]}" --input variant=modpack "$SCRIPT_DIR/templates/project-icon.typ" "$TMP_GEN/icon-modpack.png"
typst compile "${COMMON_ARGS[@]}" "$SCRIPT_DIR/templates/hero-banner.typ" "$TMP_GEN/hero-readme.png"
typst compile "${COMMON_ARGS[@]}" --input card=overview "$SCRIPT_DIR/templates/gallery-card.typ" "$TMP_GEN/gallery/01-overview.png"
typst compile "${COMMON_ARGS[@]}" --input card=settlements "$SCRIPT_DIR/templates/gallery-card.typ" "$TMP_GEN/gallery/02-settlements-economy.png"
typst compile "${COMMON_ARGS[@]}" --input card=roles "$SCRIPT_DIR/templates/gallery-card.typ" "$TMP_GEN/gallery/03-emergent-roles.png"
typst compile "${COMMON_ARGS[@]}" --input card=death-lore "$SCRIPT_DIR/templates/gallery-card.typ" "$TMP_GEN/gallery/04-death-recovery-lore.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=economy "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/economy.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=death-recovery "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/death-recovery.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=land-claims "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/land-claims.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=world-navigation "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/world-navigation.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=emergent-roles "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/emergent-roles.png"
typst compile "${COMMON_ARGS[@]}" --input subsystem=server-utilities "$SCRIPT_DIR/templates/section-header.typ" "$TMP_GEN/headers/server-utilities.png"
typst compile "${COMMON_ARGS[@]}" "$SCRIPT_DIR/templates/roles-diagram.typ" "$TMP_GEN/diagrams/emergent-roles-quadrant.png"

# Compare SHA-256 sums
DETERMINISTIC=1
for entry in "${TARGETS[@]}"; do
    IFS=":" read -r rel_path _ _ _ _ <<< "$entry"
    hash_orig=$(sha256sum "$GEN_DIR/$rel_path" | awk '{print $1}')
    hash_recomp=$(sha256sum "$TMP_GEN/$rel_path" | awk '{print $1}')

    if [ "$hash_orig" != "$hash_recomp" ]; then
        echo "❌ NON-DETERMINISTIC: $rel_path" >&2
        echo "   Original:   $hash_orig" >&2
        echo "   Recompiled: $hash_recomp" >&2
        DETERMINISTIC=0
    else
        printf "  ✓ SHA-256 MATCH: %-35s %s\n" "$rel_path" "${hash_orig:0:16}..."
    fi
done

if [ "$DETERMINISTIC" -ne 1 ]; then
    echo "ERROR: Determinism verification failed. Hashes did not match." >&2
    exit 1
fi

echo "=============================================================================="
echo " SUCCESS: All 14 graphics targets are valid, within budget, and 100% deterministic!"
echo "=============================================================================="
