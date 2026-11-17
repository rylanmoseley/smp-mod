# SMP Survival Suite - Packwiz & Modrinth Modpack

![SMP Survival Suite Modpack Icon](../docs/assets/generated/icon-modpack.png)

This directory contains the **[Packwiz](https://packwiz.github.io/)** modpack definition and configuration overrides for the **SMP Survival Suite**.

Packwiz manages client and server mods as human-readable TOML metadata files committed directly into Git, allowing seamless team collaboration, automated dependency resolution, and one-command export to **Modrinth (`.mrpack`)** and **CurseForge (`.zip`)**.

---

## 1. Modpack Structure

```
pack/
├── pack.toml                 # Modpack metadata, Minecraft/NeoForge target versions, index hash
├── index.toml                # Canonical hash manifest of all managed mods and override files
├── mods/                     # Packwiz metadata (.pw.toml) for Modrinth-hosted companion mods
│   ├── tectonic.pw.toml      # Tectonic terrain generation (cliffs, mountain biomes, deep rivers)
│   ├── lithostitched.pw.toml # Required worldgen dependency for Tectonic
│   ├── open-parties-and-claims.pw.toml # OPAC claims & land party management
│   ├── simple-voice-chat.pw.toml       # Spatial proximity voice chat
│   ├── spark.pw.toml         # Tick & memory profiler
│   ├── chunky.pw.toml        # Server world pre-generator
│   ├── ferrite-core.pw.toml  # RAM and memory allocator optimization
│   ├── jade.pw.toml          # Block/entity WAILA inspector HUD
│   └── kotlin-for-forge.pw.toml        # Required Kotlin runtime for SMP Mod Suite
├── config/                   # Configuration files shipped directly to players
│   └── smpmod-common.toml    # Balanced gameplay tuning matrix
└── README.md                 # Developer & maintenance documentation (this file)
```

---

## 2. Maintaining the Modpack with Packwiz

### Installing Packwiz
If you don't have the Packwiz CLI installed, install it via Go:
```bash
go install github.com/packwiz/packwiz@latest
```
Make sure `~/go/bin` is in your system `$PATH`.

### Adding New Mods from Modrinth
To add a new mod hosted on Modrinth:
```bash
cd pack
packwiz mr add <mod-slug-or-id>
```
*Example:* `packwiz mr add sodium`

### Updating All Mods to Latest 1.21.1 Versions
To scan Modrinth and update all `.pw.toml` references:
```bash
cd pack
packwiz update --all
```

### Refreshing Hashes & Index
Whenever you manually edit files, add config overrides in `pack/config/`, or alter mod metadata:
```bash
cd pack
packwiz refresh
```
`packwiz refresh` recalculates SHA-256 hashes for all files and updates both `index.toml` and `pack.toml`.

---

## 3. Exporting the Modpack

### Exporting for Modrinth (`.mrpack`)
To export the complete modpack bundle for Modrinth App, Prism Launcher, or ATLauncher:
```bash
cd pack
packwiz mr export --output ../build/SMP-Survival-Suite.mrpack
```
This generates an official `.mrpack` archive containing:
- `modrinth.index.json`: Manifest pointing to Modrinth CDN hashes and download links.
- `overrides/`: Direct files (such as `config/smpmod-common.toml` and custom mods).

### Exporting for CurseForge (`.zip`)
To export a standard CurseForge-compatible client zip:
```bash
cd pack
packwiz cf export --output ../build/SMP-Survival-Suite-CurseForge.zip
```

---

## 4. How Players Install & Update

### Option A: Prism Launcher (Recommended)
1. In Prism Launcher, click **Add Instance**.
2. Select **Modrinth** on the left sidebar.
3. Search for **SMP Survival Suite** (or click **Import** and choose the exported `.mrpack` file).
4. When a new version is released on GitHub or Modrinth, Prism displays an **"Update Available"** badge on the instance. Clicking **Update** downloads new mods and refreshes configuration overrides while preserving personal keybinds and options.

### Option B: Modrinth App
1. Open the Modrinth App.
2. Click the `+` button to create a new profile.
3. Choose **From file** and select `SMP-Survival-Suite.mrpack`.
4. Subsequent updates can be applied with one click from the profile settings.
