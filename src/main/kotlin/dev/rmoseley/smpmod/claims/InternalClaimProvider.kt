package dev.rmoseley.smpmod.claims

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fallback built-in claim provider.
 * Supports arbitrary noncontiguous chunk claims.
 */
class InternalClaimProvider : IClaimProvider {

    data class StoredClaim(
        val dimensionId: String,
        val chunkPos: ChunkPos,
        val ownerUuid: UUID,
        val partyMembers: MutableSet<UUID> = mutableSetOf()
    )

    // Keyed by "dim:x:z"
    private val claims = ConcurrentHashMap<String, StoredClaim>()

    private fun makeKey(level: Level, pos: ChunkPos): String {
        return "${level.dimension().location()}:${pos.x}:${pos.z}"
    }

    override fun getClaimOwner(level: Level, pos: ChunkPos): UUID? {
        return claims[makeKey(level, pos)]?.ownerUuid
    }

    override fun getAllClaimedChunks(level: Level, ownerUuid: UUID): Set<ChunkPos> {
        val dimId = level.dimension().location().toString()
        return claims.values
            .filter { it.dimensionId == dimId && (it.ownerUuid == ownerUuid || it.partyMembers.contains(ownerUuid)) }
            .map { it.chunkPos }
            .toSet()
    }

    override fun isClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean {
        val claim = claims[makeKey(level, pos)] ?: return false
        return claim.ownerUuid == player.uuid || claim.partyMembers.contains(player.uuid)
    }

    override fun canClaim(level: Level, pos: ChunkPos, player: Player): Boolean {
        // Noncontiguous claims are explicitly allowed; only verify the chunk is currently unclaimed
        return !claims.containsKey(makeKey(level, pos))
    }

    override fun claim(level: Level, pos: ChunkPos, player: Player): Boolean {
        if (!canClaim(level, pos, player)) return false
        val key = makeKey(level, pos)
        val dimId = level.dimension().location().toString()
        claims[key] = StoredClaim(dimId, pos, player.uuid)
        return true
    }
}
