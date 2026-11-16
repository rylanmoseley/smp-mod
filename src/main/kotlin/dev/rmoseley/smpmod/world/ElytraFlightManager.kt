package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/**
 * Enforces Explorer milestone gating for Elytra deployment,
 * momentum glider flight physics, Scout glide efficiency bonuses,
 * and strict Nether flight lockout with thermal penalties.
 */
object ElytraFlightManager {

    /**
     * Checks if a player qualifies to deploy Elytra flight.
     * Hard-gated behind broad unique-chunk and multi-biome exploration milestones.
     */
    fun canDeployElytra(player: ServerPlayer): Boolean {
        if (player.isCreative || player.isSpectator) return true

        // 1. Nether Flight Lockout
        if (player.level().dimension() == Level.NETHER) {
            return false
        }

        // 2. Exploration Milestones Verification
        val discoveredChunks = BiomeMobilityManager.getUniqueChunksExplored(player)
        val chunkThreshold = SmpConfig.COMMON.elytraExplorerChunkMilestone.get()

        val exploredBiomes = BiomeMobilityManager.getExploredBiomeCount(player)
        val biomeThreshold = SmpConfig.COMMON.elytraMultiBiomeMilestone.get()

        val xpData = player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())
        val hasExplorerXp = xpData.explorerXp >= 150.0

        if (discoveredChunks < chunkThreshold || exploredBiomes < biomeThreshold || !hasExplorerXp) {
            return false
        }

        return true
    }

    /**
     * Ticks player Elytra flight to enforce:
     * - Immediate glide cancellation if unqualified or in the Nether.
     * - Nether thermal penalties (downdraft & burn damage).
     * - Scout role momentum glider aerodynamic efficiency.
     */
    fun onPlayerTick(player: ServerPlayer) {
        if (!player.isFallFlying) return

        val level = player.level()

        // 1. Nether Flight Lockout & Thermal Downdraft
        if (level.dimension() == Level.NETHER) {
            player.stopFallFlying()

            // Downward thermal downdraft plunge
            player.deltaMovement = Vec3(player.deltaMovement.x * 0.5, -0.75, player.deltaMovement.z * 0.5)
            player.hurt(player.damageSources().hotFloor(), 3.0f)
            player.remainingFireTicks = 60

            val serverLevel = player.serverLevel()
            serverLevel.sendParticles(
                ParticleTypes.FLAME,
                player.x, player.y, player.z,
                20, 0.4, 0.4, 0.4, 0.05
            )
            serverLevel.playSound(
                null, player.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                1.0f, 0.8f
            )

            player.displayClientMessage(
                Component.literal("§cNether thermal updrafts collapse Elytra wings! Flight is strictly locked in the Nether."),
                true
            )
            return
        }

        // 2. Unqualified Glider Cancellation
        if (!canDeployElytra(player)) {
            player.stopFallFlying()

            val discoveredChunks = BiomeMobilityManager.getUniqueChunksExplored(player)
            val chunkThreshold = SmpConfig.COMMON.elytraExplorerChunkMilestone.get()
            val exploredBiomes = BiomeMobilityManager.getExploredBiomeCount(player)
            val biomeThreshold = SmpConfig.COMMON.elytraMultiBiomeMilestone.get()

            player.displayClientMessage(
                Component.literal("§cElytra flight locked! Requires Explorer milestone: $discoveredChunks/$chunkThreshold chunks across $exploredBiomes/$biomeThreshold biomes."),
                true
            )
            return
        }

        // 3. Scout Role Glider Aerodynamics (Rewarding launch towers & altitude conversion)
        val scoutAgility = player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)?.value ?: 1.0
        if (scoutAgility > 1.0) {
            // Scouts experience reduced atmospheric drag, preserving horizontal momentum
            val efficiencyBonus = ((scoutAgility - 1.0) * 0.035).coerceIn(0.005, 0.05)
            val horizontalScale = 1.0 + efficiencyBonus

            player.deltaMovement = Vec3(
                player.deltaMovement.x * horizontalScale,
                player.deltaMovement.y,
                player.deltaMovement.z * horizontalScale
            )

            // Occasional aerodynamic vapor particles behind wings
            if (player.tickCount % 10 == 0) {
                player.serverLevel().sendParticles(
                    ParticleTypes.CLOUD,
                    player.x, player.y + 0.2, player.z,
                    2, 0.1, 0.1, 0.1, 0.01
                )
            }
        }
    }

    /**
     * Intercepts firework rocket usage while gliding to enforce momentum-glider mechanics.
     * Mid-air rocket boosting is completely disabled.
     */
    fun shouldCancelRocketBoost(player: ServerPlayer): Boolean {
        if (player.isCreative || player.isSpectator) return false
        if (player.isFallFlying) {
            player.displayClientMessage(
                Component.literal("§6Mid-air rocket boosting is disabled! Elytra operates strictly as a momentum glider."),
                true
            )
            return true
        }
        return false
    }
}
