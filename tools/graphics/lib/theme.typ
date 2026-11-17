// SMP Mod Suite - Expedition Parchment Theme & Visual System
// Pure Typst layout tokens, color definitions, and cartographic field-guide frames.
#let tokens = toml("../tokens.toml")

#let px = 1pt

// Surfaces
#let parchment-base    = rgb(tokens.colors.parchment_base)
#let parchment-inset   = rgb(tokens.colors.parchment_inset)
#let parchment-edge    = rgb(tokens.colors.parchment_edge)
#let leather-cover     = rgb(tokens.colors.leather_cover)

// Ink & Rules
#let ink-primary       = rgb(tokens.colors.ink_primary)
#let ink-secondary     = rgb(tokens.colors.ink_secondary)
#let ink-faint         = rgb(tokens.colors.ink_faint)
#let rule-border       = rgb(tokens.colors.rule_border)

// Earth & Mineral Pigments
#let pigment-ochre     = rgb(tokens.colors.pigment_ochre)
#let pigment-plum      = rgb(tokens.colors.pigment_plum)
#let pigment-moss      = rgb(tokens.colors.pigment_moss)
#let pigment-cobalt    = rgb(tokens.colors.pigment_cobalt)
#let pigment-vermilion = rgb(tokens.colors.pigment_vermilion)
#let pigment-slate     = rgb(tokens.colors.pigment_slate)

// Typography
#let font-display = tokens.typography.font_display
#let font-heading = tokens.typography.font_heading
#let font-body    = tokens.typography.font_body
#let font-pixel   = tokens.typography.font_pixel
#let font-mono    = tokens.typography.font_mono

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

/// Recessed ledger card panel (crisp 0pt corners, hairline ink border)
#let ledger-panel(
  body,
  width: auto,
  height: auto,
  fill: parchment-inset,
  stroke: rule-border,
  thickness: 1pt,
  inset: 12pt
) = {
  box(
    width: width,
    height: height,
    fill: fill,
    stroke: thickness + stroke,
    radius: 0pt,
    inset: inset,
    outset: 0pt,
    body
  )
}

/// Square mineral pigment chapter tab / wax badge
#let chapter-stamp(label, pigment: pigment-ochre, text-color: parchment-base, size: 10.5pt) = {
  box(
    fill: pigment,
    stroke: 1pt + ink-primary,
    radius: 0pt,
    inset: (x: 8pt, y: 3.5pt),
    outset: 0pt,
    [
      #set text(font: font-heading, size: size, weight: "bold", fill: text-color)
      #label
    ]
  )
}

/// Typographic metadata stamp (classic letterspaced uppercase)
#let meta-stamp(label, text-color: ink-secondary, size: 9.5pt) = {
  box(
    fill: parchment-inset,
    stroke: 0.75pt + rule-border,
    radius: 0pt,
    inset: (x: 7pt, y: 3pt),
    [
      #set text(font: font-heading, size: size, weight: "bold", fill: text-color)
      #label
    ]
  )
}

/// Field-guide horizontal hairline separator
#let ledger-rule(length: 100%, stroke: 0.75pt + rule-border) = {
  line(length: length, stroke: stroke)
}
