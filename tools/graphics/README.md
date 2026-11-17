# Deterministic Brand & Documentation Graphics Pipeline (`tools/graphics`)

A self-contained, **100% deterministic, rule-based graphics generation pipeline** for the **SMP Mod Suite** built using **Typst** and **CeTZ**, with **zero JavaScript, zero Node.js, and zero generative AI**.

This pipeline compiles all project branding, README hero banners, documentation section headers, architecture diagrams, and Modrinth listing cards directly from structured TOML design tokens and vector math.

---

## 1. Core Architectural Constraints

- **Zero JS / Zero Headless Browsers:** No Puppeteer, no Chromium, no NPM dependencies.
- **100% Deterministic Output:**
  - Always invokes Typst with `--ignore-system-fonts --font-path tools/graphics/fonts` to ensure host-installed fonts never alter glyph layout or kerning.
  - All procedural background patterns (chunk clusters, topographic contours, dot matrices, and radar grids) derive from a pure Typst seeded PRNG in `lib/prng.typ`.
  - Running `./tools/graphics/build.sh` back-to-back produces **byte-for-byte identical PNGs** with matching SHA-256 checksums.
- **Modrinth & GitHub Compliance:**
  - Standard raster PNG export for crisp rendering across light/dark GitHub themes and Modrinth UI.
  - `512x512` project icons strictly satisfy Modrinth's `<= 256 KiB` budget (current: ~38 KiB).
  - `1280x720` gallery cards strictly satisfy Modrinth's `<= 5 MiB` budget (current: ~240-290 KiB).
  - All embedded Markdown images require descriptive `alt` text.

---

## 2. Directory Structure

```
tools/graphics/
├── README.md                 # Pipeline documentation (this file)
├── build.sh                  # Compiles all 14 PNG targets via typst compile
├── verify.sh                 # Asserts dimensions, byte budgets, and SHA-256 determinism
├── sync-modrinth.sh          # Modrinth v2 API listing synchronizer (--dry-run by default)
├── tokens.toml               # Single source of truth for design tokens, colors, & roles
├── fonts/                    # Bundled SIL OFL fonts (Inter & JetBrains Mono)
│   ├── Inter-Regular.otf
│   ├── Inter-Medium.otf
│   ├── Inter-SemiBold.otf
│   ├── Inter-Bold.otf
│   ├── JetBrainsMono-Regular.ttf
│   ├── JetBrainsMono-Bold.ttf
│   └── OFL.txt
├── lib/
│   ├── prng.typ              # Pure Typst LCG PRNG, FNV-1a hashing & 2D value noise
│   ├── theme.typ             # Loads tokens.toml, defines px unit & shared UI components
│   ├── backgrounds.typ       # CeTZ procedural generators (BFS chunk cluster, contours, radar)
│   └── icons.typ             # Crisp vector icons & smpmod isometric emblems
└── templates/
    ├── project-icon.typ      # 512x512 icon (mod & modpack variants via --input variant=...)
    ├── hero-banner.typ       # 1280x360 README top hero banner
    ├── gallery-card.typ      # 1280x720 (16:9) Modrinth showcase cards
    ├── section-header.typ    # 1200x160 subsystem section headers
    └── roles-diagram.typ     # 1200x420 4-quadrant Emergent Roles diagram
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

## 4. Generated Asset Inventory (`docs/assets/generated/`)

| Asset Path | Exact Resolution | Byte Budget | Actual Size | Description |
| :--- | :--- | :--- | :--- | :--- |
| `icon-mod.png` | `512 × 512` | `<= 256 KiB` | ~38.7 KiB | Standalone NeoForge mod project icon |
| `icon-modpack.png` | `512 × 512` | `<= 256 KiB` | ~40.2 KiB | Packwiz modpack listing icon |
| `hero-readme.png` | `1280 × 360` | `<= 1 MiB` | ~55.9 KiB | Main README and overview banner |
| `gallery/01-overview.png` | `1280 × 720` | `<= 5 MiB` | ~289.1 KiB | **Featured** Modrinth 6-subsystem overview |
| `gallery/02-settlements-economy.png`| `1280 × 720`| `<= 5 MiB` | ~233.2 KiB | Contiguous claims & digital economy showcase |
| `gallery/03-emergent-roles.png` | `1280 × 720` | `<= 5 MiB` | ~230.0 KiB | Emergent roles & Playstyle XP progression |
| `gallery/04-death-recovery-lore.png`| `1280 × 720`| `<= 5 MiB` | ~240.7 KiB | UUID buyback ledger & item lore showcase |
| `headers/economy.png` | `1200 × 160` | `<= 512 KiB`| ~31.3 KiB | Economy & Commerce section header |
| `headers/death-recovery.png` | `1200 × 160` | `<= 512 KiB`| ~28.6 KiB | Death Recovery & Item Lore header |
| `headers/land-claims.png` | `1200 × 160` | `<= 512 KiB`| ~26.4 KiB | Land Claims & Settlements header |
| `headers/world-navigation.png` | `1200 × 160` | `<= 512 KiB`| ~40.8 KiB | World Navigation & Survival header |
| `headers/emergent-roles.png` | `1200 × 160` | `<= 512 KiB`| ~33.2 KiB | Emergent Roles & Combat header |
| `headers/server-utilities.png` | `1200 × 160` | `<= 512 KiB`| ~27.6 KiB | Server Utilities & Moderation header |
| `diagrams/emergent-roles-quadrant.png`| `1200 × 420`| `<= 1 MiB` | ~96.0 KiB | 4-Quadrant combat specialization matrix |

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

### Sync Assets to Modrinth
```bash
# Preview API payloads without sending
./tools/graphics/sync-modrinth.sh --dry-run

# Perform live upload (requires MODRINTH_TOKEN and MODRINTH_PROJECT_ID)
MODRINTH_TOKEN="mrp_..." MODRINTH_PROJECT_ID="smp-mod" ./tools/graphics/sync-modrinth.sh --apply
```

---

## 6. Bundled Fonts & Licensing

All bundled fonts in `fonts/` are licensed under the **SIL Open Font License (OFL 1.1)**:
- **Inter (Regular, Medium, SemiBold, Bold):** Designed by Rasmus Andersson (https://github.com/rsms/inter).
- **JetBrains Mono (Regular, Bold):** Designed by JetBrains (https://github.com/JetBrains/JetBrainsMono).
Full license text is preserved in [fonts/OFL.txt](./fonts/OFL.txt).
