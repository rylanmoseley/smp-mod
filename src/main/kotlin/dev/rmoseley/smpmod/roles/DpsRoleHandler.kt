package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.recovery.ItemProvenance
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.ElderGuardian
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.entity.player.Player

/**
 * Handles DPS battlefield mechanics:
 * 1. Continuous burst damage scaling derived from Combat XP ratio
 * 2. Armor penetration against armored hostiles and elites
 * 3. Execution Punish Windows against dark-zone elites and boss vulnerability phases
 */
object DpsRoleHandler {

    /**
     * Modifies damage dealt by a DPS attacker.
     */
    fun processAttack(attacker: Player, target: LivingEntity, initialDamage: Float): Float {
        var damage = initialDamage

        // 1. DPS Burst Scaling
        val dpsAttr = attacker.getAttribute(ModEntityAttributes.ROLE_DPS_MULTIPLIER)
        val dpsMultiplier = dpsAttr?.value ?: 1.0
        if (dpsMultiplier > 1.0) {
            damage *= dpsMultiplier.toFloat()
        }

        // 2. Armor Penetration
        val penAttr = attacker.getAttribute(ModEntityAttributes.ROLE_ARMOR_PENETRATION)
        val penRatio = (penAttr?.value ?: 0.0).coerceIn(0.0, 1.0)
        if (penRatio > 0.0 && target.armorValue > 0) {
            val ignoredArmor = (target.armorValue * penRatio).toFloat()
            // Each 5 armor points absorbs ~20% damage; recovering ignored armor restores damage
            val armorRestoredDamage = damage * (ignoredArmor / 25.0f).coerceAtMost(0.40f)
            damage += armorRestoredDamage
        }

        // 3. Execution Punish Windows
        val isEliteOrBoss = isEliteOrBoss(target)
        val executeThreshold = SmpConfig.COMMON.dpsExecuteThresholdPercent.get()
        val isLowHealth = (target.health / target.maxHealth) <= executeThreshold
        val isVulnerable = isTargetInVulnerabilityWindow(target)

        if (isEliteOrBoss && (isLowHealth || isVulnerable)) {
            val punishMultiplier = SmpConfig.COMMON.dpsPunishBonusMultiplier.get().toFloat()
            damage *= punishMultiplier

            // Punchy visual & sound cues
            val serverLevel = attacker.level() as? ServerLevel
            serverLevel?.sendParticles(
                ParticleTypes.CRIT,
                target.x, target.y + target.bbHeight / 2.0, target.z,
                20, 0.4, 0.4, 0.4, 0.15
            )
            serverLevel?.sendParticles(
                ParticleTypes.ENCHANTED_HIT,
                target.x, target.y + target.bbHeight / 2.0, target.z,
                15, 0.3, 0.3, 0.3, 0.1
            )
            serverLevel?.playSound(
                null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS,
                1.0f, 0.7f
            )

            // If fatal, log execution to held item lore
            if (damage >= target.health && attacker is ServerPlayer) {
                val heldItem = attacker.mainHandItem
                if (!heldItem.isEmpty) {
                    val targetName = target.name.string
                    ItemProvenance.addCombatHistory(
                        heldItem,
                        "Executed $targetName in Punish Window (DPS Burst)"
                    )
                }
            }
        }

        return damage
    }

    fun isEliteOrBoss(target: LivingEntity): Boolean {
        if (target is EnderDragon || target is WitherBoss || target is Warden || target is ElderGuardian) {
            return true
        }
        val destroysLight = target.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        return destroysLight != null && destroysLight.value > 0.5
    }

    fun isTargetInVulnerabilityWindow(target: LivingEntity): Boolean {
        if (target is EnderDragon) {
            return CooperativeDragonEncounter.isDragonInPunishWindow(target)
        }
        return false
    }
}
