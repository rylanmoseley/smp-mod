# Server Datapacks Directory

Place server-wide Minecraft datapacks (`.zip` archives or unzipped folders) into this directory.

The automated Proxmox synchronization script ([sync-server.sh](file:///home/sandbox/smp-mod/server/proxmox/sync-server.sh)) automatically synchronizes all files in this directory into the production server's active world datapack directory:

```
/opt/minecraft/server/world/datapacks/
```

### Recommended Community Datapacks
- **Vanilla Tweaks (vanillatweaks.net):**
  - Armor Statues (custom poses & survival book)
  - Player Head Drops & More Mob Heads
  - Silence Mobs (name-tag "silence" to quiet loud entities)
  - Double Shulker Shells (guaranteed 2 shells per Shulker)
- **Tectonic Addons:**
  - Additional terrain features or underground river structures.
- **Custom Dungeon Loot Tables:**
  - Custom loot injections for trial vaults and dungeon chests.
