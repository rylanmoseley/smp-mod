package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk

/**
 * Tracks player-placed blocks to prevent economic laundering and artificial Gatherer XP farming.
 *
 * Technical Specification Guardrail:
 * Breaking player-placed, shop-bought, or Silk-Touched blocks NEVER counts toward:
 *   1. Natural-harvest building material shop unlocks or discount tiers
 *   2. Gatherer Playstyle XP progression
 *   3. Repeatable active-play harvest milestone cycles
 *
 * State is persistently serialized inside each LevelChunk via NeoForge attachments.
 */
object PlacedBlockTracker {

    /**
     * Marks a block coordinate as player-placed.
     */
    fun markPlaced(level: Level, pos: BlockPos) {
        if (level.isClientSide) return
        val chunk = level.getChunk(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: return

        val attachment = ModAttachmentTypes.CHUNK_PLACED_BLOCKS.get()
        val currentData = chunk.getData(attachment)
        val packed = pos.asLong()

        if (!currentData.placedPositions.contains(packed)) {
            val updatedPositions = currentData.placedPositions.toMutableSet()
            updatedPositions.add(packed)
            chunk.setData(attachment, ModAttachmentTypes.PlacedBlockChunkData(updatedPositions))
        }
    }

    /**
     * Unmarks a block coordinate when broken.
     */
    fun unmarkPlaced(level: Level, pos: BlockPos) {
        if (level.isClientSide) return
        val chunk = level.getChunk(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: return

        val attachment = ModAttachmentTypes.CHUNK_PLACED_BLOCKS.get()
        val currentData = chunk.getData(attachment)
        val packed = pos.asLong()

        if (currentData.placedPositions.contains(packed)) {
            val updatedPositions = currentData.placedPositions.toMutableSet()
            updatedPositions.remove(packed)
            chunk.setData(attachment, ModAttachmentTypes.PlacedBlockChunkData(updatedPositions))
        }
    }

    /**
     * Queries whether a block at the given coordinate was placed by a player.
     */
    fun isPlayerPlaced(level: Level, pos: BlockPos): Boolean {
        if (level.isClientSide) return false
        val chunk = level.getChunk(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: return false

        val attachment = ModAttachmentTypes.CHUNK_PLACED_BLOCKS.get()
        val currentData = chunk.getData(attachment)
        return currentData.placedPositions.contains(pos.asLong())
    }

    /**
     * Checks if the tool in the player's main hand has the Silk Touch enchantment.
     */
    fun hasSilkTouch(player: Player): Boolean {
        val tool = player.mainHandItem
        if (tool.isEmpty) return false
        val enchantments = tool.get(DataComponents.ENCHANTMENTS) ?: return false
        return enchantments.keySet().any { it.`is`(Enchantments.SILK_TOUCH) }
    }

    /**
     * Comprehensive validation checking if a block break constitutes a genuine natural harvest.
     * Breaking player-placed or Silk-Touched blocks strictly returns false.
     * If the block was player-placed, its tracking entry is automatically pruned.
     */
    fun isNaturalHarvest(player: Player, level: Level, pos: BlockPos): Boolean {
        // Guardrail 1: Silk-Touched harvests never count as natural harvest unlocks
        if (hasSilkTouch(player)) {
            // Prune placed tracking if it was placed
            if (isPlayerPlaced(level, pos)) {
                unmarkPlaced(level, pos)
            }
            return false
        }

        // Guardrail 2: Player-placed blocks (including previously placed shop-bought blocks) never count
        if (isPlayerPlaced(level, pos)) {
            unmarkPlaced(level, pos)
            return false
        }

        return true
    }
}
