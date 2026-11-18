# Server Moderation, Utilities & Companion Administration Guide

![Server Utilities & Moderation Subsystem Header](assets/generated/headers/server-utilities.png)

The **SMP Mod Suite** includes a comprehensive suite of native server moderation, player utility, and telemetry tools designed to elevate community survival play. Built natively on NeoForge 1.21.1 without heavy external dependencies, these systems reference established survival community favorites—such as Vanilla Tweaks and the popular "Tool Stats" datapacks—while integrating directly into the mod's economy, land claims, role tracking, and recovery ledgers.

---

## 1. Stat Leaderboards (`/leaderboard` & `/top`)

The native Leaderboard system provides persistent, real-time rankings across survival playstyles, economic metrics, and long-term milestones.

```
       ┌─────────────────────────────────────────────────────────────┐
       │                Leaderboard Metric Categories                │
       └──────────────────────────────┬──────────────────────────────┘
                                      │
         ┌───────────────┬────────────┴───┬───────────────┬───────────────┐
         ▼               ▼                ▼               ▼               ▼
     [WEALTH]        [COMBAT]         [BUILDER]      [GATHERER]      [EXPLORER]
   Digital Coins  Mob & Player Kills Block Variety  Ore & Crops   Biomes & Chunks
         │               │                │               │               │
         ▼               ▼                ▼               ▼               ▼
    [PLAYTIME]      [DUNGEONS]     [CONSTRUCTION]     [HARVEST]
    Server Hours   Trial Vaults    Placed Dividends  Natural Reaps
```

### Supported Categories
Rankings are tracked continuously and cached to avoid tick overhead:

| Category Key | Aliases | Metric Measured | Formatting |
| :--- | :--- | :--- | :--- |
| `wealth` | `coins`, `money`, `bal` | Current digital wallet balance | `X,XXX Coins` |
| `combat` | `kills`, `pvp` | Total Playstyle Combat XP | `X,XXX XP` |
| `builder` | `build`, `blocks` | Total Playstyle Builder XP | `X,XXX XP` |
| `gatherer` | `gather`, `mine` | Total Playstyle Gatherer XP | `X,XXX XP` |
| `explorer` | `explore`, `travel` | Total Playstyle Explorer XP | `X,XXX XP` |
| `playtime` | `time`, `hours` | Total server playtime | `XX.X hours` |
| `dungeons` | `vaults`, `trials` | Trial chambers & dungeon vaults opened | `X vaults` |
| `construction`| `placed`, `dividends`| Verified blocks placed for dividends | `X blocks` |
| `harvest` | `crops`, `farm` | Natural harvest blocks gathered | `X blocks` |

### In-Game Usage & Pagination
Players can view the top rankings using either `/leaderboard` or the shortcut `/top`:
```bash
/leaderboard <category> [page]
/top <category> [page]
```
- **Page Size:** 10 entries per page.
- **Header:** Displays category title and current page indicator (e.g. `=== SMP LEADERBOARD: WEALTH (Page 1/4) ===`).
- **Entry Formatting:** Top 3 players receive distinctive medal coloring:
  - **#1:** Gold (`§6`)
  - **#2:** Silver / Aqua (`§b`)
  - **#3:** Bronze / Yellow (`§e`)
  - **#4+:** Dark Aqua (`§3`)
- **Self-Ranking:** When querying a leaderboard, the player's personal rank and stat are displayed at the bottom of the page if they fall outside the current view.
- **Tab Completion:** All category names and aliases support full tab autocompletion.

---

## 2. Tool Statistics & Dynamic Lore Counters

Referencing the mechanics of the popular "Tool Stats" datapack, every tool, weapon, and fishing rod automatically tracks lifetime survival accomplishments directly in its persistent item data component.

```
+-------------------------------------------------+
| Diamond Pickaxe                                 |
| Efficiency V                                    |
| Unbreaking III                                  |
|                                                 |
| --- Tool Statistics ---                         |
| Blocks Mined: 14,250                            |
| Crafted by: Alex                                |
| Owned by: Steve                                 |
+-------------------------------------------------+
```

### Tracked Item Classes & Metrics
Tool stats update upon every verified game action:

1. **Digger Tools (`Pickaxes`, `Axes`, `Shovels`, `Hoes`, `Shears`):**
   - **Blocks Mined:** Incremented whenever a block break successfully yields drops and consumes item durability.
2. **Weapons (`Swords`, `Axes`, `Bows`, `Crossbows`, `Tridents`, `Maces`):**
   - **Mobs Slain:** Incremented whenever the weapon delivers the fatal blow to a non-player living entity.
   - **Player Kills:** Incremented whenever the weapon kills another player in PvP.
3. **Fishing Implements (`Fishing Rods`):**
   - **Fish Caught:** Incremented whenever a player reels in fish, treasure, or junk.

### Native Data Components (Zero Save Bloat)
In modern Minecraft 1.21.1 / NeoForge, tool statistics are stored directly in native item data components rather than legacy string tags:
- **Instant Tooltip Rendering:** Rendered directly on item tooltips with zero server tick lag, zero desynchronization risk, and no duplication exploits.
- **Styling:** Matches Vanilla Tweaks aesthetic with a gold separator (`§6--- Tool Statistics ---`) and aqua stat lines (`§bBlocks Mined: §f14,250`).

---

## 3. Multiplayer Dynamic Sleep & Sanctuary Buffs

Single-player sleep has long been a vital quality-of-life feature for survival communities. The SMP Mod Suite provides a dynamic multiplayer sleep manager that synchronizes directly with NeoForge game rules while adding immersive world broadcasts and town rest bonuses.

```
Night Falls ──> Player Enters Bed ──> Dynamic Percentage Met ──> Time Advances ──> Sunrise Broadcast & Well Rested Buffs
```

### Sleep Configuration & Gamerule Synchronization
- **Configurable Percentage (`playersSleepingPercentage`):** Defaults to `1%`, enabling single-player sleep on servers of any size. Can be tuned in `smpmod-common.toml` to require a larger fraction (e.g., 25% or 50% of online players).
- **Automated Gamerule Sync:** The server continuously keeps the vanilla gamerule `playersSleepingPercentage` synchronized with the config value on every server tick.

### World Announcements & Weather Clearing
- **Bed Entrance Alert:** When a player climbs into bed, an action bar or chat notification broadcasts sleep progress:
  ```
  [Night] Steve is resting in bed (1/8 players asleep).
  ```
- **Sunrise Announcement:** When the night successfully passes, a golden sunrise broadcast greets the realm:
  ```
  The sun rises over the realm. Good morning!
  ```
- **Storm Dissipation:** Passing the night automatically clears rain and thunder storms across all dimensions.

### Sanctuary "Well Rested" Buffs
To reward players who sleep in safe settlements rather than lone wilderness beds:
- Waking up inside an owned **Contiguous Claim Cluster** or within **48 blocks of a Village Bell** grants the **Well Rested** status for 5 minutes:
  - **Regeneration I** (10 seconds)
  - **Haste I** (5 minutes — speed up morning chores, mining, and construction)
  - **Saturation** (Restores 2 food points and saturation)

---

## 4. Discord Webhook Dispatcher

The mod includes an asynchronous, non-blocking Discord Webhook Dispatcher built natively on Java 21 without external libraries, bot tokens, or complex socket servers—simply paste an incoming Discord Webhook URL into the server config.

```
   ┌──────────────────────────────────────────────────────────┐
   │                    SMP Game Events                       │
   └─────────────┬──────────────┬─────────────┬───────────────┘
                 │              │             │
                 ▼              ▼             ▼
          Town Foundings   Dragon Kills   Salvage Drops
                 │              │             │
                 └──────────────┼─────────────┘
                                │
                                ▼
               Java 21 Async Non-Blocking Dispatcher
                                │
                                ▼
            Discord Webhook Embed (Formatted Markdown)
```

### Dispatched Server Alerts
The webhook dispatcher formats rich Discord embeds with color-coded sidebars, timestamps, and footers:

| Event Trigger | Embed Color | Information Broadcast |
| :--- | :--- | :--- |
| **Town Milestone** | Emerald Green (`#2ECC71`) | Broadcasts when a settlement levels up to **Town** (19+ chunks) or **Metropolis** (37+ chunks), celebrating the founders. |
| **Dragon Victory** | Purple (`#9B59B6`) | Congratulates the raid team on defeating the cooperative Ender Dragon, listing all participant names and battle roles. |
| **Public Salvage Drop** | Gold (`#F1C40F`) | Alerts the server when valuable gear expires from a player's 24-hour private buyback ledger and hits the public marketplace at 1.25x price. |
| **Villager Crime Report** | Crimson (`#E74C3C`) | Issues a public crime bulletin when a player murders a natural villager, noting the severe price debuff imposed on the killer. |
| **Legendary Advancement** | Cyan (`#3498DB`) | Celebrates master builders, deep cave pioneers, and dragon slayers completing high-tier achievements. |

### Configuration (`smpmod-common.toml`)
```toml
[Discord]
    # Discord Webhook URL for server event announcements (leave empty to disable)
    discordWebhookUrl = "https://discord.com/api/webhooks/..."

    # Server name displayed on Discord embed footers
    discordServerName = "SMP Realm"

    # Toggle individual event notifications
    notifyTownMilestones = true
    notifyDragonVictories = true
    notifyPublicSalvage = true
    notifyVillagerCrimes = true
    notifyAdvancements = true
```

---

## 5. Companion Server Setup & Administration Stack

While the SMP Mod Suite provides native moderation, recovery, and economy systems, running a thriving public or private survival server benefits from pairing with battle-tested companion tools.

Below is the recommended companion administration stack for Minecraft 1.21.1 NeoForge:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      Recommended Production Stack                        │
├───────────────────────────────┬──────────────────────────────────────────┤
│ System Component              │ Recommended Solution                     │
├───────────────────────────────┼──────────────────────────────────────────┤
│ Permissions & Groups          │ LuckPerms (NeoForge)                     │
│ Block Logging & Rollbacks     │ Ledger or CoreProtect NeoForge           │
│ Performance & Profiling       │ Spark                                    │
│ World Pre-generation          │ Chunky                                   │
│ Live 3D / 2D Web Map          │ BlueMap or Squaremap                     │
│ Two-Way Discord Chat Bridge   │ DiscordSRV / Simple Voice Chat           │
└───────────────────────────────┴──────────────────────────────────────────┘
```

### 1. Permissions & Ranks: **LuckPerms**
- **Why:** The industry standard for flexible, database-backed permission management.
- **Configuration:** Allows assigning command nodes (`smpmod.commands.leaderboard`, `smpmod.commands.buyback`, etc.) to guest, member, and admin groups.
- **Web Editor:** Easily manage server ranks and inheritance via the online GUI editor (`/lp editor`).

### 2. Block Logging & Grief Recovery: **Ledger**
- **Why:** In addition to OPAC claim boundaries, Ledger logs block placements, chest transactions, and entity interactions to a fast local SQLite database.
- **Rollback Tooling:** Enables administrators to inspect (`/ledger inspect`) or rollback accidental griefs or creeper explosions without resetting entire chunks.

### 3. Performance & Tick Analysis: **Spark**
- **Why:** Essential tool for diagnosing tick lag, memory leaks, and entity bottlenecks.
- **Usage:**
  - `/spark sampler --start` creates CPU profiling snapshots to analyze mod performance.
  - `/spark tickmonitor` monitors TPS and MSPT stability during heavy multiplayer events.

### 4. World Pre-generation: **Chunky**
- **Why:** Tectonic world generation creates breathtaking mountain ranges and complex underground rivers, but generating terrain on-the-fly during fast Elytra flight can stress the server tick loop.
- **Recommended Workflow:**
  ```bash
  # Pre-generate a 5,000-block radius around spawn
  /chunky radius 5000
  /chunky start
  ```
  Running Chunky during initial server setup ensures silky-smooth 20.0 TPS gameplay when players begin exploring.

### 5. Real-Time Web Map: **BlueMap** or **Squaremap**
- **Why:** Provides an interactive browser map where players can admire mountain ranges, road networks, and developing settlements.
- **OPAC Claim Integration:** Automatically renders colored polygons for claimed territories, showing town names and leaders directly on the web map.
- **BlueMap:** Best for immersive 3D viewing with dynamic lighting.
- **Squaremap:** Best for lightweight, ultra-fast 2D leaflet viewing with minimal server overhead.

### 6. Two-Way Discord Bridge: **DiscordSRV**
- **Why:** While the SMP Mod Suite includes a native one-way webhook dispatcher for major milestones, DiscordSRV provides a continuous two-way bridge syncing in-game chat with Discord channels.
- **Pairing:** Pair with **Simple Voice Chat** for seamless proximity voice chat in towns and dungeon expeditions.
