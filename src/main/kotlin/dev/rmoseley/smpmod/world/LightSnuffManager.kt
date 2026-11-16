package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.Level
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the Claim-Safe Light Snuffing / Shroud Aura strictly adhering to Section 4.3 of design-v2.md:
 *
 * Anti-Griefing Guardrail: Inside contiguous claims, mobs with `destroys_light_on_aggro`
 * CANNOT destroy blocks. Instead, they emit a temporary 20-30s snuff/shroud aura that
 * suppresses light output without damaging Builder infrastructure.
 */
object LightSnuffManager {

    data class SnuffZone(
        val dimension: ResourceLocation,
        val center: BlockPos,
        val radius: Double,
        val expireGameTime: Long
    )

    // Keyed by "dim:x:y:z"
    private val activeSnuffZones = ConcurrentHashMap<String, SnuffZone>()

    /**
     * Applies a temporary claim-safe light snuff aura around the targeted illumination source.
     * Blocks are 100% preserved.
     */
    fun applySnuffAura(level: Level, pos: BlockPos, mob: Mob?) {
        val serverLevel = level as? ServerLevel ?: return
        val durationSeconds = SmpConfig.COMMON.lightSnuffDurationSeconds.get()
        val expireTime = serverLevel.gameTime + (durationSeconds * 20L)

        val zoneKey = "${level.dimension().location()}:${pos.x}:${pos.y}:${pos.z}"
        val zone = SnuffZone(
            dimension = level.dimension().location(),
            center = pos,
            radius = 7.0,
            expireGameTime = expireTime
        )
        activeSnuffZones[zoneKey] = zone

        // Visual and auditory effects: dark shroud particles extinguish illumination ambiance
        serverLevel.sendParticles(
            ParticleTypes.SQUID_INK,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            35, 1.2, 1.2, 1.2, 0.05
        )
        serverLevel.sendParticles(
            ParticleTypes.SMOKE,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            20, 0.8, 0.8, 0.8, 0.02
        )
        serverLevel.playSound(
            null, pos,
            SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE,
            0.8f, 0.5f
        )
    }

    /**
     * Ticks active snuff zones, enforcing the shroud aura on nearby players and clearing expired zones.
     */
    fun onPlayerTick(player: ServerPlayer) {
        val level = player.serverLevel()
        val currentTick = level.gameTime
        val playerDim = level.dimension().location()

        // Clean up expired zones and apply darkness effect to players within active zones
        val iterator = activeSnuffZones.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val zone = entry.value

            if (currentTick >= zone.expireGameTime) {
                // Zone expired: restore illumination ambiance with light sparkles
                if (zone.dimension == playerDim) {
                    level.sendParticles(
                        ParticleTypes.END_ROD,
                        zone.center.x + 0.5, zone.center.y + 0.5, zone.center.z + 0.5,
                        15, 0.5, 0.5, 0.5, 0.02
                    )
                }
                iterator.remove()
                continue
            }

            if (zone.dimension == playerDim) {
                val distSq = player.distanceToSqr(
                    zone.center.x + 0.5,
                    zone.center.y + 0.5,
                    zone.center.z + 0.5
                )
                val radiusSq = zone.radius * zone.radius

                if (distSq <= radiusSq) {
                    // Inside shroud aura: suppress visibility
                    player.addEffect(MobEffectInstance(MobEffects.DARKNESS, 45, 0, false, false, true))

                    val remainingSeconds = ((zone.expireGameTime - currentTick) / 20L).coerceAtLeast(1L)
                    player.displayClientMessage(
                        Component.literal("§8[Shadow Shroud] Light suppressed by dark elite aura! (Recovers in ${remainingSeconds}s)"),
                        true
                    )

                    // Ambient smoke around player
                    if (player.tickCount % 10 == 0) {
                        level.sendParticles(
                            ParticleTypes.SMOKE,
                            player.x, player.y + 1.0, player.z,
                            3, 0.3, 0.3, 0.3, 0.01
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks if a specific position is currently covered by an active snuff zone.
     */
    fun isPositionSnuffed(level: Level, pos: BlockPos): Boolean {
        val dim = level.dimension().location()
        val currentTick = level.gameTime

        return activeSnuffZones.values.any { zone ->
            zone.dimension == dim &&
                    currentTick < zone.expireGameTime &&
                    pos.distSqr(zone.center) <= (zone.radius * zone.radius)
        }
    }
}
