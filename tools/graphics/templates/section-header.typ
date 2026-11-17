// SMP Mod Suite - Subsystem Section Header Strip (1200x160)
#import "../lib/theme.typ": px, bg-canvas, bg-surface, border-subtle, border-accent, text-primary, text-secondary, text-muted, font-sans, font-mono, pill, get-subsystem, tokens
#import "../lib/icons.typ": get-subsystem-icon
#import "../lib/backgrounds.typ": render-motif-bg

#let sub-id = sys.inputs.at("subsystem", default: "economy")
#let sub = get-subsystem(sub-id)
#let accent = rgb(sub.accent)

#set page(
  width: 1200 * px,
  height: 160 * px,
  margin: 0pt,
  fill: bg-canvas
)

#set text(
  font: font-sans,
  fill: text-primary
)

// Background canvas with subtle border frame
#place(top + left,
  rect(
    width: 1200 * px,
    height: 160 * px,
    fill: bg-surface,
    stroke: (top: 1pt + border-subtle, bottom: 1pt + border-subtle)
  )
)

// Right-side procedural motif background (faded)
#place(top + right,
  box(
    width: 480 * px,
    height: 160 * px,
    clip: true,
    [
      #render-motif-bg(sub.motif, 480, 160, accent, seed: int(sub.index) * 111 + 42)
      // Horizontal linear fade overlay
      #place(top + left,
        rect(
          width: 480 * px,
          height: 160 * px,
          fill: gradient.linear(
            angle: 0deg,
            (bg-surface, 0%),
            (bg-surface.transparentize(40%), 50%),
            (bg-surface.transparentize(90%), 100%)
          )
        )
      )
    ]
  )
)

// Left Accent Vertical Bar
#place(top + left,
  rect(
    width: 6 * px,
    height: 160 * px,
    fill: accent
  )
)

// Main Header Content
#place(top + left, dx: 36 * px, dy: 24 * px,
  box(width: 800 * px, [
    #stack(spacing: 8 * px,
      // Metadata Tagline
      stack(dir: ltr, spacing: 10 * px,
        text(
          font: font-mono,
          size: 11 * px,
          weight: "bold",
          fill: accent,
          tracking: 1.5pt,
          [SUBSYSTEM #sub.index \/\/ #upper(sub.name)]
        ),
        box(
          fill: accent.transparentize(85%),
          stroke: 0.8pt + accent.transparentize(50%),
          radius: 999pt,
          inset: (x: 8pt, y: 3pt),
          text(font: font-mono, size: 9 * px, weight: "bold", fill: accent, [ACTIVE])
        )
      ),

      // Icon + Subsystem Title
      stack(dir: ltr, spacing: 14 * px,
        get-subsystem-icon(sub.id, size: 36),
        align(horizon, [
          #text(size: 28 * px, weight: "bold", fill: white, sub.name)
        ])
      ),

      // Subtitle / Features
      text(
        size: 13 * px,
        fill: text-secondary,
        sub.subtitle
      )
    )
  ])
)
