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
