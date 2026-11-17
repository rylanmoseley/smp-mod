// SMP Mod Suite - Vector Icon & Emblem Library (CeTZ)
#import "@preview/cetz:0.5.3"
#import "theme.typ": color-emerald, color-amber, color-amethyst, color-sky, color-crimson, color-indigo, border-subtle, text-primary

/// Draws an isometric facet-shaded voxel chunk block.
#let draw-iso-chunk(cx, cy, r, color: color-emerald) = {
  let dx = r * 0.866
  let dy = r * 0.5
  let h = r * 1.0

  // Top facet (brightest)
  cetz.draw.line(
    (cx, cy + h),
    (cx + dx, cy + h - dy),
    (cx, cy + h - 2 * dy),
    (cx - dx, cy + h - dy),
    close: true,
    fill: color.lighten(30%),
    stroke: (paint: color.lighten(50%), thickness: 1.2pt)
  )

  // Left facet (medium)
  cetz.draw.line(
    (cx - dx, cy + h - dy),
    (cx, cy + h - 2 * dy),
    (cx, cy - dy),
    (cx - dx, cy),
    close: true,
    fill: color,
    stroke: (paint: color.lighten(20%), thickness: 1.2pt)
  )

  // Right facet (darker)
  cetz.draw.line(
    (cx + dx, cy + h - dy),
    (cx, cy + h - 2 * dy),
    (cx, cy - dy),
    (cx + dx, cy),
    close: true,
    fill: color.darken(25%),
    stroke: (paint: color, thickness: 1.2pt)
  )
}

/// Main smpmod project emblem: 3 interlocked isometric chunk voxels forming a stylized settlement cluster.
#let project-emblem(size: 64, variant: "mod") = {
  let s = size / 64.0
  let r = 16.0 * s

  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))

    // Center coordinates
    let mx = size * 0.5
    let my = size * 0.44

    // Left block (Emerald)
    draw-iso-chunk(mx - r * 0.75, my - r * 0.45, r * 0.9, color: color-emerald)

    // Right block (Sky)
    draw-iso-chunk(mx + r * 0.75, my - r * 0.45, r * 0.9, color: color-sky)

    // Top elevated block (Amber)
    draw-iso-chunk(mx, my + r * 0.75, r * 1.0, color: color-amber)

    // Core beacon light pulse at intersection
    cetz.draw.circle(
      (mx, my + r * 0.35),
      radius: 3.5 * s,
      fill: white,
      stroke: (paint: color-emerald.lighten(40%), thickness: 1.5pt)
    )

    // If modpack variant, add subtle corner package crate badge
    if variant == "modpack" {
      let bx = size * 0.82
      let by = size * 0.18
      let br = 8.0 * s
      cetz.draw.rect(
        (bx - br, by - br),
        (bx + br, by + br),
        fill: rgb("#A855F7"),
        stroke: (paint: white, thickness: 1.5pt),
        radius: 2pt
      )
      // Package ribbon cross
      cetz.draw.line((bx - br, by), (bx + br, by), stroke: (paint: white, thickness: 1pt))
      cetz.draw.line((bx, by - br), (bx, by + br), stroke: (paint: white, thickness: 1pt))
    }
  })
}

// -----------------------------------------------------------------------------
// Subsystem Vector Icons (32x32 baseline)
// -----------------------------------------------------------------------------

#let icon-economy(size: 32, color: color-amber) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Outer coin circle
    cetz.draw.circle((16 * s, 16 * s), radius: 13 * s, fill: color.transparentize(85%), stroke: (paint: color, thickness: 2pt * s))
    // Inner diamond currency symbol
    cetz.draw.line((16 * s, 7 * s), (23 * s, 16 * s), (16 * s, 25 * s), (9 * s, 16 * s), close: true, fill: color.transparentize(40%), stroke: (paint: color, thickness: 1.5pt * s))
    // Center dot
    cetz.draw.circle((16 * s, 16 * s), radius: 2 * s, fill: white, stroke: none)
  })
}

#let icon-death-recovery(size: 32, color: color-amethyst) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Shield boundary
    cetz.draw.line((7 * s, 26 * s), (25 * s, 26 * s), (25 * s, 16 * s), (16 * s, 5 * s), (7 * s, 16 * s), close: true, fill: color.transparentize(85%), stroke: (paint: color, thickness: 2pt * s))
    // Restoring heart / upward soul flare
    cetz.draw.line((16 * s, 9 * s), (21 * s, 16 * s), (18 * s, 16 * s), (18 * s, 22 * s), (14 * s, 22 * s), (14 * s, 16 * s), (11 * s, 16 * s), close: true, fill: color, stroke: none)
  })
}

#let icon-land-claims(size: 32, color: color-emerald) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // 2x2 chunk parcel boundary
    cetz.draw.rect((6 * s, 6 * s), (26 * s, 26 * s), fill: none, stroke: (paint: color.transparentize(50%), thickness: 1.2pt * s))
    cetz.draw.line((16 * s, 6 * s), (16 * s, 26 * s), stroke: (paint: color.transparentize(60%), thickness: 1pt * s))
    cetz.draw.line((6 * s, 16 * s), (26 * s, 16 * s), stroke: (paint: color.transparentize(60%), thickness: 1pt * s))
    // Highlighted claimed parcel (top right)
    cetz.draw.rect((16 * s, 16 * s), (26 * s, 26 * s), fill: color.transparentize(70%), stroke: (paint: color, thickness: 1.5pt * s))
    // Flag pole
    cetz.draw.line((11 * s, 9 * s), (11 * s, 23 * s), stroke: (paint: white, thickness: 1.5pt * s))
    cetz.draw.line((11 * s, 23 * s), (18 * s, 19.5 * s), (11 * s, 16 * s), close: true, fill: color, stroke: none)
  })
}

#let icon-world-navigation(size: 32, color: color-sky) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Mountain peaks
    cetz.draw.line((4 * s, 8 * s), (13 * s, 24 * s), (19 * s, 15 * s), (28 * s, 28 * s), (28 * s, 8 * s), close: true, fill: color.transparentize(85%), stroke: (paint: color, thickness: 1.8pt * s))
    // Snow caps
    cetz.draw.line((13 * s, 24 * s), (11 * s, 20 * s), (14 * s, 20 * s), (15 * s, 21 * s), close: true, fill: white, stroke: none)
    // Glider chevron
    cetz.draw.line((9 * s, 15 * s), (16 * s, 19 * s), (23 * s, 15 * s), stroke: (paint: white, thickness: 1.5pt * s))
  })
}

#let icon-emergent-roles(size: 32, color: color-crimson) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Central diamond
    cetz.draw.line((16 * s, 6 * s), (26 * s, 16 * s), (16 * s, 26 * s), (6 * s, 16 * s), close: true, fill: color.transparentize(85%), stroke: (paint: color, thickness: 1.5pt * s))
    // 4 Role nodes
    cetz.draw.circle((16 * s, 6 * s), radius: 2.5 * s, fill: color-crimson, stroke: none) // DPS (Top)
    cetz.draw.circle((26 * s, 16 * s), radius: 2.5 * s, fill: color-amber, stroke: none)   // Tank (Right)
    cetz.draw.circle((16 * s, 26 * s), radius: 2.5 * s, fill: color-emerald, stroke: none) // Support (Bottom)
    cetz.draw.circle((6 * s, 16 * s), radius: 2.5 * s, fill: color-sky, stroke: none)     // Scout (Left)
    // Cross connecting axes
    cetz.draw.line((16 * s, 8.5 * s), (16 * s, 23.5 * s), stroke: (paint: white.transparentize(30%), thickness: 1.2pt * s))
    cetz.draw.line((8.5 * s, 16 * s), (23.5 * s, 16 * s), stroke: (paint: white.transparentize(30%), thickness: 1.2pt * s))
  })
}

#let icon-server-utilities(size: 32, color: color-indigo) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Console window
    cetz.draw.rect((5 * s, 7 * s), (27 * s, 25 * s), fill: color.transparentize(85%), stroke: (paint: color, thickness: 1.8pt * s), radius: 3pt * s)
    // Top window bar
    cetz.draw.line((5 * s, 21 * s), (27 * s, 21 * s), stroke: (paint: color.transparentize(40%), thickness: 1pt * s))
    cetz.draw.circle((8 * s, 23 * s), radius: 1 * s, fill: color, stroke: none)
    cetz.draw.circle((11 * s, 23 * s), radius: 1 * s, fill: color, stroke: none)
    // Prompt `> _`
    cetz.draw.line((9 * s, 16 * s), (13 * s, 13 * s), (9 * s, 10 * s), stroke: (paint: white, thickness: 1.5pt * s))
    cetz.draw.line((15 * s, 10 * s), (20 * s, 10 * s), stroke: (paint: color.lighten(30%), thickness: 1.5pt * s))
  })
}

/// Dispatcher for subsystem icons by string ID
#let get-subsystem-icon(id, size: 32) = {
  if id == "economy" { icon-economy(size: size) }
  else if id == "death-recovery" { icon-death-recovery(size: size) }
  else if id == "land-claims" { icon-land-claims(size: size) }
  else if id == "world-navigation" { icon-world-navigation(size: size) }
  else if id == "emergent-roles" { icon-emergent-roles(size: size) }
  else if id == "server-utilities" { icon-server-utilities(size: size) }
  else { icon-economy(size: size) }
}

// -----------------------------------------------------------------------------
// Role Glyphs (32x32 baseline)
// -----------------------------------------------------------------------------

#let icon-role-dps(size: 32, color: color-crimson) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Crossed swords
    cetz.draw.line((7 * s, 7 * s), (25 * s, 25 * s), stroke: (paint: color, thickness: 2.2pt * s))
    cetz.draw.line((25 * s, 7 * s), (7 * s, 25 * s), stroke: (paint: color, thickness: 2.2pt * s))
    // Crossguards
    cetz.draw.line((8 * s, 12 * s), (12 * s, 8 * s), stroke: (paint: white, thickness: 1.5pt * s))
    cetz.draw.line((20 * s, 8 * s), (24 * s, 12 * s), stroke: (paint: white, thickness: 1.5pt * s))
    // Center impact spark
    cetz.draw.circle((16 * s, 16 * s), radius: 2.5 * s, fill: white, stroke: none)
  })
}

#let icon-role-tank(size: 32, color: color-amber) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Heavy tower shield
    cetz.draw.line((6 * s, 26 * s), (26 * s, 26 * s), (26 * s, 16 * s), (16 * s, 5 * s), (6 * s, 16 * s), close: true, fill: color.transparentize(80%), stroke: (paint: color, thickness: 2.2pt * s))
    // Shield reinforced center cross
    cetz.draw.line((16 * s, 9 * s), (16 * s, 23 * s), stroke: (paint: white, thickness: 1.8pt * s))
    cetz.draw.line((10 * s, 18 * s), (22 * s, 18 * s), stroke: (paint: white, thickness: 1.8pt * s))
  })
}

#let icon-role-support(size: 32, color: color-emerald) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Radiant beacon star
    cetz.draw.line((16 * s, 5 * s), (18 * s, 13 * s), (26 * s, 16 * s), (18 * s, 19 * s), (16 * s, 27 * s), (14 * s, 19 * s), (6 * s, 16 * s), (14 * s, 13 * s), close: true, fill: color.transparentize(75%), stroke: (paint: color, thickness: 2pt * s))
    // Center healing gem
    cetz.draw.circle((16 * s, 16 * s), radius: 3 * s, fill: white, stroke: none)
  })
}

#let icon-role-scout(size: 32, color: color-sky) = {
  let s = size / 32.0
  cetz.canvas(length: 1pt, {
    cetz.draw.set-style(stroke: (cap: "round", join: "round"))
    // Pathfinder compass ring
    cetz.draw.circle((16 * s, 16 * s), radius: 11 * s, fill: color.transparentize(85%), stroke: (paint: color, thickness: 1.8pt * s))
    // Needle pointer
    cetz.draw.line((16 * s, 7 * s), (19 * s, 16 * s), (16 * s, 14 * s), (13 * s, 16 * s), close: true, fill: color, stroke: none)
    cetz.draw.line((16 * s, 25 * s), (19 * s, 16 * s), (16 * s, 18 * s), (13 * s, 16 * s), close: true, fill: white, stroke: none)
  })
}

/// Dispatcher for role icons by string ID
#let get-role-icon(id, size: 32) = {
  if id == "dps" { icon-role-dps(size: size) }
  else if id == "tank" { icon-role-tank(size: size) }
  else if id == "support" { icon-role-support(size: size) }
  else if id == "scout" { icon-role-scout(size: size) }
  else { icon-role-dps(size: size) }
}
