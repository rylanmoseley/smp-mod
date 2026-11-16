package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.claims.ContiguousCluster
import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos

/**
 * Manages safe recovery interactions and municipal subsidies when accessing
 * buyback terminals within owned or shared contiguous land claims.
 * Aligns with Section 3 and Section 7 of design-v2.md.
 */
object SafeRecoveryManager {

    data class RecoverySafetyStatus(
        val isSafeZone: Boolean,
        val contiguousCluster: ContiguousCluster?,
        val subsidyDiscountPercent: Double,
        val statusMessage: String
    )

    /**
     * Evaluates the player's current location to determine claim safety and municipal subsidies.
     */
    fun evaluateSafety(player: ServerPlayer): RecoverySafetyStatus {
        val level = player.level()
        val chunkPos = ChunkPos(player.blockPosition())

        val isClaimed = ClaimManager.isChunkClaimedBy(level, chunkPos, player)
        if (!isClaimed) {
            return RecoverySafetyStatus(
                isSafeZone = false,
                contiguousCluster = null,
                subsidyDiscountPercent = 0.0,
                statusMessage = "§cWilderness: Outside contiguous claims. No municipal recovery subsidy applies."
            )
        }

        val cluster = ClaimManager.getContiguousCluster(level, chunkPos)
        if (cluster == null) {
            return RecoverySafetyStatus(
                isSafeZone = false,
                contiguousCluster = null,
                subsidyDiscountPercent = 0.0,
                statusMessage = "§eIsolated Claim: Not part of an active contiguous cluster. Full standard recovery fees apply."
            )
        }

        // Tier-based municipal recovery subsidy
        val baseConfigSubsidy = SmpConfig.COMMON.claimRecoverySubsidyPercent.get()
        val tierBonus = when (cluster.clusterTierName) {
            "Metropolis" -> 0.05
            "Town" -> 0.03
            "Settlement" -> 0.01
            "Hamlet" -> 0.00
            else -> -0.05
        }
        val upgradeBonus = if (dev.rmoseley.smpmod.claims.TownTreasuryManager.hasUpgrade(cluster, dev.rmoseley.smpmod.claims.CivicUpgrade.TOWN_RECOVERY_SUBSIDY)) 0.15 else 0.0
        val effectiveSubsidy = (baseConfigSubsidy + tierBonus + upgradeBonus).coerceIn(0.0, 0.50)
        val subsidyPercentInt = (effectiveSubsidy * 100).toInt()

        return RecoverySafetyStatus(
            isSafeZone = true,
            contiguousCluster = cluster,
            subsidyDiscountPercent = effectiveSubsidy,
            statusMessage = "§aProtected Claim (${cluster.clusterTierName}): Safe recovery active! $subsidyPercentInt% Municipal Subsidy applied."
        )
    }

    /**
     * Computes the discounted price after applying the contiguous claim municipal subsidy.
     */
    fun applyMunicipalSubsidy(rawPrice: Long, player: ServerPlayer): Pair<Long, Double> {
        val safety = evaluateSafety(player)
        if (!safety.isSafeZone || safety.subsidyDiscountPercent <= 0.0) {
            return Pair(rawPrice, 0.0)
        }

        val discountAmount = (rawPrice * safety.subsidyDiscountPercent).toLong()
        val finalPrice = (rawPrice - discountAmount).coerceAtLeast(10L)
        return Pair(finalPrice, safety.subsidyDiscountPercent)
    }
}
