package dev.rmoseley.smpmod.world

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level

/**
 * Enforces Nether transit hazards and disables Nether roof access.
 */
object NetherHazardHandler {

    const val NETHER_ROOF_Y_LIMIT: Int = 127

    fun checkNetherPlayer(player: ServerPlayer) {
        if (player.level().dimension() != Level.NETHER) return

        // Nether roof barrier enforcement
        if (player.y >= NETHER_ROOF_Y_LIMIT && !player.isCreative && !player.isSpectator) {
            val safeY = 120.0
            player.teleportTo(player.x, safeY, player.z)
            player.displayClientMessage(
                Component.literal("§cDimensional thermal turbulence bars Nether roof transit!"),
                true
            )
        }
    }
}
