// SMP Mod Suite - Cartographic Woodcut & Integer Pixel Sprites
// Pure CeTZ integer grid pixel-art renderer with zero anti-aliasing seams.
#import "@preview/cetz:0.5.3"
#import "theme.typ": *

// Standard Cartographer's Mineral Pigment & Iron-Gall Palette
#let sprite-palette = (
  "X": rgb("#231F1B"), // Iron-gall charcoal ink (outer outline & deep shadows)
  "B": rgb("#5A4E42"), // Warm sepia / umber (inner shadow)
  "W": rgb("#F2EAD7"), // Parchment base highlight
  "P": rgb("#E6DBC2"), // Recessed vellum tone
  "S": rgb("#8C7D6B"), // Carved stone / hairline ink
  "O": rgb("#B47B2B"), // Tarnished gold / ochre
  "Y": rgb("#D99E3D"), // Polished gold highlight
  "M": rgb("#3D6B48"), // Pine moss / territory green
  "G": rgb("#5B8A66"), // Fresh turf / leaf highlight
  "C": rgb("#355E7E"), // Cartographic cobalt blue
  "L": rgb("#5885A8"), // Sky horizon highlight
  "V": rgb("#A8382B"), // Iron oxide vermilion
  "F": rgb("#D15847"), // Beacon flame highlight
  "U": rgb("#7A3E5D"), // Imperial wax plum
  "Q": rgb("#A65B82"), // Soft plum seal highlight
  "T": rgb("#4D545E"), // Wrought iron slate
  "I": rgb("#7B8591"), // Polished steel / iron highlight
  "D": rgb("#684C32"), // Fertile loam earth brown
)

/// Pure integer pixel-art sprite renderer.
/// Merges horizontal pixel runs into single rectangles for optimal compilation and crisp edges.
#let draw-pixel-sprite(sprite-matrix, palette, size: 64pt) = {
  let n = sprite-matrix.len()
  let px-size = size / n
  cetz.canvas(length: 1pt, {
    import cetz.draw: *
    // Exact square canvas anchor to prevent bounding-box shrinkage on empty border pixels
    rect((0, 0), (size, size), stroke: none, fill: none)
    for r in range(n) {
      let row = sprite-matrix.at(r)
      let m = row.len()
      let y = (n - 1 - r) * px-size
      let c = 0
      while c < m {
        let ch = row.at(c)
        if ch in palette and palette.at(ch) != none {
          let run-len = 1
          while c + run-len < m and row.at(c + run-len) == ch {
            run-len += 1
          }
          let x0 = c * px-size
          let x1 = calc.min(size, (c + run-len) * px-size + 0.04pt)
          let y1 = calc.min(size, y + px-size + 0.04pt)
          rect((x0, y), (x1, y1), fill: palette.at(ch), stroke: none)
          c += run-len
        } else {
          c += 1
        }
      }
    }
  })
}

// ---------------------------------------------------------------------------
// 24x24 Primary Emblem: Warded Boundary Crest & Settlement Beacon
// ---------------------------------------------------------------------------
#let emblem-24 = (
  ".........XXXXXX.........",
  "........XXFFFFXX........",
  ".......XXFVYYVFXX.......",
  "......XXFVYYYYVFXX......",
  ".....XXFVYYYYYYVFXX.....",
  "....XXFFVYYYYYYVFFXX....",
  "...XXFVVVVYYYYVVVVFXX...",
  "..XXVVVVVVVVVVVVVVVVXX..",
  "XXXXXXXXXXXXXXXXXXXXXXXX",
  "XXSSSSSSSSSSSSSSSSSSSSXX",
  "XXSSSSMMMMMMMMMMMMSSSSXX",
  "XXSSSSMMMMMMMMMMMMSSSSXX",
  "XXSSSSSSMMMMMMMMSSSSSSXX",
  "XXSSSSSS..XXXX..SSSSSSXX",
  "XXSSSSSS.XXXXXX.SSSSSSXX",
  ".XXSSSSS..XXXX..SSSSSXX.",
  ".XXSSSSS.XXXXXX.SSSSSXX.",
  "..XXSSSS..XXXX..SSSSXX..",
  "..XXSSSSSSSSSSSSSSSSXX..",
  "...XXSSSSSSSSSSSSSSXX...",
  "....XXSSSSSSSSSSSSXX....",
  ".....XXSSSSSSSSSSXX.....",
  "......XXSSSSSSSSXX......",
  ".......XXXXXXXXXX.......",
)

// ---------------------------------------------------------------------------
// 16x16 Subsystem Sprites
// ---------------------------------------------------------------------------
#let sprite-economy-16 = (
  "................",
  ".....XXXXXX.....",
  "....XOOOOOOX....",
  "...XOYOYYOYOX...",
  "...XOYOYYOYOX...",
  "....XOOOOOOX....",
  "..XXXXXXXXXXXX..",
  ".XOOOOOOOOOOOOX.",
  ".XOYOYYOYOYYOYX.",
  ".XOOOOOOOOOOOOX.",
  "XXXXXXXXXXXXXXXX",
  "XOOOOOOOOOOOOOOX",
  "XOYOYYOYOYYOYOOX",
  "XOOOOOOOOOOOOOOX",
  "XXXXXXXXXXXXXXXX",
  "................",
)

#let sprite-death-recovery-16 = (
  "....XXXXXX......",
  "...XWWWWWWX.....",
  "..XWWUWWWWX.....",
  "..XWUUUWWWWX....",
  "..XWWUWWWWWWX...",
  "...XWWWWWWWWX...",
  "....XXXXXXXXX...",
  ".....XTTTTTX....",
  "....XTXXXXXTX...",
  "....XTX...XTX...",
  ".....XTXXXTX....",
  ".....XT...TX....",
  "....XTXXXXXTX...",
  "....XTTTTTTTX...",
  ".....XXXXXXX....",
  "................",
)

#let sprite-land-claims-16 = (
  ".......XX.......",
  "......XMMX......",
  ".....XMMMMX.....",
  "....XMMMMMMX....",
  "....XMMMMMMX....",
  ".....XMMMMX.....",
  "......XMMX......",
  ".......XX.......",
  ".......XX.......",
  "...XXXXXXXXXX...",
  "..XGGGGGGGGGGX..",
  "..XGGGGGGGGGGX..",
  "..XDDDDDDDDDDX..",
  "..XDDDDDDDDDDX..",
  "...XXXXXXXXXX...",
  "................",
)

#let sprite-world-navigation-16 = (
  ".......XX.......",
  "......XCCX......",
  ".....XCCCCX.....",
  "....XCCCCCCX....",
  "...XCCCCCCCCX...",
  "..XCCCCCCCCCCX..",
  ".XCCCCCCCCCCCCX.",
  ".XXXXXXXXXXXXXX.",
  "....XXXXXXXX....",
  "...XX..YY..XX...",
  "..XX..YYYY..XX..",
  "..XX...YY...XX..",
  "..XX...YY...XX..",
  "...XX......XX...",
  "....XXXXXXXX....",
  "................",
)

#let sprite-emergent-roles-16 = (
  "X..............X",
  ".XX..........XX.",
  "..XVX......XVX..",
  "...XVX....XVX...",
  "....XVX..XVX....",
  ".....XVXXVX.....",
  "......XVVX......",
  ".....XIIIIX.....",
  "....XIIIIIIX....",
  "...XIIIIIIIIX...",
  "..XIIIIXXIIIIX..",
  "..XIIIX..XIIIX..",
  "...XIX....XIX...",
  "....X......X....",
  "................",
  "................",
)

#let sprite-server-utilities-16 = (
  "......XXXX......",
  ".....XOOOOX.....",
  "....XOOOOOOX....",
  "...XOOOOOOOOX...",
  "...XOOOOOOOOX...",
  "..XOOOOOOOOOOX..",
  "..XOOOOOOOOOOX..",
  ".XOOOOOOOOOOOOX.",
  ".XXXXXXXXXXXXXX.",
  "......XTTX......",
  "......XTTX......",
  ".....XXXXXX.....",
  ".....XTTTTX.....",
  ".....XTTTTX.....",
  ".....XXXXXX.....",
  "................",
)

// ---------------------------------------------------------------------------
// 16x16 Role Sprites
// ---------------------------------------------------------------------------
#let sprite-role-dps-16 = (
  "..............XX",
  ".............XIX",
  "............XIX.",
  "...........XIX..",
  "..........XIX...",
  ".........XIX....",
  "........XIX.....",
  ".......XIX......",
  "......XIX.......",
  ".....XIX........",
  "....XXXX........",
  "...XVXVXX.......",
  "..XXVVXXX.......",
  "..X..XX.........",
  ".X..............",
  "X...............",
)

#let sprite-role-tank-16 = (
  "................",
  "..XXXXXXXXXXXX..",
  ".XTTTTTTTTTTTTX.",
  ".XTOOOOOOOOOOTX.",
  ".XTOOOOOOOOOOTX.",
  ".XTOOOXXXXOOOTX.",
  ".XTOOOXXXXOOOTX.",
  ".XTOOOXXXXOOOTX.",
  ".XTOOOOOOOOOOTX.",
  "..XTOOOOOOOOTX..",
  "..XTOOOOOOOOTX..",
  "...XTOOOOOOTX...",
  "....XTOOOOTX....",
  ".....XTOOTX.....",
  "......XXXX......",
  "................",
)

#let sprite-role-support-16 = (
  ".......XX.......",
  "......XSSX......",
  "......XSSX......",
  ".....XXXXXX.....",
  "....XMMMMMMX....",
  "...XMMYYYYMMX...",
  "...XMYYYYYYMX...",
  "...XMYYYYYYMX...",
  "...XMYYYYYYMX...",
  "...XMMYYYYMMX...",
  "....XMMMMMMX....",
  ".....XXXXXX.....",
  ".....XMMMMX.....",
  ".....XMMMMX.....",
  "......XXXX......",
  "................",
)

#let sprite-role-scout-16 = (
  "................",
  "................",
  "......XXX.......",
  ".....XCCX.......",
  "....XCCCCX......",
  "...XCCCCCCX.....",
  "...XCCCCCCX.....",
  "..XCCCCCCCCX....",
  "..XCCCCCCCCX....",
  ".XCCCCCCCCCCX...",
  "XXXXXXXXXXXXX...",
  "..XCCCCCCCCCCCX.",
  "..XCCCCCCCCCCCCX",
  "..XXXXXXXXXXXXXX",
  "................",
  "................",
)

/// High-level emblem renderer
#let render-emblem(size: 320pt) = {
  draw-pixel-sprite(emblem-24, sprite-palette, size: size)
}

/// Look up and render a subsystem sprite by ID
#let render-subsystem-sprite(subsystem-id, size: 64pt) = {
  let m = if subsystem-id == "economy" { sprite-economy-16 }
    else if subsystem-id == "death-recovery" { sprite-death-recovery-16 }
    else if subsystem-id == "land-claims" { sprite-land-claims-16 }
    else if subsystem-id == "world-navigation" { sprite-world-navigation-16 }
    else if subsystem-id == "emergent-roles" { sprite-emergent-roles-16 }
    else if subsystem-id == "server-utilities" { sprite-server-utilities-16 }
    else { sprite-economy-16 }
  draw-pixel-sprite(m, sprite-palette, size: size)
}

/// Look up and render a role sprite by ID
#let render-role-sprite(role-id, size: 48pt) = {
  let m = if role-id == "dps" { sprite-role-dps-16 }
    else if role-id == "tank" { sprite-role-tank-16 }
    else if role-id == "support" { sprite-role-support-16 }
    else if role-id == "scout" { sprite-role-scout-16 }
    else { sprite-role-dps-16 }
  draw-pixel-sprite(m, sprite-palette, size: size)
}

/// Cartographer's 8-Point Compass Rose
#let draw-compass-rose(size: 80pt) = {
  let r = size / 2
  let font-sz = calc.max(6pt, size * 0.09)
  cetz.canvas(length: 1pt, {
    import cetz.draw: *
    // Exact square canvas anchor
    rect((0, 0), (size, size), stroke: none, fill: none)
    circle((r, r), radius: r * 0.82, stroke: 1pt + ink-primary, fill: none)
    circle((r, r), radius: r * 0.76, stroke: 0.5pt + ink-faint, fill: none)
    circle((r, r), radius: r * 0.12, stroke: 1pt + ink-primary, fill: parchment-base)

    // Primary Compass Points (N, S, E, W)
    line((r, r * 0.22), (r, r * 1.78), stroke: 1.25pt + ink-primary)
    line((r * 0.22, r), (r * 1.78, r), stroke: 1.25pt + ink-primary)

    // Diagonal Points
    line((r * 0.40, r * 0.40), (r * 1.60, r * 1.60), stroke: 0.75pt + ink-secondary)
    line((r * 0.40, r * 1.60), (r * 1.60, r * 0.40), stroke: 0.75pt + ink-secondary)

    // Cardinal Letters (comfortably within square canvas bounds)
    content((r, r * 1.88), [
      #set text(font: font-heading, size: font-sz, weight: "bold", fill: ink-primary)
      N
    ])
    content((r * 1.88, r), [
      #set text(font: font-heading, size: font-sz, weight: "bold", fill: ink-primary)
      E
    ])
    content((r, r * 0.12), [
      #set text(font: font-heading, size: font-sz, weight: "bold", fill: ink-primary)
      S
    ])
    content((r * 0.12, r), [
      #set text(font: font-heading, size: font-sz, weight: "bold", fill: ink-primary)
      W
    ])
  })
}
