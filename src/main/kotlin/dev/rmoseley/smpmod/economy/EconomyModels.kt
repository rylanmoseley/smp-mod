package dev.rmoseley.smpmod.economy

import java.util.UUID

/**
 * Standard classification of digital off-inventory economy transactions.
 */
enum class TransactionType {
    CREDIT,
    DEBIT,
    P2P_TRANSFER,
    SHOP_PURCHASE,
    RECOVERY_BUYBACK,
    MILESTONE_STARTER,
    MILESTONE_REPEATABLE_HARVEST,
    MILESTONE_DUNGEON,
    ACTIVE_PLAY_STIPEND,
    BULK_LIQUIDATION,
    BULK_PURCHASE,
    SERVER_TAX
}

/**
 * Complete immutable audit record for an economic operation.
 */
data class TransactionRecord(
    val transactionId: UUID = UUID.randomUUID(),
    val timestampEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    val type: TransactionType,
    val playerUuid: UUID,
    val playerName: String,
    val targetPlayerUuid: UUID? = null,
    val targetPlayerName: String? = null,
    val amount: Long,
    val taxDeducted: Long = 0L,
    val balanceAfter: Long,
    val note: String = ""
)

/**
 * Outcome wrapper for transactional balance mutations.
 */
sealed interface TransactionResult {
    data class Success(
        val record: TransactionRecord
    ) : TransactionResult {
        val newBalance: Long get() = record.balanceAfter
    }

    data class Failure(
        val reason: String,
        val currentBalance: Long = 0L
    ) : TransactionResult
}

/**
 * Defined one-time starter progression milestones for onboarding liquidity.
 */
enum class StarterMilestone(
    val id: String,
    val displayName: String,
    val defaultPayout: Long
) {
    FIRST_JOIN("first_join", "Pioneer Arrival", 100L),
    FIRST_NATURAL_HARVEST("first_natural_harvest", "Prospector's First Stride", 50L),
    FIRST_CLAIM("first_claim", "Settlement Founder", 100L),
    FIRST_DUNGEON("first_dungeon", "Dungeon Crawler", 150L),
    PROVENANCE_CRAFT("provenance_craft", "Master Artificer", 75L),
    FIRST_P2P_TRADE("first_p2p_trade", "Mercantile Exchange", 50L);

    companion object {
        fun byId(id: String): StarterMilestone? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

/**
 * Natural harvest unlock tiers for building material shop items.
 */
enum class ShopUnlockTier(
    val requiredMultiplier: Double,
    val discountPercent: Double,
    val tierLabel: String
) {
    LOCKED(0.0, 0.0, "Locked"),
    TIER_1_UNLOCKED(1.0, 0.0, "Unlocked (Standard)"),
    TIER_2_JOURNEYMAN(5.0, 0.10, "Journeyman (-10%)"),
    TIER_3_MASTER(20.0, 0.25, "Master (-25%)")
}

/**
 * Purchase outcome for building material transactions.
 */
sealed interface ShopPurchaseResult {
    data class Success(
        val blockId: String,
        val count: Int,
        val unitPrice: Long,
        val totalCost: Long,
        val discountPercentApplied: Double,
        val remainingBalance: Long
    ) : ShopPurchaseResult

    data class Failure(
        val reason: String
    ) : ShopPurchaseResult
}

/**
 * Bulk liquidation outcome when selling standard stackables into the server store.
 */
sealed interface LiquidationResult {
    data class Success(
        val itemId: String,
        val count: Int,
        val unitPayout: Long,
        val totalPayout: Long,
        val supportBonusAmount: Long,
        val updatedBalance: Long
    ) : LiquidationResult

    data class Failure(
        val reason: String
    ) : LiquidationResult
}

/**
 * Purchase outcome when buying standard stackables from the bulk store.
 */
sealed interface BulkPurchaseResult {
    data class Success(
        val itemId: String,
        val count: Int,
        val unitPrice: Long,
        val totalCost: Long,
        val remainingBalance: Long
    ) : BulkPurchaseResult

    data class Failure(
        val reason: String
    ) : BulkPurchaseResult
}
