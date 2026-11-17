// SMP Mod Suite - Project Icon Template (512x512)
#import "../lib/theme.typ": px, bg-canvas, bg-surface, border-subtle, text-primary, text-secondary, color-emerald, color-amethyst, font-sans, font-mono
#import "../lib/icons.typ": project-emblem
#import "../lib/backgrounds.typ": chunk-cluster-grid

#let variant = sys.inputs.at("variant", default: "mod")

#set page(
  width: 512 * px,
  height: 512 * px,
  margin: 0pt,
  fill: bg-canvas
)

#set text(
  font: font-sans,
  fill: text-primary
)

// Background: subtle chunk grid
#place(top + left, chunk-cluster-grid(512, 512, seed: 991, accent: if variant == "modpack" { color-amethyst } else { color-emerald }, cell-size: 32, cluster-size: 18, secondary-cluster: false))

// Vignette overlay
#place(top + left,
  rect(
    width: 512 * px,
    height: 512 * px,
    fill: gradient.radial(
      center: (50%, 50%),
      radius: 65%,
      rgb("#0B0E1400"),
      rgb("#0B0E14EE")
    )
  )
)

// Inner safe boundary box (512 - 2*32 = 448)
#place(center + horizon,
  box(
    width: 440 * px,
    height: 440 * px,
    stroke: 1.5pt + if variant == "modpack" { color-amethyst.transparentize(60%) } else { color-emerald.transparentize(60%) },
    radius: 36pt,
    fill: bg-surface.transparentize(25%),
    [
      #align(center + horizon, [
        #v(10 * px)
        #text(
          font: font-mono,
          size: 13 * px,
          weight: "bold",
          fill: if variant == "modpack" { color-amethyst } else { color-emerald },
          tracking: 2pt,
          if variant == "modpack" { "SMP SURVIVAL SUITE" } else { "SMP MOD SUITE" }
        )
        
        #v(12 * px)
        #project-emblem(size: 230, variant: variant)
        #v(16 * px)

        #box(
          fill: if variant == "modpack" { color-amethyst.transparentize(80%) } else { color-emerald.transparentize(80%) },
          stroke: 1.2pt + if variant == "modpack" { color-amethyst } else { color-emerald },
          radius: 999pt,
          inset: (x: 16pt, y: 7pt),
          [
            #set text(font: font-mono, size: 12pt, weight: "bold", fill: white)
            #if variant == "modpack" { "PACKWIZ MODPACK" } else { "NEOFORGE 1.21.1" }
          ]
        )
      ])
    ]
  )
)
