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
