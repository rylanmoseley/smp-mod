# Contributing to the SMP Mod Suite

Thank you for your interest in contributing to the **SMP Mod Suite** (`smpmod`)! This project is built for **Minecraft 1.21.1 NeoForge** using **Kotlin for Forge (KFF)**.

To maintain code quality, architectural consistency, and a clean Git history, all contributors must follow the workflow, branching model, and commit standards outlined below.

---

## 1. Git Workflow & Branching Strategy

This repository enforces a **Standard Branching Git Flow** paired with **Conventional Commits**:

```
main          ─────────────────────────●──────────────────────── (Releases / Production)
                                      ▲
                                      │ Merge PR (--no-ff)
develop       ────●──────●────────────●─────────●─────────────── (Active Integration)
                   \      \          /         /
feature/*           ●──────●────────●         /  (Feature Branches)
                            \                /
fix/*                        ●──────────────●    (Bugfix Branches)
```

### Branch Conventions
* **`main`**: Production-ready, stable releases. Protected branch. Direct pushes are disabled; changes arrive via merges from `develop`.
* **`develop`**: Integration branch for active feature development. All pull requests and new features target `develop`.
* **`feat/<feature-name>`**: New subsystems, mechanics, or features branched from `develop`.
* **`fix/<bug-name>`**: Bug fixes branched from `develop` (or hotfixes from `main`).
* **`docs/<topic>`**: Documentation updates and player handbook revisions.
* **`chore/<task>`**: Dependency updates, build script adjustments, and CI toolchain tasks.

---

## 2. Conventional Commits Specification

All commit messages must adhere to the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Allowed Types
| Type | Description | Example |
| :--- | :--- | :--- |
| `feat` | A new feature or system implementation | `feat: implement stat leaderboards and tool stats` |
| `fix` | A bug fix | `fix: resolve NPE on empty buyback ledger queries` |
| `docs` | Documentation only changes | `docs: update server deployment and packwiz guides` |
| `style` | Formatting, missing semi-colons, whitespace | `style: reformat event handler indentation` |
| `refactor` | Code restructuring without behavior changes | `refactor: extract BFS flood-fill into ClaimManager` |
| `perf` | Performance improvements | `perf: cache leaderboard calculations on server tick` |
| `test` | Adding or updating tests | `test: add unit assertions for role XP ratios` |
| `chore` | Build system, CI, dependencies, tooling | `chore: bump neo_version to 21.1.234` |

---

## 3. Local Development & Build Workflow

### Prerequisites
- **JDK 21 (LTS):** Recommended distribution is [Eclipse Temurin 21](https://adoptium.net/).
- **Git 2.30+**

### Building and Running Locally
The repository includes the Gradle wrapper. Run standard tasks from the repository root:

```bash
# Compile and build the mod JAR
./gradlew build

# Run the Minecraft development client
./gradlew runClient

# Run the Minecraft dedicated development server
./gradlew runServer

# Run Data Generation (tags, recipes, models)
./gradlew runData

# Clean build artifacts
./gradlew clean
```

---

## 4. Pull Request & Submission Process

1. **Fork or Branch:** Create your feature or bugfix branch from the latest `develop` branch:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feat/my-new-mechanic
   ```
2. **Implement & Test:** Ensure your code compiles cleanly (`./gradlew build`) and adheres to Kotlin coding conventions.
3. **Clean Workspace:** Remove any intermediate test scripts or temporary files before committing.
4. **Commit:** Write clear, concise commit messages using conventional commit format.
5. **Open Pull Request:** Open a pull request targeting the **`develop`** branch on GitHub. Describe what changes were made, why they are needed, and how they were tested.
