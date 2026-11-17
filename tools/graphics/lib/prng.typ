// SMP Mod Suite - Deterministic PRNG & 2D Value Noise Library
// Pure Typst implementation with zero non-deterministic dependencies.

#let lcg-a = 1103515245
#let lcg-c = 12345
#let lcg-m = 2147483648 // 2^31

/// Compute a positive 31-bit seed integer from any UTF-8 string using FNV-1a.
#let seed-from-str(s) = {
  let h = 2166136261
  for ch in s.codepoints() {
    let b = str.to-unicode(ch)
    h = calc.rem(h * 16777619 + b, lcg-m)
  }
  if h <= 0 { h = 1 }
  h
}

/// Advance LCG state and return (next_state, float_in_0_1).
#let next-rand(state) = {
  let next-state = calc.rem(state * lcg-a + lcg-c, lcg-m)
  if next-state < 0 { next-state = next-state + lcg-m }
  let val = float(next-state) / float(lcg-m)
  (next-state, val)
}

/// Generate a float in [min-val, max-val).
#let rand-range(state, min-val, max-val) = {
  let (next-state, val) = next-rand(state)
  (next-state, min-val + val * (max-val - min-val))
}

/// Generate an integer in [min-val, max-val].
#let rand-int(state, min-val, max-val) = {
  let (next-state, val) = next-rand(state)
  let count = max-val - min-val + 1
  (next-state, min-val + calc.floor(val * count))
}

/// Deterministic 2D coordinate hash.
#let hash2d(x, y, seed) = {
  let xi = int(calc.floor(x))
  let yi = int(calc.floor(y))
  let n = calc.rem(xi * 374761393 + yi * 668265263 + seed * 1274126177, lcg-m)
  if n < 0 { n = n + lcg-m }
  n = calc.rem(n * 1103515245 + 12345, lcg-m)
  if n < 0 { n = n + lcg-m }
  float(n) / float(lcg-m)
}

/// Smooth 2D value noise using bilinear interpolation with quintic/cubic smoothstep.
#let noise2d(x, y, seed) = {
  let xi = calc.floor(x)
  let yi = calc.floor(y)
  let xf = x - xi
  let yf = y - yi

  // Smoothstep s(t) = t * t * (3 - 2 * t)
  let u = xf * xf * (3.0 - 2.0 * xf)
  let v = yf * yf * (3.0 - 2.0 * yf)

  let n00 = hash2d(xi, yi, seed)
  let n10 = hash2d(xi + 1, yi, seed)
  let n01 = hash2d(xi, yi + 1, seed)
  let n11 = hash2d(xi + 1, yi + 1, seed)

  let nx0 = n00 * (1.0 - u) + n10 * u
  let nx1 = n01 * (1.0 - u) + n11 * u
  nx0 * (1.0 - v) + nx1 * v
}
