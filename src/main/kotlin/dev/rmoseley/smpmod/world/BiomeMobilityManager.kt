package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Manages Overworld Biome Mobility Modifiers strictly adhering to design-v2.md:
 * - Progressive movement bonuses in biomes after players explore X unique chunks of that biome.
 * - Rewarding regional roads and mapped exploration.
 * - Synergizes with Scout combat role traversal agility.
 */
object BiomeMobilityManager {

    private val BIOME_MOBILITY_MODIFIER_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "biome_mobility_modifier")

    /**
     * Ticks player biome mobility, tracking chunk discovery and updating movement speed attributes.
     */
    fun onPlayerTick(player: ServerPlayer) {
        val level = player.level()
        if (level.dimension() != Level.OVERWORLD) {
            removeMobilityModifier(player)
            return
        }

        val chunkPos = ChunkPos(player.blockPosition())
        val chunkKey = chunkPos.toLong()

        // 1. Process Exploration and Discovery Tracking
        trackChunkExploration(player, chunkKey)

        // 2. Calculate and apply progressive mobility bonus
        updateMobilitySpeedModifier(player)
    }

    /**
     * Checks if this chunk is newly discovered; if so, updates per-biome counts and awards Explorer XP.
     */
    private fun trackChunkExploration(player: ServerPlayer, chunkKey: Long) {
        val attachment = ModAttachmentTypes.PLAYER_BIOME_EXPLORATION.get()
        val data = player.getData(attachment)

        if (!data.discoveredChunkCoords.contains(chunkKey)) {
            val biomeHolder = player.level().getBiome(player.blockPosition())
            val biomeKey = biomeHolder.unwrapKey().map { it.location().toString() }.orElse("minecraft:plains")

            val currentCount = data.biomeChunkCounts[biomeKey] ?: 0
            val newCount = currentCount + 1

            val updatedCoords = data.discoveredChunkCoords + chunkKey
            val updatedCounts = data.biomeChunkCounts + (biomeKey to newCount)

            val updatedData = ModAttachmentTypes.PlayerBiomeExplorationData(
                discoveredChunkCoords = updatedCoords,
                biomeChunkCounts = updatedCounts
            )
            player.setData(attachment, updatedData)

            // Synchronize with total discovered chunks attachment
            player.setData(ModAttachmentTypes.PLAYER_DISCOVERED_CHUNKS.get(), updatedCoords.size)

            // Award Explorer Playstyle XP and Bounty Contract progress
            PlaystyleXpTracker.awardExplorerXp(player, 15.0)
            dev.rmoseley.smpmod.economy.BountyContractManager.recordProgress(
                player,
                dev.rmoseley.smpmod.economy.BountyType.SCOUT_EXPLORATION,
                1,
                biomeKey
            )

            // Check milestone thresholds for notifications (e.g. 10, 25, 50, 100 chunks)
            val threshold = SmpConfig.COMMON.biomeMobilityThreshold.get()
            if (newCount == 10 || newCount == 25 || newCount == 50 || newCount == 100 || (newCount > 0 && newCount % threshold == 0)) {
                val cleanBiomeName = biomeKey.substringAfter(':').replace('_', ' ').replaceFirstChar { it.uppercase() }
                player.displayClientMessage(
                    Component.literal("§aExploration Milestone: Mapped $newCount chunks in $cleanBiomeName! Traversal mobility increased."),
                    true
                )
            }
        }
    }

    /**
     * Computes the total movement bonus for the player based on:
     * - Explored chunks in the current biome (tier scaling).
     * - Regional road paving bonus (walking on paths, bricks, gravel).
     * - Scout role traversal agility synergy.
     */
    private fun updateMobilitySpeedModifier(player: ServerPlayer) {
        val speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return

        val biomeHolder = player.level().getBiome(player.blockPosition())
        val biomeKey = biomeHolder.unwrapKey().map { it.location().toString() }.orElse("minecraft:plains")

        val data = player.getData(ModAttachmentTypes.PLAYER_BIOME_EXPLORATION.get())
        val exploredChunks = data.biomeChunkCounts[biomeKey] ?: 0

        // 1. Biome Exploration Tier Bonus (up to config max, default 0.20 = +20%)
        val maxBiomeBonus = SmpConfig.COMMON.biomeMobilityMaxBonus.get()
        val biomeTierBonus = when {
            exploredChunks >= 100 -> maxBiomeBonus
            exploredChunks >= 50 -> maxBiomeBonus * 0.75
            exploredChunks >= 25 -> maxBiomeBonus * 0.50
            exploredChunks >= 10 -> maxBiomeBonus * 0.25
            else -> 0.0
        }

        // 2. Regional Road Traversal Bonus (default +10% on paved surfaces)
        val isRoad = isStandingOnRoad(player)
        val roadBonus = if (isRoad) SmpConfig.COMMON.roadMobilityBonus.get() else 0.0

        val baseBonus = biomeTierBonus + roadBonus

        if (baseBonus <= 0.0) {
            removeMobilityModifier(player)
            return
        }

        // 3. Scout Role Synergy: Scout agility amplifies exploration traversal speed
        val scoutAgility = player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)?.value ?: 1.0
        val scoutMultiplier = if (scoutAgility > 1.0) 1.0 + ((scoutAgility - 1.0) * 0.5) else 1.0
        val totalBonus = baseBonus * scoutMultiplier

        // Apply attribute modifier
        speedAttr.removeModifier(BIOME_MOBILITY_MODIFIER_ID)
        speedAttr.addTransientModifier(
            AttributeModifier(
                BIOME_MOBILITY_MODIFIER_ID,
                totalBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
        )
    }

    private fun removeMobilityModifier(player: ServerPlayer) {
        val speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        speedAttr.removeModifier(BIOME_MOBILITY_MODIFIER_ID)
    }

    /**
     * Checks if the player is traversing a recognized regional road or path structure.
     */
    fun isStandingOnRoad(player: ServerPlayer): Boolean {
        val feetPos = player.blockPosition()
        val belowPos = feetPos.below()

        val stateFeet = player.level().getBlockState(feetPos)
        val stateBelow = player.level().getBlockState(belowPos)

        return isRoadBlock(stateFeet) || isRoadBlock(stateBelow)
    }

    private fun isRoadBlock(state: BlockState): Boolean {
        return state.`is`(Blocks.DIRT_PATH) ||
                state.`is`(Blocks.STONE_BRICKS) ||
                state.`is`(Blocks.MOSSY_STONE_BRICKS) ||
                state.`is`(Blocks.CRACKED_STONE_BRICKS) ||
                state.`is`(Blocks.COBBLESTONE) ||
                state.`is`(Blocks.MOSSY_COBBLESTONE) ||
                state.`is`(Blocks.GRAVEL) ||
                state.`is`(Blocks.PACKED_MUD) ||
                state.`is`(Blocks.MUD_BRICKS) ||
                state.`is`(Blocks.POLISHED_ANDESITE) ||
                state.`is`(Blocks.POLISHED_DIORITE) ||
                state.`is`(Blocks.POLISHED_GRANITE) ||
                state.`is`(Blocks.SMOOTH_STONE) ||
                state.`is`(Blocks.BRICKS)
    }

    /**
     * Query API: retrieves the count of unique biomes explored by the player.
     */
    fun getExploredBiomeCount(player: ServerPlayer): Int {
        val data = player.getData(ModAttachmentTypes.PLAYER_BIOME_EXPLORATION.get())
        return data.biomeChunkCounts.keys.size
    }

    /**
     * Query API: retrieves the total count of unique chunks mapped by the player.
     */
    fun getUniqueChunksExplored(player: ServerPlayer): Int {
        val data = player.getData(ModAttachmentTypes.PLAYER_BIOME_EXPLORATION.get())
        return data.discoveredChunkCoords.size
    }

    /**
     * Query API: retrieves the count of unique chunks mapped in a specific biome.
     */
    fun getBiomeChunksExplored(player: ServerPlayer, biomeKey: String): Int {
        val data = player.getData(ModAttachmentTypes.PLAYER_BIOME_EXPLORATION.get())
        return data.biomeChunkCounts[biomeKey] ?: 0
    }
}
