package dev.rmoseley.smpmod.claims

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import xaero.pac.common.server.api.OpenPACServerAPI
import java.util.UUID
import java.util.stream.Collectors

/**
 * Claim provider implementation delegating to Open Parties and Claims (OPAC).
 * Enforces noncontiguous claim placement while integrating OPAC protection,
 * party networks, and builder playstyle limits.
 */
class OpacClaimProvider : IClaimProvider {

    override fun getClaimOwner(level: Level, pos: ChunkPos): UUID? {
        val serverLevel = level as? ServerLevel ?: return null
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return null
        val claim = pacApi.serverClaimsManager.get(level.dimension().location(), pos)
        return claim?.playerId
    }

    override fun getAllClaimedChunks(level: Level, ownerUuid: UUID): Set<ChunkPos> {
        val serverLevel = level as? ServerLevel ?: return emptySet()
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return emptySet()
        val playerInfo = pacApi.serverClaimsManager.getPlayerInfo(ownerUuid) ?: return emptySet()
        val dimClaims = playerInfo.getDimension(level.dimension().location()) ?: return emptySet()

        return dimClaims.stream
            .flatMap { it.stream }
            .collect(Collectors.toSet())
    }

    override fun getAllPartyClaimedChunks(level: Level, player: Player): Set<ChunkPos> {
        val serverLevel = level as? ServerLevel ?: return emptySet()
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return emptySet()

        val party = pacApi.partyManager.getPartyByMember(player.uuid)
        if (party != null) {
            val memberIds = party.memberInfoStream
                .map { it.uuid }
                .toList()
                .toSet()

            val combinedChunks = mutableSetOf<ChunkPos>()
            for (memberId in memberIds) {
                combinedChunks.addAll(getAllClaimedChunks(level, memberId))
            }
            return combinedChunks
        }

        return getAllClaimedChunks(level, player.uuid)
    }

    override fun getPartyMembers(level: Level, ownerUuid: UUID): Set<UUID> {
        val serverLevel = level as? ServerLevel ?: return setOf(ownerUuid)
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return setOf(ownerUuid)
        val party = pacApi.partyManager.getPartyByMember(ownerUuid) ?: return setOf(ownerUuid)

        return party.memberInfoStream
            .map { it.uuid }
            .toList()
            .toSet()
    }

    override fun isClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean {
        val owner = getClaimOwner(level, pos) ?: return false
        if (owner == player.uuid) return true

        val serverLevel = level as? ServerLevel ?: return false
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return false

        // Check OPAC party membership
        val partyManager = pacApi.partyManager
        val playerParty = partyManager.getPartyByMember(player.uuid)
        val ownerParty = partyManager.getPartyByMember(owner)
        if (playerParty != null && playerParty == ownerParty) {
            return true
        }

        // Delegate to OPAC's native chunk protection access check
        return pacApi.chunkProtection.hasChunkAccess(player, level.dimension().location(), pos.x, pos.z)
    }

    override fun canClaim(level: Level, pos: ChunkPos, player: Player): Boolean {
        // Enforce Builder playstyle chunk limit
        if (!ClaimManager.canPlayerClaimMoreChunks(level, player)) {
            return false
        }

        val serverLevel = level as? ServerLevel ?: return false
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return false

        return pacApi.serverClaimsManager.isClaimable(level.dimension().location()) &&
                pacApi.serverClaimsManager.get(level.dimension().location(), pos) == null
    }

    override fun claim(level: Level, pos: ChunkPos, player: Player): Boolean {
        if (!canClaim(level, pos, player)) return false

        val serverLevel = level as? ServerLevel ?: return false
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return false

        val result = pacApi.serverClaimsManager.tryToClaim(
            level.dimension().location(),
            player.uuid,
            pos.x,
            pos.z,
            0,
            0,
            0,
            false
        )
        return result.resultType.success
    }

    override fun unclaim(level: Level, pos: ChunkPos, player: Player): Boolean {
        if (!isClaimedBy(level, pos, player)) return false

        val serverLevel = level as? ServerLevel ?: return false
        val server = serverLevel.server
        val pacApi = OpenPACServerAPI.get(server) ?: return false

        val result = pacApi.serverClaimsManager.tryToUnclaim(
            level.dimension().location(),
            player.uuid,
            pos.x,
            pos.z,
            0,
            0,
            false
        )
        return result.resultType.success
    }
}
