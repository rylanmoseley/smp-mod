package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.neoforged.fml.ModList
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages chunk claims and contiguous settlement cluster development leveling.
 *
 * Rule: Chunk claims CAN be noncontiguous across the world (e.g. distant outposts, mining camps).
 * However, only the contiguous chunks sharing orthogonal borders with a given claim
 * are referenced for that claim's development leveling, pooled defensive buffs, and flight.
 */
object ClaimManager {

    val provider: IClaimProvider by lazy {
        if (ModList.get().isLoaded("openpartiesandclaims")) {
            SmpMod.LOGGER.info("Open Parties and Claims (OPAC) detected. Using OpacClaimProvider.")
            OpacClaimProvider()
        } else {
            SmpMod.LOGGER.info("Open Parties and Claims not detected. Using InternalClaimProvider.")
            InternalClaimProvider()
        }
    }

    // Tracks net placed blocks and palette diversity per chunk cluster key
    private val clusterBlockCounts = ConcurrentHashMap<String, Long>()
    private val clusterPaletteDiversity = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Determines whether a chunk is claimed by the specified player or their party.
     */
    fun isChunkClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean {
        return provider.isClaimedBy(level, pos, player)
    }

    fun getClaimOwner(level: Level, pos: ChunkPos): UUID? {
        return provider.getClaimOwner(level, pos)
    }

    /**
     * Identifies the maximal contiguous cluster of claimed chunks containing the given position.
     * Uses Breadth-First Search (BFS) on orthogonal chunk neighbors.
     */
    fun getContiguousCluster(level: Level, pos: ChunkPos): ContiguousCluster? {
        val ownerUuid = getClaimOwner(level, pos) ?: return null
        val allOwnedChunks = provider.getAllClaimedChunks(level, ownerUuid)

        if (!allOwnedChunks.contains(pos)) return null

        // BFS flood-fill to find all contiguous chunks in this specific cluster
        val clusterChunks = mutableSetOf<ChunkPos>()
        val queue = ArrayDeque<ChunkPos>()
        queue.add(pos)
        clusterChunks.add(pos)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val neighbors = listOf(
                ChunkPos(current.x + 1, current.z),
                ChunkPos(current.x - 1, current.z),
                ChunkPos(current.x, current.z + 1),
                ChunkPos(current.x, current.z - 1)
            )

            for (neighbor in neighbors) {
                if (allOwnedChunks.contains(neighbor) && clusterChunks.add(neighbor)) {
                    queue.add(neighbor)
                }
            }
        }

        // Generate deterministic key for this contiguous cluster (e.g., sorted chunk coords)
        val clusterKey = "${level.dimension().location()}:${clusterChunks.minByOrNull { it.toLong() }?.toLong()}"
        val netBlocks = clusterBlockCounts[clusterKey] ?: 0L
        val paletteCount = clusterPaletteDiversity[clusterKey]?.size ?: 0

        // Calculate development level and pooled defense buffs
        val devLevel = calculateClusterDevelopmentLevel(clusterChunks.size, netBlocks, paletteCount)
        val defenseMitigation = (devLevel * 0.05).coerceAtMost(0.40) // Up to 40% pooled damage reduction

        return ContiguousCluster(
            ownerUuid = ownerUuid,
            chunks = clusterChunks,
            developmentLevel = devLevel,
            paletteVarietyScore = paletteCount * 1.5,
            pooledDefenseMitigation = defenseMitigation,
            netPlacedBlocks = netBlocks
        )
    }

    /**
     * Calculates the development level for a contiguous cluster based on chunk count,
     * net placed blocks, and palette diversity.
     */
    fun calculateClusterDevelopmentLevel(chunkCount: Int, netBlocks: Long, paletteCount: Int): Int {
        val sizeFactor = chunkCount.coerceAtLeast(1)
        val blockFactor = (netBlocks / 100).toInt()
        val diversityFactor = (paletteCount / 5)

        // Cluster development level scales from combined constructive effort within this contiguous cluster
        return (sizeFactor + blockFactor + diversityFactor).coerceAtLeast(1)
    }

    fun recordBlockPlacement(level: Level, pos: ChunkPos, block: Block) {
        val cluster = getContiguousCluster(level, pos) ?: return
        val clusterKey = "${level.dimension().location()}:${cluster.chunks.minByOrNull { it.toLong() }?.toLong()}"

        clusterBlockCounts.compute(clusterKey) { _, current -> (current ?: 0L) + 1L }
        clusterPaletteDiversity.computeIfAbsent(clusterKey) { ConcurrentHashMap.newKeySet() }
            .add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString())
    }

    fun recordBlockBreak(level: Level, pos: ChunkPos) {
        val cluster = getContiguousCluster(level, pos) ?: return
        val clusterKey = "${level.dimension().location()}:${cluster.chunks.minByOrNull { it.toLong() }?.toLong()}"

        clusterBlockCounts.compute(clusterKey) { _, current -> ((current ?: 0L) - 1L).coerceAtLeast(0L) }
    }
}
