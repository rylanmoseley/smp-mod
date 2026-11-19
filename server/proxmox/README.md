# Proxmox VE Automated Deployment & Configuration Synchronization Guide

This guide details how to host the **SMP Mod Suite** on a **Proxmox Virtual Environment (VE)** virtual machine (or LXC container) and configure it to **automatically stay synchronized with the configurations, permissions, and mod releases in this repository**.

---

## 1. Proxmox VE VM Sizing & Best Practices

For optimal Java 21 performance with Tectonic world generation:

```
┌─────────────────────────────────────────────────────────────┐
│                 Proxmox VE VM Configuration                 │
├───────────────────┬─────────────────────────────────────────┤
│ OS Template       │ Ubuntu 24.04 LTS or Debian 12 (bookworm)│
│ CPU Type          │ "host" (exposes AVX2/AES-NI to Java 21) │
│ Cores             │ 4 - 8 vCPUs                             │
│ Memory (RAM)      │ 10 GB - 16 GB                           │
│ Memory Ballooning │ DISABLED (Critical: pinned RAM for JVM) │
│ Disk              │ 60+ GB on NVMe (SSD Emulation, Discard) │
│ Network           │ VirtIO (paravirtualized)                │
└───────────────────┴─────────────────────────────────────────┘
```

> [!CAUTION]
> **Disable RAM Ballooning in Proxmox VM Hardware settings!**  
> Java G1GC allocates virtual memory up-front (`-Xms == -Xmx`). Dynamic ballooning will cause host memory reclaiming, causing severe TPS degradation or JVM OOM crashes.

---

## 2. VM Initial Setup & Dependencies

Connect to your Proxmox VM console or SSH as `root`:

```bash
# Update base system
apt update && apt upgrade -y

# Install Java 21 Temurin & operational utilities
apt install -y curl wget git jq unzip rsync mcrcon

# Install Eclipse Temurin JDK 21 (if not in distro repositories)
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" > /etc/apt/sources.list.d/adoptium.list
apt update && apt install -y temurin-21-jdk

# Create unprivileged service user
useradd -r -m -d /opt/minecraft -s /bin/bash minecraft
```

---

## 3. Directory Layout on the VM

Setup the standard directory layout under `/opt/minecraft`:

```bash
# Create operational directories
mkdir -p /opt/minecraft/server/config
mkdir -p /opt/minecraft/server/mods
mkdir -p /opt/minecraft/server/permissions
mkdir -p /opt/minecraft/scripts
mkdir -p /var/log/minecraft

# Set ownership
chown -R minecraft:minecraft /opt/minecraft
chown -R minecraft:minecraft /var/log/minecraft
```

---

## 4. Setting Up Automated Turnkey Server Provisioning & Sync

The included [sync-server.sh](file:///home/sandbox/smp-mod/server/proxmox/sync-server.sh) script is a **complete end-to-end provisioner**:

1. **NeoForge 1.21.1 Engine:** Automatically checks if `unix_args.txt` and NeoForge libraries exist; if not, downloads `neoforge-21.1.234-installer.jar` and runs headless server installation.
2. **Companion Mods:** Runs the Packwiz bootstrap engine against `pack/pack.toml` to automatically download all 19 server companion mods (Tectonic, YUNG's Suite, Curios API, Rylan's Items, OPAC claims, Simple Voice Chat, BlueMap & OpenPaC BlueMap integration, Spark, Chunky, FerriteCore, LuckPerms).
3. **Core Mod Binary:** Checks GitHub Releases via GitHub API and downloads the latest `smpmod-*.jar`.
4. **Datapacks:** Synchronizes any community or custom datapacks in `server/datapacks/` into `/opt/minecraft/server/world/datapacks/`.
5. **Configs & Permissions:** Synchronizes `smpmod-common.toml`, `server.properties`, and `luckperms-smp.txt`, accepts the EULA, and ensures scripts are executable.
6. **Graceful Restart:** If changes are detected while the server is running, broadcasts a 60s countdown warning via RCON, runs `save-all`, and safely restarts systemd.

### Step 1: Deploy the Sync Script
Copy `server/proxmox/sync-server.sh` to `/opt/minecraft/scripts/sync-server.sh`:
```bash
cp server/proxmox/sync-server.sh /opt/minecraft/scripts/sync-server.sh
chmod +x /opt/minecraft/scripts/sync-server.sh
chown minecraft:minecraft /opt/minecraft/scripts/sync-server.sh
```

### Step 2: Run Turnkey Initial Provisioning
Execute the script as the `minecraft` user:
```bash
sudo -u minecraft /opt/minecraft/scripts/sync-server.sh
```
In a single run on an empty directory, this will download NeoForge, all companion mods, configs, and core jars—leaving the server completely ready to boot!

---

## 5. Systemd Service & Automated Timer Setup

Deploy the systemd units so the server runs as a background daemon and checks for updates automatically.

### Step 1: Install Minecraft Service Unit
Copy [minecraft.service](file:///home/sandbox/smp-mod/server/proxmox/minecraft.service) to `/etc/systemd/system/minecraft.service`:
```bash
cp server/proxmox/minecraft.service /etc/systemd/system/minecraft.service
```

### Step 2: Install Update Timer & Service
Copy the update timer units:
```bash
cp server/proxmox/minecraft-update.service /etc/systemd/system/minecraft-update.service
cp server/proxmox/minecraft-update.timer /etc/systemd/system/minecraft-update.timer
```

### Step 3: Enable and Start Services
```bash
# Reload systemd daemon
systemctl daemon-reload

# Enable Minecraft dedicated server to start on VM boot
systemctl enable --now minecraft

# Enable update timer (runs daily at 4:30 AM maintenance window and on boot)
systemctl enable --now minecraft-update.timer

# Verify status
systemctl status minecraft
systemctl list-timers --all
```

---

## 6. Alternative: Zero-Maintenance Mod Sync via Packwiz Bootstrap

In addition to Git syncing, you can configure the server to automatically download and update all companion mods (Tectonic, OPAC, Voice Chat, etc.) on boot using the **Packwiz Installer Bootstrap**:

1. Download `packwiz-installer-bootstrap.jar` into your server directory:
   ```bash
   wget -O /opt/minecraft/server/packwiz-installer-bootstrap.jar \
     https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar
   ```

2. Add this command to your server startup sequence in `run.sh` before the main Java launch:
   ```bash
   java -jar packwiz-installer-bootstrap.jar \
     --bootstrap-no-update \
     https://raw.githubusercontent.com/rylanmoseley/smp-mod/main/pack/pack.toml
   ```

3. **Result:** Every time the server restarts, Packwiz contacts GitHub, compares file hashes, updates mods and configs in `pack/`, and proceeds to launch the game.

---

## 7. Proxmox Backup & Snapshot Strategy

Combine Git auto-sync with Proxmox native hypervisor backups:

1. **Daily Proxmox Snapshot / PBS Backup:**
   - Configure **Proxmox Backup Server (PBS)** or regular `vzdump` at **03:30 AM** (before the 04:30 AM Git sync timer).
   - Mode: `snapshot` (zero downtime).
2. **Rollback Safety:**
   - If a Git config change introduces an unintended balance setting, simply revert the commit in Git (`git revert <hash>` and `git push`), or restore the Proxmox snapshot in seconds via the Proxmox web GUI.
