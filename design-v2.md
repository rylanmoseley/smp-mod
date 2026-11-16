# Minecraft Server Mod Suite: Technical Architecture and Systems Overview (v2.0)

This v2.0 specification consolidates the finalized rules, closed cross-system dependencies, and implementation guardrails for the custom multiplayer Minecraft server mod suite. Synthesizing the foundational Brainstorm Draft, Economy And Claims model, Unified Survival Design, and v2.0 system resolutions, the architecture re-engineers core survival loops to reward sustained social cooperation, purposeful construction, skilled combat, and active world exploration while eliminating solitary AFK automation and unrecoverable loss.

Across every subsystem, protective or anti-grind mechanics are paired with compensating sinks, hazards, or active-play requirements so reduced tedium preserves long-term progression.

---

## 1. Core Design Pillars and Closed Dependency Architecture

Rather than treating economy, claims, world generation, and combat as isolated mods, the architecture forms a **closed dependency graph** built on three pillars:
1. **Maximize social, constructive, and long-term multiplayer gameplay.**
2. **Eliminate low-reward grinding and unrecoverable death loss without removing consequence.**
3. **Rebalance building, gathering, exploring, and combat so every playstyle carries direct strategic value into the endgame.**

| Architectural Pillar | Primary Mechanisms Enforcing the Pillar | Cross-System Dependencies |
| :--- | :--- | :--- |
| **Social, Constructive & Long-Term Play** | Contiguous shared claims with pooled defensive buffs; proximity voice chat; server-wide objectives; cosmetic/decoration expansions; provenance-tracked Item Lore. | Shared buffs use individual Builder XP accounting; P2P trades update formal item ownership; server-wide objectives converge on the cooperative Ender Dragon. |
| **Low-Grind Economy & Fair Recovery** | Off-inventory currency; multi-tier advancements + P2P/server commerce; ownership-bound UUID buybacks (24h exclusive window $\rightarrow$ 1.25x public listing, ~30-day decay); bulk store for stackables; per-block natural-harvest unlocks. | `player_placed` metadata prevents shop/Silk-Touch laundering; high-tier resources depend on active caving, dungeons, and Support bonuses after AFK farm nerfs; public listing prevents camping the 30-day decay timer. |
| **Emergent Playstyle & Threat Balance** | Roles (DPS, Tank, Support, Scout) derived strictly from Playstyle XP ratios; Explorer-gated glider Elytra; dangerous Nether transit; light-avoidant AI + `destroys_light_on_aggro` mobs. | Combat XP rewards speed and accuracy; Nether hazards & Elytra gating protect Overworld biome-exploration buffs; claim-safe light snuffing protects Builder bases from `destroys_light_on_aggro` griefing. |

---

## 2. Non-Material Economy, Provenance-Tracked Buybacks, and Anti-AFK Supply Controls

### 2.1 Non-Material Currency, Liquidity Faucets, and Commerce
Server currency operates strictly as a non-material, off-inventory digital balance. Balances cannot be dropped, burned, or lost on death, ensuring players always retain reliable access to recovery and trading systems.

Long-term economic liquidity is sustained through four coupled channels:
* **One-Time Milestones:** Starter and progression advancements that onboard players and seed early capital.
* **Repeatable Play Cycles:** Tiered, repeatable milestones tied to active play (e.g., every $Y$ natural blocks harvested, $Z$ dungeons cleared, or active non-AFK playtime blocks).
* **Player-to-Player (P2P) Trading:** Direct item sales and player-run shops/stalls enabling specialization between builders, explorers, gatherers, and fighters.
* **Server-Run Shops & Bulk Store:** A centralized building-material shop (unlocked per block) alongside a server-managed bulk store fed by stackable death drops and server commerce.

### 2.2 Ownership-Bound UUID Buybacks, Theft Protection, and Liquidation Rules
Death recovery eliminates unrecoverable gear wipeouts while preserving meaningful economic stakes and an active item market. Every unique item (tools, armor, weapons, and any **named or enchanted item**) carries a persistent UUID, an explicit **Registered Owner**, a cumulative `death_count`, and a `last_owner_death_timestamp`.

#### Recovery Routing: Buybacks vs. Bulk Store
* **UUID Buyback Ledger:** All weapons, armor, tools, **named items, enchanted items**, and designated **high-value stackable exceptions** (e.g., rare dungeon catalysts, Netherite/high-tier consumables, or rare artifacts) route to the buyback ledger on death.
* **Bulk Store Routing:** Standard stackable items (common blocks, mundane arrows, ordinary food, low-tier drops) are routed into a **bulk store** upon death rather than cluttering individual buyback ledgers, feeding server/player commerce at baseline market rates.

#### Escalating Fee, Wall-Clock Decay, and 24-Hour Public Listing
* **Escalating Cost:** An item's base buyback cost scales upward with its owner's cumulative `death_count` on that item.
* **~30-Day Wall-Clock Decay:** Over wall-clock time since death, the item's escalated buyback fee decays smoothly until it sags all the way back to the item's **1.0x core value** after approximately **1 month (~30 days)**.
* **24-Hour Exclusive Window $\rightarrow$ 1.25x Public Listing:** For the first **24 hours** after death, only the registered owner can reclaim the item at the current buyback price. After 24 hours, any unclaimed item becomes **publicly listed at 1.25x the owner's current buyback price**.
  * *Systemic Synergy:* The 24-hour public listing creates urgent natural tension against camping the 30-day decay timer—if an owner tries to leave valuable gear unclaimed for weeks to wait out fees, other players can purchase it at a 25% markup once the first day passes.

#### Ownership, Theft Protection, and P2P Transfer
To prevent PvP griefing or theft from corrupting recovery ledgers:
* **Stolen Item Protection:** If an item is taken without a formal P2P purchase (theft/looting) and the thief dies or loses the item, buyback rights route exclusively back to the **original registered owner**, not the thief. Furthermore, the item's `death_count` multiplier **only increments when the registered owner is the one who died**, ensuring thieves cannot inflate an owner's recovery fees.
* **Formal P2P Ownership Transfer & Lore:** Completing a P2P sale formally transfers registered ownership to the buyer. Item Lore preserves full provenance alongside current ownership:
  > *Example Lore Format:* `Crafted by [Player A]` *(or `Looted by [Player A]`)* / `Owned by [Player B]`

### 2.3 Per-Block Resource Unlocks and `player_placed` Guardrails
The building-material shop specifically targets tedious bulk construction grind without bypassing progression:
* Players unlock purchasability (and optional deeper discount tiers) on non-precious blocks on a **per-block basis** by naturally harvesting threshold quantities.
* Precious ores, rare dungeon loot, and high-tier equipment remain **excluded** from the building shop.
* **Anti-Laundering Metadata (`player_placed`):** The server maintains a lightweight placed-block tracking flag. Breaking blocks placed by a player (including shop-bought or Silk-Touched blocks) **never counts** toward natural-harvest shop unlocks, Gatherer XP, or harvest advancements.

### 2.4 Expanded Anti-AFK Nerfs and Active-Play Sourcing
To protect both the currency economy and active progression from solitary AFK inflation:
* **Farm & Grinder Restrictions:** Automated iron farms, vanilla-style villager breeders/trading halls, raid farms, and static mob-drop XP chutes are nerfed or disabled (including chunk/radius diminishing returns on repetitive mob kills).
* **Active Replacement Channels:** Iron, redstone, XP, and top-tier enchantments (such as Mending) are redistributed into four active loops:
  1. **Buffed Deep-Cave Ore Veins:** Rewarding active caving over passive strip-mining or farm boxes.
  2. **Dungeons & Bosses:** Primary source of high-tier enchantment tomes, rare catalysts, and high-value stackables.
  3. **Revamped Advancements:** Injecting currency and milestone rewards for active exploration and gathering.
  4. **Support Crafting & Gathering Synergy:** Converting active Gatherer progression into XP/enchanting catalysts and resource efficiency.
* **Repurposed Villages:** With villager trading halls removed, vanilla villages are repurposed into **pre-lit safe rest stops for explorers, regional Server-Wide Objective hubs, and Item Lore / Cartography landmarks**.

---

## 3. Contiguous Land Claims and Construction-Driven Buffs

### 3.1 Contiguous Topology and Builder Chunk Scaling
Construction inside a **contiguous land claim** directly strengthens territorial defense. Requiring claimed chunks to remain contiguous prevents players from dropping disconnected micro-outposts across the world to farm stat bonuses, encouraging cohesive towns and fortified settlements. A player's maximum claimable chunk cap scales upward individually with their **Builder Playstyle Level**.

### 3.2 Pooled Defensive Buffs vs. Individual Builder XP
* **Pooled Claim Buffs:** Defensive stat buffs on shared claims scale from the combined qualifying construction built by all claim members, turning cooperative bases into shared strongholds.
* **Individual XP Isolation:** Builder XP (and therefore chunk caps and Tank role scaling) is tracked strictly per individual builder, preventing passive leeching off teammates' builds.

### 3.3 Variety-Weighted Counting and Claim-Locked Builder Flight
* **Anti-Spam Construction Scoring:** Claim buffs and Builder XP evaluate **net-placed blocks** (subtracting broken blocks) weighted by **block-palette variety with diminishing returns**. Mixing structural stone, timber, glass, and decorative blocks yields full progression, while low-effort cobblestone cubes hit steep diminishing caps.
* **Temporary Builder Flight:** Unlockable ~5-second temporary building flight (sustained by continuous block placement) is **strictly restricted to owned or shared contiguous claims** and automatically disabled upon entering combat—preserving it as a construction QoL tool without bypassing Overworld terrain, dungeons, or Elytra gating.

---

## 4. World Topology, Travel Economics, Light AI, and Rebalanced Elytra

### 4.1 Dangerous Nether Travel and Overworld Mobility Modifiers
* **Nether Restrictions:** Nether roof access is disabled entirely. Built Nether highways (ice boats, rails, bridges) are actively hazardous through **thermal track/ice instability**, **barrier-breaching Piglin and Ghast aggro** targeting transit infrastructure, and **anti-solo sprint scaling** that punishes high-speed solo commuters.
* **Overworld Biome Mobility:** Players gain progressive Overworld movement bonuses (e.g., faster traversal in plains after exploring $X$ plains chunks), rewarding mapped roads, villages, and regional exploration.

### 4.2 Explorer-Gated and Rebalanced Elytra
To ensure Endgame flight does not trivialize Overworld mobility investments or Nether danger:
* **Hard Exploration Threshold:** Elytra glide deployment is gated behind a high **Explorer Playstyle milestone** (requiring broad unique-chunk and multi-biome discovery). Unqualified players cannot deploy wings even if handed a pair by a teammate.
* **Rebalanced Rocket & Flight Model:** Mid-air vanilla firework rocket boosting is disabled (or replaced with a scarce, long-cooldown alchemical launch catalyst). Elytra functions primarily as a **momentum glider**—rewarding high-altitude terrain, Builder launch towers, and Scout efficiency bonuses—and suffers **Nether flight lockout / thermal penalties** so dimensional transit stays dangerous.

### 4.3 Light-Avoidant AI, Dark-Zone Escalation, and `destroys_light_on_aggro`
* **Spatial Light AI:** Un-aggroed Overworld mobs actively avoid illuminated zones, making lit roads and claimed towns reliable safe corridors. Mobs operating in darkness become stronger, smarter, and more varied.
* **`destroys_light_on_aggro` Attribute:** Select mobs spawn with an explicit `destroys_light_on_aggro` trait—at low probability in wilderness caves and **much higher spawn weight inside dungeons**. Once aggroed onto a player, these mobs actively target and extinguish nearby light sources, collapsing passive torch-spam tactics.
* **Claim-Safe Anti-Griefing Guardrail:**
  * **Wilderness & Dungeons:** `destroys_light_on_aggro` mobs break/drop temporary lights (torches, campfires) and temporarily snuff durable lights.
  * **Inside Contiguous Claims:** These mobs **cannot destroy blocks**; instead, they apply a temporary **20–30 second snuff/shroud aura** that suppresses light output without damaging Builder infrastructure.

---

## 5. Ratio-Driven Cooperative Combat Roles and Endgame Encounter

### 5.1 Emergent Role Derivation (Zero Elective Selection)
Players do **not** pick a class, swap loadouts at an altar, or toggle elective role stances. Instead, a player's combat profile across the four cooperative roles (**DPS, Tank, Support, Scout**) is calculated dynamically from the **ratio of their accumulated Playstyle XP** across the four survival tracks:

$$\text{Role Affinity}_i = f\!\left(\frac{\text{XP}_i}{\text{XP}_{\text{Combat}} + \text{XP}_{\text{Builder}} + \text{XP}_{\text{Gatherer}} + \text{XP}_{\text{Explorer}}}\right)$$

| Combat Role | Paired Playstyle XP | Emergent Combat Scaling & Battlefield Identity |
| :--- | :--- | :--- |
| **DPS** | **Combat XP** | Scales damage output, armor penetration, and burst execution windows against dark-zone elites and boss punish phases. |
| **Tank** | **Builder XP** | Translates construction defense into frontline mitigation, portable cover/shielding, and threat control (pulling `destroys_light_on_aggro` elites off backline lights). |
| **Support** | **Gatherer XP** | Converts gathering mastery into team healing/sustain, consumable efficiency, enchantment/repair catalysts, and **Warded Light flares** resistant to `destroys_light_on_aggro` snuffing. |
| **Scout** | **Explorer XP** | Projects biome-exploration mobility into combat agility, End Crystal/objective traversal, dark-zone threat/trap detection, and Elytra glide efficiency. |

Because role potency flows naturally from what a player actually spends time doing, specialists develop crisp, unmistakable role identities while balanced players emerge as flexible hybrids—without hard locks that trap players forever if their playstyle focus shifts over time.

### 5.2 Cooperative Ender Dragon Encounter
The massively upgraded Ender Dragon serves as the centerpiece cooperative endgame test integrating proximity voice chat and server-wide objectives: **Tanks** absorb breath and dive phases, **Scouts** navigate pillars and crystal mechanics, **Support** maintains team sustain and warded visibility, and **DPS** specialists capitalize on burst windows.

---

## 6. Playstyle XP Systems, Advancements, and Scoped Features

### 6.1 The Four Playstyle XP Tracks
* **Combat XP (Speed & Accuracy Weighted):** Combat XP evaluates **encounter speed (time-to-kill / combat tempo)** and **accuracy (hit precision, crits, and low whiff rate)** scaled by mob tier—paired with anti-grinder chunk decay. Fast, accurate combat against dangerous dark-zone or dungeon threats yields high XP, whereas slow corner-cheesing or trapped-mob farming yields minimal gain.
* **Builder XP:** Awarded individually from net variety-weighted block placement within contiguous claims.
* **Gatherer XP:** Awarded strictly from harvesting naturally generated (`!player_placed`) ores, crops, timber, and rare nodes, with higher multipliers in deep caves and perilous zones.
* **Explorer XP:** Awarded from discovering unique chunks, mapping new biomes, locating dungeons/villages, and completing distant expeditions.

### 6.2 Revamped Advancement System Contracts
The advancement tree anchors two system-wide contracts:
1. **Economic Minting:** Feeds the non-material currency supply through one-time progression milestones and repeatable active-play cycles.
2. **Progression Gating:** Tracks natural-harvest thresholds for shop unlocks/discounts and verifies multi-biome chunk thresholds for Elytra unlocking.

### 6.3 Social Infrastructure, Item Lore, and Scoped Decisions
* **Active Social Systems:** Proximity voice chat, server-wide cooperative objectives, expanded cosmetic/decoration options, and rich **Item Lore** (tracking origin `Crafted by`/`Looted by`, current `Owned by`, and recovery/combat history).
* **Item Variation:** Included in lightweight form—subtle forge/dungeon roll variations paired directly with Item Lore so unique gear carries real identity worth recovering through buybacks and trading.
* **Weather & Seasons:** **Dropped from scope** to keep world light/dark AI, travel modifiers, and gathering balance predictable and tightly tuned.

---

## 7. Finalized Architectural Rules and Tuning Matrix

All high-level architectural conflicts are resolved. Implementation and playtesting focus strictly on calibrating the numeric constants listed below:

| Subsystem | Finalized Architecture Rule | Remaining Numeric Tuning Constants |
| :--- | :--- | :--- |
| **Economy & Commerce** | Non-material currency; 1-time + repeatable advancements; P2P trading; building shop + bulk store. | Advancement payout amounts; repeatable milestone thresholds; P2P/shop tax rates. |
| **Death Recovery & Lore** | Named/enchanted + high-value stackables $\rightarrow$ UUID buyback; standard stackables $\rightarrow$ bulk store; 24h exclusive $\rightarrow$ 1.25x public; ~30d wall-clock decay to 1.0x; theft protects owner & `death_count`; P2P transfers ownership + Lore. | Exact per-death multiplier step; whitelist of high-value stackable exceptions; core valuation table. |
| **Anti-AFK & Sourcing** | `player_placed` flag; nerfed iron/villager/raid/XP farms; buffed deep caves, dungeons, Support catalysts, and repurposed Villages. | Chunk/radius grinder decay curve; deep-vein density multipliers; dungeon tome drop rates. |
| **Claims & Building** | Contiguous claims; pooled defensive buffs vs. individual Builder XP; variety-weighted net blocks; claim-locked ~5s flight (no combat). | Chunk-cap per Builder level; variety diminishing-return curve; flight block-cadence grace timer. |
| **World, AI & Elytra** | No Nether roof; hazardous highways; Explorer-gated glider Elytra (no rocket spam, Nether lockout); `destroys_light_on_aggro` (claim snuff-only vs. wild/dungeon break). | Explorer chunk/biome gate for Elytra; wilderness vs. dungeon `destroys_light_on_aggro` spawn %; snuff duration (20–30s). |
| **Roles & XP** | Zero elective selection—DPS/Tank/Support/Scout derived strictly from XP ratios; Combat XP factors speed + accuracy; Weather/Seasons dropped. | Exact ratio-to-ability scaling curve; speed/accuracy Combat XP formula weights; Dragon phase tuning. |