// SMP Mod Suite - Procedural Background Generators (CeTZ)
#import "@preview/cetz:0.5.3"
#import "prng.typ": seed-from-str, next-rand, rand-int, rand-range, noise2d
#import "theme.typ": bg-canvas, bg-surface, border-subtle, border-accent

/// Renders a contiguous BFS chunk cluster grid mirroring smpmod's contiguous claim leveling.
#let chunk-cluster-grid(
  w,
  h,
  seed: 1337,
  accent: rgb("#10B981"),
  cell-size: 28,
  cluster-size: 14,
  secondary-cluster: true
) = {
  let cols = calc.max(calc.floor(w / cell-size), 2)
  let rows = calc.max(calc.floor(h / cell-size), 2)

  let state = seed

  // Cluster 1 generation via BFS flood-fill
  let start-c = calc.floor(cols * 0.55)
  let start-r = calc.floor(rows * 0.50)
  let cluster = ((start-c, start-r),)

  let in-cluster(c, r, list) = {
    for pt in list {
      if pt.at(0) == c and pt.at(1) == r { return true }
    }
    false
  }

  for step in range(cluster-size - 1) {
    let candidates = ()
    for pt in cluster {
      let pc = pt.at(0)
      let pr = pt.at(1)
      let neighbors = ((pc + 1, pr), (pc - 1, pr), (pc, pr + 1), (pc, pr - 1))
      for n in neighbors {
        let nc = n.at(0)
        let nr = n.at(1)
        if nc >= 0 and nc < cols and nr >= 0 and nr < rows {
          if not in-cluster(nc, nr, cluster) and not in-cluster(nc, nr, candidates) {
            candidates.push((nc, nr))
          }
        }
      }
    }
    if candidates.len() > 0 {
      let pick-idx = 0
      (state, pick-idx) = rand-int(state, 0, candidates.len() - 1)
      cluster.push(candidates.at(pick-idx))
    }
  }

  // Optional secondary outpost cluster
  let outpost = ()
  if secondary-cluster and cols > 8 and rows > 6 {
    let out_c = calc.max(start-c - 5, 1)
    let out_r = calc.min(start-r + 4, rows - 2)
    outpost.push((out_c, out_r))
    let out-candidates = ((out_c + 1, out_r), (out_c, out_r - 1), (out_c - 1, out_r))
    for n in out-candidates {
      if n.at(0) >= 0 and n.at(0) < cols and n.at(1) >= 0 and n.at(1) < rows {
        outpost.push(n)
      }
    }
  }

  cetz.canvas(length: 1pt, {
    // Base canvas rectangle
    cetz.draw.rect((0, 0), (w, h), fill: bg-canvas, stroke: none)

    // Unclaimed background chunk grid
    for c in range(cols) {
      for r in range(rows) {
        let x0 = c * cell-size
        let y0 = r * cell-size
        let is-main = in-cluster(c, r, cluster)
        let is-outpost = in-cluster(c, r, outpost)

        if is-main {
          // Claimed contiguous town parcel
          cetz.draw.rect(
            (x0, y0),
            (x0 + cell-size, y0 + cell-size),
            fill: accent.transparentize(82%),
            stroke: (paint: accent.transparentize(30%), thickness: 1.5pt)
          )
          // Center core marker on initial founder chunk
          if c == start-c and r == start-r {
            cetz.draw.rect(
              (x0 + cell-size * 0.25, y0 + cell-size * 0.25),
              (x0 + cell-size * 0.75, y0 + cell-size * 0.75),
              fill: accent.transparentize(40%),
              stroke: (paint: accent, thickness: 1pt)
            )
          }
        } else if is-outpost {
          // Claimed outpost parcel (different accent tint)
          cetz.draw.rect(
            (x0, y0),
            (x0 + cell-size, y0 + cell-size),
            fill: rgb("#0EA5E9").transparentize(85%),
            stroke: (paint: rgb("#0EA5E9").transparentize(50%), thickness: 1.2pt)
          )
        } else {
          // Regular unclaimed wilderness chunk
          cetz.draw.rect(
            (x0, y0),
            (x0 + cell-size, y0 + cell-size),
            fill: none,
            stroke: (paint: border-subtle.transparentize(65%), thickness: 0.5pt)
          )
        }
      }
    }

    // Grid coordinates markers on corners
    for c in range(0, cols, step: 4) {
      for r in range(0, rows, step: 4) {
        let px = c * cell-size
        let py = r * cell-size
        cetz.draw.circle((px, py), radius: 1.5, fill: border-accent.transparentize(40%), stroke: none)
      }
    }
  })
}

/// Renders smooth topographic elevation contours evoking Tectonic alpine peaks.
#let topographic-contours(
  w,
  h,
  seed: 42,
  accent: rgb("#0EA5E9"),
  bands: 5
) = {
  cetz.canvas(length: 1pt, {
    cetz.draw.rect((0, 0), (w, h), fill: bg-canvas, stroke: none)

    let segments = 24
    let dx = w / segments

    for band in range(bands) {
      let b-pct = float(band) / float(bands)
      let base-y = h * (0.20 + b-pct * 0.65)
      let amp = h * (0.12 - b-pct * 0.015)
      let k1 = (1.5 + float(band) * 0.8) * 3.14159 / w
      let k2 = (3.2 + float(band) * 1.2) * 3.14159 / w
      let phi = float(seed + band * 73) * 0.35

      let pts = ()
      for i in range(segments + 1) {
        let x = i * dx
        let y = base-y + amp * calc.sin(k1 * x + phi) + (amp * 0.45) * calc.sin(k2 * x - phi * 0.7)
        pts.push((x, y))
      }

      // Draw elevation stroke line
      let stroke-color = accent.transparentize(30% + b-pct * 40%)
      for i in range(pts.len() - 1) {
        cetz.draw.line(pts.at(i), pts.at(i + 1), stroke: (paint: stroke-color, thickness: 1.5pt))
      }

      // Sampled elevation contour dots
      if pts.len() > 6 {
        for i in range(2, pts.len() - 2, step: 4) {
          let p = pts.at(i)
          cetz.draw.circle(p, radius: 2.0, fill: stroke-color, stroke: none)
        }
      }
    }
  })
}

/// Renders an aligned dot matrix with radial falloff.
#let dot-matrix(
  w,
  h,
  seed: 99,
  accent: rgb("#F59E0B"),
  spacing: 22
) = {
  let cols = calc.floor(w / spacing) + 1
  let rows = calc.floor(h / spacing) + 1
  let cx = w * 0.5
  let cy = h * 0.5
  let max-dist = calc.sqrt(cx * cx + cy * cy)

  cetz.canvas(length: 1pt, {
    cetz.draw.rect((0, 0), (w, h), fill: bg-canvas, stroke: none)

    for c in range(cols) {
      for r in range(rows) {
        let x = c * spacing
        let y = r * spacing
        let d = calc.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
        let intensity = calc.max(1.0 - (d / max-dist), 0.1)

        let dot-r = 1.0 + intensity * 2.0
        let alpha = 85% - intensity * 55%

        cetz.draw.circle(
          (x, y),
          radius: dot-r,
          fill: accent.transparentize(alpha),
          stroke: none
        )
      }
    }
  })
}

/// Renders a quadrant radar motif for Emergent Roles.
#let quadrant-radar(
  w,
  h,
  seed: 777,
  accent: rgb("#F43F5E")
) = {
  let cx = w * 0.5
  let cy = h * 0.5
  let r-max = calc.min(w, h) * 0.45

  cetz.canvas(length: 1pt, {
    cetz.draw.rect((0, 0), (w, h), fill: bg-canvas, stroke: none)

    // Concentric range rings
    for ring in (0.25, 0.50, 0.75, 1.0) {
      let r = r-max * ring
      cetz.draw.circle(
        (cx, cy),
        radius: r,
        fill: none,
        stroke: (paint: accent.transparentize(80% - ring * 15%), thickness: 1pt)
      )
    }

    // Crosshairs
    cetz.draw.line((cx - r-max * 1.1, cy), (cx + r-max * 1.1, cy), stroke: (paint: border-accent.transparentize(40%), thickness: 1pt))
    cetz.draw.line((cx, cy - r-max * 1.1), (cx, cy + r-max * 1.1), stroke: (paint: border-accent.transparentize(40%), thickness: 1pt))

    // Diagonal axis dividers
    let d = r-max * 0.707
    cetz.draw.line((cx - d, cy - d), (cx + d, cy + d), stroke: (paint: border-subtle.transparentize(60%), thickness: 0.8pt))
    cetz.draw.line((cx - d, cy + d), (cx + d, cy - d), stroke: (paint: border-subtle.transparentize(60%), thickness: 0.8pt))
  })
}

/// Renders a technical terminal grid for server utilities.
#let terminal-grid(
  w,
  h,
  seed: 555,
  accent: rgb("#6366F1"),
  cell: 30
) = {
  let cols = calc.floor(w / cell) + 1
  let rows = calc.floor(h / cell) + 1

  cetz.canvas(length: 1pt, {
    cetz.draw.rect((0, 0), (w, h), fill: bg-canvas, stroke: none)

    for c in range(cols) {
      for r in range(rows) {
        let x = c * cell
        let y = r * cell
        // Grid intersections
        cetz.draw.line((x - 3, y), (x + 3, y), stroke: (paint: accent.transparentize(75%), thickness: 0.8pt))
        cetz.draw.line((x, y - 3), (x, y + 3), stroke: (paint: accent.transparentize(75%), thickness: 0.8pt))
      }
    }

    // Horizontal scanline bands
    for row in range(0, rows, step: 3) {
      let y = row * cell
      cetz.draw.line((0, y), (w, y), stroke: (paint: accent.transparentize(92%), thickness: 0.5pt))
    }
  })
}

/// High-level motif dispatcher based on subsystem token motif string
#let render-motif-bg(motif, w, h, accent, seed: 1337) = {
  if motif == "chunk-cluster" {
    chunk-cluster-grid(w, h, seed: seed, accent: accent)
  } else if motif == "topographic" {
    topographic-contours(w, h, seed: seed, accent: accent)
  } else if motif == "dot-ledger" {
    dot-matrix(w, h, seed: seed, accent: accent)
  } else if motif == "quadrant-radar" {
    quadrant-radar(w, h, seed: seed, accent: accent)
  } else if motif == "terminal-grid" {
    terminal-grid(w, h, seed: seed, accent: accent)
  } else {
    // Default fallback: chunk cluster grid
    chunk-cluster-grid(w, h, seed: seed, accent: accent)
  }
}
