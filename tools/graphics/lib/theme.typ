// SMP Mod Suite - Design Theme & Reusable UI Components
#let tokens = toml("../tokens.toml")

#let px = 1pt

// Color Definitions
#let bg-canvas = rgb(tokens.colors.bg_canvas)
#let bg-surface = rgb(tokens.colors.bg_surface)
#let bg-elevated = rgb(tokens.colors.bg_elevated)
#let border-subtle = rgb(tokens.colors.border_subtle)
#let border-accent = rgb(tokens.colors.border_accent)
#let text-primary = rgb(tokens.colors.text_primary)
#let text-secondary = rgb(tokens.colors.text_secondary)
#let text-muted = rgb(tokens.colors.text_muted)

// Accent Palette
#let color-amber = rgb(tokens.colors.amber)
#let color-amethyst = rgb(tokens.colors.amethyst)
#let color-emerald = rgb(tokens.colors.emerald)
#let color-sky = rgb(tokens.colors.sky)
#let color-crimson = rgb(tokens.colors.crimson)
#let color-indigo = rgb(tokens.colors.indigo)

// Typography
#let font-sans = tokens.typography.font_sans
#let font-mono = tokens.typography.font_mono

/// Look up a subsystem by string ID from tokens.toml
#let get-subsystem(id) = {
  for s in tokens.subsystems {
    if s.id == id { return s }
  }
  tokens.subsystems.at(0)
}

/// Look up an emergent role by string ID from tokens.toml
#let get-role(id) = {
  for r in tokens.roles {
    if r.id == id { return r }
  }
  tokens.roles.at(0)
}

/// Crisp metadata pill tag
#let pill(text-label, accent: color-emerald, fill-color: none, text-color: none) = {
  let bg = if fill-color != none { fill-color } else { accent.transparentize(85%) }
  let fg = if text-color != none { text-color } else { accent }
  box(
    fill: bg,
    stroke: 1pt + accent.transparentize(60%),
    radius: 999pt,
    inset: (x: 10pt, y: 5pt),
    outset: 0pt,
    [
      #set text(font: font-mono, size: 10pt, weight: "bold", fill: fg)
      #text-label
    ]
  )
}

/// Elevated glass/obsidian card frame
#let card-panel(
  body,
  width: auto,
  height: auto,
  fill: bg-surface,
  stroke: border-subtle,
  radius: 8pt,
  inset: 16pt
) = {
  box(
    width: width,
    height: height,
    fill: fill,
    stroke: 1pt + stroke,
    radius: radius,
    inset: inset,
    body
  )
}
