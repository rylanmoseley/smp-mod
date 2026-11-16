package dev.rmoseley.smpmod.claims

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.UUID

/**
 * Interface abstraction for claim providers.
 * Supports arbitrary noncontiguous chunk claiming while allowing contiguous cluster leveling.
 */
interface IClaimProvider {

    /**
     * Returns the UUID of the owner of the claim at the given chunk, or null if unclaimed.
     */
    fun getClaimOwner(level: Level, pos: ChunkPos): UUID?

    /**
     * Returns all chunks claimed directly by this owner in the specified dimension.
     * Chunks may be noncontiguous across the world.
     */
    fun getAllClaimedChunks(level: Level, ownerUuid: UUID): Set<ChunkPos>

    /**
     * Returns all chunks claimed by this player or their party/team in the specified dimension.
     */
    fun getAllPartyClaimedChunks(level: Level, player: Player): Set<ChunkPos>

    /**
     * Returns the set of party/team member UUIDs sharing this claim with the owner.
     */
    fun getPartyMembers(level: Level, ownerUuid: UUID): Set<UUID>

    /**
     * Checks if the given chunk is claimed by anyone.
     */
    fun isChunkClaimed(level: Level, pos: ChunkPos): Boolean {
        return getClaimOwner(level, pos) != null
    }

    /**
     * Checks if the given chunk is claimed by the player or their party/team.
     */
    fun isClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean

    /**
     * Determines whether the player can claim the given chunk.
     * Noncontiguous claims are explicitly permitted anywhere.
     * Must respect the player's Builder-scaled claim cap.
     */
    fun canClaim(level: Level, pos: ChunkPos, player: Player): Boolean

    /**
     * Claims the chunk for the player.
     */
    fun claim(level: Level, pos: ChunkPos, player: Player): Boolean

    /**
     * Unclaims the chunk.
     */
    fun unclaim(level: Level, pos: ChunkPos, player: Player): Boolean
}
