// SMP Mod Suite - Emergent Roles 4-Quadrant Diagram (1200x420)
#import "../lib/theme.typ": px, bg-canvas, bg-surface, bg-elevated, border-subtle, border-accent, text-primary, text-secondary, text-muted, color-emerald, color-amber, color-sky, color-crimson, font-sans, font-mono, pill, card-panel, tokens
#import "../lib/icons.typ": get-role-icon
#import "../lib/backgrounds.typ": quadrant-radar

#set page(
  width: 1200 * px,
  height: 420 * px,
  margin: 0pt,
  fill: bg-canvas
)

#set text(
  font: font-sans,
  fill: text-primary
)

// Background: subtle quadrant radar
#place(top + left, quadrant-radar(1200, 420, seed: 888, accent: color-crimson))

// Vignette overlay
#place(top + left,
  rect(
    width: 1200 * px,
    height: 420 * px,
    fill: gradient.radial(
      center: (50%, 50%),
      radius: 75%,
      rgb("#0B0E1400"),
      rgb("#0B0E14F4")
    )
  )
)

// Header strip
#place(top + left, dx: 48 * px, dy: 24 * px,
  box(width: 1104 * px, [
    #stack(dir: ltr, spacing: 16 * px,
      align(horizon, [
        #stack(spacing: 4 * px,
          stack(dir: ltr, spacing: 8 * px,
            pill("CLASSLESS SURVIVAL COMBAT", accent: color-crimson),
            pill("PLAYSTYLE XP MATRIX", accent: color-sky),
          ),
          text(size: 24 * px, weight: "bold", fill: white, [Emergent Role Profiles & Specialization Ratios])
        )
      ]),
      align(right + horizon, [
        #text(font: font-mono, size: 11 * px, fill: text-muted, [
          THRESHOLD: 40% PRIMARY AFFINITY · COOPERATIVE SYNERGY
        ])
      ])
    )
  ])
)

// 4 Role Columns Layout
#place(top + left, dx: 48 * px, dy: 96 * px,
  box(width: 1104 * px, height: 290 * px, [
    #grid(
      columns: (1fr, 1fr, 1fr, 1fr),
      gutter: 16 * px,

      // 1. DPS
      card-panel(
        height: 290 * px,
        fill: bg-surface,
        stroke: color-crimson.transparentize(50%),
        stack(spacing: 10 * px,
          stack(dir: ltr, spacing: 10 * px,
            get-role-icon("dps", size: 36),
            align(horizon, [
              #text(size: 20 * px, weight: "bold", fill: color-crimson, [DPS])
              #linebreak()
              #text(font: font-mono, size: 10 * px, fill: text-muted, [Striker])
            ])
          ),
          pill("COMBAT XP RATIO", accent: color-crimson),
          line(length: 100%, stroke: 0.8pt + border-subtle),
          text(size: 11 * px, font: font-mono, fill: text-secondary, [PRIMARY FOCUS: SPEED & CRITS]),
          text(size: 11 * px, fill: text-muted, [
            • 1.8x Execute Punish on targets < 35% HP
            • Fast Time-to-Kill XP scaling
            • Boss armor perforation multipliers
          ])
        )
      ),

      // 2. TANK
      card-panel(
        height: 290 * px,
        fill: bg-surface,
        stroke: color-amber.transparentize(50%),
        stack(spacing: 10 * px,
          stack(dir: ltr, spacing: 10 * px,
            get-role-icon("tank", size: 36),
            align(horizon, [
              #text(size: 20 * px, weight: "bold", fill: color-amber, [TANK])
              #linebreak()
              #text(font: font-mono, size: 10 * px, fill: text-muted, [Frontline Anchor])
            ])
          ),
          pill("BUILDER XP RATIO", accent: color-amber),
          line(length: 100%, stroke: 0.8pt + border-subtle),
          text(size: 11 * px, font: font-mono, fill: text-secondary, [PRIMARY FOCUS: MITIGATION]),
          text(size: 11 * px, fill: text-muted, [
            • 16m Threat Taunt pulse
            • 75% Kinetic Barrier projectile shielding
            • 6s Ender Dragon dive stagger intercept
          ])
        )
      ),

      // 3. SUPPORT
      card-panel(
        height: 290 * px,
        fill: bg-surface,
        stroke: color-emerald.transparentize(50%),
        stack(spacing: 10 * px,
          stack(dir: ltr, spacing: 10 * px,
            get-role-icon("support", size: 36),
            align(horizon, [
              #text(size: 20 * px, weight: "bold", fill: color-emerald, [SUPPORT])
              #linebreak()
              #text(font: font-mono, size: 10 * px, fill: text-muted, [Team Sustain])
            ])
          ),
          pill("GATHERER XP RATIO", accent: color-emerald),
          line(length: 100%, stroke: 0.8pt + border-subtle),
          text(size: 11 * px, font: font-mono, fill: text-secondary, [PRIMARY FOCUS: SANCTUARY]),
          text(size: 11 * px, fill: text-muted, [
            • 300s Warded Light mob-proof flares
            • 8m radius team health pulse (+1.5 HP)
            • High-tier harvest shop discount scaling
          ])
        )
      ),

      // 4. SCOUT
      card-panel(
        height: 290 * px,
        fill: bg-surface,
        stroke: color-sky.transparentize(50%),
        stack(spacing: 10 * px,
          stack(dir: ltr, spacing: 10 * px,
            get-role-icon("scout", size: 36),
            align(horizon, [
              #text(size: 20 * px, weight: "bold", fill: color-sky, [SCOUT])
              #linebreak()
              #text(font: font-mono, size: 10 * px, fill: text-muted, [Recon & Transit])
            ])
          ),
          pill("EXPLORER XP RATIO", accent: color-sky),
          line(length: 100%, stroke: 0.8pt + border-subtle),
          text(size: 11 * px, font: font-mono, fill: text-secondary, [PRIMARY FOCUS: RECON & TRANSIT]),
          text(size: 11 * px, fill: text-muted, [
            • 24m Dark-Zone trap radar
            • +20% Biome & +10% Road speed bonus
            • Gated momentum Elytra glider flight
          ])
        )
      )
    )
  ])
)
