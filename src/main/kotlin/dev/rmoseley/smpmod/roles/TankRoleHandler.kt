package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.AABB

/**
 * Handles Tank battlefield mechanics:
 * 1. Frontline damage mitigation stacking with contiguous claim pooled defensive buffs
 * 2. Portable cover and kinetic barrier shielding nearby allies from AoE and projectiles
 * 3. Threat control and taunt: peeling destroys_light_on_aggro elites off backline lights and teammates
 */
object TankRoleHandler {

    /**
     * Calculates total mitigated damage received by a Tank player.
     */
    fun processIncomingDamage(tank: Player, rawDamage: Float): Float {
        var damage = rawDamage

        // 1. Frontline Role Mitigation Attribute
        val mitigationAttr = tank.getAttribute(ModEntityAttributes.ROLE_TANK_MITIGATION)
        val roleMitigation = (mitigationAttr?.value ?: 0.0).coerceIn(0.0, 0.65)
        damage *= (1.0 - roleMitigation).toFloat()

        // 2. Contiguous Claim Pooled Defensive Buffs
        val chunkPos = ChunkPos(tank.blockPosition())
        val cluster = ClaimManager.getContiguousCluster(tank.level(), chunkPos)
        if (cluster != null && cluster.isMember(tank.uuid)) {
            val pooledMitigation = cluster.pooledDefenseMitigation.coerceIn(0.0, 0.40)
            damage *= (1.0 - pooledMitigation).toFloat()
        }

        return damage
    }

    /**
     * Checks if an ally standing near/behind a blocking Tank receives portable cover protection.
     */
    fun checkPortableCoverForAlly(ally: Player, rawDamage: Float): Float {
        val level = ally.level()
        val searchBox = AABB(ally.blockPosition()).inflate(5.0)
        val nearbyTanks = level.getEntitiesOfClass(ServerPlayer::class.java, searchBox) { p ->
            p != ally && p.isBlocking && isTankSpecialist(p)
        }

        if (nearbyTanks.isEmpty()) return rawDamage

        val protectingTank = nearbyTanks.first()
        val barrierReduction = SmpConfig.COMMON.tankKineticBarrierReduction.get().toFloat()
        val reducedDamage = rawDamage * (1.0f - barrierReduction)

        // Visual feedback
        val serverLevel = level as? ServerLevel
        serverLevel?.sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            ally.x, ally.y + 1.0, ally.z,
            8, 0.3, 0.3, 0.3, 0.05
        )

        return reducedDamage
    }

    /**
     * Intercepts projectiles aimed at allies when near a blocking Tank's kinetic barrier.
     */
    fun interceptProjectileWithBarrier(projectile: Projectile): Boolean {
        val level = projectile.level()
        val box = AABB(projectile.blockPosition()).inflate(3.0)
        val blockingTanks = level.getEntitiesOfClass(ServerPlayer::class.java, box) { p ->
            p.isBlocking && isTankSpecialist(p)
        }

        if (blockingTanks.isNotEmpty()) {
            val tank = blockingTanks.first()
            val serverLevel = level as? ServerLevel
            serverLevel?.sendParticles(
                ParticleTypes.CRIT,
                projectile.x, projectile.y, projectile.z,
                10, 0.2, 0.2, 0.2, 0.1
            )
            serverLevel?.playSound(
                null, tank.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                1.0f, 1.2f
            )
            projectile.discard()
            return true
        }

        return false
    }

    /**
     * Threat generation: forces a target mob to aggro on the Tank when attacked.
     */
    fun onTankDamageMob(tank: ServerPlayer, mob: Mob) {
        mob.target = tank

        // If mob has destroys_light_on_aggro trait, peel it off lights immediately
        val destroysLight = mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        if (destroysLight != null && destroysLight.value > 0.5) {
            mob.target = tank
            tank.displayClientMessage(
                Component.literal("§9[Tank Threat] §fTaunted ${mob.name.string} off backline lights!"),
                true
            )
        }
    }

    /**
     * Periodic Taunt Pulse: pulses every few seconds during combat, pulling aggro within radius.
     */
    fun performTauntPulse(tank: ServerPlayer) {
        if (!isTankSpecialist(tank)) return

        val radius = SmpConfig.COMMON.tankThreatRadius.get().toDouble()
        val searchBox = AABB(tank.blockPosition()).inflate(radius)
        val nearbyMobs = tank.level().getEntitiesOfClass(Mob::class.java, searchBox) { m ->
            m.isAlive && !m.isAlliedTo(tank)
        }

        var tauntedCount = 0
        for (mob in nearbyMobs) {
            val destroysLight = mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
            val isDarkElite = destroysLight != null && destroysLight.value > 0.5

            if (isDarkElite || mob.target != tank) {
                mob.target = tank
                tauntedCount++
            }
        }

        if (tauntedCount > 0) {
            val serverLevel = tank.serverLevel()
            serverLevel.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                tank.x, tank.y + 1.5, tank.z,
                tauntedCount.coerceAtMost(10), 0.5, 0.3, 0.5, 0.0
            )
        }
    }

    fun isTankSpecialist(player: Player): Boolean {
        val profile = RoleCalculator.calculateProfile(player)
        return profile.tankRatio >= 0.35 || profile.primaryRole == RoleCalculator.Role.TANK
    }
}
