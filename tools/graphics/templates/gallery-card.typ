// SMP Mod Suite - Modrinth Gallery Card Template (1280x720, 16:9)
#import "../lib/theme.typ": px, bg-canvas, bg-surface, bg-elevated, border-subtle, border-accent, text-primary, text-secondary, text-muted, color-emerald, color-amber, color-amethyst, color-sky, color-crimson, color-indigo, font-sans, font-mono, pill, card-panel, tokens
#import "../lib/icons.typ": project-emblem, get-subsystem-icon, get-role-icon
#import "../lib/backgrounds.typ": chunk-cluster-grid, topographic-contours, dot-matrix, quadrant-radar

#let card = sys.inputs.at("card", default: "overview")

#set page(
  width: 1280 * px,
  height: 720 * px,
  margin: 0pt,
  fill: bg-canvas
)

#set text(
  font: font-sans,
  fill: text-primary
)

// Background generation per card type
#if card == "overview" [
  #place(top + left, chunk-cluster-grid(1280, 720, seed: 101, accent: color-emerald, cell-size: 36, cluster-size: 28, secondary-cluster: true))
] else if card == "settlements" [
  #place(top + left, chunk-cluster-grid(1280, 720, seed: 202, accent: color-emerald, cell-size: 32, cluster-size: 36, secondary-cluster: true))
] else if card == "roles" [
  #place(top + left, quadrant-radar(1280, 720, seed: 303, accent: color-crimson))
] else [
  #place(top + left, dot-matrix(1280, 720, seed: 404, accent: color-amethyst, spacing: 28))
]

// Vignette overlay
#place(top + left,
  rect(
    width: 1280 * px,
    height: 720 * px,
    fill: gradient.radial(
      center: (50%, 50%),
      radius: 70%,
      rgb("#0B0E1400"),
      rgb("#0B0E14F6")
    )
  )
)

// Main Content Container
#place(top + left, dx: 64 * px, dy: 48 * px,
  box(width: 1152 * px, height: 624 * px, [

    // =========================================================================
    // CARD 1: OVERVIEW (Featured)
    // =========================================================================
    #if card == "overview" [
      #stack(spacing: 12 * px,
        stack(dir: ltr, spacing: 12 * px,
          pill("MODRINTH FEATURED SHOWCASE", accent: color-emerald),
          pill("NEOFORGE 1.21.1", accent: color-sky),
          pill("SMP MOD SUITE", accent: color-amber),
        ),
        stack(dir: ltr, spacing: 18 * px,
          project-emblem(size: 64, variant: "mod"),
          align(horizon, [
            #text(size: 46 * px, weight: "bold", fill: white, [SMP Mod Suite: Complete Architecture])
          ])
        ),
        text(size: 19 * px, fill: text-secondary, [
          Re-engineering survival multiplayer around long-term construction, fair death recovery, and emergent combat.
        ]),
        v(12 * px),

        // 2x3 Grid of All 6 Subsystems
        grid(
          columns: (1fr, 1fr, 1fr),
          rows: (150 * px, 150 * px),
          gutter: 16 * px,
          // 01 Economy
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("economy", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-amber, [01 \/\/ ECONOMY])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [Economy & Commerce]),
              text(size: 12 * px, fill: text-secondary, [Non-material digital wallet, milestone faucets, villager encapsulation, and bulk store.])
            )
          ),
          // 02 Death Recovery
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("death-recovery", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-amethyst, [02 \/\/ RECOVERY])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [Death Recovery & Lore]),
              text(size: 12 * px, fill: text-secondary, [UUID buybacks, 24h exclusive window, 30d fee decay, and persistent item provenance.])
            )
          ),
          // 03 Land Claims
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("land-claims", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-emerald, [03 \/\/ CLAIMS])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [Land Claims & Settlements]),
              text(size: 12 * px, fill: text-secondary, [OPAC integration, contiguous BFS clusters, variety dividends, and tiered builder flight.])
            )
          ),
          // 04 World Navigation
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("world-navigation", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-sky, [04 \/\/ SURVIVAL])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [World & Navigation]),
              text(size: 12 * px, fill: text-secondary, [Tectonic alpine summits, Nether roof lockout, road convoys, and light-snuffing AI.])
            )
          ),
          // 05 Emergent Roles
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("emergent-roles", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-crimson, [05 \/\/ COMBAT])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [Emergent Combat Roles]),
              text(size: 12 * px, fill: text-secondary, [Zero elective classes: DPS, Tank, Support, Scout ratios, Warded Light, and Dragon raid.])
            )
          ),
          // 06 Server Utilities
          card-panel(
            stack(spacing: 8 * px,
              stack(dir: ltr, spacing: 10 * px,
                get-subsystem-icon("server-utilities", size: 28),
                text(font: font-mono, size: 12 * px, weight: "bold", fill: color-indigo, [06 \/\/ UTILITIES])
              ),
              text(size: 16 * px, weight: "bold", fill: white, [Server Moderation]),
              text(size: 12 * px, fill: text-secondary, [Real-time stat leaderboards, Vanilla Tweaks tool stats, dynamic sleep, and Discord webhook.])
            )
          )
        ),
        v(6 * px),
        align(center, [
          #text(font: font-mono, size: 11 * px, fill: text-muted, [
            ZERO ELECTIVE CLASSES · DIGITAL NON-MATERIAL ECONOMY · CONTIGUOUS CLUSTER BFS · PROVENANCE TRACKING
          ])
        ])
      )
    ]

    // =========================================================================
    // CARD 2: SETTLEMENTS & ECONOMY
    // =========================================================================
    else if card == "settlements" [
      #stack(spacing: 12 * px,
        stack(dir: ltr, spacing: 12 * px,
          pill("SUBSYSTEM SPOTLIGHT", accent: color-emerald),
          pill("LAND CLAIMS & COMMERCE", accent: color-amber),
        ),
        text(size: 42 * px, weight: "bold", fill: white, [Contiguous Settlements & Digital Commerce]),
        text(size: 18 * px, fill: text-secondary, [
          Bases grow stronger together. OPAC claims evaluate BFS clusters to award pooled defense and construction dividends.
        ]),
        v(14 * px),
        grid(
          columns: (1fr, 1.15fr),
          gutter: 24 * px,
          // Left features
          stack(spacing: 16 * px,
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-emerald, [CONTIGUOUS BFS CLUSTER ANALYSIS]),
                text(size: 16 * px, weight: "bold", fill: white, [Tiered Settlement Upgrades]),
                text(size: 12 * px, fill: text-secondary, [Outpost (1-3) -> Hamlet (4-8) -> Settlement (9-18) -> Town (19-36) -> Metropolis (37+ chunks) with pooled defense buffs and community treasuries.])
              )
            ),
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-amber, [CONSTRUCTION DIVIDENDS & BUILDER FLIGHT]),
                text(size: 16 * px, weight: "bold", fill: white, [Rewarded Craftsmanship]),
                text(size: 12 * px, fill: text-secondary, [Recurring coin dividends awarded per 100 blocks placed with palette variety bonuses. Temporary builder flight sustained by building.])
              )
            ),
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-sky, [NON-MATERIAL COMMERCE & MOBILITY]),
                text(size: 16 * px, weight: "bold", fill: white, [Living Villager Relocation]),
                text(size: 12 * px, fill: text-secondary, [Encapsulate villagers into tradable specimens. Restocks strictly require free movement—putting an end to 1x1 trading cells.])
              )
            )
          ),
          // Right visual radar
          card-panel(
            height: 380 * px,
            fill: bg-canvas.transparentize(20%),
            [
              #place(top + left, chunk-cluster-grid(580, 340, seed: 777, accent: color-emerald, cell-size: 28, cluster-size: 20, secondary-cluster: true))
              #place(top + left, dx: 12 * px, dy: 10 * px,
                pill("CLAIMED TOWN NEXUS: FORTIFIED TOWN", accent: color-emerald)
              )
              #place(bottom + left, dx: 12 * px, dy: -10 * px,
                box(
                  fill: bg-surface,
                  inset: (x: 10pt, y: 6pt),
                  radius: 4pt,
                  stroke: 1pt + border-subtle,
                  text(font: font-mono, size: 10 * px, fill: text-secondary, [24 Contiguous Chunks · Pooled Regeneration I · Bell Alarm Active])
                )
              )
            ]
          )
        )
      )
    ]

    // =========================================================================
    // CARD 3: EMERGENT ROLES
    // =========================================================================
    else if card == "roles" [
      #stack(spacing: 12 * px,
        stack(dir: ltr, spacing: 12 * px,
          pill("COMBAT RE-BALANCING", accent: color-crimson),
          pill("CLASSLESS PROGRESSION", accent: color-sky),
        ),
        text(size: 42 * px, weight: "bold", fill: white, [Emergent Roles: Dynamic Playstyle XP]),
        text(size: 18 * px, fill: text-secondary, [
          Zero class selection menus. Your combat role is calculated continuously from what you actually do in survival.
        ]),
        v(14 * px),
        // 4 Roles Grid
        grid(
          columns: (1fr, 1fr, 1fr, 1fr),
          gutter: 16 * px,
          // DPS
          card-panel(
            height: 390 * px,
            stroke: color-crimson.transparentize(60%),
            stack(spacing: 12 * px,
              align(center, get-role-icon("dps", size: 48)),
              align(center, text(size: 20 * px, weight: "bold", fill: color-crimson, [DPS])),
              align(center, pill("SOURCE: COMBAT XP", accent: color-crimson)),
              v(4 * px),
              text(size: 13 * px, weight: "bold", fill: white, [Archetype: Striker]),
              text(size: 12 * px, fill: text-secondary, [Focuses on critical hit precision and fast time-to-kill. Triggers 1.8x boss execution punish damage when target is below 35% HP.])
            )
          ),
          // TANK
          card-panel(
            height: 390 * px,
            stroke: color-amber.transparentize(60%),
            stack(spacing: 12 * px,
              align(center, get-role-icon("tank", size: 48)),
              align(center, text(size: 20 * px, weight: "bold", fill: color-amber, [TANK])),
              align(center, pill("SOURCE: BUILDER XP", accent: color-amber)),
              v(4 * px),
              text(size: 13 * px, weight: "bold", fill: white, [Archetype: Frontline Anchor]),
              text(size: 12 * px, fill: text-secondary, [Constructive mastery converts into armor toughness, 16m threat taunts, 75% kinetic barrier mitigation, and Ender Dragon dive intercepts.])
            )
          ),
          // SUPPORT
          card-panel(
            height: 390 * px,
            stroke: color-emerald.transparentize(60%),
            stack(spacing: 12 * px,
              align(center, get-role-icon("support", size: 48)),
              align(center, text(size: 20 * px, weight: "bold", fill: color-emerald, [SUPPORT])),
              align(center, pill("SOURCE: GATHERER XP", accent: color-emerald)),
              v(4 * px),
              text(size: 13 * px, weight: "bold", fill: white, [Archetype: Team Sustain]),
              text(size: 12 * px, fill: text-secondary, [Resource harvesters deploy Warded Light flares immune to light-snuffing mob AI and radiate sustained health pulses in dark caves.])
            )
          ),
          // SCOUT
          card-panel(
            height: 390 * px,
            stroke: color-sky.transparentize(60%),
            stack(spacing: 12 * px,
              align(center, get-role-icon("scout", size: 48)),
              align(center, text(size: 20 * px, weight: "bold", fill: color-sky, [SCOUT])),
              align(center, pill("SOURCE: EXPLORER XP", accent: color-sky)),
              v(4 * px),
              text(size: 13 * px, weight: "bold", fill: white, [Archetype: Path Recon]),
              text(size: 12 * px, fill: text-secondary, [Trail pioneers enjoy +20% explored biome mobility, paved road speed boosts, 24m dark-zone trap detection, and momentum Elytra gliding.])
            )
          )
        )
      )
    ]

    // =========================================================================
    // CARD 4: DEATH RECOVERY & ITEM LORE
    // =========================================================================
    else [
      #stack(spacing: 12 * px,
        stack(dir: ltr, spacing: 12 * px,
          pill("FAIR MULTIPLAYER RECOVERY", accent: color-amethyst),
          pill("HISTORICAL PROVENANCE", accent: color-amber),
        ),
        text(size: 42 * px, weight: "bold", fill: white, [UUID Buybacks & Item Provenance Lore]),
        text(size: 18 * px, fill: text-secondary, [
          Eliminating catastrophic item loss in lava and despawn clocks without removing meaningful survival consequence.
        ]),
        v(14 * px),
        grid(
          columns: (1.2fr, 1fr),
          gutter: 24 * px,
          // Left features
          stack(spacing: 16 * px,
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-amethyst, [OWNERSHIP-BOUND UUID BUYBACK LEDGER]),
                text(size: 16 * px, weight: "bold", fill: white, [24-Hour Exclusive Reclamation]),
                text(size: 12 * px, fill: text-secondary, [Enchanted and named gear routes to a personal recovery terminal upon death. Mundane stackables route directly to the server bulk store.])
              )
            ),
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-amber, [CUMULATIVE ESCALATION & 30-DAY DECAY]),
                text(size: 16 * px, weight: "bold", fill: white, [Consequential Economy Sinks]),
                text(size: 12 * px, fill: text-secondary, [Reclaim fees escalate by +0.25x per cumulative item death. Fees smoothly decay back to baseline over 30 real-world wall-clock days.])
              )
            ),
            card-panel(
              stack(spacing: 6 * px,
                text(font: font-mono, size: 11 * px, weight: "bold", fill: color-sky, [ANTI-THEFT PUBLIC MARKETPLACE AUCTION]),
                text(size: 16 * px, weight: "bold", fill: white, [Public Salvage Drop]),
                text(size: 12 * px, fill: text-secondary, [Unclaimed items past the 24h exclusive window appear in the public buyback marketplace at 1.25x price, accessible to all players.])
              )
            )
          ),
          // Right: Tooltip Lore Card Mockup
          card-panel(
            height: 380 * px,
            fill: rgb("#11151F"),
            stroke: border-subtle,
            [
              #stack(spacing: 10 * px,
                text(size: 18 * px, weight: "bold", fill: rgb("#55FFFF"), [Diamond Pickaxe]),
                text(size: 13 * px, fill: rgb("#AAAAAA"), [Efficiency V]),
                text(size: 13 * px, fill: rgb("#AAAAAA"), [Unbreaking III]),
                text(size: 13 * px, fill: rgb("#AAAAAA"), [Fortune III]),
                line(length: 100%, stroke: 0.8pt + rgb("#FFAA00")),
                text(size: 12 * px, font: font-mono, fill: rgb("#FFAA00"), [--- Tool Statistics ---]),
                text(size: 12 * px, font: font-mono, fill: rgb("#55FFFF"), [Blocks Mined: #text(fill: white, [14,250])]),
                line(length: 100%, stroke: 0.8pt + border-subtle),
                text(size: 12 * px, font: font-mono, fill: rgb("#FFAA00"), [Crafted by: Alex]),
                text(size: 12 * px, font: font-mono, fill: rgb("#55FFFF"), [Owned by: Steve]),
                text(size: 11 * px, font: font-mono, fill: text-muted, [UUID: 8a7c...4f1e · Deaths: 2]),
                v(8 * px),
                box(
                  fill: color-amethyst.transparentize(80%),
                  stroke: 1pt + color-amethyst,
                  radius: 4pt,
                  inset: (x: 10pt, y: 6pt),
                  text(font: font-mono, size: 11 * px, weight: "bold", fill: white, [Buyback Cost: 125 Coins (1.50x)])
                )
              )
            ]
          )
        )
      )
    ]
  ])
)
