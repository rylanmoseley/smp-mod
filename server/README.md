# Dedicated Server Operations & Administration Guide

This directory contains the production-ready server configuration, launch scripts, tuning profiles, and administrative notes for running the **SMP Mod Suite** on a dedicated NeoForge 1.21.1 server.

---

## 1. Production Server Architecture & System Sizing

### Minimum & Recommended System Specifications
The SMP Mod Suite runs on **NeoForge 1.21.1** with **Tectonic** terrain generation and high-altitude alpine terrain (up to Y=320), requiring adequate heap and CPU single-thread throughput:

| Specification | Minimum (2-6 Players) | Recommended (10-25+ Players) | High-Traffic SMP (50+ Players) |
| :--- | :--- | :--- | :--- |
| **CPU** | 4 Cores (3.8+ GHz single-core) | 6-8 Cores (4.5+ GHz modern Ryzen/Intel) | 8-16 Cores (5.0+ GHz Zen 4/5 or Raptor Lake) |
| **Dedicated RAM** | 6 GB allocated to JVM | 8 GB allocated to JVM | 12 - 16 GB allocated to JVM |
| **Storage** | 30 GB NVMe SSD | 80 GB NVMe PCIe 4.0 SSD | 200+ GB Enterprise NVMe SSD |
| **Network** | 50 Mbps symmetric | 250+ Mbps symmetric | 1 Gbps unmetered |
| **Java Runtime** | Eclipse Temurin OpenJDK 21 | Eclipse Temurin OpenJDK 21 | Eclipse Temurin OpenJDK 21 |

> [!IMPORTANT]
> Always use **Java 21 (LTS)**. Minecraft 1.21.1 and NeoForge require Java 21. Do not use Java 17 or Java 8.

> [!NOTE]
> **Linux-Only Environment:** This dedicated server suite, automation toolchain, and systemd service scripts directly support **Linux environments** (Debian, Ubuntu, Proxmox VE, Alpine, RHEL/Rocky). Windows batch files (`.bat` / `.cmd`) are not supported.

---

## 2. Server Installation & Initial Setup

### Step 1: Install NeoForge Server
1. Download the official NeoForge installer for version **21.1.234** (matching [gradle.properties](file:///home/sandbox/smp-mod/gradle.properties)):
   ```bash
   curl -O https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.234/neoforge-21.1.234-installer.jar
   ```
2. Run the installer in headless server mode:
   ```bash
   java -jar neoforge-21.1.234-installer.jar --installServer
   ```
3. Verify that the `libraries/` directory and `run.sh` script are generated.

### Step 2: Accept the Minecraft EULA
Edit `eula.txt` and set:
```properties
eula=true
```

### Step 3: Deploy Server Configurations
Copy the pre-tuned configs from this `./server/` directory into your server root:
```bash
cp ./server/server.properties /path/to/server/server.properties
cp -r ./server/config/* /path/to/server/config/
```

### Step 4: Install Required Companion Mods
Place the companion mod `.jar` files into the server's `mods/` directory (or synchronize them automatically via Packwiz bootstrap):
1. **`smpmod-0.1.0-alpha.jar`** (built from this repository: `./gradlew build`)
2. **`kotlinforforge-5.13.0-all.jar`** (Kotlin for Forge runtime)
3. **`curios-neoforge-9.5.1+1.21.1.jar`** (Curios API accessory slot framework)
4. **`rylansitems-0.0.1-prerelease1.jar`** (Rylan's Items artifact & ability expansion)
5. **`tectonic-3.0.28-neoforge-21.1.jar`** (Tectonic mountain & underground river generation)
6. **`lithostitched-1.8.1-neoforge-21.1.jar`** (Required worldgen dependency for Tectonic)
7. **`open-parties-and-claims-neoforge-1.21.1-0.33.0.jar`** (OPAC claims)
8. **`voicechat-neoforge-1.21.1-2.6.27.jar`** (Simple Voice Chat proximity voice server)
9. **`spark-1.10.124-neoforge.jar`** (Performance profiler)
10. **`Chunky-NeoForge-1.4.23.jar`** (Terrain pre-generator)
11. **`ferritecore-7.0.3-neoforge.jar`** (Server RAM optimization)
12. **`LuckPerms-NeoForge-5.4.140.jar`** (Permissions manager)
13. **`squaremap-neoforge-mc1.21.1-1.3.2.jar`** (Web map engine)
14. **YUNG's Dungeon & Structure Suite** (`YungsApi`, `YungsBetterDungeons`, `YungsBetterStrongholds`, `YungsBetterDesertTemples`, `YungsBetterNetherFortresses`)

> [!TIP]
> **Automated Companion Mod Download:**  
> Instead of downloading each JAR manually, you can automatically provision all 18 companion mods directly from [pack/pack.toml](file:///home/sandbox/smp-mod/pack/pack.toml) using the headless Packwiz bootstrap engine:
> ```bash
> curl -sL -o packwiz-installer-bootstrap.jar "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar"
> java -jar packwiz-installer-bootstrap.jar -g --side server "file://$(realpath ../pack/pack.toml)"
> ```

---

## 3. Java 21 Garbage Collection & JVM Tuning

Modern Java 21 provides advanced low-latency collectors (**G1GC** and **ZGC**). For NeoForge 1.21.1 servers with 6–12 GB heap, the following optimized **G1GC** flags (Aikar's modern profile tuned for Java 21) provide steady 20.0 TPS with minimal garbage collection pauses:

```bash
java -Xms8G -Xmx8G \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+AlwaysPreTouch \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1ReservePercent=20 \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=4 \
  -XX:InitiatingHeapOccupancyPercent=15 \
  -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:G1RSetUpdatingPauseTimePercent=5 \
  -XX:SurvivorRatio=32 \
  -XX:+PerfDisableSharedMem \
  -XX:MaxTenuringThreshold=1 \
  -Dusing.aikars.flags=https://mcflags.emc.gs \
  -Daikars.new.flags=true \
  @libraries/net/neoforged/neoforge/21.1.234/unix_args.txt "$@"
```

> [!TIP]
> Use the included [run.sh](file:///home/sandbox/smp-mod/server/run.sh) script, which wraps these flags with an automatic restart loop and graceful SIGINT/SIGTERM shutdown handlers.

---

## 4. World Pre-Generation Runbook (Critical for Tectonic)

Tectonic generates complex 3D noise caves, cliffs up to Y=320, and underground river channels. Generating chunks on-the-fly while players explore with Elytras will cause tick lag and MSPT spikes.

**Always pre-generate a 5,000-block radius before opening the server to players:**

```bash
# 1. Stop the server and make sure Chunky is in mods/
# 2. Start the server and run the following in server console:

# Center pre-generation around world spawn
chunky spawn

# Set radius to 5,000 blocks (square boundary of 10,000 x 10,000 blocks)
chunky radius 5000

# Set chunk generation shape (square or circle)
chunky shape circle

# Begin pre-generation task
chunky start

# Monitor progress (updates every second in console)
chunky progress

# If needed to pause or resume:
chunky pause
chunky continue
```

Pre-generation takes between 45 to 120 minutes depending on CPU speed and SSD write throughput. Once finished, world exploration will remain at a smooth 20.0 TPS.

---

## 5. In-Game Diagnostics & Spark Profiling

The **Spark** profiler is pre-configured for live performance diagnostics:

| Command | Purpose |
| :--- | :--- |
| `/spark healthreport` | Instant snapshot of TPS, MSPT, CPU usage, and JVM memory allocation. |
| `/spark sampler --start` | Begins sampling CPU ticks and mod method execution times. |
| `/spark sampler --stop` | Stops sampling and generates a shareable web report link. |
| `/spark tickmonitor` | Real-time console log alerting whenever a tick exceeds 50ms. |
| `/spark gc` | Inspects GC pause duration and heap collection frequencies. |

---

## 6. Proxmox VE Automated Configuration Synchronization

If you are hosting the server on a **Proxmox VE** virtual machine or container, you can configure it to automatically track this repository for updates:

- **Automated Sync Script ([sync-server.sh](file:///home/sandbox/smp-mod/server/proxmox/sync-server.sh)):** Pulls Git commits, updates configs, downloads the latest `smpmod-*.jar` from GitHub Releases, and safely restarts the server with in-game countdown alerts.
- **Systemd Daemon & Daily Maintenance Timer:** Pre-configured [minecraft.service](file:///home/sandbox/smp-mod/server/proxmox/minecraft.service) and [minecraft-update.timer](file:///home/sandbox/smp-mod/server/proxmox/minecraft-update.timer) to run update checks automatically during off-peak hours (4:30 AM).
- **Packwiz Bootstrap Alternative:** Point `packwiz-installer-bootstrap.jar` directly to GitHub to update all companion mods on every boot.

See the complete deployment runbook in **[proxmox/README.md](file:///home/sandbox/smp-mod/server/proxmox/README.md)**.

---

## 7. Directory File Index

The files in this `./server/` directory provide out-of-the-box configurations:

- [server.properties](file:///home/sandbox/smp-mod/server/server.properties): High-performance server settings (optimized view/simulation distance, network compression, whitelist).
- [run.sh](file:///home/sandbox/smp-mod/server/run.sh): Production Linux launch script with Java 21 G1GC flags and auto-restart loop.
- [eula.txt](file:///home/sandbox/smp-mod/server/eula.txt): EULA confirmation template.
- [config/smpmod-common.toml](file:///home/sandbox/smp-mod/server/config/smpmod-common.toml): Production tuning matrix for economy, claims, sleep, roles, and Discord webhook.
- [permissions/luckperms-smp.txt](file:///home/sandbox/smp-mod/server/permissions/luckperms-smp.txt): Ready-to-import LuckPerms group permissions hierarchy.
- [maintenance/backup.sh](file:///home/sandbox/smp-mod/server/maintenance/backup.sh): Automated hot backup script with tar.gz compression and rotation.
- [proxmox/](file:///home/sandbox/smp-mod/server/proxmox/README.md): Proxmox VE auto-sync script, systemd service, and update timer.
