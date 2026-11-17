// SMP Mod Suite - Modrinth Gallery Card Template (1280x720, 16:9)
// Expedition Parchment & Cartographer's Field Plates (Screenshot-Ready)
#import "../lib/theme.typ": *
#import "../lib/icons.typ": render-emblem, render-subsystem-sprite, render-role-sprite, draw-compass-rose
#import "../lib/backgrounds.typ": paper-grain, survey-plot-map, topographic-atlas, expedition-border

#let card = sys.inputs.at("card", default: "overview")
#let screenshot = sys.inputs.at("screenshot", default: "")

#set page(
  width: 1280 * px,
  height: 720 * px,
  margin: 0pt,
  fill: parchment-base
)

#set text(
  font: font-body,
  fill: ink-primary
)

// 1. Full Parchment Paper Texture & Margin Wash
#place(top + left, paper-grain(1280, 720, seed: 101))

// 2. Double-Rule Cartographic Border Framing (2pt outer, 0.75pt inner)
#place(top + left, expedition-border(1280, 720, inset: 12, outer-stroke: 2pt, inner-stroke: 0.75pt))

#if screenshot != "" and screenshot != none {
  // =========================================================================
  // SCREENSHOT MODE: Pinned In-Game Plate with Parchment Caption Bar
  // =========================================================================
  let (plate-title, plate-numeral, plate-accent, plate-summary) = if card == "overview" {
    ("Expedition Overview & Complete Architecture", "I", pigment-moss, "Cohesive survival systems designed for cooperative, long-term multiplayer.")
  } else if card == "settlements" {
    ("Contiguous Settlement Claims & Sound Economy", "II", pigment-ochre, "OPAC BFS chunk cluster tiering, builder flight, and non-material coin faucets.")
  } else if card == "roles" {
    ("Emergent Specializations & Combat Synergies", "III", pigment-vermilion, "Playstyle XP dynamically calculates DPS, Tank, Support, and Scout profiles.")
  } else {
    ("UUID Death Buybacks & Provenance Item Lore", "IV", pigment-plum, "Permanent historical item lore and escalating wall-clock recovery ledgers.")
  }

  // Pinned Expedition Photo Plate (78% height)
  place(top + left, dx: 36 * px, dy: 30 * px,
    box(
      width: (1280 - 72) * px,
      height: 530 * px,
      fill: parchment-inset,
      stroke: 1.5pt + ink-primary,
      [
        #place(top + left, dx: 6 * px, dy: 6 * px,
          box(
            width: (1280 - 84) * px,
            height: (530 - 12) * px,
            stroke: 0.75pt + ink-faint,
            clip: true,
            image(screenshot, width: 100%, height: 100%, fit: "cover")
          )
        )
      ]
    )
  )

  // Bottom Caption Bar
  place(bottom + left, dx: 36 * px, dy: -30 * px,
    box(
      width: (1280 - 72) * px,
      height: 100 * px,
      fill: parchment-inset,
      stroke: 1pt + ink-primary,
      inset: (x: 20pt, y: 14pt),
      [
        #grid(
          columns: (1fr, auto),
          align: horizon,
          [
            #grid(
              columns: (auto, 1fr),
              gutter: 14 * px,
              align: horizon,
              chapter-stamp("PLATE " + plate-numeral, pigment: plate-accent, size: 8.5pt),
              [
                #text(font: font-display, size: 22 * px, weight: "bold", fill: ink-primary, plate-title)
                #v(-2 * px)
                #text(font: font-body, size: 14 * px, fill: ink-secondary, plate-summary)
              ]
            )
          ],
          [
            #meta-stamp("EXPEDITION FIELD RECORD", text-color: ink-secondary, size: 8pt)
          ]
        )
      ]
    )
  )
} else {
  // =========================================================================
  // SCHEMATIC FALLBACK MODE: Pure Typst + CeTZ Field Guide Plate
  // =========================================================================
  place(top + left, dx: 44 * px, dy: 52 * px,
    box(width: (1280 - 88) * px, height: (720 - 72) * px, [

      // ---------------------------------------------------------------------
      // 01-OVERVIEW PLATE
      // ---------------------------------------------------------------------
      #if card == "overview" [
        // Top Title Header
        #grid(
          columns: (72 * px, 1fr, auto),
          gutter: 18 * px,
          align: horizon,
          render-emblem(size: 68pt),
          [
            #text(font: font-heading, size: 11 * px, weight: "bold", fill: pigment-ochre, tracking: 2pt, [FIELD GUIDE · EXPEDITION MANUAL])
            #v(2 * px)
            #text(font: font-display, size: 34 * px, weight: "bold", fill: ink-primary, [SMP Mod Suite: Complete Architecture])
            #v(2 * px)
            #text(font: font-body, size: 15 * px, fill: ink-secondary, [Six cohesive, balanced survival multiplayer subsystems built on NeoForge 1.21.1])
          ],
          [
            #chapter-stamp([CHAPTER I — OVERVIEW], pigment: pigment-moss, size: 8.5pt)
          ]
        )

        #v(10 * px)
        #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
        #v(14 * px)

        // 2x3 Grid of 6 Subsystem Ledger Boxes
        #grid(
          columns: (1fr, 1fr, 1fr),
          rows: (230 * px, 230 * px),
          gutter: 16 * px,

          // 1. Economy
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("economy", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Economy & Coin]),
              chapter-stamp("I", pigment: pigment-ochre, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-ochre)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              Non-material digital wallet, milestone faucets, villager encapsulation spawn trades, and server bulk store.
            ])
          ]),

          // 2. Recovery
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("death-recovery", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Death Recovery]),
              chapter-stamp("II", pigment: pigment-plum, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-plum)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              UUID buyback ledgers, 24h exclusive window, escalating wall-clock decay, and persistent historical item lore.
            ])
          ]),

          // 3. Claims
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("land-claims", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Land Claims]),
              chapter-stamp("III", pigment: pigment-moss, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-moss)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              OPAC integration, contiguous chunk cluster BFS, variety-based construction dividends, and builder flight.
            ])
          ]),

          // 4. World Nav
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("world-navigation", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Navigation]),
              chapter-stamp("IV", pigment: pigment-cobalt, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-cobalt)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              Tectonic worldgen, Nether roof lockout, road convoy speed bonuses, momentum Elytra, and light-snuffing mobs.
            ])
          ]),

          // 5. Roles
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("emergent-roles", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Emergent Roles]),
              chapter-stamp("V", pigment: pigment-vermilion, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-vermilion)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              Classless combat profiles (DPS, Tank, Support, Scout) continuously derived from your Playstyle XP actions.
            ])
          ]),

          // 6. Server Utils
          ledger-panel(width: 100%, height: 100%, inset: 12pt, [
            #grid(
              columns: (40 * px, 1fr, auto),
              gutter: 10 * px,
              align: horizon,
              render-subsystem-sprite("server-utilities", size: 40pt),
              text(font: font-display, size: 17 * px, weight: "bold", fill: ink-primary, [Server Utilities]),
              chapter-stamp("VI", pigment: pigment-slate, size: 7.5pt)
            )
            #v(8 * px)
            #line(length: 100%, stroke: 1pt + pigment-slate)
            #v(8 * px)
            #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
              Native /leaderboard rankings across 9 metrics, Vanilla Tweaks tool stats lore, multiplayer sleep, and Discord bot.
            ])
          ])
        )
      ] else if card == "settlements" [
        // ---------------------------------------------------------------------
        // 02-SETTLEMENTS & ECONOMY PLATE
        // ---------------------------------------------------------------------
        #grid(
          columns: (1fr, auto),
          align: horizon,
          [
            #text(font: font-heading, size: 11 * px, weight: "bold", fill: pigment-moss, tracking: 2pt, [FIELD GUIDE · CHAPTER III — SETTLEMENTS & ECONOMY])
            #v(2 * px)
            #text(font: font-display, size: 32 * px, weight: "bold", fill: ink-primary, [Contiguous Claims & Sound Coinage])
          ],
          [
            #chapter-stamp([SURVEY PLATE II], pigment: pigment-moss, size: 8.5pt)
          ]
        )
        #v(4 * px)
        #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
        #v(14 * px)

        #grid(
          columns: (640 * px, 1fr),
          gutter: 20 * px,

          // Left: Surveyor's Parcel Map
          box(
            height: 500 * px,
            fill: parchment-inset,
            stroke: 1.5pt + ink-primary,
            [
              #place(top + left, survey-plot-map(640, 500, seed: 202, accent: pigment-moss, cell-size: 32, cluster-target: 18))
              #place(top + right, dx: -16 * px, dy: 16 * px, draw-compass-rose(size: 72pt))
              #place(bottom + left, dx: 16 * px, dy: -14 * px,
                box(
                  fill: parchment-base,
                  stroke: 1pt + ink-primary,
                  inset: (x: 10pt, y: 5pt),
                  [
                    #text(font: font-heading, size: 8.5pt, weight: "bold", fill: ink-primary, [CONTIGUOUS CLUSTER BFS: 18 CLAIMS (TIER 3 TOWN)])
                  ]
                )
              )
            ]
          ),

          // Right: Town Ledger Panel
          stack(spacing: 16 * px,
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [Contiguous Settlement Scaling])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                Claims must connect contiguously to share defense perks, builder flight, and town tier bonuses. Disconnected outposts function independently.
              ])
            ]),
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [Construction Dividends])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                Earn currency bonuses for structural diversity. Combining stone masonry, timber framing, and glass earns recurring construction payouts.
              ])
            ]),
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [Villager Trade Relocation])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                Sneak-click adult villagers to encapsulate them into tradable specimens with discount levels and master trade offers intact.
              ])
            ])
          )
        )
      ] else if card == "roles" [
        // ---------------------------------------------------------------------
        // 03-EMERGENT ROLES PLATE
        // ---------------------------------------------------------------------
        #grid(
          columns: (1fr, auto),
          align: horizon,
          [
            #text(font: font-heading, size: 11 * px, weight: "bold", fill: pigment-vermilion, tracking: 2pt, [FIELD GUIDE · CHAPTER V — SPECIALIZATION BESTIARY])
            #v(2 * px)
            #text(font: font-display, size: 32 * px, weight: "bold", fill: ink-primary, [Dynamic Playstyle Derivation])
          ],
          [
            #chapter-stamp([BESTIARY PLATE III], pigment: pigment-vermilion, size: 8.5pt)
          ]
        )
        #v(4 * px)
        #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
        #v(14 * px)

        // 4 Columns
        #grid(
          columns: (1fr, 1fr, 1fr, 1fr),
          gutter: 14 * px,

          ledger-panel(height: 500 * px, inset: 14pt, [
            #render-role-sprite("dps", size: 60pt)
            #v(10 * px)
            #text(font: font-display, size: 22 * px, weight: "bold", fill: pigment-vermilion, [DPS])
            #v(-2 * px)
            #text(font: font-heading, size: 9 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [COMBAT XP])
            #v(6 * px)
            #line(length: 100%, stroke: 1.5pt + pigment-vermilion)
            #v(10 * px)
            #text(font: font-body, size: 12 * px, weight: "semibold", fill: ink-primary, [Archetype: Precision Striker])
            #v(8 * px)
            #text(font: font-body, size: 11.5 * px, fill: ink-secondary, [
              • Execute punish windows\
              #v(4 * px)
              • Boss critical multipliers\
              #v(4 * px)
              • Fast TTK XP feedback\
              #v(4 * px)
              • Dark-zone combat scaling
            ])
          ]),

          ledger-panel(height: 500 * px, inset: 14pt, [
            #render-role-sprite("tank", size: 60pt)
            #v(10 * px)
            #text(font: font-display, size: 22 * px, weight: "bold", fill: pigment-ochre, [TANK])
            #v(-2 * px)
            #text(font: font-heading, size: 9 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [BUILDER XP])
            #v(6 * px)
            #line(length: 100%, stroke: 1.5pt + pigment-ochre)
            #v(10 * px)
            #text(font: font-body, size: 12 * px, weight: "semibold", fill: ink-primary, [Archetype: Frontline Anchor])
            #v(8 * px)
            #text(font: font-body, size: 11.5 * px, fill: ink-secondary, [
              • Frontline damage soak\
              #v(4 * px)
              • Kinetic barrier pulses\
              #v(4 * px)
              • Portable cover shields\
              #v(4 * px)
              • Dragon stagger intercept
            ])
          ]),

          ledger-panel(height: 500 * px, inset: 14pt, [
            #render-role-sprite("support", size: 60pt)
            #v(10 * px)
            #text(font: font-display, size: 22 * px, weight: "bold", fill: pigment-moss, [SUPPORT])
            #v(-2 * px)
            #text(font: font-heading, size: 9 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [GATHERER XP])
            #v(6 * px)
            #line(length: 100%, stroke: 1.5pt + pigment-moss)
            #v(10 * px)
            #text(font: font-body, size: 12 * px, weight: "semibold", fill: ink-primary, [Archetype: Sustain Keeper])
            #v(8 * px)
            #text(font: font-body, size: 11.5 * px, fill: ink-secondary, [
              • Warded Light flares\
              #v(4 * px)
              • Team sustain healing\
              #v(4 * px)
              • Consumable conservation\
              #v(4 * px)
              • Harvest yield dividends
            ])
          ]),

          ledger-panel(height: 500 * px, inset: 14pt, [
            #render-role-sprite("scout", size: 60pt)
            #v(10 * px)
            #text(font: font-display, size: 22 * px, weight: "bold", fill: pigment-cobalt, [SCOUT])
            #v(-2 * px)
            #text(font: font-heading, size: 9 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [EXPLORER XP])
            #v(6 * px)
            #line(length: 100%, stroke: 1.5pt + pigment-cobalt)
            #v(10 * px)
            #text(font: font-body, size: 12 * px, weight: "semibold", fill: ink-primary, [Archetype: Trail Vanguard])
            #v(8 * px)
            #text(font: font-body, size: 11.5 * px, fill: ink-secondary, [
              • Road caravan velocity\
              #v(4 * px)
              • Ambush & trap detection\
              #v(4 * px)
              • Momentum Elytra glide\
              #v(4 * px)
              • Specimen packaging
            ])
          ])
        )
      ] else [
        // ---------------------------------------------------------------------
        // 04-DEATH RECOVERY & LORE PLATE
        // ---------------------------------------------------------------------
        #grid(
          columns: (1fr, auto),
          align: horizon,
          [
            #text(font: font-heading, size: 11 * px, weight: "bold", fill: pigment-plum, tracking: 2pt, [FIELD GUIDE · CHAPTER II — DEATH RECOVERY & PROVENANCE])
            #v(2 * px)
            #text(font: font-display, size: 32 * px, weight: "bold", fill: ink-primary, [UUID Buyback Ledgers & Item Provenance])
          ],
          [
            #chapter-stamp([LEDGER PLATE IV], pigment: pigment-plum, size: 8.5pt)
          ]
        )
        #v(4 * px)
        #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
        #v(14 * px)

        #grid(
          columns: (640 * px, 1fr),
          gutter: 20 * px,

          // Left: Death Buyback Ledger Page
          ledger-panel(height: 500 * px, inset: 14pt, [
            #text(font: font-heading, size: 11 * px, weight: "bold", fill: ink-primary, tracking: 1.5pt, [RECOVERY TERMINAL · OFFICIAL REGISTER])
            #v(6 * px)
            #line(length: 100%, stroke: 1pt + ink-primary)
            #v(10 * px)

            #table(
              columns: (1.5fr, 1.2fr, 1fr, 1.2fr),
              stroke: 0.5pt + rule-border,
              fill: (col, row) => if row == 0 { parchment-base } else { none },
              inset: 8pt,
              align: (col, row) => if col == 0 { left } else { center },
              table.header(
                [*Item Name*], [*Provenance*], [*Buyback*], [*Status*]
              ),
              [Netherite Pickaxe], [Crafted: Alice], [45 Coins], [Exclusive 24h],
              [Elytra Wings], [Owned: Bob], [120 Coins], [Exclusive 24h],
              [Bow of Flame], [Slain: Ender], [30 Coins], [Market Auction],
              [Diamond Chestplate], [Crafted: Charlie], [75 Coins], [Decayed 30d],
            )

            #v(16 * px)
            #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: ink-secondary, [PERSISTENT ITEM PROVENANCE])
            #v(6 * px)
            #box(
              fill: parchment-base,
              stroke: 1pt + ink-secondary,
              inset: 10pt,
              width: 100%,
              [
                #text(font: font-mono, size: 10 * px, fill: pigment-plum, weight: "bold", [
                  Crafted by: Alice_99\
                  Owned by: Bob_The_Builder\
                  UUID: 7a3e5d10-4bc2-4f1a-b89e-001a4e8d3c22
                ])
              ]
            )
          ]),

          // Right: Mechanics Explanation
          stack(spacing: 16 * px,
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [Zero-Loss Recovery Loop])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                Prized equipment never despawns in lava or the void. Items are automatically registered to your recovery ledger upon death.
              ])
            ]),
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [Exclusive Owner Window])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                You retain an exclusive 24-hour window to buy back your dropped equipment. After 24 hours, items enter the public marketplace with a 1.25x fee.
              ])
            ]),
            ledger-panel(width: 100%, height: 156 * px, inset: 14pt, [
              #text(font: font-display, size: 18 * px, weight: "bold", fill: ink-primary, [30-Day Wall-Clock Decay])
              #v(6 * px)
              #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
                Reckless death fees decay smoothly over 30 days of real time, rewarding safe play while preventing permanent bankruptcy.
              ])
            ])
          )
        )
      ]
    ])
  )
}
