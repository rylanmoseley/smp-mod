package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.economy.TransactionType
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages communal Town Treasuries and Municipal Civic Upgrades.
 * Funded via P2P sales taxes, death buyback municipal surcharges, and direct resident donations.
 * Unlocks high-value civic upgrades for contiguous settlements.
 */
object TownTreasuryManager {

    data class TownTreasury(
        val ownerUuid: UUID,
        var balance: Long = 0L,
        val activeUpgrades: MutableSet<CivicUpgrade> = ConcurrentHashMap.newKeySet(),
        var totalTaxesCollected: Long = 0L
    )

    // Keyed by Town / Cluster Owner UUID
    private val treasuries = ConcurrentHashMap<UUID, TownTreasury>()

    fun getTreasury(ownerUuid: UUID): TownTreasury {
        return treasuries.computeIfAbsent(ownerUuid) { TownTreasury(ownerUuid) }
    }

    /**
     * Residents deposit personal currency into their settlement's communal treasury.
     */
    fun deposit(player: ServerPlayer, amount: Long): Boolean {
        if (amount <= 0) return false

        val result = EconomyManager.debit(player, amount, TransactionType.DEBIT, "Town Treasury Deposit")
        if (result !is dev.rmoseley.smpmod.economy.TransactionResult.Success) return false

        val treasury = getTreasury(player.uuid)
        synchronized(treasury) {
            treasury.balance += amount
        }
        dev.rmoseley.smpmod.advancements.EconomyAdvancements.awardTownDonation(player, amount)
        return true
    }

    /**
     * Town leader withdraws funds from the communal treasury into personal balance.
     */
    fun withdraw(player: ServerPlayer, amount: Long): Boolean {
        if (amount <= 0) return false

        val treasury = getTreasury(player.uuid)
        synchronized(treasury) {
            if (treasury.balance < amount) return false
            treasury.balance -= amount
        }

        EconomyManager.credit(player, amount, TransactionType.CREDIT, "Town Treasury Withdrawal")
        return true
    }

    /**
     * Automatically routes a cut of P2P sales taxes or buyback municipal fees to the town treasury.
     */
    fun depositTax(ownerUuid: UUID, amount: Long) {
        if (amount <= 0) return
        val treasury = getTreasury(ownerUuid)
        synchronized(treasury) {
            treasury.balance += amount
            treasury.totalTaxesCollected += amount
        }
    }

    /**
     * Attempts to unlock a civic upgrade for the contiguous settlement.
     */
    fun purchaseUpgrade(player: ServerPlayer, cluster: ContiguousCluster, upgrade: CivicUpgrade): Pair<Boolean, String> {
        if (cluster.ownerUuid != player.uuid) {
            return Pair(false, "Only the town founder/owner can authorize civic upgrades.")
        }

        if (cluster.clusterTier.ordinal < upgrade.requiredTier.ordinal) {
            return Pair(
                false,
                "Settlement tier ${cluster.clusterTierName} is too low. Requires ${upgrade.requiredTier.displayName} (${upgrade.requiredTier.minChunks}+ contiguous chunks)."
            )
        }

        val treasury = getTreasury(cluster.ownerUuid)
        synchronized(treasury) {
            if (treasury.activeUpgrades.contains(upgrade)) {
                return Pair(false, "Civic upgrade ${upgrade.displayName} is already active.")
            }

            if (treasury.balance < upgrade.cost) {
                return Pair(
                    false,
                    "Insufficient town funds. Requires ${upgrade.cost} coins (Treasury balance: ${treasury.balance})."
                )
            }

            treasury.balance -= upgrade.cost
            treasury.activeUpgrades.add(upgrade)
            dev.rmoseley.smpmod.advancements.SettlementAdvancements.checkCivicUpgrades(player, treasury.activeUpgrades.size)
            dev.rmoseley.smpmod.advancements.SettlementAdvancements.checkWardedBastion(player, treasury.activeUpgrades)
        }

        return Pair(true, "Successfully unlocked civic upgrade: ${upgrade.displayName}!")
    }

    fun hasUpgrade(cluster: ContiguousCluster?, upgrade: CivicUpgrade): Boolean {
        if (cluster == null) return false
        val treasury = treasuries[cluster.ownerUuid] ?: return false
        return treasury.activeUpgrades.contains(upgrade)
    }

    fun hasUpgradeAt(level: Level, pos: BlockPos, upgrade: CivicUpgrade): Boolean {
        val chunkPos = ChunkPos(pos)
        val cluster = ClaimManager.getContiguousCluster(level, chunkPos) ?: return false
        return hasUpgrade(cluster, upgrade)
    }

    fun getActiveUpgrades(ownerUuid: UUID): Set<CivicUpgrade> {
        return treasuries[ownerUuid]?.activeUpgrades ?: emptySet()
    }
}
