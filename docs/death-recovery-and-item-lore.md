# Death Recovery & Item Lore System

![Death Recovery & Item Lore Subsystem Header](assets/generated/headers/death-recovery.png)

In traditional Minecraft, death often results in catastrophic, unrecoverable loss—valuable gear burns in lava, falls into the void, or despawns during a frantic corpse run. 

The SMP Mod Suite replaces this punitive loop with an **Ownership-Bound UUID Buyback Ledger** paired with **Persistent Item Lore**. You will never permanently lose your prized equipment, but reckless deaths carry meaningful economic consequences and real market tension.

---

## 1. What Happens When You Die?

Upon death, your dropped inventory is automatically sorted into two separate recovery paths:

```
                                Player Death
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           ▼                                                   ▼
┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
│        UUID Buyback Ledger          │     │          Server Bulk Store          │
│                                     │     │                                     │
│ • Armor, Weapons, Tools             │     │ • Cobblestone, Dirt, Sand           │
│ • Any Named or Enchanted item       │     │ • Common Mundane Arrows & Torches   │
│ • Rare Catalysts & High-Tier Loot   │     │ • Basic Crops & Common Mob Drops    │
│                                     │     │                                     │
│ ➔ Escalating Fee                    │     │ ➔ Sold to server stockpile          │
│ ➔ 24h Exclusive Reclaim Window      │     │ ➔ Available in bulk building shop   │
│ ➔ 1.25x Public Auction after 24h    │     │ ➔ No individual recovery fees       │
│ ➔ ~30-day decay back to baseline   │     │                                     │
└─────────────────────────────────────┘     └─────────────────────────────────────┘
```

1. **UUID Buyback Ledger:**  
   Every piece of equipment, weapon, armor, tool, and any item that is **named or enchanted** (along with rare catalysts like Netherite ingots or dungeon relics) receives a persistent UUID and is securely transferred to your private buyback ledger. It will never despawn, burn, or void.
2. **Bulk Store Routing:**  
   Standard mundane stackables (dirt, stone, torches, common food) are deposited into the server's bulk store, feeding market liquidity without cluttering your recovery inventory.

---

## 2. Escalating Fees and Wall-Clock Decay

### Escalating Cost per Death
Every tracked item carries a cumulative `death_count`. The base recovery fee scales upward with each death:

$$\text{Current Fee} = \text{Base Value} \times \left(1.0 + \text{death\_count} \times \text{escalation\_step}\right)$$

* *Default Step:* $+25\%$ per death on that item.
* An enchanted diamond sword died with once might cost **125** currency. If you die with it 4 times, the fee escalates to **200** currency.
* This ensures that carelessness carries tangible stakes.

### ~30-Day Wall-Clock Fee Decay
Over real-world wall-clock time since death, an escalated buyback fee gradually decays back toward its baseline **1.0x core value**:

* **Decay Period:** Smooth reduction over approximately **30 real-world days**.
* If you cannot immediately afford an escalated recovery fee, the cost will become cheaper over time as the item rests in the ledger.

---

## 3. The 24-Hour Exclusive Window & Public Listing

To prevent players from passively "camping" the 30-day decay timer to recover gear at rock-bottom prices, the market introduces active social tension:

```
Death Occurs
 │
 ├── 0h to 24h: EXCLUSIVE OWNER WINDOW
 │   • Only the Registered Owner can reclaim the item.
 │   • Cost = Owner's calculated escalated fee.
 │
 └── After 24h: 1.25x PUBLIC MARKETPLACE LISTING
     • The item becomes visible to the entire server via `/buyback public`.
     • Any player can purchase the item at 1.25x (25% markup) of the owner's price!
     • Owner can still reclaim at their normal price, but risks losing it to other buyers.
```

> [!WARNING]
> **Don't Wait Too Long:** If you leave your god-tier sword in the buyback ledger for more than 24 hours to wait out the decay timer, another player on the server can pay a 25% markup and permanently claim your weapon!

---

## 4. Stolen Item Protection & Anti-Griefing Guardrails

In multiplayer, items are sometimes looted or stolen without a formal transaction. The recovery system includes strict protections against theft and griefing:

1. **Recovery Routing on Thief Death:**  
   If Player B steals Player A's sword without a formal P2P sale and Player B subsequently dies, the sword **routes back to Player A's buyback ledger**, not the thief's!
2. **Fee Griefing Immunity:**  
   The item's `death_count` multiplier **only increments when the registered owner is the one who died**. A thief cannot grab your weapon and jump into lava repeatedly to artificially inflate your recovery costs.

---

## 5. Persistent Item Lore & Provenance

Items in the SMP Mod Suite tell a story. Using Minecraft 1.21.1 Data Components, every unique or enchanted item maintains a living historical record:

```
┌──────────────────────────────────────────────┐
│ Netherite Halberd of the Deep                │
│ Sharpness V, Unbreaking III, Mending         │
│                                              │
│ §7Crafted by Steve                           │
│ §aOwned by Alex                              │
│ §cDeath Recovery Count: 2                    │
└──────────────────────────────────────────────┘
```

* **Origin Tracking:** Displays whether the item was `Crafted by [Player]` or `Looted by [Player]`.
* **Current Ownership:** Displays `Owned by [Player]`.
* **Formal P2P Handover:** When an item is sold through a formal P2P trade stall or `/economy transfer-item`, the `Owned by` field automatically updates to the buyer, transferring all legitimate buyback rights.
* **Trophy & Auction Value:** Weapons wielded in famous server battles or dragon raids accumulate authentic provenance, making them prized collector's items on the public market.

---

## 6. Masterwork Affixes & Item Variations

Unique weapons, armor, and tools carry special **Masterwork Affixes** rolled during forging or uncovered in perilous deep dungeons. These lightweight variations create distinct gear identities worth recovering through buybacks and trading:

| Masterwork Affix | Color | Equipment Target | Direct In-Game Effect |
| :--- | :--- | :--- | :--- |
| **Keen** | `§c` Red | Weapons | **+10% Critical Strike Damage** on falling jump crits. |
| **Sturdy** | `§9` Blue | All Gear | **+25% Enhanced Durability** (25% chance to negate durability damage). |
| **Balanced** | `§e` Yellow | Weapons / Tools | **+0.2 Attack Speed** & **-15% Attack Whiff Recovery Window**. |
| **Warded** | `§b` Aqua | Armor | **+8% Damage Resistance** against dark-zone hostile mobs. |
| **Aerodynamic** | `§a` Green | Chestplate / Elytra | **+10% Elytra Glide Efficiency** & reduced air drag. |
| **Titanic** | `§7` Gray | Armor / Shield | **+10% Knockback Resistance** & **+5% Armor Damage Mitigation**. |
| **Precise** | `§d` Pink | Weapons | **+15% Combat Hit Precision** & bonus Tempo Playstyle XP. |

### Sourcing & Mastery Scaling
* **Crafting Quality:** Base 25% chance when crafting buyback-candidate gear, scaling up to **75%** for dedicated Builder & Gatherer specialists (`tankRatio + supportRatio`).
* **Dungeon Tomes & Chests:** Deep cavern vaults and trial chambers carry a **60% base chance** to generate with masterwork affixes.
* **Lore Integration:** Masterwork affixes display directly on item tooltips in custom gold lettering alongside the maker's mark and historical ownership ledger.

---

## 7. How to Recover Your Gear

1. Open your chat and type `/buyback` (or right-click any **Town Buyback Terminal** in a claimed settlement).
2. Browse your pending items, showing their current recovery cost, cumulative deaths, and time remaining on the 24-hour exclusive window.
3. Click **Reclaim** to pay the fee from your digital balance and instantly restore the item to your inventory.
4. Type `/buyback public` to view gear forfeited by other players after the 24-hour window has expired.

---

## 8. Advancements & Rewards Progression

The **Artificing & Provenance** advancement track celebrates the dedication of master craftsmen, dungeon excavators, cautious survivors, and antique gear collectors. Advancements reward players for creating enduring gear identities, utilizing support alchemy, participating in fair municipal buybacks, and forging legendary masterwork arsenals.

Completing tiered artificing milestones distributes automated rewards through `AdvancementRewardManager`:
* **Digital Currency Payouts:** Direct deposits into player bank accounts for commercial and craft investment.
* **Playstyle Role XP:** Builder and Gatherer XP allocations that elevate crafting quality and masterwork roll probabilities.
* **Tangible Recovery Catalysts:** Physical alchemical items (`smpmod:recovery_catalyst`) used to mitigate buyback penalty escalations.
* **Server-Wide Broadcasts:** Realm-wide notifications honoring Legendary crafting feats.

### Artificing Progression Tree

```
root (Artificing & Provenance)
 ├── first_craft_provenance (Maker's Mark) [Novice]
 │    ├── masterwork_forge (Masterwork Forging) [Journeyman]
 │    │    ├── relic_restorer (Relic Conservator) [Journeyman]
 │    │    ├── triumvirate_armament (Armament of the Aegis) [Master] ── broadcast!
 │    │    └── paragon_artificer (Paragon of the Forge) [Legendary] ── broadcast!
 └── safe_buyback (Prudent Recovery) [Journeyman]
      ├── catalytic_refinement (Alchemical Catalyst) [Journeyman]
      │    └── catalyst_synthesizer (Alchemical Transmutation) [Journeyman]
      └── public_auctioneer (Grand Appraiser) [Master]
           └── provenance_heirloom (Living Provenance) [Master]
                └── chronicles_of_lineage (Chronicles of the Ancients) [Master] ── broadcast!
```

### Tiered Milestones & Rewards Table

| Advancement ID | Display Title | Tier & Frame | Requirement & In-Game Description | Digital Payout | Playstyle XP | Tangible Rewards | Broadcast |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `artificing/root` | **Artificing & Provenance** | **Novice** (Task) | Root tab for Artificing & Provenance; unlocks upon entering the world. | — | — | — | No |
| `artificing/first_craft_provenance` | **Maker's Mark** | **Novice** (Task) | Craft your first piece of unique equipment with permanent maker lore. | **+75 coins** | **+25 Builder XP**<br>**+25 Gatherer XP** | — | No |
| `artificing/masterwork_forge` | **Masterwork Forging** | **Journeyman** (Task) | Forge or loot an item bearing a Masterwork Affix (Keen, Sturdy, Balanced, Warded, Aerodynamic, Titanic, or Precise). | **+150 coins** | **+50 Builder XP** | — | No |
| `artificing/relic_restorer` | **Relic Conservator** | **Journeyman** (Task) | Repair or polish a masterwork relic at an anvil while preserving its historic maker mark lore. | **+175 coins** | **+60 Builder XP** | — | No |
| `artificing/safe_buyback` | **Prudent Recovery** | **Journeyman** (Task) | Safely recover lost equipment from a town Buyback Terminal within a protected municipal claim. | **+100 coins** | — | **1x Recovery Catalyst** | No |
| `artificing/catalytic_refinement` | **Alchemical Catalyst** | **Journeyman** (Goal) | Craft and deploy a Recovery Catalyst to discount buyback escalation fees. | **+200 coins** | **+75 Gatherer XP** | **1x Recovery Catalyst** | No |
| `artificing/catalyst_synthesizer` | **Alchemical Transmutation** | **Journeyman** (Goal) | Synthesize or acquire 3 or more rare Recovery Catalysts for buyback security. | **+200 coins** | **+75 Gatherer XP** | — | No |
| `artificing/provenance_heirloom` | **Living Provenance** | **Master** (Challenge) | Own or trade an heirloom item possessing 3 or more historical ownership transitions in its provenance ledger. | **+350 coins** | **+100 Builder XP** | **2x Recovery Catalysts** | No |
| `artificing/chronicles_of_lineage` | **Chronicles of the Ancients** | **Master** (Challenge) | Wield an heirloom tool or weapon possessing 5 or more distinct historical ownership transfers. | **+400 coins** | **+150 Gatherer XP** | **1x Recovery Catalyst** | **Yes** |
| `artificing/public_auctioneer` | **Grand Appraiser** | **Master** (Goal) | Purchase an unclaimed public auction item past the 24-hour exclusive window. | **+300 coins** | **+100 Gatherer XP** | **1x Recovery Catalyst** | No |
| `artificing/triumvirate_armament` | **Armament of the Aegis** | **Master** (Challenge) | Simultaneously equip a Masterwork weapon, offhand item, and chestplate. | **+450 coins** | **+180 Builder XP** | **1x Recovery Catalyst** | **Yes** |
| `artificing/paragon_artificer` | **Paragon of the Forge** | **Legendary** (Challenge) | Forge and equip a complete four-piece set of masterwork equipment with pristine provenance. | **+1,000 coins** | **+300 Builder XP** | **3x Recovery Catalysts** | **Yes** |

### Deep Dungeon Integration & Sourcing
* **Vault Delves & Catalysts:** Conquering spawner rooms and vault chambers in complex structures (YUNG's Better Dungeons, When Dungeons Arise, Trial Chambers) via `StructureDelveManager` provides a **60% drop chance** for Recovery Catalysts.
* **Dungeon Masterworks:** Chest loot discovered inside complex dungeon structures rolls with a **60%+ masterwork affix chance**, enabling explorers to secure high-tier `masterwork_forge` completions without prior high-level crafting masteries.

### Anti-Exploit & One-Time Claim Guarantees
* **Attachment Ledger Tracking:** Player milestone completions are saved persistently within `ModAttachmentTypes.PLAYER_MILESTONES`. Repeating crafting cycles, discarding and re-acquiring items, or repeated buyback commands cannot duplicate rewards.
* **Authentic Handover Verification:** The `provenance_heirloom` advancement validates distinct registered owner transfers recorded through formal P2P transactions (`/economy transfer-item`) or public auctions, preventing circular self-transfers.
* **Safe Zone Subsidy Validation:** `safe_buyback` requires actual terminal reclamation inside recognized contiguous town claims (`SafeRecoveryManager`), rewarding players who plan safe fallback bases.

