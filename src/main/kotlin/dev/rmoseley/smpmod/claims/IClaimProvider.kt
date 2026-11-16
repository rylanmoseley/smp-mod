package dev.rmoseley.smpmod.claims

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.UUID

/**
 * Interface abstraction for claim providers (e.g. Open Parties and Claims, FTB Chunks, or Internal).
 */
interface IClaimProvider {

    /**
     * Returns the UUID of the owner of the claim at the given chunk, or null if unclaimed.
     */
    fun getClaimOwner(level: Level, pos: ChunkPos): UUID?

    /**
     * Returns all chunks claimed by this owner in the specified dimension.
     * Chunks may be noncontiguous.
     */
    fun getAllClaimedChunks(level: Level, ownerUuid: UUID): Set<ChunkPos>

    /**
     * Checks if the given chunk is claimed by the player or their party/team.
     */
    fun isClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean

    /**
     * Determines whether the player can claim the given chunk.
     * Noncontiguous claims are explicitly permitted.
     */
    fun canClaim(level: Level, pos: ChunkPos, player: Player): Boolean

    /**
     * Claims the chunk for the player.
     */
    fun claim(level: Level, pos: ChunkPos, player: Player): Boolean
}
