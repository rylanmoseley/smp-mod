package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks Explorer Playstyle XP:
 * Awarded from mapping unique chunks, discovering new biomes, finding dungeons
 * and villages, and completing distant expeditions.
 */
object ExplorerXpTracker {

    // Player UUID -> Set of discovered chunk coordinates (packed as Long)
    private val discoveredChunks = ConcurrentHashMap<UUID, MutableSet<Long>>()

    // Player UUID -> Set of discovered Biome ResourceLocation strings
    private val discoveredBiomes = ConcurrentHashMap<UUID, MutableSet<String>>()

    // Player UUID -> Set of discovered Structure keys
    private val discoveredStructures = ConcurrentHashMap<UUID, MutableSet<String>>()

    // Player UUID -> Highest expedition distance milestone reached in meters
    private val expeditionMilestones = ConcurrentHashMap<UUID, Int>()

    /**
     * Ticked periodically for each player to track spatial exploration progress.
     */
    fun onPlayerTick(player: ServerPlayer) {
        val level = player.serverLevel()
        val pos = player.blockPosition()
        val chunkPos = ChunkPos(pos)
        val chunkKey = chunkPos.toLong()

        // 1. Unique Chunk Discovery
        val playerChunks = discoveredChunks.computeIfAbsent(player.uuid) { ConcurrentHashMap.newKeySet() }
        if (playerChunks.add(chunkKey)) {
            // Update persistent attachment
            val attachment = ModAttachmentTypes.PLAYER_DISCOVERED_CHUNKS.get()
            val totalDiscovered = player.getData(attachment) + 1
            player.setData(attachment, totalDiscovered)

            // Award Explorer XP for new chunk mapped
            PlaystyleXpTracker.incrementExplorerXp(player, 5.0)
        }

        // 2. New Biome Discovery
        val biomeHolder = level.getBiome(pos)
        val biomeId = biomeHolder.unwrapKey().map { it.location().toString() }.orElse("unknown")
        val playerBiomes = discoveredBiomes.computeIfAbsent(player.uuid) { ConcurrentHashMap.newKeySet() }
        if (playerBiomes.add(biomeId)) {
            val biomeName = biomeId.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }
            player.sendSystemMessage(
                Component.literal("§6[Explorer] §fDiscovered new biome: §e$biomeName §a(+50 Explorer XP)")
            )
            PlaystyleXpTracker.incrementExplorerXp(player, 50.0)

            // Trigger World Exploration advancement milestones
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onExplorationProgress(
                player,
                playerChunks.size,
                playerBiomes.size
            )
        }

        // 3. Distance Expedition Milestones
        val spawnPos = level.sharedSpawnPos
        val dx = pos.x - spawnPos.x
        val dz = pos.z - spawnPos.z
        val distance = Math.sqrt((dx * dx + dz * dz).toDouble()).toInt()
        val currentMaxMilestone = expeditionMilestones.getOrDefault(player.uuid, 0)

        val milestoneTiers = listOf(1000, 2500, 5000, 10000, 20000, 50000)
        for (tier in milestoneTiers) {
            if (distance >= tier && currentMaxMilestone < tier) {
                expeditionMilestones[player.uuid] = tier
                player.sendSystemMessage(
                    Component.literal("§6[Expedition] §fReached ${tier}m expedition milestone! §a(+100 Explorer XP)")
                )
                PlaystyleXpTracker.incrementExplorerXp(player, 100.0)
                break
            }
        }
    }

    /**
     * Awards bonus exploration XP when a player locates an active structure or dungeon.
     */
    fun onStructureDiscovered(player: ServerPlayer, structureKey: String, displayName: String) {
        val playerStructures = discoveredStructures.computeIfAbsent(player.uuid) { ConcurrentHashMap.newKeySet() }
        if (playerStructures.add(structureKey)) {
            player.sendSystemMessage(
                Component.literal("§6[Explorer] §fUncharted site discovered: §e$displayName §a(+150 Explorer XP)")
            )
            PlaystyleXpTracker.incrementExplorerXp(player, 150.0)
        }
    }

    fun getDiscoveredChunkCount(player: Player): Int {
        return player.getData(ModAttachmentTypes.PLAYER_DISCOVERED_CHUNKS.get())
    }

    fun getDiscoveredBiomeCount(player: Player): Int {
        return discoveredBiomes[player.uuid]?.size ?: 0
    }
}
