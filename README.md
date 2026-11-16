# SMP Mod Suite (`smpmod`)

[![Build and Verify](https://github.com/rylanmoseley/smp-mod/actions/workflows/build.yml/badge.svg)](https://github.com/rylanmoseley/smp-mod/actions/workflows/build.yml)

A high-cohesion Minecraft server mod suite built for **NeoForge 1.21.1** in **Kotlin** using **Kotlin for Forge (KFF)**.

Based on the [Technical Architecture and Systems Overview (v2.0)](./design-v2.md), this mod re-engineers survival gameplay loops around three fundamental pillars:
1. **Maximize social, constructive, and long-term multiplayer gameplay.**
2. **Eliminate low-reward grinding and unrecoverable death loss without removing consequence.**
3. **Rebalance building, gathering, exploring, and combat so every playstyle carries direct strategic value into the endgame.**

---

## Architecture & Subsystems

The codebase is organized into modular packages under `dev.rmoseley.smpmod`:

```
dev.rmoseley.smpmod
├── SmpMod.kt                    # Mod entry point (@Mod) and event bus registration
├── config/                      # NeoForge mod configuration & tuning matrix constants
│   └── SmpConfig.kt
├── registry/                    # Deferred registers
│   ├── ModDataComponents.kt     # Data Components (UUID, lore provenance, death count)
│   ├── ModAttachmentTypes.kt    # NeoForge data attachments (balances, XP, harvest counts)
│   └── ModEntityAttributes.kt   # Emergent role attributes & light aggro traits
├── economy/                     # Non-material currency, bulk store, & shop unlocks
│   ├── EconomyManager.kt
│   ├── ShopUnlockManager.kt
│   └── BulkStoreManager.kt
├── recovery/                    # Ownership-bound buybacks & provenance lore
│   ├── BuybackLedgerManager.kt
│   └── ItemProvenance.kt
├── claims/                      # Contiguous land claims & construction scoring
│   ├── ClaimManager.kt
│   ├── VarietyScorer.kt
│   └── BuilderFlightController.kt
├── world/                       # World topology, Nether hazards, & AI
│   ├── NetherHazardHandler.kt
│   ├── ElytraFlightManager.kt
│   └── LightMobAIHandler.kt
├── roles/                       # Emergent ratio-driven combat roles & Playstyle XP
│   ├── RoleCalculator.kt
│   └── PlaystyleXpTracker.kt
└── event/                       # Mod bus and game bus event listeners
    ├── CommonModEvents.kt
    └── GameForgeEvents.kt
```

### Core Mechanisms
- **Non-Material Economy:** Digital player balances safe from death drops, fed by starter & repeatable milestones, P2P commerce, and natural harvest shop unlocks.
- **Provenance-Tracked UUID Buybacks:** Named, enchanted, and high-tier items receive persistent UUIDs and track `Crafted by` / `Owned by` lore. On death, items route to a buyback ledger with an escalating fee, a 24h exclusive reclamation window, and smooth ~30-day decay. Mundane stackables route to the server bulk store.
- **Contiguous Claims & Builder Flight:** Claimed chunks must maintain contiguous topology. Variety-weighted block placements generate pooled defensive buffs and grant temporary (~5s) building flight within claims.
- **Light AI & World Hazards:** Dark zones escalate mob strength, while mobs with `destroys_light_on_aggro` snuff lights (break/drop in wilderness, temporary shroud aura inside claims). Nether roof access is blocked, transit highways carry hazards, and Elytra is gated behind exploration milestones with momentum gliding.
- **Emergent Combat Roles:** Zero elective classes. A player's combat profile across **DPS**, **Tank**, **Support**, and **Scout** is dynamically derived from their Playstyle XP ratio ($\text{Combat}$, $\text{Builder}$, $\text{Gatherer}$, $\text{Explorer}$).

---

## Development Environment & Toolchain

- **Target Game Version:** Minecraft `1.21.1`
- **Mod Loader:** NeoForge `21.1.234+`
- **Language:** Kotlin `2.0.21` via Kotlin for Forge `5.10.0+`
- **Gradle Plugin:** `net.neoforged.moddev` (ModDevGradle `2.0.142`)
- **JVM Target:** Java 21 (`JavaLanguageVersion.of(21)`)
- **Mappings:** Official Mojang Mappings + Parchment (`2024.11.17`)

### Building the Project

Run standard Gradle tasks via the wrapper:

```bash
# Build the mod JAR
./gradlew build

# Run Minecraft Client with the mod
./gradlew runClient

# Run Dedicated Server with the mod
./gradlew runServer

# Run Data Generation (recipes, tags, models)
./gradlew runData
```

---

## Git Workflow & Branching Strategy

This repository enforces a **Standard Branching Git Flow** paired with **Conventional Commits**:

```
main          ─────────────────────────●──────────────────────── (Releases / Stable)
                                      ▲
                                      │ Merge PR
develop       ────●──────●────────────●─────────●─────────────── (Active Integration)
                   \      \          /         /
feature/*           ●──────●────────●         /  (Feature Branches)
                            \                /
fix/*                        ●──────────────●    (Bugfix Branches)
```

### Branch Conventions
* **`main`**: Production-ready, stable releases. Protected branch.
* **`develop`**: Integration branch for active feature development. All pull requests target `develop`.
* **`feat/<feature-name>`**: New subsystems, mechanics, or features branched from `develop`.
* **`fix/<bug-name>`**: Bug fixes branched from `develop` (or hotfixes from `main`).
* **`docs/<topic>`**: Documentation updates.
* **`chore/<task>`**: Dependency updates, build script adjustments, toolchain tasks.

### Conventional Commits
All commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
* `feat:` A new feature or system implementation
* `fix:` A bug fix
* `docs:` Documentation only changes
* `style:` Code style, formatting, missing semi-colons, etc.
* `refactor:` Code change that neither fixes a bug nor adds a feature
* `perf:` Performance improvements
* `test:` Adding or updating tests
* `chore:` Build system, CI, dependencies, or tool configurations

---

## License

This project is licensed under the [MIT License](./LICENSE).
