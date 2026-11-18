# Economy & Commerce System

![Economy & Commerce Subsystem Header](assets/generated/headers/economy.png)

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
   * Every $Y$ naturally harvested blocks (default 200 logs, ores, or crops awards **+35 coins**).
   * Every $Z$ verified construction blocks placed inside claims (default 100 blocks awards **+25 to +40 coins**).
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
* **Locked by Default:** All non-precious structural blocks start in a **Locked** state.
* **Harvest Threshold:** To unlock purchasing eligibility for a specific block, a player must naturally harvest **100 blocks** of that material in the wild.
* **Fair Server Rates:** Once unlocked, builders can purchase bulk quantities using earned digital currency without stripping local landscapes.

### Natural Harvest Mastery Tiers & Escalating Discounts
As a player continues to naturally harvest an unlocked material, their per-block discount scales automatically:

| Unlock Tier | Natural Harvests Required | Multiplier | Discount Applied | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Locked** | `< 100` | — | — | Block cannot be purchased from the catalog |
| **Tier 1 (Unlocked)** | `100` | `1.0x` | **0%** | Standard catalog baseline price |
| **Tier 2 (Journeyman)**| `500` | `5.0x` | **10% off** | Experienced quarry / harvest specialist rate |
| **Tier 3 (Master)** | `2,000` | `20.0x` | **25% off** | Master artisan / industrial architect rate |

### Cross-Subsystem Discount Stacking (Capped at 50% Max)
Discounts across complementary systems stack cumulatively up to a hard **50% maximum discount ceiling**:
1. **Natural Harvest Mastery Tier:** Up to **-25%** based on lifetime wild harvest count.
2. **Contiguous Town Settlement Tier:** Up to **-20%** for active residents of developed settlements (Outpost: 0%, Hamlet: -5%, Settlement: -10%, Town: -15%, Metropolis: -20%).
3. **Support Role Efficiency Affinity:** Up to **-15%** scaling with the player's Support role mercantile efficiency passive.
4. **Gatherer Specialization XP:** Up to **-25%** proportional to the continuous Gatherer playstyle XP ratio.

$$\text{Final Unit Price} = \max\left(1, \text{Base Price} \times \left(1.0 - \min\left(0.50, \sum \text{Discounts}\right)\right)\right)$$

### Catalog Scope & Strict Exclusions
* **Approved Building Catalog (40+ Materials):**
  * *Stone & Deepslate Variants:* Stone, Cobblestone, Smooth Stone, Stone Bricks, Mossy Bricks, Andesite/Polished, Diorite/Polished, Granite/Polished, Deepslate, Cobbled Deepslate, Deepslate Bricks/Tiles/Chiseled, Tuff/Polished/Bricks.
  * *Sandstones & Bricks:* Sandstone (Cut/Smooth), Red Sandstone (Cut/Smooth), Bricks, Mud Bricks.
  * *Timbers & Planks:* Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson, Warped (both Planks and Logs).
  * *Architectural & Glass:* Terracotta (White, Orange, Light Gray, Brown, Red, Natural), Glass, Tinted Glass.
  * *Dimension & Monument Stones:* Blackstone (Polished/Bricks), Basalt/Smooth Basalt, Quartz/Smooth Quartz, Prismarine/Bricks/Dark Prismarine, End Stone/Bricks, Purpur Blocks/Pillars.
* **Strictly Excluded Items:**
  * All precious resource ores including diamonds, netherite, ancient debris, emeralds, and gold/iron blocks.
  * Spawners, beacons, conduits, dragon eggs, bedrock, end portal frames, and barrier blocks.
  * High-tier tools, weapons, armor, and rare catalysts. These can only be acquired through survival exploration, caving, combat, or P2P commerce.

---

## 3. Anti-Laundering & Natural Harvest Verification

To prevent players from buying blocks from the shop or placing and re-mining Silk-Touched blocks to artificially exploit harvest thresholds:

* The server tracks whether blocks were placed by players or generated naturally in the world.
* Breaking player-placed or recycled blocks:
  * **Never counts** toward natural-harvest shop unlock thresholds.
  * **Never awards** Gatherer Playstyle XP.
  * **Never triggers** repeatable harvest advancement payouts.
* Only genuine, wild world blocks advance economic and playstyle progression.

---

## 4. Server Bulk Store: Routing, Dynamic Market & Liquidation

The **Server Bulk Store** serves as the central stockpile and dynamic commodity market for mundane goods across the realm.

### Death-Drop Routing: Buyback Ledger vs. Bulk Store
When a player dies, dropped inventory items are automatically classified:
* **UUID Buyback Ledger:** Unique, high-value, enchanted, damaged, or named gear (armor, weapons, tools, netherite, totems, elytra, heavy cores) routes directly to the player's personal buyback ledger.
* **Bulk Store Reserve:** Standard, un-enchanted, undamaged stackable items (cobblestone, dirt, common mob drops, arrows, basic crops, raw copper/iron, coal, blaze rods, ender pearls) route directly into the server's bulk stockpile.
* **Why this matters:** Players recovering from death do not pay escalating recovery fees for mundane dirt or arrows; those items can simply be restocked from the bulk store or re-gathered.

### Dynamic Supply-and-Demand Pricing Curve
The Bulk Store evaluates its current inventory against a target baseline stock (**1,000 units** per item):

```
Stock Level:   Empty (0) ────── Target Baseline (1,000) ────── Saturated (5,000+)
Buy Price:       2.00x                 1.35x                         0.70x
Payout:          0.80x                 0.50x                         0.25x
```

* **Dynamic Purchase Price (Server selling to Player):**
  * When reserves are oversaturated with stock, purchase prices drop down to **0.70x** base price.
  * In times of extreme scarcity or empty reserves, prices scale up to **2.00x** base price.
* **Dynamic Liquidation Payout (Player selling to Server):**
  * When stockpiles are flooded, liquidation payouts scale down to **0.25x** base payout to prevent market glutting.
  * When server reserves are running low on critical supplies, liquidation payouts climb up to **0.80x** base payout to incentivize player harvesting.

### Bulk Liquidation & Support Role Synergy
* **Liquidating Excess Supplies:** Players can sell surplus mundane stackable goods directly into the server bulk store for instant digital coins via bulk liquidation.
* **Support Role Bonus:** Players specializing in the **Support Role** possess an innate mercantile efficiency passive, granting up to a **+25% bonus coin payout** on all bulk liquidations.

### Bulk Purchasing
* Players can purchase bulk supplies out of the stockpile using their digital wallet balance, with Gatherer specialization discounts applied directly at checkout.

---

## 5. Anti-AFK Nerfs and Active Resource Sourcing

Automated AFK farming invalidates cooperative play and causes severe economic hyperinflation. The mod suite implements systemic nerfs against automated farming and redistributes resources into active survival gameplay:

| Passive Farm Nerfed | Systemic Nerf Applied | Active Gameplay Replacement |
| :--- | :--- | :--- |
| **Automated Iron Farms** | Golem spawning restricted outside natural village events. | **Buffed Deep-Cave Ore Veins:** Deepslate iron and copper veins generate significantly denser clusters in perilous deep caves. |
| **Villager 1x1 Exploitation & Infinite Breeding** | Villagers only spawn in natural villages and cannot reproduce. Cramped booths halt restocks and hike prices. | **Explorer Relocation & Living Markets:** Seasoned Explorers capture and trade villagers as spawn eggs. Merchants require 3x3+ open roaming areas to restock. |
| **Static XP Mob Chutes** | Diminishing returns applied to mob kills within static chunks; drops and XP decay to zero if mobs are trapped without player combat engagement. | **Combat Speed & Accuracy XP:** Active, high-tempo combat against dangerous mobs awards massive Combat XP and currency milestones. |
| **Automated Raid Farms** | Bad Omen stacking and static raid funneling are rebalanced. | **Server-Wide Objectives & Support Crafting:** Gatherers and Crafters produce alchemical catalysts that multiply resource yields. |

### Living Villager Overhaul & Settlement Commerce
Rather than completely deactivating villager trading or tolerating dehumanizing 1x1 trading cells, the mod suite introduces a nuanced, immersive living villager ecosystem:

1. **Natural Village Spawning & Reproduction Suppression:**
   * Villagers only spawn in naturally generated world villages.
   * Villagers **do not reproduce**: breeding AI, love food consumption, and baby entity generation are strictly suppressed realm-wide.
2. **Explorer Relocation (Encapsulated Villagers):**
   * Seasoned Explorers (**Scouts** or players with **100+ Explorer XP**) can sneak-right-click an adult villager to encapsulate them into a tradable specimen egg.
   * **Intact Trades & Levels:** The encapsulated egg preserves the villager's exact profession, master level, experience, and custom trade recipes.
   * **Commerce:** Explorers can sell these specimens to town builders on the player-to-player market or deploy them into their own settlements by right-clicking the ground.
3. **Anti-1x1 Mobility Restocking & Captivity Surcharges:**
   * Villagers require open space to live, trade, and restock. The server continuously evaluates each merchant's horizontal 3x3 footprint at feet level.
   * **Cramped Booths (<4 Walkable Blocks or Boat/Cart Confinement):**
     * Trade restocking is **completely frozen**—exhausted offers cannot replenish.
     * All trades carry a steep **+10 emerald captivity distress surcharge**.
     * Merchants emit visual distress smoke particles.
   * **Free-Roaming Merchants (≥4 Walkable Blocks):**
     * Restocks occur smoothly according to their natural daily schedule.
     * Captivity surcharges are cleared, and happy villager particles appear upon roaming freely.
4. **Permanent Villager Slayer Surcharge & Bad Luck Curse:**
   * Slaying an innocent villager permanently increments the player's lifetime record of slain villagers.
   * **Realm-Wide Price Hike:** Merchants throughout the entire realm despise villager slayers. Every trade permanently costs an extra **+5 to +30 emeralds** (scaling with kill count).
   * **Curse of Unluck:** Villager slayers are afflicted with persistent **Unluck** and Bad Omen debuffs, reducing their rare loot chances in deep caves and dungeons.

### Repurposed Villages
Vanilla villages serve as **safe rest stops for weary explorers**:
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
| `combat_snuffers` | **Combat / Hunting** | Slay 3 light-snuffing elite mobs. | **300 coins** + **150 Combat XP** |

### Contract Guidelines
1. **Concurrency Limit:** Players may hold a maximum of **2 active contracts** simultaneously.
2. **Anti-Griefing Verification:** Harvest bounties only count natural blocks verified by the anti-laundering tracker; player-placed or recycled blocks are ignored.
3. **Automatic Progress Tracking:** Progress updates seamlessly in the background as you explore, harvest, and fight.

### Bounty Commands
* `/bounty list`: Browse all available community contracts and reward payouts.
* `/bounty accept <id>`: Accept a bounty contract into your active log.
* `/bounty status`: View your current active contracts and progress towards objectives.
* `/bounty claim <id>`: Collect your coin and Playstyle XP rewards upon completing an objective.
* `/bounty abandon <id>`: Drop an active contract to free up your concurrent contract slot.

---

## 7. Advancements & Rewards Progression

The **Frontier Commerce** advancement branch rewards players for active economic integration, trading, municipal liquidation, contract completion, civic philanthropy, and personal capital accumulation. 

Unlike conventional achievements, completing tiered economic milestones dispatches direct rewards:
* **Non-material digital currency** injected directly into player balances.
* **Playstyle Role XP** (Gatherer and Builder proficiencies).
* **Tangible Recovery Catalysts** ensuring essential gear preservation on death.
* **Server-Wide Broadcasts** honoring Master and Legendary market achievements.

### Economy Progression Tree

```
root (Frontier Commerce)
 ├── first_harvest (Pioneer Prospector) [Novice]
 │    ├── bulk_store_supplier (Municipal Supplier) [Journeyman]
 │    └── deep_prospector (Mineral Vein Monopolist) [Master]
 └── first_p2p_trade (Mercantile Handshake) [Novice]
      ├── public_consignor (Liquidity Provider) [Journeyman]
      ├── contract_complete (Mercenary Surveyor) [Journeyman]
      │    ├── bounty_veteran (Contract Syndicate) [Journeyman]
      │    └── contract_master (Guild Commissioner) [Master] ── broadcast!
      │         └── wealth_tycoon (Sovereign Merchant) [Legendary] ── broadcast!
      └── town_patron (Civic Benefactor) [Master]
           └── civic_philanthropist (Grand Civic Benefactor) [Master] ── broadcast!
```

### Tiered Milestones & Rewards Table

| Advancement ID | Display Title | Tier & Frame | Requirement & Description | Digital Payout | Playstyle XP | Tangible Rewards | Broadcast |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `economy/root` | **Frontier Commerce** | **Novice** (Task) | Establish presence in the frontier; unlocks the Commerce tree upon first joining the realm. | — | — | — | No |
| `economy/first_harvest` | **Pioneer Prospector** | **Novice** (Task) | Naturally harvest your first raw survival resource in the wild without player-placed laundering. | **+50 coins** | **+25 Gatherer XP** | — | No |
| `economy/first_p2p_trade` | **Mercantile Handshake** | **Novice** (Task) | Execute your first peer-to-peer trade or direct digital transfer (`/economy pay`) with another player. | **+75 coins** | **+30 Gatherer XP** | — | No |
| `economy/bulk_store_supplier` | **Municipal Supplier** | **Journeyman** (Task) | Liquidate naturally gathered resources into the server bulk store liquidation system. | **+150 coins** | **+60 Gatherer XP** | — | No |
| `economy/contract_complete` | **Mercenary Surveyor** | **Journeyman** (Task) | Complete and claim your first objective from the Community Bounty Board (`/bounty claim`). | **+200 coins** | **+75 Gatherer XP** | — | No |
| `economy/bounty_veteran` | **Contract Syndicate** | **Journeyman** (Goal) | Prove seasoned reliability by fulfilling 5 community bounty board contracts. | **+200 coins** | **+80 Gatherer XP** | **1x Recovery Catalyst** | No |
| `economy/public_consignor` | **Liquidity Provider** | **Journeyman** (Task) | Consign or purchase an unclaimed salvage item on the public 24-hour buyback market (`/buyback public`). | **+150 coins** | **+60 Gatherer XP** | — | No |
| `economy/contract_master` | **Guild Commissioner** | **Master** (Challenge) | Prove supreme reliability by completing and claiming 10 Community Bounty Board contracts. | **+500 coins** | **+150 Gatherer XP** | — | **Yes** |
| `economy/deep_prospector` | **Mineral Vein Monopolist** | **Master** (Challenge) | Naturally prospect and harvest 20+ non-player-placed deepslate ore veins below $Y < 0$. | **+400 coins** | **+150 Gatherer XP** | **1x Recovery Catalyst** | No |
| `economy/town_patron` | **Civic Benefactor** | **Master** (Goal) | Invest in communal municipal development by donating 500+ coins to a town treasury (`/town deposit`). | **+250 coins** | **+100 Builder XP** | **1x Recovery Catalyst** | No |
| `economy/civic_philanthropist` | **Grand Civic Benefactor** | **Master** (Challenge) | Endow communal infrastructure by investing 1,500+ cumulative coins into town treasuries. | **+450 coins** | **+180 Builder XP** | **2x Recovery Catalysts** | **Yes** |
| `economy/wealth_tycoon` | **Sovereign Merchant** | **Legendary** (Challenge) | Amass an off-inventory fortune of 5,000+ digital currency coins in your personal wallet. | **+1,000 coins** | **+250 Gatherer XP** | **3x Recovery Catalysts** | **Yes** |

### Anti-Exploit & Progression Guarantees
* **Lifetime De-duplication:** All advancement rewards are recorded persistently on your character. Re-triggering or resetting criteria will not re-award coins, playstyle XP, or catalysts.
* **Natural Harvest Verification:** Harvest-related advancements strictly require blocks verified as naturally generated.
* **Atomic Balances:** Coin payouts are processed atomically and recorded to the server audit ledger.

