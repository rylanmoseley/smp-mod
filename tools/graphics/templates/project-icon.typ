// SMP Mod Suite - Project Icon Template (512x512)
// Parchment / Cartographer's Expedition Field-Guide Icon
#import "../lib/theme.typ": *
#import "../lib/icons.typ": render-emblem, draw-pixel-sprite, emblem-24, sprite-palette
#import "../lib/backgrounds.typ": paper-grain, expedition-border

#let variant = sys.inputs.at("variant", default: "mod")

#set page(
  width: 512 * px,
  height: 512 * px,
  margin: 0pt,
  fill: if variant == "modpack" { leather-cover } else { parchment-base }
)

#if variant == "mod" {
  // -------------------------------------------------------------------------
  // Mod Icon: Warm Parchment Atlas Plate (Zero Text)
  // -------------------------------------------------------------------------
  // 1. Paper grain texture & margin wash
  place(top + left, paper-grain(512, 512, seed: 101))

  // 2. Double-rule cartographic margin framing with corner registration ticks (+)
  place(top + left, expedition-border(512, 512, inset: 22, outer-stroke: 1.75pt, inner-stroke: 0.75pt))

  // 3. Centered 24x24 pixel-art emblem (~320pt x 320pt, ~62% of canvas)
  place(center + horizon,
    render-emblem(size: 320pt)
  )
} else {
  // -------------------------------------------------------------------------
  // Modpack Icon: Bound Saddle-Leather Tome with Inset Bookplate
  // -------------------------------------------------------------------------
  // 1. Leather cover base with stitched gold/ochre border
  place(top + left,
    rect(
      width: 512 * px,
      height: 512 * px,
      fill: leather-cover,
      stroke: (paint: pigment-ochre, thickness: 2.5pt)
    )
  )

  // Stitched hairline perimeter
  place(top + left, dx: 12 * px, dy: 12 * px,
    rect(
      width: (512 - 24) * px,
      height: (512 - 24) * px,
      fill: none,
      stroke: (paint: pigment-ochre.transparentize(40%), thickness: 1pt, dash: "dashed")
    )
  )

  // 2. Inset aged parchment bookplate (380pt x 380pt)
  place(center + horizon,
    box(
      width: 380 * px,
      height: 380 * px,
      fill: parchment-base,
      stroke: 2pt + ink-primary,
      [
        #place(top + left, paper-grain(380, 380, seed: 202))
        #place(top + left, expedition-border(380, 380, inset: 14, outer-stroke: 1pt, inner-stroke: 0.5pt))
        #place(center + horizon,
          render-emblem(size: 260pt)
        )
      ]
    )
  )
}
