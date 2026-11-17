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
#place(top + left, dx: 44 * px, dy: 22 * px,
  box(width: 1112 * px, [
    #grid(
      columns: (1fr, auto),
      align: horizon,
      [
        #text(font: font-heading, size: 13.5 * px, weight: "bold", fill: pigment-vermilion, tracking: 2pt, [FIELD GUIDE · CHAPTER V — CLASSLESS DERIVATION])
        #v(3 * px)
        #text(font: font-display, size: 28 * px, weight: "bold", fill: ink-primary, [The Four Emergent Survival Roles])
      ],
      [
        #box(
          fill: parchment-inset,
          stroke: 1.25pt + ink-primary,
          inset: (x: 12pt, y: 6pt),
          [
            #text(font: font-heading, size: 10.5pt, weight: "bold", fill: ink-secondary, [DYNAMIC XP RATIO EQUILIBRIUM])
          ]
        )
      ]
    )
    #v(6 * px)
    #text(font: font-body, size: 15.5 * px, fill: ink-secondary, [
      Profiles derive organically from your continuous survival gameplay — zero elective class menus, arbitrary point grinding, or permanent lockouts.
    ])
    #v(8 * px)
    #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
  ])
)

// 4. Four Bestiary Ledger Columns
#place(top + left, dx: 44 * px, dy: 136 * px,
  box(width: 1112 * px, height: 256 * px, [
    #grid(
      columns: (1fr, 1fr, 1fr, 1fr),
      gutter: 0 * px,

      // --- 1. DPS ---
      box(
        width: 100%,
        height: 256 * px,
        inset: (x: 18pt, y: 10pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (52 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("dps", size: 52pt),
            [
              #text(font: font-display, size: 25 * px, weight: "bold", fill: pigment-vermilion, [DPS])
              #v(-2 * px)
              #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [COMBAT XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-vermilion)
          #v(10 * px)
          #text(font: font-body, size: 13.5 * px, weight: "bold", fill: ink-primary, [Archetype: Precision Striker])
          #v(8 * px)
          #text(font: font-body, size: 13 * px, fill: ink-secondary, [
            • Execute low-HP monsters\
            #v(4 * px)
            • Armor-piercing burst crits\
            #v(4 * px)
            • Fast Time-to-Kill XP loops\
            #v(4 * px)
            • Endgame Dragon scaling
          ])
        ]
      ),

      // --- 2. TANK ---
      box(
        width: 100%,
        height: 256 * px,
        inset: (x: 18pt, y: 10pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (52 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("tank", size: 52pt),
            [
              #text(font: font-display, size: 25 * px, weight: "bold", fill: pigment-ochre, [TANK])
              #v(-2 * px)
              #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [BUILDER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-ochre)
          #v(10 * px)
          #text(font: font-body, size: 13.5 * px, weight: "bold", fill: ink-primary, [Archetype: Frontline Anchor])
          #v(8 * px)
          #text(font: font-body, size: 13 * px, fill: ink-secondary, [
            • Threat taunts & kinetic soak\
            #v(4 * px)
            • Claim builder defense perks\
            #v(4 * px)
            • Portable team cover shield\
            #v(4 * px)
            • Boss stagger interception
          ])
        ]
      ),

      // --- 3. SUPPORT ---
      box(
        width: 100%,
        height: 256 * px,
        inset: (x: 18pt, y: 10pt),
        stroke: (right: 1pt + rule-border),
        [
          #grid(
            columns: (52 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("support", size: 52pt),
            [
              #text(font: font-display, size: 25 * px, weight: "bold", fill: pigment-moss, [SUPPORT])
              #v(-2 * px)
              #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [GATHERER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-moss)
          #v(10 * px)
          #text(font: font-body, size: 13.5 * px, weight: "bold", fill: ink-primary, [Archetype: Sustain Keeper])
          #v(8 * px)
          #text(font: font-body, size: 13 * px, fill: ink-secondary, [
            • Warded Light flare radius\
            #v(4 * px)
            • Team health sustain auras\
            #v(4 * px)
            • Consumable conservation\
            #v(4 * px)
            • Harvest dividend bonuses
          ])
        ]
      ),

      // --- 4. SCOUT ---
      box(
        width: 100%,
        height: 256 * px,
        inset: (x: 18pt, y: 10pt),
        [
          #grid(
            columns: (52 * px, 1fr),
            gutter: 12 * px,
            align: horizon,
            render-role-sprite("scout", size: 52pt),
            [
              #text(font: font-display, size: 25 * px, weight: "bold", fill: pigment-cobalt, [SCOUT])
              #v(-2 * px)
              #text(font: font-heading, size: 10.5 * px, weight: "bold", fill: ink-secondary, tracking: 1.5pt, [EXPLORER XP])
            ]
          )
          #v(8 * px)
          #line(length: 100%, stroke: 1.5pt + pigment-cobalt)
          #v(10 * px)
          #text(font: font-body, size: 13.5 * px, weight: "bold", fill: ink-primary, [Archetype: Trail Vanguard])
          #v(8 * px)
          #text(font: font-body, size: 13 * px, fill: ink-secondary, [
            • Paved road convoy speed\
            #v(4 * px)
            • Dark-zone ambush detection\
            #v(4 * px)
            • Momentum Elytra soaring\
            #v(4 * px)
            • Villager relocation perks
          ])
        ]
      )
    )
  ])
)
