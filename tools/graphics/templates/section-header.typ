// SMP Mod Suite - Subsystem Section Header Strip (1200x160)
// Cartographer's Field Guide Chapter Header Strip
#import "../lib/theme.typ": *
#import "../lib/icons.typ": render-subsystem-sprite
#import "../lib/backgrounds.typ": paper-grain, render-cartographic-motif, expedition-border

#let sub-id = sys.inputs.at("subsystem", default: "economy")
#let sub = get-subsystem(sub-id)
#let accent = rgb(sub.accent)

#set page(
  width: 1200 * px,
  height: 160 * px,
  margin: 0pt,
  fill: parchment-base
)

#set text(
  font: font-body,
  fill: ink-primary
)

// 1. Paper grain base
#place(top + left, paper-grain(1200, 160, seed: 3300 + int(sub.index) * 17))

// 2. Right-side subtle cartographic engraving matching subsystem motif
#place(top + right,
  box(
    width: 440 * px,
    height: 160 * px,
    clip: true,
    [
      #render-cartographic-motif(sub.motif, 440, 160, accent: accent, seed: 500 + int(sub.index) * 99)

      // Smooth horizontal paper wash gradient fade
      #place(top + left,
        rect(
          width: 440 * px,
          height: 160 * px,
          fill: gradient.linear(
            angle: 0deg,
            (parchment-base, 0%),
            (parchment-base.transparentize(20%), 40%),
            (parchment-base.transparentize(85%), 100%)
          )
        )
      )
    ]
  )
)

// 3. Crisp outer 2pt iron-gall border + hairline inner border
#place(top + left,
  rect(
    width: 1200 * px,
    height: 160 * px,
    stroke: 2pt + ink-primary,
    fill: none
  )
)
#place(top + left, dx: 4 * px, dy: 4 * px,
  rect(
    width: (1200 - 8) * px,
    height: (160 - 8) * px,
    stroke: 0.75pt + rule-border,
    fill: none
  )
)

// 4. Solid mineral pigment stripe on the far left edge
#place(top + left,
  rect(
    width: 7 * px,
    height: 160 * px,
    fill: accent,
    stroke: none
  )
)

// 5. Left Stamp Box: 70x70 pixel sprite in square parchment stamp box
#place(top + left, dx: 32 * px, dy: 32 * px,
  box(
    width: 96 * px,
    height: 96 * px,
    fill: parchment-inset,
    stroke: 1.5pt + ink-primary,
    [
      #place(center + horizon,
        render-subsystem-sprite(sub.id, size: 70pt)
      )
    ]
  )
)

// 6. Header Typography & Grounded Subsystem Copy
#place(top + left, dx: 150 * px, dy: 36 * px,
  box(width: 820 * px, [
    #stack(
      spacing: 10 * px,
      text(
        font: font-heading,
        size: 14 * px,
        weight: "bold",
        fill: accent,
        tracking: 2pt,
        [CHAPTER #sub.numeral   —   #upper(sub.at("kicker", default: sub.name))]
      ),
      text(
        font: font-display,
        size: 34 * px,
        weight: "bold",
        fill: ink-primary,
        tracking: -0.25pt,
        sub.name
      ),
      text(
        font: font-body,
        size: 17 * px,
        weight: "medium",
        fill: ink-secondary,
        sub.subtitle
      )
    )
  ])
)
