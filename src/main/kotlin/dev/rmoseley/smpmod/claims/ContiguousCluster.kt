package dev.rmoseley.smpmod.claims

import net.minecraft.world.level.ChunkPos
import java.util.UUID

/**
 * Represents a maximal contiguous component of claimed chunks.
 * Even though a player's claims may be noncontiguous across the world,
 * each contiguous cluster levels its development, pooled buffs, and flight independently.
 */
data class ContiguousCluster(
    val clusterId: UUID = UUID.randomUUID(),
    val ownerUuid: UUID,
    val chunks: Set<ChunkPos>,
    val developmentLevel: Int,
    val paletteVarietyScore: Double,
    val pooledDefenseMitigation: Double,
    val netPlacedBlocks: Long
) {
    val chunkCount: Int
        get() = chunks.size

    val clusterTierName: String
        get() = when {
            chunkCount >= 16 -> "Metropolis"
            chunkCount >= 9 -> "Town"
            chunkCount >= 4 -> "Settlement"
            chunkCount >= 2 -> "Hamlet"
            else -> "Outpost"
        }
}
