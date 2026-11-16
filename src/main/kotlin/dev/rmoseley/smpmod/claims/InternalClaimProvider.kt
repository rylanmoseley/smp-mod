package dev.rmoseley.smpmod.claims

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fallback built-in claim provider.
 * Supports arbitrary noncontiguous chunk claims with party-sharing capabilities.
 */
class InternalClaimProvider : IClaimProvider {

    data class StoredClaim(
        val dimensionId: String,
        val chunkPos: ChunkPos,
        val ownerUuid: UUID,
        val partyMembers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    )

    // Keyed by "dim:x:z"
    private val claims = ConcurrentHashMap<String, StoredClaim>()

    // Global party rosters: ownerUuid -> set of member UUIDs
    private val partyRosters = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    private fun makeKey(level: Level, pos: ChunkPos): String {
        return "${level.dimension().location()}:${pos.x}:${pos.z}"
    }

    override fun getClaimOwner(level: Level, pos: ChunkPos): UUID? {
        return claims[makeKey(level, pos)]?.ownerUuid
    }

    override fun getAllClaimedChunks(level: Level, ownerUuid: UUID): Set<ChunkPos> {
        val dimId = level.dimension().location().toString()
        return claims.values
            .filter { it.dimensionId == dimId && it.ownerUuid == ownerUuid }
            .map { it.chunkPos }
            .toSet()
    }

    override fun getAllPartyClaimedChunks(level: Level, player: Player): Set<ChunkPos> {
        val dimId = level.dimension().location().toString()
        val partyMembers = getPartyMembers(level, player.uuid) + player.uuid

        return claims.values
            .filter { it.dimensionId == dimId && (partyMembers.contains(it.ownerUuid) || it.partyMembers.any { m -> partyMembers.contains(m) }) }
            .map { it.chunkPos }
            .toSet()
    }

    override fun getPartyMembers(level: Level, ownerUuid: UUID): Set<UUID> {
        // Return explicit roster or members associated with this owner's claims
        val roster = partyRosters[ownerUuid]
        if (roster != null && roster.isNotEmpty()) {
            return roster.toSet()
        }

        val foundMembers = mutableSetOf<UUID>()
        for (claim in claims.values) {
            if (claim.ownerUuid == ownerUuid) {
                foundMembers.addAll(claim.partyMembers)
            } else if (claim.partyMembers.contains(ownerUuid)) {
                foundMembers.add(claim.ownerUuid)
                foundMembers.addAll(claim.partyMembers)
            }
        }
        return foundMembers
    }

    fun addPartyMember(ownerUuid: UUID, memberUuid: UUID) {
        partyRosters.computeIfAbsent(ownerUuid) { ConcurrentHashMap.newKeySet() }.add(memberUuid)
        // Update all existing claims owned by this player
        for (claim in claims.values) {
            if (claim.ownerUuid == ownerUuid) {
                claim.partyMembers.add(memberUuid)
            }
        }
    }

    fun removePartyMember(ownerUuid: UUID, memberUuid: UUID) {
        partyRosters[ownerUuid]?.remove(memberUuid)
        for (claim in claims.values) {
            if (claim.ownerUuid == ownerUuid) {
                claim.partyMembers.remove(memberUuid)
            }
        }
    }

    override fun isClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean {
        val claim = claims[makeKey(level, pos)] ?: return false
        if (claim.ownerUuid == player.uuid || claim.partyMembers.contains(player.uuid)) {
            return true
        }
        // Check if player's party owns this claim
        val playerParty = partyRosters[player.uuid]
        return playerParty != null && playerParty.contains(claim.ownerUuid)
    }

    override fun canClaim(level: Level, pos: ChunkPos, player: Player): Boolean {
        // Must be unclaimed
        if (claims.containsKey(makeKey(level, pos))) return false

        // Must not exceed player's Builder playstyle chunk cap
        return ClaimManager.canPlayerClaimMoreChunks(level, player)
    }

    override fun claim(level: Level, pos: ChunkPos, player: Player): Boolean {
        if (!canClaim(level, pos, player)) return false

        val key = makeKey(level, pos)
        val dimId = level.dimension().location().toString()
        val members = partyRosters[player.uuid] ?: ConcurrentHashMap.newKeySet()
        val storedClaim = StoredClaim(dimId, pos, player.uuid, ConcurrentHashMap.newKeySet())
        storedClaim.partyMembers.addAll(members)
        claims[key] = storedClaim
        return true
    }

    override fun unclaim(level: Level, pos: ChunkPos, player: Player): Boolean {
        val key = makeKey(level, pos)
        val claim = claims[key] ?: return false
        if (claim.ownerUuid == player.uuid || claim.partyMembers.contains(player.uuid)) {
            claims.remove(key)
            return true
        }
        return false
    }

    fun clear() {
        claims.clear()
        partyRosters.clear()
    }
}
