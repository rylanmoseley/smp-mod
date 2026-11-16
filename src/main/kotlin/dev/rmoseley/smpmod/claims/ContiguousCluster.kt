package dev.rmoseley.smpmod.claims

import net.minecraft.world.level.ChunkPos
import java.util.UUID

/**
 * Settlement progression tiers derived strictly from contiguous chunk count.
 * Noncontiguous claims (outposts, distant mines) are evaluated independently.
 */
enum class SettlementTier(
    val minChunks: Int,
    val displayName: String,
    val shopDiscountPercent: Double,
    val description: String
) {
    OUTPOST(1, "Outpost", 0.0, "Isolated frontier claim (1 chunk)"),
    HAMLET(2, "Hamlet", 0.05, "Small gathering of contiguous claims (2-3 chunks)"),
    SETTLEMENT(4, "Settlement", 0.10, "Developing community (4-8 contiguous chunks)"),
    TOWN(9, "Town", 0.15, "Fortified settlement (9-15 contiguous chunks)"),
    METROPOLIS(16, "Metropolis", 0.20, "Major regional capital (16+ contiguous chunks)");

    companion object {
        fun fromChunkCount(count: Int): SettlementTier = when {
            count >= METROPOLIS.minChunks -> METROPOLIS
            count >= TOWN.minChunks -> TOWN
            count >= SETTLEMENT.minChunks -> SETTLEMENT
            count >= HAMLET.minChunks -> HAMLET
            else -> OUTPOST
        }
    }
}

/**
 * Represents a maximal contiguous component of claimed chunks.
 *
 * Architectural Rule:
 * Chunk claims CAN be noncontiguous across the world (e.g., outposts, distant portals, mines).
 * However, only contiguous chunks within a specific cluster contribute to that claim's:
 *   1. Development Leveling
 *   2. Block-palette variety score and net construction
 *   3. Pooled territorial defensive buffs (damage reduction, health regen, light shroud duration reduction)
 *   4. Claim-locked builder flight boundaries!
 */
data class ContiguousCluster(
    val clusterId: UUID = UUID.randomUUID(),
    val ownerUuid: UUID,
    val dimensionId: String,
    val chunks: Set<ChunkPos>,
    val developmentLevel: Int,
    val paletteVarietyScore: Double,
    val pooledDefenseMitigation: Double,
    val pooledHealthRegenSeconds: Int,
    val lightSnuffReductionPercent: Double,
    val netPlacedBlocks: Long,
    val blockCountsByType: Map<String, Long> = emptyMap(),
    val members: Set<UUID> = emptySet()
) {
    val chunkCount: Int
        get() = chunks.size

    val clusterTier: SettlementTier
        get() = SettlementTier.fromChunkCount(chunkCount)

    val clusterTierName: String
        get() = clusterTier.displayName

    fun isMember(playerUuid: UUID): Boolean {
        return ownerUuid == playerUuid || members.contains(playerUuid)
    }

    fun containsChunk(pos: ChunkPos): Boolean {
        return chunks.contains(pos)
    }
}
