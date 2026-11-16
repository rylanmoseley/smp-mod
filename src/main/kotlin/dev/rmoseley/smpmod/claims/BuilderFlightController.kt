package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Controls temporary builder flight.
 *
 * Rules:
 * 1. Sustained strictly by continuous block placement inside an owned/shared contiguous claim cluster.
 * 2. ~5-second grace window refreshed upon each qualifying block placement.
 * 3. Strictly restricted to the borders of that specific contiguous cluster.
 * 4. Instantly revoked upon leaving the cluster, entering combat, or taking damage.
 */
object BuilderFlightController {

    data class FlightSession(
        val clusterId: UUID,
        val dimensionId: String,
        val allowedChunks: Set<ChunkPos>,
        var expiresAtMs: Long
    )

    private val activeSessions = ConcurrentHashMap<UUID, FlightSession>()

    /**
     * Called whenever a player places a block. Refreshes or initiates temporary builder flight
     * if the placement occurred within a valid contiguous claim cluster owned or shared by the player.
     */
    fun recordBlockPlacement(player: ServerPlayer, pos: BlockPos) {
        val chunkPos = ChunkPos(pos)
        val level = player.level()

        // Verify player has claim rights at this chunk
        if (!ClaimManager.isChunkClaimedBy(level, chunkPos, player)) {
            revokeFlight(player)
            return
        }

        // Retrieve the contiguous cluster
        val cluster = ClaimManager.getContiguousCluster(level, chunkPos)
        if (cluster == null || !cluster.isMember(player.uuid)) {
            revokeFlight(player)
            return
        }

        val graceSeconds = SmpConfig.COMMON.builderFlightGraceSeconds.get()
        val expiresAt = System.currentTimeMillis() + (graceSeconds * 1000L)
        val dimId = level.dimension().location().toString()

        val existingSession = activeSessions[player.uuid]
        if (existingSession != null && existingSession.clusterId == cluster.clusterId) {
            existingSession.expiresAtMs = expiresAt
        } else {
            activeSessions[player.uuid] = FlightSession(
                clusterId = cluster.clusterId,
                dimensionId = dimId,
                allowedChunks = cluster.chunks,
                expiresAtMs = expiresAt
            )
        }

        enableFlight(player)
    }

    /**
     * Periodic check on player tick enforcing time expiration and strict cluster boundary confinement.
     */
    fun onTickPlayer(player: ServerPlayer) {
        val session = activeSessions[player.uuid] ?: return
        val currentChunk = ChunkPos(player.blockPosition())
        val currentDim = player.level().dimension().location().toString()

        // 1. Time grace period expired
        if (System.currentTimeMillis() > session.expiresAtMs) {
            revokeFlight(player)
            return
        }

        // 2. Strict spatial confinement: crossed outside the contiguous cluster's chunks or changed dimension
        if (currentDim != session.dimensionId || !session.allowedChunks.contains(currentChunk)) {
            revokeFlight(player)
            return
        }
    }

    /**
     * Instantly revokes flight when taking damage or attacking in combat.
     */
    fun onCombatEngaged(player: ServerPlayer) {
        ClaimManager.recordCombatEngagement(player)
        revokeFlight(player)
    }

    fun isFlightActive(player: Player): Boolean {
        return activeSessions.containsKey(player.uuid)
    }

    fun getRemainingGraceSeconds(player: Player): Double {
        val session = activeSessions[player.uuid] ?: return 0.0
        val remainingMs = session.expiresAtMs - System.currentTimeMillis()
        return max(0.0, remainingMs / 1000.0)
    }

    private fun enableFlight(player: ServerPlayer) {
        if (!player.abilities.mayfly) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
    }

    fun revokeFlight(player: ServerPlayer) {
        val hadSession = activeSessions.remove(player.uuid) != null
        if (!player.isCreative && !player.isSpectator && (player.abilities.mayfly || hadSession)) {
            player.abilities.mayfly = false
            player.abilities.flying = false
            player.resetFallDistance()
            player.onUpdateAbilities()
        }
    }
}
