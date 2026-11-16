package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.roles.BuilderXpTracker
import dev.rmoseley.smpmod.roles.CombatXpTracker
import dev.rmoseley.smpmod.roles.ExplorerXpTracker
import dev.rmoseley.smpmod.roles.GathererXpTracker
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class BountyType {
    SCOUT_EXPLORATION,
    GATHERER_HARVEST,
    COMBAT_ELITE_SLAY
}

data class BountyContract(
    val contractId: String,
    val type: BountyType,
    val title: String,
    val description: String,
    val targetQuantity: Int,
    val currencyReward: Long,
    val roleXpReward: Double,
    val targetIdentifier: String = ""
)

/**
 * Community Bounty & Contract Board:
 * Allows players to accept community contracts aligned with their playstyles.
 * Complements emergent roles by providing non-grinding, objective-based currency faucets
 * and role progression.
 */
object BountyContractManager {

    data class PlayerBountyProgress(
        val contractId: String,
        var currentProgress: Int = 0
    )

    // Standard community contracts pool
    private val availableContracts = listOf(
        BountyContract(
            "scout_recon",
            BountyType.SCOUT_EXPLORATION,
            "Deep Cavern Recon",
            "Survey 15 deep underground chunks (< Y: 0)",
            15,
            150L,
            50.0
        ),
        BountyContract(
            "scout_biomes",
            BountyType.SCOUT_EXPLORATION,
            "Frontier Expedition",
            "Explore 5 distinct biomes across the realm",
            5,
            200L,
            75.0
        ),
        BountyContract(
            "gather_timber",
            BountyType.GATHERER_HARVEST,
            "Municipal Timber Drive",
            "Naturally harvest 64 raw log blocks",
            64,
            120L,
            60.0
        ),
        BountyContract(
            "gather_deep_minerals",
            BountyType.GATHERER_HARVEST,
            "Deep Mineral Reclamation",
            "Naturally harvest 32 deepslate ore blocks",
            32,
            180L,
            80.0
        ),
        BountyContract(
            "combat_cull",
            BountyType.COMBAT_ELITE_SLAY,
            "Dark-Zone Purge",
            "Slay 12 hostile monsters in deep caverns",
            12,
            200L,
            90.0
        ),
        BountyContract(
            "combat_snuffers",
            BountyType.COMBAT_ELITE_SLAY,
            "Snuffer Elite Suppression",
            "Defeat 3 light-destroying elite hostiles",
            3,
            300L,
            150.0,
            "destroys_light"
        )
    )

    private val playerContracts = ConcurrentHashMap<UUID, MutableList<PlayerBountyProgress>>()

    fun getAvailableContracts(): List<BountyContract> = availableContracts

    fun getContractById(id: String): BountyContract? = availableContracts.firstOrNull { it.contractId.equals(id, ignoreCase = true) }

    fun getPlayerContracts(playerUuid: UUID): MutableList<PlayerBountyProgress> {
        return playerContracts.computeIfAbsent(playerUuid) { mutableListOf() }
    }

    /**
     * Accepts a contract (maximum 2 simultaneous contracts per player).
     */
    fun acceptContract(player: ServerPlayer, contractId: String): Pair<Boolean, String> {
        val contract = getContractById(contractId) ?: return Pair(false, "Contract '$contractId' not found.")
        val active = getPlayerContracts(player.uuid)

        synchronized(active) {
            if (active.size >= 2) {
                return Pair(false, "You cannot hold more than 2 active bounty contracts simultaneously.")
            }
            if (active.any { it.contractId.equals(contractId, ignoreCase = true) }) {
                return Pair(false, "You already have this contract active.")
            }
            active.add(PlayerBountyProgress(contract.contractId, 0))
            return Pair(true, "Accepted contract: ${contract.title}!")
        }
    }

    fun abandonContract(player: ServerPlayer, contractId: String): Boolean {
        val active = getPlayerContracts(player.uuid)
        synchronized(active) {
            return active.removeIf { it.contractId.equals(contractId, ignoreCase = true) }
        }
    }

    /**
     * Records progress towards active contracts.
     */
    fun recordProgress(player: ServerPlayer, type: BountyType, amount: Int = 1, identifier: String = "") {
        val active = getPlayerContracts(player.uuid)
        if (active.isEmpty()) return

        synchronized(active) {
            for (p in active) {
                val contract = getContractById(p.contractId) ?: continue
                if (contract.type != type) continue

                // Check identifier match if contract specifies one
                if (contract.targetIdentifier.isNotEmpty() && !identifier.contains(contract.targetIdentifier, ignoreCase = true)) {
                    continue
                }

                p.currentProgress = (p.currentProgress + amount).coerceAtMost(contract.targetQuantity)
                if (p.currentProgress >= contract.targetQuantity) {
                    player.sendSystemMessage(
                        Component.literal("§6[Bounty Complete] §e${contract.title} §ais ready to claim! Type §6/bounty claim ${contract.contractId}")
                    )
                }
            }
        }
    }

    /**
     * Claims the rewards for a completed contract.
     */
    fun claimReward(player: ServerPlayer, contractId: String): Pair<Boolean, String> {
        val contract = getContractById(contractId) ?: return Pair(false, "Contract '$contractId' not found.")
        val active = getPlayerContracts(player.uuid)

        val canClaim = synchronized(active) {
            val entry = active.firstOrNull { it.contractId.equals(contractId, ignoreCase = true) }
                ?: return Pair(false, "You do not have this contract active.")

            if (entry.currentProgress < contract.targetQuantity) {
                return Pair(false, "Contract not yet complete: ${entry.currentProgress}/${contract.targetQuantity}.")
            }

            // Remove contract
            active.remove(entry)
            true
        }

        if (!canClaim) return Pair(false, "Could not process claim.")

        // Award currency
        EconomyManager.credit(player, contract.currencyReward, TransactionType.CREDIT, "Bounty Reward: ${contract.title}")

        // Award playstyle role XP
        when (contract.type) {
            BountyType.SCOUT_EXPLORATION -> PlaystyleXpTracker.incrementExplorerXp(player, contract.roleXpReward)
            BountyType.GATHERER_HARVEST -> PlaystyleXpTracker.incrementGathererXp(player, contract.roleXpReward)
            BountyType.COMBAT_ELITE_SLAY -> PlaystyleXpTracker.incrementCombatXp(player, contract.roleXpReward)
        }

        dev.rmoseley.smpmod.advancements.EconomyAdvancements.awardContractClaim(player)

        return Pair(true, "Claimed §e${contract.currencyReward} coins §aand §b+${contract.roleXpReward.toInt()} Role XP §afor completing §6${contract.title}§a!")
    }
}
