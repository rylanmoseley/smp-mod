package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.world.level.block.Block
import kotlin.math.ln
import kotlin.math.max

/**
 * Calculates variety-weighted construction scoring for Builder XP and cluster development leveling.
 *
 * Implements logarithmic diminishing returns for repetitive blocks within a cluster or build,
 * heavily penalizing monoculture builds (e.g., solid cobblestone cubes) while exponentially
 * rewarding structural diversity (mixing timber, masonry, glass, tiles, and detailing).
 */
object VarietyScorer {

    /**
     * Marginal XP/score awarded for placing a single block, factoring in the existing count
     * of this specific block type in the cluster and the cluster's current palette variety.
     */
    fun calculateBlockPlacementScore(block: Block, currentCountOfThisBlock: Int, uniquePaletteCount: Int): Double {
        val baseWeight = SmpConfig.COMMON.varietyPaletteWeightBase.get()
        val repetitionPenalty = calculateRepetitionPenalty(currentCountOfThisBlock.toLong())
        val diversityBonus = calculateDiversityMultiplier(uniquePaletteCount)

        return (baseWeight * repetitionPenalty * diversityBonus).coerceAtLeast(0.01)
    }

    /**
     * Computes the repetition penalty factor using logarithmic diminishing returns:
     * factor = 1.0 / (1.0 + ln(1.0 + count))
     *
     * count = 0:    1.000 (100% full value)
     * count = 10:   0.294 (~29% value)
     * count = 100:  0.178 (~18% value)
     * count = 1000: 0.126 (~13% value)
     * count = 5000: 0.105 (~10% value)
     */
    fun calculateRepetitionPenalty(count: Long): Double {
        val nonNegative = max(0L, count)
        return 1.0 / (1.0 + ln(1.0 + nonNegative.toDouble()))
    }

    /**
     * Computes the diversity bonus multiplier based on unique block palette types:
     * Up to +200% bonus (3.0x multiplier) at 20 distinct block types.
     */
    fun calculateDiversityMultiplier(uniquePaletteCount: Int): Double {
        val cappedVariety = uniquePaletteCount.coerceIn(0, 20)
        return 1.0 + (0.10 * cappedVariety)
    }

    /**
     * Computes the total variety-weighted palette score for an entire contiguous cluster
     * based on its aggregated net placed block counts.
     */
    fun calculateClusterVarietyScore(blockCountsByType: Map<String, Long>): Double {
        if (blockCountsByType.isEmpty()) return 0.0

        val baseWeight = SmpConfig.COMMON.varietyPaletteWeightBase.get()
        var weightedSum = 0.0
        var activePaletteCount = 0

        for ((_, count) in blockCountsByType) {
            if (count > 0L) {
                activePaletteCount++
                // Effective block score with logarithmic curve: count / (1.0 + ln(1.0 + count))
                val effectiveContribution = count.toDouble() / (1.0 + ln(1.0 + count.toDouble()))
                weightedSum += effectiveContribution
            }
        }

        val diversityBonus = calculateDiversityMultiplier(activePaletteCount)
        return weightedSum * diversityBonus * baseWeight
    }
}
