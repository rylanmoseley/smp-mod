package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import kotlin.math.ln
import kotlin.math.max

/**
 * Calculates variety-weighted construction scoring for Builder XP and claim buffs.
 * Evaluates net-placed blocks with diminishing returns per block type.
 */
object VarietyScorer {

    /**
     * Computes the variety score contribution of a block given the current palette distribution.
     * Uses logarithmic diminishing returns: blocks placed repeatedly within the same palette yield progressively less.
     */
    fun calculateBlockPlacementScore(block: Block, currentCountOfThisBlock: Int, uniquePaletteCount: Int): Double {
        val baseWeight = SmpConfig.COMMON.varietyPaletteWeightBase.get()

        // Diminishing returns factor for repetitive blocks: 1.0 / (1.0 + ln(1 + count))
        val repetitionPenalty = 1.0 / (1.0 + ln(1.0 + max(0, currentCountOfThisBlock)))

        // Diversity bonus scaling with total distinct block types in the build
        val diversityBonus = 1.0 + (0.1 * uniquePaletteCount.coerceAtMost(20))

        return baseWeight * repetitionPenalty * diversityBonus
    }
}
