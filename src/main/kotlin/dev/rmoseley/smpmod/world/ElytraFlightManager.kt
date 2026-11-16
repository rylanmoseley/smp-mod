package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * Enforces Explorer milestone gating for Elytra deployment,
 * disables mid-air rocket boosting, and prevents Nether flight abuse.
 */
object ElytraFlightManager {

    fun canDeployElytra(player: ServerPlayer): Boolean {
        if (player.isCreative || player.isSpectator) return true

        // Nether flight lockout
        if (player.level().dimension() == Level.NETHER) {
            player.displayClientMessage(
                Component.literal("§cNether thermal updrafts prevent Elytra glide stability!"),
                true
            )
            return false
        }

        val discoveredChunks = player.getData(ModAttachmentTypes.PLAYER_DISCOVERED_CHUNKS.get())
        val threshold = SmpConfig.COMMON.elytraExplorerChunkMilestone.get()

        if (discoveredChunks < threshold) {
            player.displayClientMessage(
                Component.literal("§6Elytra gliding requires Explorer milestone ($discoveredChunks / $threshold chunks mapped)."),
                true
            )
            return false
        }

        return true
    }

    /**
     * Intercepts firework rocket usage while gliding to enforce momentum-glider mechanics.
     */
    fun shouldCancelRocketBoost(player: ServerPlayer): Boolean {
        if (player.isCreative || player.isSpectator) return false
        return player.isFallFlying
    }
}
