# World Navigation & Survival Mechanics

Exploring and traversing the world has been fundamentally re-engineered. Safe transit corridors must be earned, Nether highways carry real environmental danger, and endgame flight functions as an aerodynamic glider rather than infinite rocket-propelled creative flight.

---

## 1. Dangerous Nether Travel & Highway Economics

The Nether serves as an intensely hostile dimension rather than a trivial sub-minute bypass for Overworld distance.

### Total Nether Roof Lockdown
* **Bedrock Ceiling Enforced:** Access above the Nether roof ($Y \ge 128$) is completely blocked.
* Any player attempting to glitch through bedrock or ender-pearl onto the roof is immediately teleported back down to a safe elevation below the ceiling, accompanied by a thermal turbulence notification.
* Building above the Nether roof is strictly disabled.

### Nether Transit Hazards
Built Nether highways (ice boat lanes, high-speed rail lines, and elevated bridges) face dynamic environmental hazards:
* **Thermal Track & Ice Instability:** High-speed ice-boat traversal suffers from thermal friction instability. Continuous sustained speed can cause ice tracks to destabilize or melt if unmaintained.
* **Aggressive Infrastructure Aggro:** Piglins and Ghasts actively detect and target high-speed transit bridges, breaching protective barriers with explosive attacks.
* **Anti-Solo Sprint Scaling:** High-speed solo commuting across the Nether generates an accumulating heat/fatigue penalty, encouraging group travel or fortified staging stations.

---

## 2. Overworld Biome Mobility Modifiers

To make mapping and exploring the Overworld rewarding:

* **Progressive Biome Mastery:** As you map and explore unique chunks within a specific biome (e.g., Plains, Savanna, Deserts), you unlock progressive movement speed bonuses within that biome type:
  * *100 Chunks Mapped:* $+10\%$ movement speed in that biome.
  * *300 Chunks Mapped:* $+20\%$ movement speed + reduced hunger consumption.
* **Mapped Roads & Waypoints:** When combined with player-built highways and bridges, biome mastery turns familiar Overworld territories into rapid, reliable travel corridors.

---

## 3. Explorer-Gated Momentum Elytra

Vanilla Elytra flight with firework rocket spam trivialize Overworld terrain, Nether hazards, and community roads. The SMP Mod Suite replaces this with an **Explorer-Gated Momentum Glider**:

```
Explorer XP Progression
      │
      ├── < Threshold (500 Chunks): ELYTRA LOCKED
      │   • Player cannot deploy wings (even if handed Elytra).
      │   • Prompt: "Gliding requires Explorer milestone (X / 500 chunks mapped)."
      │
      └── ≥ Threshold: MOMENTUM GLIDER DEPLOYMENT UNLOCKED
          │
          ├── Mid-Air Firework Rocket Spam: DISABLED
          │   • Elytra functions as a momentum glider.
          │   • Rewarding high-altitude terrain, Builder launch towers, & Scout bonuses.
          │
          └── Dimensional Lockout: NETHER FLIGHT PROHIBITED
              • Nether thermal updrafts collapse wing lift, preserving Nether travel danger.
```

### Key Elytra Rules
1. **Hard Exploration Milestone:** Deployment is gated behind discovering a substantial threshold of unique chunks (default 500) and multiple distinct biomes. Novices cannot fly until they have explored the world on foot.
2. **Zero Mid-Air Rocket Spam:** Right-clicking firework rockets while gliding is disabled. Elytra functions as an aerodynamic momentum glider. Launching from high mountain peaks, Builder launch catapults, or Scout updrafts rewards genuine flight skill and kinetic energy management.
3. **Nether Flight Lockout:** Gliding inside the Nether is disabled due to thermal dimensional turbulence, ensuring Nether highways remain the primary method of transit.

---

## 4. Spatial Light AI & Dark-Zone Escalation

Lighting is no longer just a binary spawn check; it fundamentally alters hostile mob behavior and territorial safety:

### Spatial Light Avoidance AI
* **Un-Aggroed Avoidance:** Monsters wandering in the Overworld actively pathfind away from illuminated areas.
* **Safe Corridors:** Thoroughly lit roads, claimed towns, and fortified pathways become reliable safe havens where mobs will not wander unless provoked.

### Dark-Zone Escalation
* Mobs that spawn and remain in deep, unlit caves or night wilderness grow progressively more dangerous:
  * Escalated attack damage and armor penetration.
  * Faster movement speed and coordinated pack pathfinding.
  * Increased spawn weight of dark-zone elite variants.

---

## 5. The `destroys_light_on_aggro` Trait

Select hostile mobs spawn with an explicit trait: `destroys_light_on_aggro` (rare in wilderness caves, frequent inside dungeons and deep trial chambers).

```
Hostile Mob Aggroes onto Player
      │
      ├── Wilderness / Dungeons: DESTRUCTIVE SNUFF
      │   • Mob targets and physical breaks fragile lights (torches, campfires).
      │   • Forces active combat repositioning; collapses passive torch-spam.
      │
      └── Inside Contiguous Claim: CLAIM-SAFE SHROUD AURA
          • Mobs CANNOT break blocks inside claims.
          • Applies a temporary 20–30s light-snuff aura, suppressing light output.
          • Infrastructure is 100% protected; lights restore after aura fades.
```

* **Anti-Torch Cheesing:** When aggroed, these elites actively target and extinguish nearby torches, collapsing passive torch-spam strategies.
* **Claim-Safe Protection:** Inside claimed player towns, this trait switches to a temporary shroud aura, guaranteeing that Builder infrastructure is never damaged or destroyed.
* **Support Counterplay:** Support specialists can deploy **Warded Light Flares** that are completely immune to mob snuffing!

---

## 6. Repurposed Vanilla Villages

With automated villager breeding and trading halls disabled, vanilla villages serve new social roles:
* **Pre-Lit Sanctuaries:** Immune to dark-zone mob escalation, providing safe rest stops for traveling caravans.
* **Objective Hubs:** Host regional Server-Wide Objectives where players gather to initiate community quests.
* **Item Lore Landmarks:** Serve as cartographic landmarks and trading posts for formal P2P commerce.

---

## 7. Permanent Warded Lanterns

While standard torches and lanterns can be snuffed or broken by dark-zone hostiles, players can craft permanent **Warded Lanterns** (`smpmod:warded_lantern`):

```
 Crafting Recipe:
      [ Amethyst Shard ]
 [ Amethyst Shard ] [ Lantern ] [ Amethyst Shard ]  ➔  [ Warded Lantern ]
      [ Amethyst Shard ]
```

* **Max Illumination:** Emits constant light level **15**.
* **Total Snuff & Destruction Immunity:** Hostile mobs possessing the `destroys_light_on_aggro` trait cannot target, break, or snuff Warded Lanterns in the wilderness or inside claims.
* **Infrastructure Security:** Essential for fortifying frontier outposts, underground transit tunnels, and deep cavern mining hubs.

---

## 8. Caravan & Highway Transit

Active land travel remains the bedrock of server presence. The Caravan system incentivizes organized group expeditions, pack animals, and regional road networks.

### Forming an Active Caravan
A player is recognized as part of a caravan when:
1. **Multi-Player Group:** Traveling within **24 blocks** of another player, **OR**
2. **Pack Animal Convoy:** Riding or leading leashed pack animals (Llamas, Donkeys, Mules, Horses, Camels), **OR**
3. **Railway Transit:** Riding a minecart along tracks.

### Caravan Traversal Benefits
* **Highway Speed Boost:** Traveling along recognized roads (dirt paths, cobblestone, stone bricks, packed mud) or rails grants **Speed I** (+20% movement speed).
* **Town Paved Highway Synergy:** Passing through a settlement with the `PAVED_HIGHWAY_NETWORK` civic upgrade upgrades this buff to **Speed II** (+40% movement speed).
* **Endurance & Hunger Conservation:** Caravan members suffer **50% less hunger exhaustion** while on roads.
* **Nether Convoy Immunity:** Caravans completely negate the Nether solo-sprint thermal exhaustion penalty, allowing safe group commuting.

---

## 9. Proximity Voice Chat Acoustic Hooks

The mod suite includes dedicated acoustic simulation hooks designed to seamlessly integrate with proximity voice chat mods (such as Simple Voice Chat and Plasmo Voice) and enhance survival immersion:

### Town Alarm Bell Acoustic Broadcast
* When a village or settlement Bell (`Blocks.BELL`) is struck or rung, it triggers a high-priority **Acoustic Broadcast** echoing up to **96 blocks** away.
* All players in range hear resonant directional chimes and receive an urgent defender alert in chat: `[Town Alarm] Bell rung at [X, Y, Z]! Assemble defenders.`
* Hostile mobs within 32 blocks of the alarm bell are tagged with the **Glowing** effect for 10 seconds, exposing sneaky nocturnal raiders.

### Acoustic Dampening & Cavern Reverb
* **Wool & Curtain Occlusion:** Calculates environmental dampening (up to **85% sound muffling**) in rooms insulated with wool blocks, carpets, or curtains, suppressing footstep noise and speech propagation.
* **Cavernous Reverb:** Dynamically calculates acoustic echo in deep underground caverns ($Y < 0$).
* **Nether Thermal Distortion:** Models thick, reverberant atmospheric distortion across the Nether's scorching wastes.

---

## 10. Unified Generated Structure Revamps & Complex Dungeons

To eliminate repetitive vanilla box dungeons and elevate subterranean exploration, the mod features a **Unified Structure Revamp Integration Engine** ([StructureDelveManager.kt](file:///home/sandbox/smp-mod/src/main/kotlin/dev/rmoseley/smpmod/world/StructureDelveManager.kt)). It links subterranean overhauls (**YUNG's Better Dungeons**, Catacombs, Fortresses) and colossal rogue-like citadels (**When Dungeons Arise**, Bandit Towers, Foundries, Monasteries) directly into the core gameplay loops:

```
                  Player Crosses into Complex Dungeon Piece
                                      │
           ┌──────────────────────────┼──────────────────────────┐
           ▼                          ▼                          ▼
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│  Combat Escalation   │   │ Exploration Discovery│   │ Masterwork & Catalysts│
│                      │   │                      │   │                      │
│ • destroys_light     │   │ • Entry Alert Toast  │   │ • 60%+ Masterwork    │
│   chance jumps to    │   │ • +150 Explorer XP   │   │   affix roll chance  │
│   35% - 50%          │   │ • Scout Sensory      │   │ • Guaranteed high-tier│
│ • 48m mob pathing    │   │   spawner radar      │   │   enchantment tomes  │
│ • Spatial light      │   │ • Increments         │   │ • 60% Recovery       │
│   avoidance AI       │   │   dungeonsCleared    │   │   Catalyst drop      │
└──────────────────────┘   └──────────────────────┘   └──────────────────────┘
```

### 1. Combat & Light Destruction Escalation
* **Elevated Threat:** Inside tagged complex dungeon structures (`#smpmod:complex_dungeons`), hostile mobs spawn with the `destroys_light_on_aggro` trait at a **35% to 50% rate**.
* **Tactical Illumination:** Fragile torches and campfires are systematically targeted and extinguished, making **Support Warded Light Flares** and **Permanent Warded Lanterns** essential for surviving multi-level catacombs.

### 2. Exploration Milestones & Scout Radar
* **Discovery Toast & Rewards:** Stepping into an uncharted dungeon complex displays a HUD banner (`[Dungeon Delve] Entered [Structure Name]`) and immediately awards **+150 Explorer XP**.
* **Scout Sensory Radar:** Scouts detect hidden mob spawners, trial spawners, and ambush traps up to 24 blocks away, highlighting them through walls.
* **Vault Clearance:** Conquering spawner rooms or defeating elite dungeon encounters increments the player's lifetime `dungeonsCleared` statistic, granting **+100 Combat XP** and **+100 Explorer XP**.

### 3. Loot Sinks, Masterwork Gear & Catalysts
* **Masterwork Vault Chests:** Loot generated inside deep dungeon vaults has a **60%+ chance** to roll Masterwork Affixes (`Keen`, `Sturdy`, `Titanic`, `Aerodynamic`, etc.).
* **Recovery Catalysts:** Clearing dungeon vaults has a **60% chance** to yield a rare **Recovery Catalyst**, providing critical discounts at Town Buyback Terminals.
* **Non-AFK Mending Tomes:** Guaranteed high-tier enchantment books (Mending, Unbreaking III, Silk Touch) replace vanilla villager trading halls.

---

## 11. Advancements & Rewards Progression: Wilds & Expeditions Tree

The SMP Mod Suite introduces a deep, tiered progression tree for world exploration, transit, and survival navigation managed by [WorldExplorationAdvancements.kt](file:///home/sandbox/smp-mod/src/main/kotlin/dev/rmoseley/smpmod/advancements/WorldExplorationAdvancements.kt) and dispatched via [AdvancementRewardManager.kt](file:///home/sandbox/smp-mod/src/main/kotlin/dev/rmoseley/smpmod/advancements/AdvancementRewardManager.kt).

Each advancement represents a meaningful milestone in expanding server frontiers—from initial biome scouting and deep cavern descent to Nether caravans, paved highway logistics, and aerodynamic glider deployment. Rewards provide non-material digital currency, Playstyle Role XP, and tangible **Recovery Catalysts** stored across persistent player attachments.

### Progression Tree Architecture

```
                                  [exploration/root]
                                 Wilds & Expeditions
                                     (Root Tab)
                                          │
                    ┌─────────────────────┴─────────────────────┐
                    ▼                                           ▼
       [exploration/into_the_wilds]                 [exploration/deep_caverns]
              Frontier Scout                             Abyss Descender
                 (Novice)                                    (Novice)
                    │                                           │
         ┌──────────┼──────────┐                     ┌──────────┴──────────┐
         ▼          ▼          ▼                     ▼                     ▼
[sanctuary_ringer] [nether] [highway_caravan] [dungeon_delver] [subterranean_voyager]
Sanctuary Watchman  Convoy    Caravan Master   Dungeon Delver   Subterranean Mariner
   (Journeyman)  (Journeyman)  (Journeyman)     (Journeyman)       (Journeyman)
                                    │                │
                                    ▼                ▼
                     [master_cartographer]   [ancient_archeologist]
                      Realm Cartographer       Relics of the Deep
                           (Master)                 (Master)
                              │
     ┌────────────────────────┼────────────────────────┐
     ▼                        ▼                        ▼
[momentum_glider] [transcontinental_pioneer]   [Server Broadcast]
Master of the Winds Vanguard of the Frontier
    (Master)                 (Master)
       │                        │
       ▼                        ▼
[alpine_summit]        [Server Broadcast]
Peak of the World               │
 (Journeyman)                   ▼
                [exploration/abyssal_sovereign]
                        Horizon Pioneer
                          (Legendary)
                                │
                                ▼
                        [Server Broadcast]
```

### Detailed Advancement Progression & Reward Tiers

| Advancement ID | Display Title | Tier | Frame | Criteria & Milestones | Economy Reward | Playstyle XP | Tangible Reward | Server Broadcast |
|---|---|---|---|---|---|---|---|:---:|
| `exploration/root` | **Wilds & Expeditions** | Novice | Task | Join or step beyond initial spawn settlement borders | — | — | — | No |
| `exploration/into_the_wilds` | **Frontier Scout** | Novice | Task | Venture into uncharted lands and discover your 1st unique biome | **+50 Coins** | **+30 Explorer XP** | — | No |
| `exploration/deep_caverns` | **Abyss Descender** | Novice | Task | Descend below elevation $Y \le 0$ into deepslate caverns | **+75 Coins** | **+40 Explorer XP** | — | No |
| `exploration/subterranean_voyager` | **Subterranean Mariner** | Journeyman | Goal | Navigate a boat along a subterranean river below elevation $Y < 0$ in Tectonic cavern systems | **+200 Coins** | **+75 Explorer XP** | — | No |
| `exploration/sanctuary_ringer` | **Sanctuary Watchman** | Journeyman | Task | Ring a landmark sanctuary bell at night to scan darkness and alert the settlement | **+150 Coins** | **+60 Explorer XP** | — | No |
| `exploration/dungeon_delver` | **Deep Dungeon Delver** | Journeyman | Goal | Infiltrate a tagged complex dungeon (`#smpmod:complex_dungeons`) or trial chamber | **+200 Coins** | **+80 Explorer XP** | **1x Recovery Catalyst** | No |
| `exploration/nether_convoy` | **Thermal Convoy** | Journeyman | Goal | Traverse the Nether in an active caravan ($Y < 128$) without roof glitching | **+150 Coins** | **+75 Explorer XP** | — | No |
| `exploration/highway_caravan` | **Highway Caravan Master** | Journeyman | Goal | Lead pack animals or active caravan on recognized roads or rail corridors | **+150 Coins** | **+60 Explorer XP** | — | No |
| `exploration/ancient_archeologist` | **Relics of the Deep** | Master | Challenge | Excavate ancient suspicious gravel/sand or unlock an ominous trial vault in ruins | **+350 Coins** | **+140 Explorer XP** | **1x Recovery Catalyst** | No |
| `exploration/master_cartographer` | **Realm Cartographer** | Master | Challenge | Map **250+ unique chunks** across at least **10 distinct biomes** | **+350 Coins** | **+150 Explorer XP** | **1x Recovery Catalyst** | No |
| `exploration/momentum_glider` | **Master of the Winds** | Master | Challenge | Unlock and deploy aerodynamic Elytra glider flight via exploration milestones | **+400 Coins** | **+180 Explorer XP** | **1x Recovery Catalyst** | **Yes (Server-Wide)** |
| `exploration/alpine_summit` | **Peak of the World** | Journeyman | Goal | Scale an alpine summit above $Y \ge 220$ in Tectonic mountain terrain and launch an unboosted momentum glide | **+250 Coins** | **+100 Explorer XP** | **1x Recovery Catalyst** | No |
| `exploration/transcontinental_pioneer` | **Vanguard of the Frontier** | Master | Challenge | Venture over 5,000 blocks away from world spawn across diverse continental biomes | **+450 Coins** | **+180 Explorer XP** | **1x Recovery Catalyst** | **Yes (Server-Wide)** |
| `exploration/abyssal_sovereign` | **Horizon Pioneer** | Legendary | Challenge | Map **1,000+ unique chunks** across the frontier and conquer deep structures | **+1,000 Coins** | **+300 Explorer XP** | **3x Recovery Catalysts** | **Yes (Server-Wide)** |

### System Integration & Reward Mechanics

1. **Anti-Duplication & Lifetime Persistence:**
   - Every advancement claim is validated against `PLAYER_MILESTONES` (`claimedMilestones` set). Once claimed, rewards cannot be duplicated or farmed.
2. **Economic Liquidity Injection:**
   - Currency is credited directly to the player's non-material bank balance via `EconomyManager.credit(player, payout, TransactionType.MILESTONE_STARTER)`.
3. **Continuous Playstyle Role XP:**
   - Explorer Playstyle XP directly elevates a player's Scout capabilities, increasing sensory radar range, speed boosts in mapped biomes, and momentum glider glide efficiency.
4. **Tangible Recovery Catalysts:**
   - High-tier achievements award physical **Recovery Catalysts** (`smpmod:recovery_catalyst`). These rare artifacts are essential for crafting and securing item buybacks at Town Buyback Terminals, subsidizing equipment recovery after death in hostile zones.
5. **Server-Wide Prestige Announcements:**
   - Master and Legendary accomplishments (`Master of the Winds` and `Horizon Pioneer`) broadcast gold-accented server-wide announcements to celebrate pioneer achievements across the community.

---

## 12. Tectonic World Generation & Alpine Transits

To provide a vast, dramatic, and exploration-worthy canvas for the SMP Mod Suite, the server integrates **Tectonic** (v3 Everest Update) via **Lithostitched**. Tectonic completely transforms terrain generation while remaining fully compatible with vanilla structures, biome tags, and modded dungeon revamps.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Tectonic Worldgen Integration                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
     ┌──────────────────────────────┼──────────────────────────────┐
     ▼                              ▼                              ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│  Monumental Peaks    │ │  Alpine Pass Transit │ │  Underground Rivers  │
│      (Y: 200-300)    │ │      (Y >= 140)      │ │   & Deep Continents  │
│                      │ │                      │ │                      │
│ • Natural Glider     │ │ • Caravan Freezing   │ │ • Subterranean Boat  │
│   Launch Ramps       │ │   Immunity on Roads  │ │   Navigation         │
│ • +4% Alpine Thermal │ │ • Mount Frostbite    │ │ • Oceanic Trade      │
│   Updraft & Speed    │ │   Protection         │ │   Corridors          │
│ • "Peak of the       │ │ • Powder Snow Hazard │ │ • Vast Coastal       │
│   World" Milestone   │ │   Mitigation         │ │   Settlements        │
└──────────────────────┘ └──────────────────────┘ └──────────────────────┘
```

### Monumental Mountain Ranges & Launch Ramps
* **Elevation Scaling:** Mountain peaks naturally ascend into the skybox at elevations from $Y: 220$ up to $Y: 300+$.
* **Natural Glider Launchpads:** Because mid-air firework rocket boosting is disabled, these monumental summits function as natural launch towers. Scaling a mountain peak rewards players with unobstructed glide trajectories spanning multiple kilometers across the continent.
* **Alpine Thermal Updrafts ($Y \ge 200$):** When deploying Elytra glide at high alpine altitudes:
  * Players receive a passive **+4% horizontal momentum preservation** and slight vertical updraft boost.
  * Aerodynamic snowflake and cloud vapor particles trail the glider's wings.
  * Awards the **"Peak of the World"** (`exploration/alpine_summit`) milestone (+250 coins, +100 Explorer XP, 1x Recovery Catalyst).

### Alpine Mountain Pass Caravans ($Y \ge 140$)
* **Pass Hazards:** Tectonic mountain passes feature intense blizzard hazards, freezing powder snow drifts, and cold attrition that threaten pack animals and solo travelers.
* **Caravan Road Endurance:** When leading an organized caravan (multi-player convoy, leashed pack horses/donkeys/llamas, or camels) along recognized paved highway blocks (`BiomeMobilityManager.isStandingOnRoad`) or rail corridors through mountain passes:
  * Travelers and mounts receive **complete freezing immunity** (`ticksFrozen = 0`).
  * Pack animals and mounts are protected against freezing damage in powder snow drifts along the paved corridor.
  * Players maintain their caravan Speed and reduced hunger exhaustion benefits, ensuring crucial overland trade routes through alpine passes remain viable year-round.

### Underground Rivers & Continental Ocean Corridors
* **Subterranean River Networks:** Tectonic generates seamless underground watercourses flowing through massive cave systems, enabling boat navigation beneath continental landmasses.
* **Vast Continents & Deep Oceans:** Continents are separated by expansive oceans thousands of blocks wide, making boat travel, harbor settlements, and coastal highway infrastructure vital components of global trade.


