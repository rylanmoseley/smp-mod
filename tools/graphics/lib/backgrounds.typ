// SMP Mod Suite - Cartographer's Survey & Aged Paper Procedural Backgrounds
// Pure CeTZ deterministic rendering with zero non-deterministic dependencies.
#import "@preview/cetz:0.5.3"
#import "theme.typ": *
#import "prng.typ" as prng

/// Deterministic aged paper texture with fiber specks and subtle edge wash.
#let paper-grain(w, h, seed: 1337) = {
  let s = seed
  let specks = ()
  for i in range(48) {
    let (s1, rx) = prng.rand-range(s, 10, w - 10)
    let (s2, ry) = prng.rand-range(s1, 10, h - 10)
    let (s3, rlen) = prng.rand-range(s2, 1.5, 4.0)
    let (s4, angle) = prng.rand-range(s3, 0, 3.14159)
    s = s4
    specks.push((rx, ry, rlen, angle))
  }

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Base paper wash
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Soft aged margin washes
    rect((0, 0), (w, h), stroke: (paint: parchment-edge.transparentize(80%), thickness: 3pt), fill: none)
    rect((3, 3), (w - 3, h - 3), stroke: (paint: parchment-edge.transparentize(90%), thickness: 4pt), fill: none)

    // Scattered tiny paper fiber dashes
    for sp in specks {
      let (x, y, l, a) = sp
      let dx = calc.cos(a) * l
      let dy = calc.sin(a) * l
      line((x, y), (x + dx, y + dy), stroke: 0.5pt + ink-faint.transparentize(75%))
    }
  })
}

/// Cartographer's Land Survey Plot Map.
/// Renders thin sepia dashed grid lines, a deterministic contiguous parcel BFS cluster
/// washed in translucent mineral pigment, solid 1.5pt charcoal boundary lines, and corner survey stakes.
#let survey-plot-map(
  w,
  h,
  seed: 42,
  accent: pigment-moss,
  cell-size: 32,
  cluster-target: 12
) = {
  let cols = calc.max(1, calc.floor(w / cell-size))
  let rows = calc.max(1, calc.floor(h / cell-size))

  // Seeded BFS expansion for contiguous claimed parcels
  let start-c = calc.floor(cols / 2)
  let start-r = calc.floor(rows / 2)
  let claimed = (str(start-c) + ":" + str(start-r): true)
  let queue = ((start-c, start-r),)
  let s = seed

  while queue.len() > 0 and claimed.len() < cluster-target {
    let curr = queue.at(0)
    queue = queue.slice(1)
    let (cc, cr) = curr

    let neighbors = (
      (cc + 1, cr),
      (cc - 1, cr),
      (cc, cr + 1),
      (cc, cr - 1),
    )

    // Shuffle neighbors deterministically
    let (s_next, r_ord) = prng.next-rand(s)
    s = s_next
    if r_ord > 0.5 {
      neighbors = (neighbors.at(2), neighbors.at(0), neighbors.at(3), neighbors.at(1))
    }

    for nb in neighbors {
      let (nc, nr) = nb
      if nc >= 0 and nc < cols and nr >= 0 and nr < rows {
        let key = str(nc) + ":" + str(nr)
        if key not in claimed and claimed.len() < cluster-target {
          claimed.insert(key, true)
          queue.push((nc, nr))
        }
      }
    }
  }

  // Pre-calculate cluster boundary segments
  let boundaries = ()
  for key in claimed.keys() {
    let parts = key.split(":")
    let c = int(parts.at(0))
    let r = int(parts.at(1))

    // Check North (r + 1)
    if (str(c) + ":" + str(r + 1)) not in claimed {
      boundaries.push((c * cell-size, (r + 1) * cell-size, (c + 1) * cell-size, (r + 1) * cell-size))
    }
    // Check South (r - 1)
    if (str(c) + ":" + str(r - 1)) not in claimed {
      boundaries.push((c * cell-size, r * cell-size, (c + 1) * cell-size, r * cell-size))
    }
    // Check East (c + 1)
    if (str(c + 1) + ":" + str(r)) not in claimed {
      boundaries.push(((c + 1) * cell-size, r * cell-size, (c + 1) * cell-size, (r + 1) * cell-size))
    }
    // Check West (c - 1)
    if (str(c - 1) + ":" + str(r)) not in claimed {
      boundaries.push((c * cell-size, r * cell-size, c * cell-size, (r + 1) * cell-size))
    }
  }

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Background paper
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Dashed surveyor grid lines
    for c in range(0, cols + 1) {
      let x = c * cell-size
      line((x, 0), (x, h), stroke: (paint: ink-faint.transparentize(65%), thickness: 0.6pt, dash: "dashed"))
    }
    for r in range(0, rows + 1) {
      let y = r * cell-size
      line((0, y), (w, y), stroke: (paint: ink-faint.transparentize(65%), thickness: 0.6pt, dash: "dashed"))
    }

    // Corner survey stakes (+) at grid intersections
    for c in range(0, cols + 1, step: 2) {
      for r in range(0, rows + 1, step: 2) {
        let x = c * cell-size
        let y = r * cell-size
        line((x - 2.5, y), (x + 2.5, y), stroke: 0.75pt + ink-secondary.transparentize(50%))
        line((x, y - 2.5), (x, y + 2.5), stroke: 0.75pt + ink-secondary.transparentize(50%))
      }
    }

    // Contiguous Claimed Parcels (14% mineral wash)
    for key in claimed.keys() {
      let parts = key.split(":")
      let c = int(parts.at(0))
      let r = int(parts.at(1))
      let x0 = c * cell-size
      let y0 = r * cell-size
      rect((x0, y0), (x0 + cell-size, y0 + cell-size), fill: accent.transparentize(86%), stroke: none)
    }

    // Perimeter boundary lines (solid 1.5pt charcoal ink)
    for b in boundaries {
      let (x0, y0, x1, y1) = b
      line((x0, y0), (x1, y1), stroke: 1.5pt + ink-primary)
    }
  })
}

/// Cartographer's Topographic Atlas Background.
/// Renders stepped elevation contour curves, peak marks (▲), and margin degree ticks.
#let topographic-atlas(w, h, seed: 777, accent: pigment-cobalt) = {
  let step-x = 40
  let step-y = 30
  let cols = calc.floor(w / step-x)
  let rows = calc.floor(h / step-y)

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Paper background
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Margin degree ticks
    for x in range(20, int(w), step: 25) {
      line((x, 0), (x, 4), stroke: 0.75pt + ink-secondary)
      line((x, h), (x, h - 4), stroke: 0.75pt + ink-secondary)
    }
    for y in range(20, int(h), step: 25) {
      line((0, y), (4, y), stroke: 0.75pt + ink-secondary)
      line((w, y), (w - 4, y), stroke: 0.75pt + ink-secondary)
    }

    // Stepped elevation curves
    for layer in (0.35, 0.48, 0.62, 0.75) {
      let pts = ()
      for c in range(0, cols + 1) {
        let x = c * step-x
        let n = prng.noise2d(float(c) * 0.4, layer * 3.0, seed)
        let y = layer * h + (n - 0.5) * 55
        pts.push((x, calc.max(5.0, calc.min(h - 5.0, y))))
      }
      for i in range(pts.len() - 1) {
        let p0 = pts.at(i)
        let p1 = pts.at(i + 1)
        line(p0, p1, stroke: 0.85pt + ink-secondary.transparentize(40%))
      }
    }

    // Triangle alpine peaks
    let peaks = (
      (w * 0.25, h * 0.72),
      (w * 0.68, h * 0.65),
      (w * 0.45, h * 0.35),
    )
    for pk in peaks {
      let (px, py) = pk
      line((px - 5, py - 4), (px, py + 5), stroke: 1pt + ink-primary)
      line((px, py + 5), (px + 5, py - 4), stroke: 1pt + ink-primary)
      line((px - 5, py - 4), (px + 5, py - 4), stroke: 1pt + ink-primary)
    }
  })
}

/// Cartographer's Double-Entry Survey Ledger Background.
/// Renders authentic horizontal accounting rules, vertical debit/credit ledger columns,
/// and subtle mineral tally wash.
#let survey-ledger(w, h, seed: 100, accent: pigment-ochre) = {
  let row-h = 16
  let rows = calc.floor(h / row-h)

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Paper background
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Vertical column wash (balance column)
    rect((w * 0.58, 0), (w * 0.82, h), fill: accent.transparentize(92%), stroke: none)

    // Horizontal ledger rules
    for r in range(1, rows + 1) {
      let y = r * row-h
      line((0, y), (w, y), stroke: 0.5pt + ink-faint.transparentize(70%))
    }

    // Top ledger header double line
    let top-y = h - 22
    line((0, top-y), (w, top-y), stroke: 0.85pt + ink-secondary.transparentize(40%))
    line((0, top-y + 3), (w, top-y + 3), stroke: 0.5pt + ink-faint.transparentize(60%))

    // Vertical column divider rules
    let col-xs = (w * 0.28, w * 0.58, w * 0.82, w * 0.94)
    for cx in col-xs {
      line((cx, 0), (cx, h), stroke: 0.6pt + ink-secondary.transparentize(60%))
    }

    // Double rule on primary total column
    line((w * 0.58 - 2.5, 0), (w * 0.58 - 2.5, h), stroke: 0.6pt + accent.transparentize(55%))

    // Tally marks / balance registration ticks
    for r in range(2, rows - 1, step: 2) {
      let y = r * row-h + row-h * 0.5
      // Small currency registration hash
      line((w * 0.87, y - 3), (w * 0.89, y + 3), stroke: 0.75pt + accent.transparentize(50%))
      line((w * 0.90, y - 3), (w * 0.92, y + 3), stroke: 0.75pt + accent.transparentize(50%))
    }
  })
}

/// Archival Testament & Notary Wax Seal Register Background.
/// Renders an engraved notary seal, radiating sunburst rays, and delicate certificate border lines.
#let sealed-ledger(w, h, seed: 200, accent: pigment-plum) = {
  let cx = w * 0.68
  let cy = h * 0.50

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Paper background
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Faint diagonal security watermark waves
    for d in range(-6, 12, step: 2) {
      let offset = d * 35
      line((offset, 0), (offset + h * 0.7, h), stroke: 0.4pt + accent.transparentize(90%))
    }

    // Outer notched certificate seal frame
    circle((cx, cy), radius: 54, stroke: 1.25pt + accent.transparentize(40%), fill: accent.transparentize(95%))
    circle((cx, cy), radius: 48, stroke: (paint: accent.transparentize(50%), thickness: 0.6pt, dash: "dashed"))
    circle((cx, cy), radius: 40, stroke: 0.75pt + ink-secondary.transparentize(45%))
    circle((cx, cy), radius: 26, stroke: 0.5pt + accent.transparentize(60%))

    // 16 Radiating sunburst ticks along seal perimeter
    for i in range(16) {
      let angle = i * 22.5 * 3.14159 / 180.0
      let r0 = 48.0
      let r1 = 54.0
      let x0 = cx + calc.cos(angle) * r0
      let y0 = cy + calc.sin(angle) * r0
      let x1 = cx + calc.cos(angle) * r1
      let y1 = cy + calc.sin(angle) * r1
      line((x0, y0), (x1, y1), stroke: 0.75pt + accent.transparentize(45%))
    }

    // Provenance registration corner stamps
    line((cx - 68, cy - 50), (cx - 68, cy - 38), stroke: 0.75pt + ink-secondary)
    line((cx - 68, cy - 50), (cx - 56, cy - 50), stroke: 0.75pt + ink-secondary)
    line((cx + 68, cy + 50), (cx + 68, cy + 38), stroke: 0.75pt + ink-secondary)
    line((cx + 68, cy + 50), (cx + 56, cy + 50), stroke: 0.75pt + ink-secondary)
  })
}

/// Naturalist's Specialization Bestiary & Combat Chart Background.
/// Renders 4-quadrant combat crosshairs, concentric target rings, and quadrant node markers.
#let field-bestiary(w, h, seed: 500, accent: pigment-vermilion) = {
  let cx = w * 0.65
  let cy = h * 0.50

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Paper background
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Concentric range rings
    circle((cx, cy), radius: 22, stroke: 0.75pt + accent.transparentize(60%))
    circle((cx, cy), radius: 44, stroke: (paint: accent.transparentize(45%), thickness: 0.85pt, dash: "dashed"))
    circle((cx, cy), radius: 64, stroke: 0.6pt + ink-secondary.transparentize(65%))

    // 4-Quadrant crosshair axes
    line((cx - 85, cy), (cx + 85, cy), stroke: 0.85pt + ink-secondary.transparentize(45%))
    line((cx, cy - 65), (cx, cy + 65), stroke: 0.85pt + ink-secondary.transparentize(45%))

    // Diagonal navigation diagonals
    let d = 52.0 * 0.707
    line((cx - d, cy - d), (cx + d, cy + d), stroke: (paint: ink-faint.transparentize(65%), thickness: 0.5pt, dash: "dotted"))
    line((cx - d, cy + d), (cx + d, cy - d), stroke: (paint: ink-faint.transparentize(65%), thickness: 0.5pt, dash: "dotted"))

    // Diamond target nodes for the four roles (DPS, Tank, Support, Scout)
    let nodes = (
      (cx + 34, cy + 28), // Top-Right (DPS)
      (cx - 34, cy + 28), // Top-Left (Tank)
      (cx - 34, cy - 28), // Bottom-Left (Support)
      (cx + 34, cy - 28), // Bottom-Right (Scout)
    )
    for n in nodes {
      let (nx, ny) = n
      line((nx - 4, ny), (nx, ny + 4), stroke: 1pt + accent)
      line((nx, ny + 4), (nx + 4, ny), stroke: 1pt + accent)
      line((nx + 4, ny), (nx, ny - 4), stroke: 1pt + accent)
      line((nx, ny - 4), (nx - 4, ny), stroke: 1pt + accent)
    }

    // Degree margin ticks along the outer ring
    for deg in (0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330) {
      let rad = deg * 3.14159 / 180.0
      let x0 = cx + calc.cos(rad) * 61
      let y0 = cy + calc.sin(rad) * 61
      let x1 = cx + calc.cos(rad) * 67
      let y1 = cy + calc.sin(rad) * 67
      line((x0, y0), (x1, y1), stroke: 0.65pt + ink-secondary.transparentize(50%))
    }
  })
}

/// Artisan Technical Drafting & Surveyor Benchmark Scale Background.
/// Renders precision drafting ruler ticks, technical crosshairs, and slate telemetry rules.
#let wrought-ledger(w, h, seed: 600, accent: pigment-slate) = {
  let cell = 24
  let cols = calc.floor(w / cell)
  let rows = calc.floor(h / cell)

  cetz.canvas(length: 1pt, {
    import cetz.draw: *

    // Paper background
    rect((0, 0), (w, h), fill: parchment-base, stroke: none)

    // Horizontal benchmark scan bands
    rect((0, h * 0.35), (w, h * 0.65), fill: accent.transparentize(94%), stroke: none)
    line((0, h * 0.35), (w, h * 0.35), stroke: 0.6pt + accent.transparentize(65%))
    line((0, h * 0.65), (w, h * 0.65), stroke: 0.6pt + accent.transparentize(65%))

    // Precision drafting ruler scale along top edge
    for x in range(10, int(w), step: 6) {
      let is-major = calc.rem(x - 10, 30) == 0
      let tick-h = if is-major { 8 } else { 4 }
      let s-thick = if is-major { 0.85pt } else { 0.5pt }
      line((x, h), (x, h - tick-h), stroke: s-thick + ink-secondary.transparentize(40%))
    }

    // Precision drafting ruler scale along bottom edge
    for x in range(10, int(w), step: 6) {
      let is-major = calc.rem(x - 10, 30) == 0
      let tick-h = if is-major { 8 } else { 4 }
      let s-thick = if is-major { 0.85pt } else { 0.5pt }
      line((x, 0), (x, tick-h), stroke: s-thick + ink-secondary.transparentize(40%))
    }

    // Surveyor grid crosshairs (+) across the canvas
    for c in range(1, cols) {
      for r in range(1, rows) {
        let x = c * cell
        let y = r * cell
        line((x - 2.5, y), (x + 2.5, y), stroke: 0.55pt + ink-secondary.transparentize(60%))
        line((x, y - 2.5), (x, y + 2.5), stroke: 0.55pt + ink-secondary.transparentize(60%))
      }
    }
  })
}

/// Central procedural cartographic motif dispatcher for subsystem backgrounds.
#let render-cartographic-motif(motif, w, h, accent: pigment-moss, seed: 100) = {
  if motif == "survey-ledger" {
    survey-ledger(w, h, seed: seed, accent: accent)
  } else if motif == "sealed-ledger" {
    sealed-ledger(w, h, seed: seed, accent: accent)
  } else if motif == "survey-parcels" {
    survey-plot-map(w, h, seed: seed, accent: accent, cell-size: 26, cluster-target: 8)
  } else if motif == "topographic-atlas" {
    topographic-atlas(w, h, seed: seed, accent: accent)
  } else if motif == "field-bestiary" {
    field-bestiary(w, h, seed: seed, accent: accent)
  } else if motif == "wrought-ledger" {
    wrought-ledger(w, h, seed: seed, accent: accent)
  } else {
    survey-plot-map(w, h, seed: seed, accent: accent, cell-size: 26, cluster-target: 8)
  }
}

/// Authentic field-guide plate framing: double rules and corner registration ticks (+).
#let expedition-border(w, h, inset: 12, outer-stroke: 2pt, inner-stroke: 0.75pt) = {
  cetz.canvas(length: 1pt, {
    import cetz.draw: *
    // Outer border
    rect((0, 0), (w, h), stroke: outer-stroke + ink-primary, fill: none)
    // Inner double-rule
    rect((inset, inset), (w - inset, h - inset), stroke: inner-stroke + ink-faint, fill: none)

    // Corner registration ticks (+)
    let ticks = (
      (inset, inset),
      (w - inset, inset),
      (inset, h - inset),
      (w - inset, h - inset),
    )
    for t in ticks {
      let (tx, ty) = t
      line((tx - 4, ty), (tx + 4, ty), stroke: 1pt + ink-primary)
      line((tx, ty - 4), (tx, ty + 4), stroke: 1pt + ink-primary)
    }
  })
}
