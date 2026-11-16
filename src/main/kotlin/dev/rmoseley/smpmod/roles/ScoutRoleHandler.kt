package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

/**
 * Handles Scout battlefield mechanics:
 * 1. Traversal mobility, smooth step height, and fall damage reduction
 * 2. Elytra glide efficiency and crash kinetic dampening
 * 3. Dark-zone threat and trap detection radar
 * 4. End Crystal navigation, pillar scaling, and caged crystal disruption
 */
object ScoutRoleHandler {

    /**
     * Reduces fall damage for Scout specialists based on agility.
     */
    fun processFallDamage(player: Player, initialDamage: Float): Float {
        val agilityAttr = player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)
        val agility = (agilityAttr?.value ?: 1.0) - 1.0
        val reduction = (agility * 1.5).coerceIn(0.0, 0.70)
        return initialDamage * (1.0f - reduction.toFloat())
    }

    /**
     * Dampens kinetic collision damage during Elytra flight.
     */
    fun processElytraCrashDamage(player: Player, initialDamage: Float): Float {
        if (!isScoutSpecialist(player)) return initialDamage
        return initialDamage * 0.35f // 65% crash reduction
    }

    /**
     * Periodic sensory radar: scans for dark-zone elites, mob spawners, and concealed traps.
     */
    fun performThreatRadarScan(scout: ServerPlayer) {
        if (!isScoutSpecialist(scout)) return

        val radius = SmpConfig.COMMON.scoutTrapDetectionRadius.get().toDouble()
        val scoutPos = scout.blockPosition()
        val level = scout.serverLevel()

        // 1. Scan for Dark-Zone Elites (destroys_light_on_aggro)
        val searchBox = AABB(scoutPos).inflate(radius)
        val nearbyHostiles = level.getEntitiesOfClass(Mob::class.java, searchBox) { m ->
            m.isAlive && !m.isAlliedTo(scout)
        }

        var eliteDetected = 0
        for (mob in nearbyHostiles) {
            val trait = mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
            if (trait != null && trait.value > 0.5) {
                // Highlight dark-zone elite with Glowing effect
                mob.addEffect(MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false))
                eliteDetected++
            }
        }

        // 2. Scan for Concealed Traps & Spawners within radius
        var trapFound = false
        var trapName = ""
        val intRadius = radius.toInt().coerceAtMost(16)

        // Check select sample coordinates in sensory radius
        for (x in -intRadius..intRadius step 3) {
            for (y in -4..4 step 2) {
                for (z in -intRadius..intRadius step 3) {
                    val testPos = scoutPos.offset(x, y, z)
                    val state = level.getBlockState(testPos)
                    val block = state.block

                    if (block == Blocks.TRIPWIRE || block == Blocks.TRIPWIRE_HOOK ||
                        block == Blocks.TNT || block == Blocks.SPAWNER ||
                        block == Blocks.SCULK_SHRIEKER
                    ) {
                        trapFound = true
                        trapName = block.name.string
                        break
                    }
                }
                if (trapFound) break
            }
            if (trapFound) break
        }

        // Radar cues
        if (eliteDetected > 0 || trapFound) {
            val alert = if (eliteDetected > 0 && trapFound) {
                "§e[Radar] $eliteDetected Dark-Zone Elite(s) & Trap ($trapName) detected nearby!"
            } else if (eliteDetected > 0) {
                "§e[Radar] $eliteDetected Dark-Zone Elite(s) detected! Highlighted for team."
            } else {
                "§c[Radar Warning] Concealed hazard detected: $trapName!"
            }
            scout.displayClientMessage(Component.literal(alert), true)

            level.sendParticles(
                ParticleTypes.INSTANT_EFFECT,
                scout.x, scout.y + 0.1, scout.z,
                5, 0.3, 0.1, 0.3, 0.05
            )
        }
    }

    /**
     * Checks if a Scout can safely disrupt an End Crystal without explosive blowback.
     */
    fun canSafelyDisruptCrystal(scout: Player, crystal: EndCrystal): Boolean {
        return isScoutSpecialist(scout)
    }

    fun isScoutSpecialist(player: Player): Boolean {
        val profile = RoleCalculator.calculateProfile(player)
        return profile.scoutRatio >= 0.35 || profile.primaryRole == RoleCalculator.Role.SCOUT
    }
}
