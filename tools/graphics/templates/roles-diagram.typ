// SMP Mod Suite - Emergent Roles 4-Column Field Guide Bestiary (1200x420)
// Cartographer's Survival Ledger & Role Specialization Matrix
#import "../lib/theme.typ": *
#import "../lib/icons.typ": render-role-sprite
#import "../lib/backgrounds.typ": paper-grain, expedition-border

#set page(
  width: 1200 * px,
  height: 420 * px,
  margin: 0pt,
  fill: parchment-base
)

#set text(
  font: font-body,
  fill: ink-primary
)

// 1. Paper grain base
#place(top + left, paper-grain(1200, 420, seed: 888))

// 2. Double-rule cartographic border framing
#place(top + left, expedition-border(1200, 420, inset: 10, outer-stroke: 2pt, inner-stroke: 0.75pt))

// 3. Top Ledger Header Plate
#place(top + left, dx: 44 * px, dy: 24 * px,
  box(width: 1112 * px, [
    #grid(
      columns: (1fr, auto),
      align: horizon,
      [
        #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: pigment-vermilion, tracking: 2pt, [FIELD GUIDE · CHAPTER V — CLASSLESS DERIVATION])
        #v(2 * px)
        #text(font: font-display, size: 24 * px, weight: "bold", fill: ink-primary, [The Four Emergent Survival Roles])
      ],
      [
        #box(
          fill: parchment-inset,
          stroke: 1pt + ink-primary,
          inset: (x: 10pt, y: 5pt),
          [
            #text(font: font-heading, size: 8.5pt, weight: "bold", fill: ink-secondary, [DYNAMIC XP RATIO EQUILIBRIUM])
          ]
        )
      ]
    )
    #v(4 * px)
    #text(font: font-body, size: 12.5 * px, fill: ink-secondary, [
      Profiles derive organically from your continuous survival gameplay — zero elective class menus, arbitrary point grinding, or permanent lockouts.
    ])
    #v(6 * px)
    #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
  ])
)

// 4. Four Bestiary Ledger Columns
#place(top + left, dx: 44 * px, dy: 106 * px,
  box(width: 1112 * px, height: 286 * px, [
    #grid(
      columns: (1fr, 1fr, 1fr, 1fr),
      gutter: 0 * px,

      // --- 1. DPS ---
      box(
        width: 100%,
        height: 286 * px,
        inset: (x: 18pt, y: 8pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (48 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("dps", size: 44pt),
            [
              #text(font: font-display, size: 20 * px, weight: "bold", fill: pigment-vermilion, [DPS])
              #v(-3 * px)
              #text(font: font-heading, size: 8.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [COMBAT XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-vermilion)
          #v(8 * px)
          #text(font: font-body, size: 11.5 * px, weight: "semibold", fill: ink-primary, [Archetype: Precision Striker])
          #v(6 * px)
          #text(font: font-body, size: 11 * px, fill: ink-secondary, [
            • Execute punish windows on low-HP monsters\
            • Armor-piercing burst multipliers\
            • Fast Time-to-Kill XP feedback loops\
            • Endgame Dragon damage scaling
          ])
        ]
      ),

      // --- 2. TANK ---
      box(
        width: 100%,
        height: 286 * px,
        inset: (x: 18pt, y: 8pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (48 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("tank", size: 44pt),
            [
              #text(font: font-display, size: 20 * px, weight: "bold", fill: pigment-ochre, [TANK])
              #v(-3 * px)
              #text(font: font-heading, size: 8.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [BUILDER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-ochre)
          #v(8 * px)
          #text(font: font-body, size: 11.5 * px, weight: "semibold", fill: ink-primary, [Archetype: Frontline Anchor])
          #v(6 * px)
          #text(font: font-body, size: 11 * px, fill: ink-secondary, [
            • Threat taunts & kinetic damage absorption\
            • Claim-locked builder defense synergy\
            • Portable cover & team light guarding\
            • Boss stagger & sweep interception
          ])
        ]
      ),

      // --- 3. SUPPORT ---
      box(
        width: 100%,
        height: 286 * px,
        inset: (x: 18pt, y: 8pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (48 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("support", size: 44pt),
            [
              #text(font: font-display, size: 20 * px, weight: "bold", fill: pigment-moss, [SUPPORT])
              #v(-3 * px)
              #text(font: font-heading, size: 8.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [GATHERER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-moss)
          #v(8 * px)
          #text(font: font-body, size: 11.5 * px, weight: "semibold", fill: ink-primary, [Archetype: Sustain Keeper])
          #v(6 * px)
          #text(font: font-body, size: 11 * px, fill: ink-secondary, [
            • Warded Light illumination flares\
            • Team health sustain & regeneration auras\
            • Potion & consumable conservation\
            • Crop & resource yield dividends
          ])
        ]
      ),

      // --- 4. SCOUT ---
      box(
        width: 100%,
        height: 286 * px,
        inset: (x: 18pt, y: 8pt),
        [
          #grid(
            columns: (48 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("scout", size: 44pt),
            [
              #text(font: font-display, size: 20 * px, weight: "bold", fill: pigment-cobalt, [SCOUT])
              #v(-3 * px)
              #text(font: font-heading, size: 8.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [EXPLORER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-cobalt)
          #v(8 * px)
          #text(font: font-body, size: 11.5 * px, weight: "semibold", fill: ink-primary, [Archetype: Trail Vanguard])
          #v(6 * px)
          #text(font: font-body, size: 11 * px, fill: ink-secondary, [
            • Paved highway & rail convoy speed\
            • Dark-zone trap & ambush detection\
            • Momentum Elytra thermal soaring\
            • Living villager relocation expertise
          ])
        ]
      )
    )
  ])
)
