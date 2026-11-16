package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.claims.VarietyScorer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks Builder Playstyle XP:
 * Individually awarded from net variety-weighted block placement within contiguous claims,
 * scaled by the cluster's development leveling and diversity.
 */
object BuilderXpTracker {

    // Anti-oscillating exploit cache: tracks recent positions placed by player
    private val recentPlacements = ConcurrentHashMap<UUID, ConcurrentHashMap<BlockPos, Long>>()

    /**
     * Evaluates and awards Builder XP when a player places a block.
     */
    fun onBlockPlaced(player: ServerPlayer, pos: BlockPos, block: Block) {
        val level = player.serverLevel()
        val chunkPos = ChunkPos(pos)

        // 1. Strict guardrail: must be inside player's or party's claim
        if (!ClaimManager.isChunkClaimedBy(level, chunkPos, player)) {
            return
        }

        // 2. Anti-exploit check: rapid place-break on identical coordinates yields zero XP
        val playerCache = recentPlacements.computeIfAbsent(player.uuid) { ConcurrentHashMap() }
        val now = System.currentTimeMillis()
        val lastPlaced = playerCache[pos]
        if (lastPlaced != null && (now - lastPlaced) < 15_000L) {
            // Placed at this coordinate recently
            return
        }
        playerCache[pos] = now
        if (playerCache.size > 200) {
            playerCache.values.removeIf { now - it > 60_000L }
        }

        // 3. Retrieve Contiguous Cluster metrics
        val cluster = ClaimManager.getContiguousCluster(level, chunkPos)
        val clusterDevLevel = cluster?.developmentLevel ?: 1
        val paletteCount = (cluster?.paletteVarietyScore ?: 1.0).toInt().coerceAtLeast(1)
        val netBlockCount = (cluster?.netPlacedBlocks ?: 1L).toInt()

        // 4. Calculate variety-weighted score with diminishing returns
        val baseVarietyScore = VarietyScorer.calculateBlockPlacementScore(
            block = block,
            currentCountOfThisBlock = netBlockCount,
            uniquePaletteCount = paletteCount
        )

        // 5. Contiguous cluster development level multiplier (encourages building up town hubs)
        val clusterLevelBonus = 1.0 + (clusterDevLevel * 0.05).coerceAtMost(1.0)
        val awardedXp = baseVarietyScore * clusterLevelBonus

        PlaystyleXpTracker.incrementBuilderXp(player, awardedXp)
    }
}
