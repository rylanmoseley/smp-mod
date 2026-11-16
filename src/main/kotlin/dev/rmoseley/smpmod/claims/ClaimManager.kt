package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages contiguous land claims, topology verification, and Builder chunk scaling.
 */
object ClaimManager {

    data class ClaimedChunk(
        val pos: ChunkPos,
        val ownerUuid: UUID,
        val memberUuids: MutableSet<UUID> = mutableSetOf()
    )

    // Keyed by ChunkPos.toLong()
    private val worldClaims = ConcurrentHashMap<Long, ClaimedChunk>()

    /**
     * Determines whether a new chunk can be claimed contiguously.
     * The new chunk must share an orthogonal edge with an existing chunk owned by the player/party,
     * unless this is the player's very first claimed chunk.
     */
    fun canClaimContiguous(player: Player, newPos: ChunkPos): Boolean {
        val playerUuid = player.uuid
        val existingClaims = worldClaims.values.filter { it.ownerUuid == playerUuid }

        // First claim is always allowed
        if (existingClaims.isEmpty()) return true

        // Orthogonal adjacency check (N, S, E, W)
        val adjacent = listOf(
            ChunkPos(newPos.x + 1, newPos.z),
            ChunkPos(newPos.x - 1, newPos.z),
            ChunkPos(newPos.x, newPos.z + 1),
            ChunkPos(newPos.x, newPos.z - 1)
        )

        return existingClaims.any { claim ->
            adjacent.any { it.x == claim.pos.x && it.z == claim.pos.z }
        }
    }

    /**
     * Calculates maximum allowed claimable chunks based on Builder level.
     */
    fun getMaxClaimableChunks(builderLevel: Int): Int {
        val base = SmpConfig.COMMON.maxBaseClaimChunks.get()
        val perLevel = SmpConfig.COMMON.chunksPerBuilderLevel.get()
        return base + (builderLevel * perLevel)
    }

    fun claimChunk(player: Player, pos: ChunkPos, builderLevel: Int): Boolean {
        val key = pos.toLong()
        if (worldClaims.containsKey(key)) return false

        val currentCount = worldClaims.values.count { it.ownerUuid == player.uuid }
        if (currentCount >= getMaxClaimableChunks(builderLevel)) return false

        if (!canClaimContiguous(player, pos)) return false

        worldClaims[key] = ClaimedChunk(pos, player.uuid)
        return true
    }

    fun isChunkClaimedBy(pos: ChunkPos, player: Player): Boolean {
        val claim = worldClaims[pos.toLong()] ?: return false
        return claim.ownerUuid == player.uuid || claim.memberUuids.contains(player.uuid)
    }

    fun getClaimAt(pos: ChunkPos): ClaimedChunk? {
        return worldClaims[pos.toLong()]
    }
}
