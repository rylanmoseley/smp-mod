# Emergent Roles & Combat Progression

The combat system breaks away from traditional RPG mods. There are **zero elective class menus**, no arbitrary stat-point grinding, and no permanent lockouts. Instead, your combat role profile flows naturally and continuously from how you actually spend your time playing survival.

---

## 1. Emergent Dynamic Role Derivation

A player's combat specialization across the four cooperative roles (**DPS, Tank, Support, Scout**) is calculated mathematically from the **ratio of their accumulated Playstyle XP**:

$$\text{Role Affinity}_i = f\!\left(\frac{\text{XP}_i}{\text{XP}_{\text{Combat}} + \text{XP}_{\text{Builder}} + \text{XP}_{\text{Gatherer}} + \text{XP}_{\text{Explorer}}}\right)$$

```
                               Surplus Playstyle XP
                                        │
           ┌────────────────────────────┼────────────────────────────┐
           ▼                            ▼                            ▼                            ▼
     [ Combat XP ]               [ Builder XP ]              [ Gatherer XP ]              [ Explorer XP ]
           │                            │                            │                            │
           ▼                            ▼                            ▼                            ▼
      DPS ROLE                     TANK ROLE                   SUPPORT ROLE                  SCOUT ROLE
 • Burst execution            • Frontline mitigation        • Team heal & sustain         • Agility & traversal
 • Armor penetration          • Portable cover & shielding  • Consumable efficiency       • Trap & threat detection
 • Dark-zone punishes         • Threat taunt & light guard  • Warded Light flares         • Crystal navigation
```

* **Natural Specialization:** Spend hundreds of hours building intricate stone fortresses? You naturally emerge as a heavily fortified **Tank**. Spend your time caving, gathering resources, and farming? You become a vital **Support** sustain anchor.
* **Organic Hybrid Flexibility:** If your focus shifts over time—say, an architect who takes up adventuring—your ratios adjust smoothly. There are no penalty fees or altar respec rituals required.
* **Inspect Your Role:** Run `/role profile` at any time to view your current ratios, primary affinity, and active attribute buffs.

---

## 2. The Four Playstyle XP Tracks

How you earn XP in each track:

### 1. Combat XP (Tempo & Accuracy Weighted)
Combat XP is not farmed by standing next to a 1-hit zombie chute. It evaluates active combat performance:
* **Encounter Speed (Tempo):** Faster Time-To-Kill (TTK) against threatening mobs yields higher XP multipliers. Slow corner-cheesing yields minimal XP.
* **Hit Precision & Accuracy:** Landing critical hits, precision sweeps, and maintaining a low whiff/miss rate amplifies XP gains.
* **Mob Tier & Anti-Grinder Decay:** Tougher dark-zone elites award substantial XP, but killing repetitive mobs in the same chunk suffers rapid diminishing returns.

### 2. Builder XP
* Awarded individually from net variety-weighted block placement within claimed contiguous settlements.
* Evaluates palette diversity (mixing timber, masonry, and decor), rewarding creative effort.

### 3. Gatherer XP
* Awarded strictly from harvesting naturally generated world blocks (`!player_placed`).
* Deep-cave ore veins and rare dangerous nodes award significantly higher multipliers.

### 4. Explorer XP
* Awarded from discovering unique chunks, mapping distinct biomes, locating uncharted villages, and conquering distant structures.

---

## 3. The Four Battlefield Identities

| Combat Role | Paired XP Track | Battlefield Scaling & Unique Abilities | Synergistic Masterwork Affixes |
| :--- | :--- | :--- | :--- |
| **DPS** | **Combat XP** | • **Burst Execution:** Scales attack damage and critical multipliers.<br>• **Armor Penetration:** Punishes heavily armored dark-zone elites.<br>• **Vulnerability Windows:** Deals massive burst damage during boss stagger phases. | **Keen** (+10% Crit Damage)<br>**Precise** (+15% Hit Precision & Tempo XP) |
| **Tank** | **Builder XP** | • **Mitigation:** Reduces incoming physical and explosive damage by up to $50\%$.<br>• **Cover & Shielding:** Deploys temporary barricade shields for teammates.<br>• **Threat Control:** Actively taunts and pulls `destroys_light_on_aggro` mobs off backline lights and vulnerable teammates. | **Titanic** (+10% Knockback Resist, +5% Armor)<br>**Sturdy** (+25% Durability) |
| **Support** | **Gatherer XP** | • **Team Sustain:** Radiates an out-of-combat rejuvenation aura for nearby allies.<br>• **Consumable Efficiency:** Multiplies potion durations and food saturation for the party.<br>• **Warded Light Flares:** Deploys radiant flares that **cannot be snuffed or extinguished** by hostile mob auras! | **Warded** (+8% Dark-Zone Mob Resistance)<br>**Permanent Warded Lanterns** (Crafted block) |
| **Scout** | **Explorer XP** | • **Agility:** Boosts movement speed, sprint efficiency, and dodge recovery.<br>• **Trap & Threat Detection:** Highlights hidden dark-zone ambushes and trial spawners.<br>• **Aerial Traversal:** Maximum Elytra glide momentum efficiency and pillar climbing. | **Aerodynamic** (+10% Elytra Glide Efficiency)<br>**Balanced** (+0.2 Speed, -15% Whiff Window) |

---

## 4. The Centerpiece Cooperative Ender Dragon Encounter

The Ender Dragon encounter has been redesigned from a solo 5-minute arrow cheese into a multi-phase cooperative raid requiring team coordination and proximity voice communication:

```
                           Ender Dragon Raid Phases
                                       │
     ┌─────────────────────────────────┼─────────────────────────────────┐
     ▼                                 ▼                                 ▼
[ Crystal Navigation ]         [ Breath & Dive Phases ]          [ Vulnerability Punish ]
Scouts scale tall obsidian     Tanks absorb aerial dive impacts   Support deploys Warded Flares;
pillars and neutralize high    and dragon breath clusters with    DPS unloads burst executions
cages with mobility agility.   mitigation cover.                  during grounded stagger phases.
```

1. **Phase 1: Crystal Traversal (Scouts Lead):**  
   High cages and exploding crystals require Scout traversal agility and precision pillar scaling while avoiding breath traps.
2. **Phase 2: Dive Impact & Breath Tanking (Tanks Lead):**  
   The dragon performs aggressive aerial dives that deal catastrophic area-of-effect damage. Tanks deploy defensive cover and absorb dive impacts, shielding their party.
3. **Phase 3: Visibility & Team Sustain (Support Leads):**  
   Dark-zone shadows extinguish End lighting. Support specialists illuminate the arena with **Warded Light Flares** that withstand dragon shroud auras, while providing steady health sustain.
4. **Phase 4: Grounded Execution Windows (DPS Leads):**  
   When the dragon perches on the bedrock portal, DPS specialists capitalize on tight execution punish windows, cracking dragon armor with critical strikes.

---

## 5. Advancements & Rewards Progression

The **Dark-Zone Trials** (`combat/*`) advancement tree provides a tiered progression structure bridging individual combat mastery, dungeon delving, emergent role specialization, and cooperative raid execution.

Advancements in this tree are registered through `CombatRaidAdvancements` and processed dynamically by `AdvancementRewardManager`. Earning an advancement distributes non-material digital coins directly to the player's account, awards playstyle role XP, and grants physical **Recovery Catalysts** (used to insure high-tier equipment against catastrophic dark-zone item loss). Master and Legendary achievements trigger custom visual effects and server-wide broadcast banners.

### Progression Tree Overview

```
                                [ combat/root ]
                               Dark-Zone Trials
                                       │
                                       ▼
                          [ combat/first_kill_tempo ]
                             High-Tempo Skirmisher
                                       │
                                       ▼
                            [ combat/snuffer_slayer ]
                                Beacon Protector
                                       │
                                       ▼
                            [ combat/trial_survivor ]
                                  Vault Delver
                                       │
             ┌─────────────────────────┼─────────────────────────┐
             ▼                         ▼                         ▼
 [ combat/scout_crystal_neutralizer ]  [ combat/tank_dive_intercept ]  [ combat/support_rejuvenation ]
         Spire Infiltrator                     Unyielding Aegis               Field Medic Radiance
         (Scout Agility)                       (Tank Bulwark)                 (Support Sustain)
                                       │
                                       ▼
                            [ combat/dps_execute_strike ]
                               Executioner's Precision
                                    (DPS Punish)
                                       │
                                       ▼
                        [ combat/cooperative_dragon_triumph ]
                                Vanguard of the End
                               (Legendary Raid Triumph)
```

### Tiered Advancement Rewards Matrix

| Advancement ID | Display Title | Tier & Frame | Icon | Unlock Condition | Currency | Playstyle XP | Recovery Catalysts | Broadcast |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `combat/root` | **Dark-Zone Trials** | Novice (`task`) | Netherite Sword | Enter the world and step into dark-zone combat | — | — | — | No |
| `combat/first_kill_tempo` | **High-Tempo Skirmisher** | Novice (`task`) | Iron Sword | Defeat a hostile mob with fast Time-To-Kill (TTK) | +50 coins | +30 Combat XP | — | No |
| `combat/snuffer_slayer` | **Beacon Protector** | Novice (`task`) | Soul Torch | Defeat an elite mob possessing the `destroys_light_on_aggro` trait | +100 coins | +50 Combat XP | — | No |
| `combat/light_guardian` | **Extinguish the Shadows** | Journeyman (`goal`) | Lantern | Slay 5+ darkness-stalking mobs before they extinguish tactical lights | +200 coins | +80 Combat XP | — | No |
| `combat/trial_survivor` | **Vault Delver** | Journeyman (`goal`) | Trial Key | Clear a deep trial chamber vault or spawner dungeon room | +200 coins | +80 Combat XP | — | No |
| `combat/scout_crystal_neutralizer` | **Spire Infiltrator** | Journeyman (`goal`) | End Crystal | Scale obsidian heights and shatter an End Crystal tethered to the Dragon | +250 coins | +100 Combat XP<br>+40 Explorer XP | +1 Catalyst | No |
| `combat/tomb_cleanser` | **Tomb Cleanser** | Master (`challenge`) | Spawner | Purge 5+ monster spawners within complex subterranean dungeons | +400 coins | +170 Combat XP | +1 Catalyst | **Server-Wide** |
| `combat/raid_bastion` | **Bulwark Against the Horde** | Master (`challenge`) | Ominous Bottle | Defend a settlement or village sanctuary against an ominous raid | +350 coins | +150 Combat XP | +1 Catalyst | No |
| `combat/tank_dive_intercept` | **Unyielding Aegis** | Master (`goal`) | Shield | Intercept an Ender Dragon dive or boss attack using Tank barrier mitigation | +300 coins | +120 Combat XP | +1 Catalyst | No |
| `combat/dps_execute_strike` | **Executioner's Precision** | Master (`challenge`) | Diamond Axe | Deal massive burst execution damage during a boss vulnerability window | +300 coins | +120 Combat XP | +1 Catalyst | No |
| `combat/support_rejuvenation` | **Field Medic Radiance** | Master (`challenge`) | Golden Apple | Deploy warded lights and sustain teammates through intense raid phases | +350 coins | +150 Combat XP | +2 Catalysts | No |
| `combat/warlord_dominance` | **Paragon of the Vanguard** | Master (`challenge`) | Netherite Sword | Accumulate 1,000+ Combat XP across dark-zone trials and raids | +500 coins | +200 Combat XP | +2 Catalysts | **Server-Wide** |
| `combat/cooperative_dragon_triumph` | **Vanguard of the End** | Legendary (`challenge`) | Dragon Head | Triumph over the Cooperative Ender Dragon centerpiece raid alongside your party | +1,000 coins | +350 Combat XP | +3 Catalysts | **Server-Wide** |

---

### Detailed Milestone Mechanics

#### 1. Novice Tier: The Basics of Dark-Zone Survival
* **High-Tempo Skirmisher (`combat/first_kill_tempo`):**  
  Encourages active engagement over passive grinding chutes. Evaluates the player's initial combat encounter, rewarding swift Time-To-Kill and accuracy against hostile monsters.
* **Beacon Protector (`combat/snuffer_slayer`):**  
  Teaches the dangers of dark-zone light-destruction mechanics. Granted upon neutralizing an elite monster carrying the `destroys_light_on_aggro` AI trait before it permanently snuffs local torches or lanterns.

#### 2. Journeyman Tier: Depths & Infiltration
* **Vault Delver (`combat/trial_survivor`):**  
  Integrated directly with `StructureDelveManager`, recognizing players who clear spawner vault rooms across Trial Chambers, YUNG's Better Dungeons, and When Dungeons Arise complexes. Conquering these high-density chambers yields bonus XP and catalyst drop rolls.
* **Spire Infiltrator (`combat/scout_crystal_neutralizer`):**  
  Tailored for the **Scout** role. Granted when scaling pillar heights and destroying an End Crystal while it is actively beaming/healing the Ender Dragon, triggering a backlash shockwave that briefly staggers the boss.

#### 3. Master Tier: Role Specialization & Boss Encounters
* **Unyielding Aegis (`combat/tank_dive_intercept`):**  
  Tailored for the **Tank** role. Awarded when a player actively blocks and absorbs an Ender Dragon aerial dive (`CHARGING_PLAYER` phase), deploying kinetic barriers that absorb the impact and open an extended vulnerability window for teammates.
* **Executioner's Precision (`combat/dps_execute_strike`):**  
  Tailored for the **DPS** role. Granted when unloading heavy critical strike bursts during the dragon's grounded stagger or central bedrock perch punish windows, puncturing boss armor.
* **Field Medic Radiance (`combat/support_rejuvenation`):**  
  Tailored for the **Support** role. Awarded when deploying non-extinguishable Warded Light Flares that dissolve purple dragon breath acid clouds while sustaining team health through out-of-combat rejuvenation pulses and radiant potion sharing.

#### 4. Legendary Tier: Endgame Cooperative Raid Triumph
* **Vanguard of the End (`combat/cooperative_dragon_triumph`):**  
  The pinnacle achievement of the combat progression. Granted to every party member present in the End dimension when the overhauled Cooperative Ender Dragon is defeated. Awards +1,000 digital coins, +350 Combat XP, +3 Recovery Catalysts, permanently imprints combat provenance lore onto held weapons, and announces the victory to the entire server.

---

### Reward Dispatch & Anti-Exploit Enforcement

1. **One-Time Lifetime Claims:**  
   Every advancement reward is recorded in the player's persistent `ModAttachmentTypes.PLAYER_MILESTONES` data component. Repeated advancement triggers or reset events will not award duplicate currency or catalysts.
2. **Non-Material Economy Distribution:**  
   Coin rewards flow directly into the player's digital balance managed by `EconomyManager.credit()`, avoiding material item bloat while fueling server trade.
3. **Recovery Catalyst Influx:**  
   Higher-tier combat achievements inject physical **Recovery Catalysts** into player inventories, providing the vital insurance items required for high-risk dark-zone expeditions without devaluing death penalties.

