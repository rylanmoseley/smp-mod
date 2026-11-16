# Server Configuration & Tuning Matrix Reference

For server administrators and modpack creators, the SMP Mod Suite provides a centralized configuration file located at:

```
config/smpmod-common.toml
```

All values in this file map directly to the tuning constants defined in **Section 7 of the Technical Architecture Specification (v2.0)**. Changes can be applied live or upon server restart.

---

## 1. Tuning Matrix Configuration Parameters

### `[Economy]`
Controls currency minting, milestone payouts, and trade taxes.

```toml
[Economy]
    # Currency awarded upon completing onboarding starter advancements
    # Range: 0 to 100000 | Default: 100
    starterAdvancementPayout = 100

    # Number of natural block harvests required per repeatable currency payout
    # Range: 50 to 10000 | Default: 500
    repeatableHarvestThreshold = 500

    # Currency awarded per repeatable harvest cycle completion
    # Range: 1 to 10000 | Default: 25
    repeatableHarvestPayout = 25

    # Transaction fee percentage deducted from formal P2P shop transfers (0.0 to 1.0)
    # Range: 0.0 to 1.0 | Default: 0.05 (5%)
    p2pTradeTaxPercent = 0.05
```

---

### `[RecoveryAndBuyback]`
Governs death recovery ledgers, fee escalation, wall-clock decay, and market auctions.

```toml
[RecoveryAndBuyback]
    # Escalating multiplier increment per cumulative death on an owned item
    # e.g., 0.25 = +25% fee per death
    # Range: 0.01 to 5.0 | Default: 0.25
    baseBuybackEscalationStep = 0.25

    # Exclusive reclaim window duration for registered owner before public listing
    # Default: 86400 seconds (24 hours)
    # Range: 60 to 604800 (1 minute to 7 days)
    exclusiveOwnerWindowSeconds = 86400

    # Public listing buyback price markup after the exclusive 24h window expires
    # Range: 1.0 to 5.0 | Default: 1.25 (25% markup)
    publicListingMarkup = 1.25

    # Number of real-world days required for escalated buyback fees to smoothly decay back to 1.0x baseline
    # Range: 1 to 365 | Default: 30 days
    wallClockDecayDays = 30
```

---

### `[AntiAFK]`
Adjusts farm suppression thresholds and rewards active resource sourcing.

```toml
[AntiAFK]
    # Radius in chunks where repetitive mob kills begin suffering steep diminishing XP and drop returns
    # Range: 1 to 16 | Default: 3 chunks
    grinderChunkDecayRadius = 3

    # Ore vein density multiplier in deep caves (Y < 0) to reward active caving over passive strip-mining
    # Range: 1.0 to 5.0 | Default: 1.5x
    deepCaveVeinDensityMultiplier = 1.5

    # High-tier enchantment tome drop weight multiplier in dungeons and trial structures
    # Range: 1.0 to 10.0 | Default: 2.0x
    dungeonTomeDropMultiplier = 2.0
```

---

### `[ClaimsAndBuilding]`
Calibrates claim caps, variety scoring, and builder flight duration.

```toml
[ClaimsAndBuilding]
    # Max claimable chunks unlocked per Builder Playstyle level
    # Range: 1 to 10 | Default: 2
    chunksPerBuilderLevel = 2

    # Base claimable chunk limit awarded to any new player
    # Range: 1 to 64 | Default: 4
    maxBaseClaimChunks = 4

    # Base multiplier for variety-weighted block placement diversity
    # Range: 0.1 to 10.0 | Default: 1.0
    varietyPaletteWeightBase = 1.0

    # Temporary building flight grace duration in seconds, refreshed by continuous block placement
    # Range: 1 to 30 | Default: 5 seconds
    builderFlightGraceSeconds = 5
```

---

### `[WorldAndAI]`
Controls Elytra exploration gating, mob traits, and lighting behavior.

```toml
[WorldAndAI]
    # Number of unique chunks mapped/discovered required to deploy Elytra glider flight
    # Range: 10 to 50000 | Default: 500 chunks
    elytraExplorerChunkMilestone = 500

    # Duration in seconds that light-snuff aura extinguishes lights within contiguous claims (20-30s)
    # Range: 5 to 120 | Default: 25 seconds
    lightSnuffDurationSeconds = 25

    # Spawn probability of destroys_light_on_aggro mobs in wild caves
    # Range: 0.0 to 1.0 | Default: 0.05 (5%)
    wildernessLightDestroyChance = 0.05

    # Spawn probability of destroys_light_on_aggro mobs inside dungeons and trial structures
    # Range: 0.0 to 1.0 | Default: 0.35 (35%)
    dungeonLightDestroyChance = 0.35
```

---

### `[RolesAndXP]`
Tuning parameters for Combat XP formulas and emergent role ratios.

```toml
[RolesAndXP]
    # Weight applied to time-to-kill speed in Combat XP calculation
    # Range: 0.1 to 5.0 | Default: 1.2
    combatXpSpeedWeight = 1.2

    # Weight applied to hit precision/crits in Combat XP calculation
    # Range: 0.1 to 5.0 | Default: 1.5
    combatXpAccuracyWeight = 1.5
```

---

## 2. In-Game Commands Reference

### Player Commands
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/economy balance` | All | Displays your current digital non-material balance. |
| `/economy pay <player> <amount>` | All | Transfers digital funds to another online player (subject to server tax). |
| `/bounty list` | All | Displays available community contracts on the Bounty Board. |
| `/bounty accept <id>` | All | Accepts an active contract (max 2 concurrent contracts). |
| `/bounty status` | All | Displays current objective progress for your active contracts. |
| `/bounty claim <id>` | All | Collects coins and Playstyle XP for a completed contract. |
| `/bounty abandon <id>` | All | Abandons an active contract to free up a slot. |
| `/town balance` | All | Displays town treasury balance and lifetime collected taxes. |
| `/town deposit <amount>` | All | Donates personal currency into the communal town treasury. |
| `/town withdraw <amount>` | Claim Owner | Withdraws municipal funds into owner balance. |
| `/town upgrades` | All | Lists active and available civic upgrades for the settlement. |
| `/town upgrade buy <id>` | Claim Owner | Unlocks a civic upgrade using communal treasury funds. |
| `/buyback` | All | Opens your private UUID Buyback Ledger interface to reclaim lost equipment. |
| `/buyback public` | All | Opens the public marketplace showing unclaimed items past the 24h exclusive window. |
| `/claims claim` | All | Claims the current chunk via Open Parties and Claims (OPAC). |
| `/claims cluster` | All | Displays the contiguous settlement tier, development level, and pooled buffs for the current chunk. |
| `/role profile` | All | Displays your current Playstyle XP breakdown and derived combat role affinities. |

### Administrator Commands (OP Level 2+)
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/economy admin set <player> <amount>` | OP Level 2 | Overwrites a player's digital balance. |
| `/economy admin give <player> <amount>` | OP Level 2 | Grants digital currency to a player. |
| `/claims admin bypass` | OP Level 2 | Toggles admin claim-protection bypass. |
| `/role admin addxp <player> <track> <amount>` | OP Level 2 | Grants Combat, Builder, Gatherer, or Explorer XP to a player. |
| `/role admin reset <player>` | OP Level 2 | Resets a player's accumulated Playstyle XP to baseline. |
