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
