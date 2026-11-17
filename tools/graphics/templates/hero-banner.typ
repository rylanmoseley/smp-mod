// SMP Mod Suite - Hero Banner Template (1280x360)
#import "../lib/theme.typ": px, bg-canvas, bg-surface, bg-elevated, border-subtle, border-accent, text-primary, text-secondary, text-muted, color-emerald, color-amber, color-amethyst, color-sky, color-crimson, color-indigo, font-sans, font-mono, pill
#import "../lib/icons.typ": project-emblem, icon-role-dps, icon-role-tank, icon-role-support, icon-role-scout
#import "../lib/backgrounds.typ": chunk-cluster-grid

#set page(
  width: 1280 * px,
  height: 360 * px,
  margin: 0pt,
  fill: bg-canvas
)

#set text(
  font: font-sans,
  fill: text-primary
)

// Background: subtle full-width procedural grid
#place(top + left, chunk-cluster-grid(1280, 360, seed: 1042, accent: color-emerald, cell-size: 32, cluster-size: 22, secondary-cluster: true))

// Gradient scrim overlay: dark on left for text legibility, transparent on right
#place(top + left,
  rect(
    width: 1280 * px,
    height: 360 * px,
    fill: gradient.linear(
      angle: 0deg,
      (rgb("#0B0E14FC"), 0%),
      (rgb("#0B0E14F0"), 55%),
      (rgb("#0B0E1499"), 85%),
      (rgb("#0B0E14CC"), 100%)
    )
  )
)

// Left Content Area (Columns layout)
#place(top + left, dx: 64 * px, dy: 38 * px,
  box(width: 720 * px, [
    // Top system badge pills
    #stack(spacing: 8 * px,
      stack(dir: ltr, spacing: 8 * px,
        pill("NEOFORGE 1.21.1", accent: color-emerald),
        pill("KOTLIN", accent: color-amethyst),
        pill("SMP MOD SUITE", accent: color-sky),
        pill("V2 ARCHITECTURE", accent: color-amber),
      ),
      v(10 * px),
      // Main Title with embedded emblem
      stack(dir: ltr, spacing: 18 * px,
        project-emblem(size: 58, variant: "mod"),
        align(horizon, [
          #text(size: 46 * px, weight: "bold", tracking: -0.5pt, fill: white, [SMP Mod Suite])
        ])
      ),
      v(4 * px),
      // Tagline
      text(
        size: 19 * px,
        weight: "medium",
        fill: text-secondary,
        [Cooperative Survival Multiplayer Re-engineered]
      ),
      v(6 * px),
      // Subtitle / value props
      text(
        size: 13 * px,
        fill: text-muted,
        [Contiguous Town Leveling · UUID Death Buybacks · Emergent Roles · Tectonic Worldgen]
      ),
      v(14 * px),
      // 4 Roles preview strip
      stack(dir: ltr, spacing: 16 * px,
        stack(dir: ltr, spacing: 6 * px, icon-role-dps(size: 18), text(size: 11 * px, font: font-mono, weight: "bold", fill: color-crimson, [DPS])),
        stack(dir: ltr, spacing: 6 * px, icon-role-tank(size: 18), text(size: 11 * px, font: font-mono, weight: "bold", fill: color-amber, [TANK])),
        stack(dir: ltr, spacing: 6 * px, icon-role-support(size: 18), text(size: 11 * px, font: font-mono, weight: "bold", fill: color-emerald, [SUPPORT])),
        stack(dir: ltr, spacing: 6 * px, icon-role-scout(size: 18), text(size: 11 * px, font: font-mono, weight: "bold", fill: color-sky, [SCOUT])),
      )
    )
  ])
)

// Right Visual Card: Framed Contiguous Settlement Radar
#place(top + right, dx: -64 * px, dy: 35 * px,
  box(
    width: 420 * px,
    height: 290 * px,
    fill: bg-surface.transparentize(20%),
    stroke: 1.5pt + border-subtle,
    radius: 12pt,
    clip: true,
    [
      // Inner procedural preview
      #chunk-cluster-grid(420, 290, seed: 4096, accent: color-emerald, cell-size: 26, cluster-size: 16, secondary-cluster: true)
      
      // Top HUD bar
      #place(top + left, dx: 14 * px, dy: 12 * px,
        box(
          fill: bg-canvas.transparentize(30%),
          stroke: 1pt + border-subtle,
          radius: 4pt,
          inset: (x: 8pt, y: 4pt),
          text(font: font-mono, size: 10 * px, weight: "bold", fill: color-emerald, [● CONTIGUOUS SETTLEMENT CLUSTER])
        )
      )

      // Bottom HUD bar
      #place(bottom + left, dx: 14 * px, dy: -12 * px,
        box(
          fill: bg-canvas.transparentize(30%),
          stroke: 1pt + border-subtle,
          radius: 4pt,
          inset: (x: 8pt, y: 4pt),
          text(font: font-mono, size: 9 * px, fill: text-secondary, [BFS Flood-Fill · Tier 4 Town · Pooled Buffs Active])
        )
      )
    ]
  )
)
