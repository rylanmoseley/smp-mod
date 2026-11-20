package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import kotlin.math.pow

/**
 * Derives player combat roles (DPS, Tank, Support, Scout) purely from continuous
 * Playstyle XP ratios:
 *
 * Role Affinity_i = f( XP_i / (XP_Combat + XP_Builder + XP_Gatherer + XP_Explorer) )
 *
 * Zero elective class selection.
 * Potency flows dynamically from survival activities: specialists develop crisp battlefield
 * identities while balanced players emerge as versatile hybrids.
 */
object RoleCalculator {

    enum class Role(val displayName: String, val colorCode: String) {
        DPS("DPS", "§c"),
        TANK("Tank", "§9"),
        SUPPORT("Support", "§a"),
        SCOUT("Scout", "§b"),
        HYBRID("Hybrid", "§d")
    }

    data class RoleProfile(
        val primaryRole: Role,
        val secondaryRole: Role?,
        val dpsRatio: Double,
        val tankRatio: Double,
        val supportRatio: Double,
        val scoutRatio: Double,
        val dpsAffinity: Double,
        val tankAffinity: Double,
        val supportAffinity: Double,
        val scoutAffinity: Double,
        val totalXp: Double
    ) {
        fun isSpecialist(role: Role): Boolean {
            val threshold = if (SmpConfig.SPEC.isLoaded) SmpConfig.COMMON.specialistThresholdRatio.get() else 0.40
            return when (role) {
                Role.DPS -> dpsRatio >= threshold
                Role.TANK -> tankRatio >= threshold
                Role.SUPPORT -> supportRatio >= threshold
                Role.SCOUT -> scoutRatio >= threshold
                Role.HYBRID -> false
            }
        }

        fun getAffinity(role: Role): Double {
            return when (role) {
                Role.DPS -> dpsAffinity
                Role.TANK -> tankAffinity
                Role.SUPPORT -> supportAffinity
                Role.SCOUT -> scoutAffinity
                Role.HYBRID -> 0.25
            }
        }

        fun getRatio(role: Role): Double {
            return when (role) {
                Role.DPS -> dpsRatio
                Role.TANK -> tankRatio
                Role.SUPPORT -> supportRatio
                Role.SCOUT -> scoutRatio
                Role.HYBRID -> 0.25
            }
        }

        fun getFormattedTitle(): String {
            val primary = primaryRole
            val isSpec = isSpecialist(primary)
            return when (primary) {
                Role.DPS -> if (isSpec) "Execution Specialist" else "Skirmisher"
                Role.TANK -> if (isSpec) "Bulwark Sentinel" else "Defender"
                Role.SUPPORT -> if (isSpec) "Sanctuary Priest" else "Alchemist"
                Role.SCOUT -> if (isSpec) "Pathfinder Vanguard" else "Ranger"
                Role.HYBRID -> {
                    if (secondaryRole != null) "Adept Hybrid (${primaryRole.displayName}/${secondaryRole.displayName})"
                    else "Versatile Adventurer"
                }
            }
        }
    }

    /**
     * Mathematical derivation function f(r):
     * Non-linear scaling function converting continuous Playstyle XP ratios into effective Role Affinities.
     * Rewarding focused specialization with accelerating returns while preserving hybrid viability.
     */
    fun deriveAffinity(ratio: Double): Double {
        val clamped = ratio.coerceIn(0.0, 1.0)
        return if (clamped <= 0.25) {
            // Below baseline 25% share, scales linearly
            clamped
        } else {
            // Progressive exponentiation above baseline share
            val excess = clamped - 0.25
            val accelerated = 0.25 + (excess * 1.25)
            accelerated.coerceIn(0.0, 1.0)
        }
    }

    /**
     * Calculates the complete dynamic RoleProfile for a player based on their accumulated Playstyle XP.
     */
    fun calculateProfile(player: Player): RoleProfile {
        val xpData = player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())
        val totalXp = xpData.combatXp + xpData.builderXp + xpData.gathererXp + xpData.explorerXp

        if (totalXp <= 0.0) {
            return RoleProfile(
                primaryRole = Role.HYBRID,
                secondaryRole = null,
                dpsRatio = 0.25,
                tankRatio = 0.25,
                supportRatio = 0.25,
                scoutRatio = 0.25,
                dpsAffinity = 0.25,
                tankAffinity = 0.25,
                supportAffinity = 0.25,
                scoutAffinity = 0.25,
                totalXp = 0.0
            )
        }

        val dpsRatio = (xpData.combatXp / totalXp).coerceIn(0.0, 1.0)
        val tankRatio = (xpData.builderXp / totalXp).coerceIn(0.0, 1.0)
        val supportRatio = (xpData.gathererXp / totalXp).coerceIn(0.0, 1.0)
        val scoutRatio = (xpData.explorerXp / totalXp).coerceIn(0.0, 1.0)

        val dpsAffinity = deriveAffinity(dpsRatio)
        val tankAffinity = deriveAffinity(tankRatio)
        val supportAffinity = deriveAffinity(supportRatio)
        val scoutAffinity = deriveAffinity(scoutRatio)

        val roleRatios = listOf(
            Role.DPS to dpsRatio,
            Role.TANK to tankRatio,
            Role.SUPPORT to supportRatio,
            Role.SCOUT to scoutRatio
        ).sortedByDescending { it.second }

        val threshold = if (SmpConfig.SPEC.isLoaded) SmpConfig.COMMON.specialistThresholdRatio.get() else 0.40
        val topRole = roleRatios[0]
        val secondRole = roleRatios[1]

        val primary = if (topRole.second >= threshold) topRole.first else Role.HYBRID
        val secondary = if (primary == Role.HYBRID && topRole.second >= 0.28) {
            topRole.first
        } else if (secondRole.second >= 0.25) {
            secondRole.first
        } else null

        return RoleProfile(
            primaryRole = primary,
            secondaryRole = secondary,
            dpsRatio = dpsRatio,
            tankRatio = tankRatio,
            supportRatio = supportRatio,
            scoutRatio = scoutRatio,
            dpsAffinity = dpsAffinity,
            tankAffinity = tankAffinity,
            supportAffinity = supportAffinity,
            scoutAffinity = scoutAffinity,
            totalXp = totalXp
        )
    }

    /**
     * Dynamically updates player entity attributes based on current role affinity curves.
     */
    fun applyRoleAttributes(player: Player) {
        val profile = calculateProfile(player)

        // 1. DPS scaling: Base attack multiplier from 1.0x up to 2.5x
        player.getAttribute(ModEntityAttributes.ROLE_DPS_MULTIPLIER)?.baseValue =
            1.0 + (profile.dpsAffinity * 1.5)

        // 2. Tank frontline mitigation: Damage reduction up to 50%
        player.getAttribute(ModEntityAttributes.ROLE_TANK_MITIGATION)?.baseValue =
            profile.tankAffinity * 0.50

        // 3. Support efficiency: Consumable duration and radius scaling (1.0x to 2.0x)
        player.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)?.baseValue =
            1.0 + profile.supportAffinity

        // 4. Scout agility: Movement agility bonus up to +35%
        player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)?.baseValue =
            1.0 + (profile.scoutAffinity * 0.35)

        // 5. Armor penetration: DPS bypasses up to 45% of enemy armor
        player.getAttribute(ModEntityAttributes.ROLE_ARMOR_PENETRATION)?.baseValue =
            profile.dpsAffinity * 0.45

        // 6. Threat generation: Tank generates up to 4.0x threat/aggro
        player.getAttribute(ModEntityAttributes.ROLE_THREAT_GENERATION)?.baseValue =
            1.0 + (profile.tankAffinity * 3.0)

        // 7. Scout smooth step height: Scouts with high affinity step over 1.0 block height smoothly
        val stepAttr = player.getAttribute(Attributes.STEP_HEIGHT)
        if (stepAttr != null) {
            stepAttr.baseValue = if (profile.scoutAffinity >= 0.35) 1.0 else 0.6
        }
    }
}
