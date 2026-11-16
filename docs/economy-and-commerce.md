# Economy & Commerce System

The economy operates strictly on a **non-material, digital balance** designed to eliminate physical coin clutter, prevent loss on death, and foster an active player-to-player market while preventing AFK inflation.

---

## 1. Digital Currency & Liquidity Faucets

### Off-Inventory Digital Wallet
* **Zero Inventory Slots:** Your money is stored as an abstract account balance on the server, not as physical gold coins or paper notes.
* **Death-Proof:** Digital currency is never dropped, burned in lava, or lost upon death. You always retain your funds to recover gear via the Buyback Ledger.
* **Commands:**
  * `/economy balance` — Displays your current available balance.
  * `/economy pay <player> <amount>` — Transfers currency directly to another player. A configurable transaction tax (default 5%) is deducted to serve as an economic sink.

### The Four Liquidity Faucets
Rather than printing money through passive AFK mob grinders or automated farms, currency enters the economy through active survival milestones:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Economic Inflow Channels                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    │                               │                               │
    ▼                               ▼                               ▼
┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
│ One-Time Starter      │   │ Repeatable Cycles     │   │ P2P & Server Commerce │
│ Milestones            │   │                       │   │                       │
│ • First iron tool     │   │ • Every 500 natural   │   │ • Player stalls       │
│ • Net-zero death runs │   │   blocks harvested    │   │ • Building shop       │
│ • First claim formed  │   │ • Active dungeon runs │   │ • Bulk liquidation   │
└───────────────────────┘   └───────────────────────┘   └───────────────────────┘
```

1. **One-Time Starter Milestones:**  
   Essential onboarding advancements that inject early capital into new players (e.g., establishing a home claim, crafting first tool sets, mapping regional landmarks).
2. **Repeatable Active Play Cycles:**  
   Tiered, repeatable milestones that continuously reward active gameplay:
   * Every $Y$ naturally harvested blocks (e.g., 500 logs, ores, or crops).
   * Clearing dangerous dungeons or defeating elite dark-zone threats.
   * Sustained, non-AFK active exploration blocks.
3. **Player-to-Player (P2P) Commerce:**  
   Direct trade between specialized builders, miners, hunters, and explorers.
4. **Server Bulk Store:**  
   Liquidation of mundane materials into digital funds.

---

## 2. Building-Material Shop & Natural Harvest Unlocks

To eliminate the mindless grind of quarrying tens of thousands of basic structural blocks for massive towns without bypassing survival progression, the server provides a centralized **Building-Material Shop**.

### Per-Block Natural Harvest Unlocks
* Common building blocks (smooth stone, deepslate tiles, quartz blocks, terracotta, glass, stripped wood) are locked by default.
* To unlock purchasing eligibility for a specific block, a player must naturally harvest a set threshold quantity (default 100 blocks) of that material in the wild.
* Once unlocked, builders can purchase bulk quantities at fair server rates using their earned digital currency.

> [!IMPORTANT]
> **Precious Materials Excluded:** Precious resource ores (diamonds, ancient debris, emeralds), top-tier gear, and rare boss catalysts are strictly **excluded** from the building shop. They can only be acquired through active caving, dungeons, or player trading.

---

## 3. Anti-Laundering Guardrails (`player_placed`)

To prevent players from buying blocks from the shop or placing and re-mining Silk-Touched blocks to artificially exploit harvest thresholds:

* The server tracks a lightweight `player_placed` metadata flag on blocks placed by any player.
* Breaking a block flagged as `player_placed`:
  * **Never counts** toward natural-harvest shop unlock thresholds.
  * **Never awards** Gatherer Playstyle XP.
  * **Never triggers** repeatable harvest advancement payouts.
* Only naturally generated world blocks (`!player_placed`) advance economic and playstyle progression.

---

## 4. Server Bulk Store Routing

When a player dies, standard mundane stackable items do not clutter the individual UUID Buyback Ledger:

* **What Routes to Bulk Store:** Mundane arrows, cobblestone, dirt, common mob drops (rotten flesh, bones), ordinary crops, and un-enchanted standard materials.
* **Market Sinks:** These items are automatically deposited into the server's central bulk stockpile, which feeds player-accessible supply crates at baseline market prices.
* **Why this matters:** Players recovering from death don't have to pay escalating buyback fees for mundane dirt or arrows; those items can simply be restocked from the bulk store or re-gathered.

---

## 5. Anti-AFK Nerfs and Active Resource Sourcing

Automated AFK farming invalidates cooperative play and causes severe economic hyperinflation. The mod suite implements systemic nerfs against automated farming and redistributes resources into active survival gameplay:

| Passive Farm Nerfed | Systemic Nerf Applied | Active Gameplay Replacement |
| :--- | :--- | :--- |
| **Automated Iron Farms** | Golem spawning restricted outside natural village events. | **Buffed Deep-Cave Ore Veins:** Deepslate iron and copper veins generate significantly denser clusters in perilous deep caves. |
| **Villager Trading Halls** | Vanilla villager trade refresh loops and discount exploitation are disabled. | **Dungeon Tomes & Bosses:** Top-tier enchantment books (Mending, Unbreaking III, Silk Touch) drop as guaranteed rewards from dungeons, trial chambers, and bosses. |
| **Static XP Mob Chutes** | Diminishing returns applied to mob kills within static chunks; drops and XP decay to zero if mobs are trapped without player combat engagement. | **Combat Speed & Accuracy XP:** Active, high-tempo combat against dangerous mobs awards massive Combat XP and currency milestones. |
| **Automated Raid Farms** | Bad Omen stacking and static raid funneling are rebalanced. | **Server-Wide Objectives & Support Crafting:** Gatherers and Crafters produce alchemical catalysts that multiply resource yields. |

### Repurposed Villages
With static villager trading halls disabled, vanilla villages are transformed into **safe rest stops for weary explorers**:
* Pre-lit safe zones immune to aggressive dark-zone mob escalation.
* Regional **Server-Wide Objective Hubs** where players coordinate cooperative quests.
* Landmarks for Item Lore registration, cartography mapping, and regional trade stalls.

---

## 6. Community Bounty & Contract Board

To complement natural gathering and prevent monotonous resource grinding, the server provides a living **Community Bounty Board**. Players can take on objective contracts tailored to their active playstyles, earning non-inflationary digital currency and direct Playstyle XP.

### Available Contract Tracks

| Contract ID | Category | Objective | Rewards |
| :--- | :--- | :--- | :--- |
| `scout_recon` | **Scout / Exploration** | Survey 15 deep cavern chunks ($Y < 0$). | **150 coins** + **50 Explorer XP** |
| `scout_biomes` | **Scout / Exploration** | Discover 5 distinct biomes across the realm. | **200 coins** + **75 Explorer XP** |
| `gather_timber` | **Gatherer / Harvesting** | Naturally harvest 64 raw log blocks. | **120 coins** + **60 Gatherer XP** |
| `gather_deep_minerals` | **Gatherer / Harvesting** | Naturally harvest 32 deepslate ore blocks. | **180 coins** + **80 Gatherer XP** |
| `combat_cull` | **Combat / Hunting** | Slay 12 hostile monsters in deep caverns. | **200 coins** + **90 Combat XP** |
| `combat_snuffers` | **Combat / Hunting** | Slay 3 `destroys_light` elite mobs. | **300 coins** + **150 Combat XP** |

### Contract Guidelines
1. **Concurrency Limit:** Players may hold a maximum of **2 active contracts** simultaneously.
2. **Anti-Griefing Verification:** Harvest bounties only count natural blocks verified by the anti-laundering tracker; player-placed or Silk-Touched blocks are ignored.
3. **Automatic Progress Tracking:** Progress updates seamlessly in the background as you explore, harvest, and fight.

### Bounty Commands
* `/bounty list`: Browse all available community contracts and reward payouts.
* `/bounty accept <id>`: Accept a bounty contract into your active log.
* `/bounty status`: View your current active contracts and progress towards objectives.
* `/bounty claim <id>`: Collect your coin and Playstyle XP rewards upon completing an objective.
* `/bounty abandon <id>`: Drop an active contract to free up your concurrent contract slot.
