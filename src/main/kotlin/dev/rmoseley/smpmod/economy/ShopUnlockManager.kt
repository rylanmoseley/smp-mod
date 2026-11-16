package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block

/**
 * Manages per-block natural harvest shop unlocks and player_placed guardrails.
 * Breaking player-placed or Silk-Touched blocks never counts toward unlocks.
 */
object ShopUnlockManager {

    // Default harvest threshold to unlock purchasing non-precious building blocks
    const val DEFAULT_UNLOCK_THRESHOLD: Int = 100

    fun recordNaturalHarvest(player: Player, block: Block) {
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        val attachment = ModAttachmentTypes.PLAYER_NATURAL_HARVEST.get()
        val currentData = player.getData(attachment)
        val currentCount = currentData.counts[blockId] ?: 0
        val updatedCounts = currentData.counts.toMutableMap()
        updatedCounts[blockId] = currentCount + 1
        player.setData(attachment, ModAttachmentTypes.NaturalHarvestData(updatedCounts))
    }

    fun getHarvestCount(player: Player, block: Block): Int {
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        val attachment = ModAttachmentTypes.PLAYER_NATURAL_HARVEST.get()
        return player.getData(attachment).counts[blockId] ?: 0
    }

    fun isBlockUnlocked(player: Player, block: Block, requiredThreshold: Int = DEFAULT_UNLOCK_THRESHOLD): Boolean {
        return getHarvestCount(player, block) >= requiredThreshold
    }
}
