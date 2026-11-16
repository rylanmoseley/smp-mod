package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.world.entity.player.Player

/**
 * Derives player combat roles (DPS, Tank, Support, Scout) purely from their
 * dynamic Playstyle XP ratios. Zero elective class selection.
 */
object RoleCalculator {

    enum class Role {
        DPS,
        TANK,
        SUPPORT,
        SCOUT,
        HYBRID
    }

    data class RoleProfile(
        val primaryRole: Role,
        val dpsRatio: Double,
        val tankRatio: Double,
        val supportRatio: Double,
        val scoutRatio: Double
    )

    fun calculateProfile(player: Player): RoleProfile {
        val xpData = player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())
        val totalXp = xpData.combatXp + xpData.builderXp + xpData.gathererXp + xpData.explorerXp

        if (totalXp <= 0.0) {
            return RoleProfile(Role.HYBRID, 0.25, 0.25, 0.25, 0.25)
        }

        val dpsRatio = (xpData.combatXp / totalXp).coerceIn(0.0, 1.0)
        val tankRatio = (xpData.builderXp / totalXp).coerceIn(0.0, 1.0)
        val supportRatio = (xpData.gathererXp / totalXp).coerceIn(0.0, 1.0)
        val scoutRatio = (xpData.explorerXp / totalXp).coerceIn(0.0, 1.0)

        val ratios = mapOf(
            Role.DPS to dpsRatio,
            Role.TANK to tankRatio,
            Role.SUPPORT to supportRatio,
            Role.SCOUT to scoutRatio
        )

        val maxEntry = ratios.maxByOrNull { it.value }
        val primary = if (maxEntry != null && maxEntry.value > 0.45) maxEntry.key else Role.HYBRID

        return RoleProfile(primary, dpsRatio, tankRatio, supportRatio, scoutRatio)
    }

    /**
     * Updates player entity attributes dynamically based on current role affinity ratios.
     */
    fun applyRoleAttributes(player: Player) {
        val profile = calculateProfile(player)

        // DPS scaling: 1.0x to 2.5x base damage multiplier
        player.getAttribute(ModEntityAttributes.ROLE_DPS_MULTIPLIER)?.baseValue = 1.0 + (profile.dpsRatio * 1.5)

        // Tank mitigation: up to 50% damage reduction
        player.getAttribute(ModEntityAttributes.ROLE_TANK_MITIGATION)?.baseValue = profile.tankRatio * 0.50

        // Support efficiency: 1.0x to 2.0x consumable duration / radius
        player.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)?.baseValue = 1.0 + profile.supportRatio

        // Scout agility: up to +35% movement bonus
        player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)?.baseValue = 1.0 + (profile.scoutRatio * 0.35)
    }
}
