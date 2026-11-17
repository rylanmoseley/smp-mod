// SMP Mod Suite - Hero Banner Template (1280x360)
// Cartographer's Field Guide Title Plate
#import "../lib/theme.typ": *
#import "../lib/icons.typ": render-emblem, draw-compass-rose
#import "../lib/backgrounds.typ": paper-grain, survey-plot-map, expedition-border

#set page(
  width: 1280 * px,
  height: 360 * px,
  margin: 0pt,
  fill: parchment-base
)

#set text(
  font: font-body,
  fill: ink-primary
)

// 1. Full Parchment Paper Texture & Margin Wash
#place(top + left, paper-grain(1280, 360, seed: 1042))

// 2. Double-Rule Cartographic Border Framing (2pt outer, 0.75pt inner, inset: 10pt)
#place(top + left, expedition-border(1280, 360, inset: 10, outer-stroke: 2pt, inner-stroke: 0.75pt))

// 3. Left Content Area: Title, Emblem, Subtitle, & Classic Hairline Rules
#place(top + left, dx: 44 * px, dy: 36 * px,
  box(width: 716 * px, height: 288 * px, [
    // Top Plate Category & Roman Numeral Header
    #text(
      font: font-heading,
      size: 10.5 * px,
      weight: "bold",
      fill: pigment-ochre,
      tracking: 2pt,
      [FIELD GUIDE & SURVIVAL ATLAS · VOL. I]
    )

    #v(8 * px)
    #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
    #v(18 * px)

    // Title Row: Pixel Emblem + Grand Serif Typography
    #grid(
      columns: (72 * px, 1fr),
      gutter: 18 * px,
      align: horizon,
      render-emblem(size: 72pt),
      [
        #text(
          font: font-display,
          size: 44 * px,
          weight: "bold",
          fill: ink-primary,
          tracking: -0.5pt,
          [SMP Mod Suite]
        )
        #v(-3 * px)
        #text(
          font: font-body,
          size: 16.5 * px,
          weight: "medium",
          fill: ink-secondary,
          [Cohesive NeoForge 1.21.1 Systems for Cooperative Survival]
        )
      ]
    )

    #v(18 * px)
    #ledger-rule(length: 100%, stroke: 0.75pt + rule-border)
    #v(16 * px)

    // Grounded Typographic Metadata Line (No pill capsules)
    #grid(
      columns: (1fr, auto),
      align: horizon,
      [
        #text(
          font: font-heading,
          size: 10 * px,
          weight: "bold",
          fill: ink-secondary,
          tracking: 1.5pt,
          [NEOFORGE 1.21.1   ·   KOTLIN 2.2   ·   MIT LICENSE   ·   ZERO-LOSS LEDGERS]
        )
      ],
      [
        #chapter-stamp([OFFICIAL ATLAS], pigment: pigment-moss, size: 7.5pt)
      ]
    )

    #v(12 * px)
    #text(
      font: font-body,
      size: 12.5 * px,
      fill: ink-faint,
      [Contiguous Town Leveling  ·  Digital Economy  ·  Emergent Roles  ·  Tectonic Navigation]
    )
  ])
)

// 4. Right Visual Illustration: Framed Surveyor's Parcel Map with Compass Rose
#place(top + right, dx: -44 * px, dy: 36 * px,
  box(
    width: 440 * px,
    height: 288 * px,
    fill: parchment-inset,
    stroke: 1.5pt + ink-primary,
    [
      // Inset Parcel Survey Map
      #place(top + left, survey-plot-map(440, 288, seed: 4096, accent: pigment-moss, cell-size: 28, cluster-target: 14))

      // Inner double-line border
      #place(top + left, dx: 6 * px, dy: 6 * px,
        rect(
          width: (440 - 12) * px,
          height: (288 - 12) * px,
          fill: none,
          stroke: 0.75pt + ink-faint
        )
      )

      // Hand-drawn-style Compass Rose in top-right corner
      #place(top + right, dx: -14 * px, dy: 14 * px,
        draw-compass-rose(size: 68pt)
      )

      // Bottom Map Legend Stamp
      #place(bottom + left, dx: 14 * px, dy: -12 * px,
        box(
          fill: parchment-base,
          stroke: 1pt + ink-primary,
          inset: (x: 8pt, y: 4pt),
          [
            #text(font: font-heading, size: 8pt, weight: "bold", fill: ink-primary, tracking: 1pt, [SURVEY PLATE I — CONTIGUOUS CLAIMS])
          ]
        )
      )
    ]
  )
)
