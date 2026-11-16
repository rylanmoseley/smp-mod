package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Tracks Gatherer Playstyle XP:
 * Strictly awarded from harvesting naturally generated (!player_placed) ores, crops,
 * timber, and rare nodes, with deep-cave density multipliers and perilous zone scaling.
 */
object GathererXpTracker {

    /**
     * Evaluates a harvested block and awards Gatherer XP if natural.
     */
    fun onBlockHarvested(player: ServerPlayer, pos: BlockPos, state: BlockState, isPlayerPlaced: Boolean) {
        // Strict guardrail: player-placed or Silk-Touched replaced blocks NEVER award Gatherer XP
        if (isPlayerPlaced) {
            return
        }

        val block = state.block
        val baseValue = evaluateNaturalResourceValue(state, block)
        if (baseValue <= 0.0) {
            return
        }

        // 1. Deep-Cave Multiplier (below Y=0 or in deep slate depths)
        val isDeepCave = pos.y < 0
        val caveMultiplier = if (isDeepCave) {
            SmpConfig.COMMON.deepCaveVeinDensityMultiplier.get()
        } else {
            1.0
        }

        // 2. Perilous Zone Multiplier (Nether, End, Dark Zones)
        val dim = player.level().dimension()
        val perilousMultiplier = when (dim) {
            Level.NETHER -> 1.5
            Level.END -> 2.0
            else -> if (pos.y < -30) 1.25 else 1.0
        }

        val totalGathererXp = baseValue * caveMultiplier * perilousMultiplier
        PlaystyleXpTracker.incrementGathererXp(player, totalGathererXp)
    }

    fun evaluateNaturalResourceValue(state: BlockState, block: Block): Double {
        // High-Value Ores
        if (block == Blocks.ANCIENT_DEBRIS) return 20.0
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 10.0
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return 12.0
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return 4.5
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return 3.0
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return 3.0
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return 2.5
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return 1.5
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return 1.0
        if (block == Blocks.NETHER_QUARTZ_ORE) return 2.0

        // Rare Subterranean Nodes
        if (block == Blocks.AMETHYST_CLUSTER) return 5.0
        if (block == Blocks.SCULK_CATALYST || block == Blocks.SCULK_SHRIEKER) return 8.0
        if (block == Blocks.SCULK_SENSOR) return 4.0

        // Crops (Strictly award on mature harvest)
        if (block is CropBlock) {
            return if (block.isMaxAge(state)) 2.0 else 0.0
        }
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) return 1.5
        if (block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO || block == Blocks.CACTUS) return 0.5
        if (block == Blocks.CHORUS_FLOWER) return 4.0
        if (block == Blocks.NETHER_WART) return 2.0

        // Natural Timber
        val key = BuiltInRegistries.BLOCK.getKey(block).path
        if (key.endsWith("_log") || key.endsWith("_stem")) {
            return 1.0
        }

        return 0.0
    }
}
