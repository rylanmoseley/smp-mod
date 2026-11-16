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
