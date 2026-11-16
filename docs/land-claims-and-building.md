# Land Claims & Construction System

The land claiming and construction system empowers cooperative town building, fortified settlements, and aesthetic construction without the risk of passive leeching or disconnected outpost stat-farming.

---

## 1. Noncontiguous Claims vs. Contiguous Settlement Clusters

Unlike traditional land claim systems that either force rigid contiguous shapes or allow disconnected chunks to share stat bonuses, the SMP Mod Suite balances freedom and realism:

```
                World Dimension Map (Example Player Claims)
    
       Contiguous Cluster A (Main City)         Disconnected Outpost B
       ┌───┬───┬───┬───┐                               ┌───┐
       │ C │ C │ C │ C │                               │ C │ (Mining Camp)
       ├───┼───┼───┼───┤                               └───┘
       │ C │ C │ C │ C │ ➔ Metropolis Tier                   ➔ Outpost Tier
       ├───┼───┼───┼───┤   (High Dev Level,                   (Independent Dev
       │ C │ C │ C │ C │    Full Pooled Buffs,                 Level 1, Minimal
       ├───┼───┼───┼───┤    Builder Flight)                    Buffs)
       │ C │ C │ C │ C │
       └───┴───┴───┴───┘
```

### Noncontiguous Freedom
* Players and parties are free to claim chunks anywhere across the world—such as a remote mountain observatory, a Nether portal hub, or a deep-underground mining outpost.
* Claims are managed seamlessly via **Open Parties and Claims (OPAC)**. Press the claim map key (`'` by default) or run `/claims claim`.

### Contiguous Cluster Development Leveling
* **The Rule:** While claims can be scattered noncontiguously across the map, **only contiguous chunks** sharing orthogonal borders (North, South, East, West) are referenced for a given claim's **development leveling**, **pooled defensive buffs**, and **temporary builder flight**!
* **Connected Component Analysis:** The server automatically analyzes the topology of your claims and groups adjacent chunks into independent **Settlement Clusters**:
  * **Outpost:** 1 chunk (Base development)
  * **Hamlet:** 2–3 contiguous chunks
  * **Settlement:** 4–8 contiguous chunks
  * **Town:** 9–15 contiguous chunks
  * **Metropolis:** 16+ contiguous chunks
* **Why this matters:** Dropping a single 1x1 chunk outpost in the wilderness will not inherit the high-level defense buffs or flight privileges of your 16-chunk capital city. Every settlement must earn its development through local constructive investment.

---

## 2. Variety-Weighted Construction Scoring

To reward thoughtful, aesthetic architecture and prevent players from spamming low-effort cobblestone monoliths to farm Builder XP:

* **Net Placed Blocks:** The server evaluates *net* block placement (blocks placed minus blocks broken), preventing place-and-break exploits.
* **Palette Variety & Logarithmic Diminishing Returns:**  
  Placing the same block type repeatedly suffers steep diminishing returns:

$$\text{Block Score} = \text{Base Weight} \times \frac{1.0}{1.0 + \ln(1.0 + \text{Count})} \times (1.0 + 0.1 \times \text{Unique Palette Count})$$

* **Aesthetic Incentive:** A build mixing polished stone, oak timber, glass panes, terracotta trim, and lanterns yields maximum Builder XP and accelerates cluster development leveling. In contrast, thousands of identical cobblestone blocks quickly hit a diminishing score floor.

---

## 3. Builder Chunk Scaling & Individual XP Isolation

* **Individual Chunk Caps:** A player's maximum claimable chunk limit scales upward as they level up their individual **Builder Playstyle Level**:

$$\text{Max Chunks} = \text{Base Cap (4)} + (\text{Builder Level} \times 2)$$

* **Individual XP Isolation:** Builder XP is awarded strictly to the player who actually places the qualifying construction blocks. Teammates who stand AFK in a massive town do not passively leech Builder XP—each builder's personal level reflects their genuine physical contribution to the settlement.

---

## 4. Pooled Claim Defensive Buffs

While individual Builder XP remains strictly isolated, the **defensive bonuses** of a shared contiguous settlement are **pooled for all team members**:

* **Stronghold Synergy:** As builders cooperate to develop a contiguous town, the settlement cluster unlocks escalating defensive benefits for all party members standing within its borders:
  * **Damage Mitigation:** Up to $+40\%$ passive damage reduction against hostile monsters.
  * **Regeneration Aura:** Accelerated natural healing when out of combat.
  * **Light Shroud Resistance:** Reduces the duration of hostile mob light-snuffing auras.
* Turning cooperative towns into shared strongholds gives builders a central role in protecting the community.

---

## 5. Claim-Locked Temporary Builder Flight

Building high roofs, cathedral ceilings, and large towers in survival is often frustrating and hazardous. The suite introduces an intuitive, claim-safe construction flight mechanic:

```
Place Block in Contiguous Claim ➔ Grants ~5-Second Flight Grace Window
      │                                 │
      ├── Place Another Block           └── Stop Placing Blocks
      │   ➔ Refreshes Grace Window (5s)     ➔ Flight Grace Expires (Revoked)
      │
      └── Enter Combat / Take Damage
          ➔ Flight INSTANTLY Revoked (Anti-Exploit)
```

### Flight Rules
1. **Cadence-Sustained:** Placing a structural block inside an owned or shared contiguous claim grants a **~5-second temporary flight grace window**. Placing blocks continuously maintains flight indefinitely.
2. **Claim-Bounded:** Temporary flight is strictly restricted to the borders of your contiguous settlement cluster. Flying out into the wilderness or over an enemy claim immediately revokes flight.
3. **Instant Combat Revocation:** If you take damage from a mob, projectile, or player, flight is **instantly disabled**. It cannot be used as an evasive combat escape tool or to bypass dungeons.

---

## 6. Claim-Safe Anti-Griefing Guardrails

Wilderness and dungeon monsters possessing the `destroys_light_on_aggro` trait actively destroy torches, lanterns, and campfires. However, inside your claimed settlements:

* **No Block Damage:** Hostile mobs **cannot break blocks** inside contiguous claims.
* **Temporary Snuff Aura:** Instead of destroying Builder lighting, aggroed mobs apply a temporary **20–30 second light shroud aura** that suppresses light output without damaging infrastructure. Once the aura expires or the mob is slain, illumination naturally restores.

---

## 7. Town Civic Upgrades & Communal Treasuries

Contiguous settlements can establish communal **Town Treasuries** to finance high-value civic infrastructure upgrades for all residents and visitors.

### Treasury Funding Mechanics
Town funds accumulate automatically without tedious tax forms:
1. **P2P Sales Tax Dividend:** When players execute formal `/p2p` shop trades inside a town claim, the 5% transaction tax is deposited directly into the town treasury.
2. **Municipal Buyback Share:** A portion of death recovery fees spent at town terminals flows into the settlement coffers.
3. **Resident Donations:** Community members can donate personal digital funds using `/town deposit <amount>`.

### Civic Upgrades Catalog
Settlement leaders (the claim founder/owner) can purchase permanent civic upgrades once their contiguous chunk count reaches the required tier:

| Civic Upgrade | Cost | Required Tier | Municipal Effect |
| :--- | :--- | :--- | :--- |
| **Paved Highway Network** (`paved_highways`) | 500 coins | **Settlement** (4+ chunks) | Doubles transit speed buffs (**Speed II, +40% speed**) on roads and rails inside and departing town claims. |
| **Municipal Quarantine** (`municipal_quarantine`) | 750 coins | **Settlement** (4+ chunks) | Reduces dark-zone light snuff aura duration by **50%** and suppresses hostile mob aggro speed. |
| **Town Recovery Subsidy** (`recovery_subsidy`) | 1,000 coins | **Town** (9+ chunks) | Grants residents an extra **+15% discount** on all death buybacks completed within the claim. |
| **Warded Streetlights** (`warded_streetlights`) | 1,500 coins | **Town** (9+ chunks) | Automatically protects all lanterns within town boundaries from mob destruction and snuffing. |

### Town Management Commands
* `/town balance`: View your town's current treasury funds and lifetime taxes collected.
* `/town deposit <amount>`: Donate digital currency into the communal treasury.
* `/town withdraw <amount>`: Town leader withdrawal of municipal funds.
* `/town upgrades`: Browse available and unlocked civic upgrades for your current settlement tier.
* `/town upgrade buy <id>`: Purchase and activate a civic upgrade for the contiguous settlement.
