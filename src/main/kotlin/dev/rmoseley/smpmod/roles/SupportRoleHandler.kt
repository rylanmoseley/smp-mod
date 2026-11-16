package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.recovery.RecoveryCatalystManager
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import dev.rmoseley.smpmod.world.WardedLightManager
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

/**
 * Handles Support battlefield mechanics:
 * 1. Team healing and continuous sustain pulses
 * 2. Consumable efficiency and shared aura buffs
 * 3. Support craftable Recovery & Repair Catalysts reducing buyback fees
 * 4. Warded Light Flares resistant to destroys_light_on_aggro snuffing
 */
object SupportRoleHandler {

    /**
     * Periodic Support Sustain Pulse: heals and restores saturation to nearby allies.
     */
    fun performSustainPulse(supporter: ServerPlayer) {
        val efficiencyAttr = supporter.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)
        val efficiency = efficiencyAttr?.value ?: 1.0

        val baseRadius = SmpConfig.COMMON.supportAuraRadius.get()
        val radius = baseRadius * (efficiency / 1.5)
        val healAmount = (SmpConfig.COMMON.supportHealAmount.get() * efficiency).toFloat()

        val searchBox = AABB(supporter.blockPosition()).inflate(radius)
        val allies = supporter.level().getEntitiesOfClass(ServerPlayer::class.java, searchBox) { p ->
            p.isAlive && (p == supporter || p.team == supporter.team || !p.isSpectator)
        }

        var healedAny = false
        for (ally in allies) {
            if (ally.health < ally.maxHealth) {
                ally.heal(healAmount)
                healedAny = true

                // Visual feedback
                val serverLevel = supporter.serverLevel()
                serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    ally.x, ally.y + 1.2, ally.z,
                    3, 0.3, 0.3, 0.3, 0.05
                )
            }
        }

        if (healedAny) {
            val serverLevel = supporter.serverLevel()
            serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                supporter.x, supporter.y + 0.5, supporter.z,
                8, 0.4, 0.2, 0.4, 0.02
            )
        }
    }

    /**
     * Consumable efficiency: radiates positive potion effects to nearby allies.
     */
    fun onPotionConsumed(supporter: ServerPlayer, effect: MobEffectInstance) {
        // Only share beneficial effects
        if (effect.effect.value().category != MobEffectCategory.BENEFICIAL) return

        val efficiencyAttr = supporter.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)
        val efficiency = efficiencyAttr?.value ?: 1.0

        val radius = SmpConfig.COMMON.supportAuraRadius.get()
        val sharedDuration = (effect.duration * 0.75 * efficiency).toInt()

        val searchBox = AABB(supporter.blockPosition()).inflate(radius)
        val allies = supporter.level().getEntitiesOfClass(ServerPlayer::class.java, searchBox) { p ->
            p != supporter && p.isAlive
        }

        for (ally in allies) {
            ally.addEffect(MobEffectInstance(effect.effect, sharedDuration, effect.amplifier, true, true))
            supporter.serverLevel().sendParticles(
                ParticleTypes.GLOW,
                ally.x, ally.y + 1.0, ally.z,
                10, 0.3, 0.3, 0.3, 0.05
            )
        }

        if (allies.isNotEmpty()) {
            val effectName = effect.effect.value().descriptionId.substringAfterLast('.')
            supporter.displayClientMessage(
                Component.literal("§a[Support Synergy] Radiated $effectName to ${allies.size} allies!"),
                true
            )
        }
    }

    /**
     * Crafts a Support Recovery Catalyst item to assist in UUID buyback fee reduction.
     */
    fun craftRecoveryCatalyst(crafter: ServerPlayer): ItemStack {
        return RecoveryCatalystManager.createCatalyst(crafter, RecoveryCatalystManager.CatalystTier.STANDARD)
    }

    /**
     * Deploys a Warded Light Flare at target position resistant to destroys_light_on_aggro.
     */
    fun deployWardedFlare(supporter: ServerPlayer, pos: BlockPos): Boolean {
        return WardedLightManager.empowerLight(supporter, pos)
    }

    fun isSupportSpecialist(player: Player): Boolean {
        val profile = RoleCalculator.calculateProfile(player)
        return profile.supportRatio >= 0.35 || profile.primaryRole == RoleCalculator.Role.SUPPORT
    }
}
