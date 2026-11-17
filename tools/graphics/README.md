# Expedition Parchment & Cartographer's Field Guide Graphics Pipeline (`tools/graphics`)

A self-contained, **100% deterministic, rule-based graphics generation pipeline** for the **SMP Mod Suite** (`smpmod`, Modrinth ID `4IsC6f5v`) built using **Typst** and **CeTZ**, with **zero JavaScript, zero Node.js, and zero generative AI**.

This pipeline compiles all project branding, README hero banners, documentation section headers, architecture diagrams, and Modrinth gallery cards directly from structured TOML design tokens, iron-gall pigment rules, integer pixel-art sprites, and cartographic vector math.

---

## 1. Aesthetic Direction: The Cartographer's Field Guide

- **Authentic Aged Parchment:** Warm tactile vellum (`#F2EAD7`), recessed ledger paper (`#E6DBC2`), margin wash (`#D5C5A3`), and deep saddle leather (`#2C221B`).
- **Iron-Gall Ink & Mineral Pigments:**
  - Charcoal ink (`#231F1B`) and warm sepia umber (`#5A4E42`).
  - Tarnished Ochre Gold (`#B47B2B`) — Chapter I: Economy & Commerce
  - Imperial Plum Wax (`#7A3E5D`) — Chapter II: Death Recovery & Item Lore
  - Pine Territory Moss (`#3D6B48`) — Chapter III: Land Claims & Settlements
  - Cartographic Cobalt (`#355E7E`) — Chapter IV: World Navigation & Survival
  - Iron Oxide Vermilion (`#A8382B`) — Chapter V: Emergent Roles & Combat
  - Wrought Iron Slate (`#4D545E`) — Chapter VI: Server Utilities & Moderation
- **Integer Pixel-Grid Sprites:** $16\times16$ and $24\times24$ Minecraft-native woodcut sprites rendered via pure CeTZ coordinate math with horizontal pixel run-merging (no anti-aliasing seams).
- **100% Deterministic:** Pure Typst seeded PRNG (`lib/prng.typ`) driving parcel survey maps, topographic elevation curves, and paper fiber grain. Back-to-back `./tools/graphics/build.sh` runs produce **byte-for-byte identical PNGs** with matching SHA-256 hashes.

---

## 2. Directory Structure

```
tools/graphics/
├── README.md                 # Pipeline and aesthetic documentation (this file)
├── build.sh                  # Compiles all 14 PNG targets via typst compile
├── verify.sh                 # Asserts dimensions, byte budgets, and SHA-256 determinism
├── sync-modrinth.sh          # Modrinth v2 API listing synchronizer (--dry-run by default)
├── tokens.toml               # Single source of truth for design tokens, colors, & roles
├── fonts/                    # Bundled SIL OFL fonts
│   ├── Cinzel.ttf
│   ├── Fraunces.ttf
│   ├── SourceSans3.ttf
│   ├── Silkscreen-Regular.ttf
│   ├── Silkscreen-Bold.ttf
│   ├── JetBrainsMono-Regular.ttf
│   ├── JetBrainsMono-Bold.ttf
│   └── OFL.txt
├── screenshots/              # Optional in-game screenshot drops (.gitkeep)
│   ├── 01-overview.png
│   ├── 02-settlements.png
│   ├── 03-roles.png
│   └── 04-death-lore.png
├── lib/
│   ├── prng.typ              # Pure Typst LCG PRNG, FNV-1a hashing & 2D value noise
│   ├── theme.typ             # Loads tokens.toml, defines px unit & ledger framing
│   ├── backgrounds.typ       # CeTZ procedural generators (paper grain, survey BFS, contours)
│   └── icons.typ             # Integer pixel-art sprites & cartographer's compass rose
└── templates/
    ├── project-icon.typ      # 512x512 icon (mod & modpack variants, zero text)
    ├── hero-banner.typ       # 1280x360 field-guide title plate
    ├── gallery-card.typ      # 1280x720 (16:9) Modrinth plates (screenshot-ready)
    ├── section-header.typ    # 1200x160 subsystem chapter headers
    └── roles-diagram.typ     # 1200x420 4-column bestiary & specialization ledger
```

---

## 3. Typst Exact Pixel Sizing Convention

Typst defines raster dimension output via points and pixels-per-inch:
$$\text{pixels} = \text{points} \times \left(\frac{\text{ppi}}{72}\right)$$

By configuring every template with `#let px = 1pt` and compiling with **`--ppi 72`**, Typst point coordinates map $1:1$ to exact PNG pixel dimensions:
- `512pt` at `72 ppi` $\rightarrow$ exact **`512 × 512` pixels**
- `1280pt × 360pt` at `72 ppi` $\rightarrow$ exact **`1280 × 360` pixels**
- `1280pt × 720pt` at `72 ppi` $\rightarrow$ exact **`1280 × 720` pixels**
- `1200pt × 160pt` at `72 ppi` $\rightarrow$ exact **`1200 × 160` pixels**
- `1200pt × 420pt` at `72 ppi` $\rightarrow$ exact **`1200 × 420` pixels**

---

## 4. Screenshot Integration (How to Upgrade Gallery Cards)

The 4 Modrinth gallery cards (`1280 × 720`) support **real in-game Minecraft screenshots**.

1. Capture crisp in-game screenshots (`F1` to hide HUD) showcasing survival gameplay:
   - `01-overview.png`: Wide landscape or settlement overview
   - `02-settlements.png`: Contiguous town build or village market
   - `03-roles.png`: Combat raid or dungeon encounter
   - `04-death-lore.png`: Recovery terminal or anvil/lore item inspect
2. Place them into `tools/graphics/screenshots/`.
3. Run `./tools/graphics/build.sh`.

When a screenshot is present, `gallery-card.typ` automatically frames it on warm vellum as a pinned expedition photo plate with an authentic bottom caption bar. When no screenshot is present, it renders a structured cartographic/ledger schematic plate.

---

## 5. Usage & Scripts

### Compile All Assets
```bash
./tools/graphics/build.sh
```

### Verify Dimensions, Byte Budgets & Determinism
```bash
./tools/graphics/verify.sh
```

### Dry-Run Modrinth Asset Synchronization
```bash
./tools/graphics/sync-modrinth.sh --dry-run
```
