package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Controls temporary builder flight.
 * Flight lasts for ~5 seconds of grace, refreshed by continuous block placement inside claims,
 * and is immediately revoked upon taking damage or entering combat.
 */
object BuilderFlightController {

    private val activeFlightTimers = ConcurrentHashMap<UUID, Long>()

    fun recordBlockPlacement(player: ServerPlayer) {
        val chunkPos = ChunkPos(player.blockPosition())
        if (!ClaimManager.isChunkClaimedBy(chunkPos, player)) {
            revokeFlight(player)
            return
        }

        val graceSeconds = SmpConfig.COMMON.builderFlightGraceSeconds.get()
        val expiresAt = System.currentTimeMillis() + (graceSeconds * 1000L)
        activeFlightTimers[player.uuid] = expiresAt

        enableFlight(player)
    }

    fun onTickPlayer(player: ServerPlayer) {
        val expiresAt = activeFlightTimers[player.uuid] ?: return
        val chunkPos = ChunkPos(player.blockPosition())

        // Expired or left claim boundaries
        if (System.currentTimeMillis() > expiresAt || !ClaimManager.isChunkClaimedBy(chunkPos, player)) {
            revokeFlight(player)
        }
    }

    fun onCombatEngaged(player: ServerPlayer) {
        revokeFlight(player)
    }

    private fun enableFlight(player: ServerPlayer) {
        if (!player.abilities.mayfly) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
    }

    private fun revokeFlight(player: ServerPlayer) {
        activeFlightTimers.remove(player.uuid)
        if (!player.isCreative && !player.isSpectator && player.abilities.mayfly) {
            player.abilities.mayfly = false
            player.abilities.flying = false
            player.onUpdateAbilities()
        }
    }
}
