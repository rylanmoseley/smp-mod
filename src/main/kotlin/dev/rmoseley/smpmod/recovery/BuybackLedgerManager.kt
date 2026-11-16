package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Manages the Ownership-Bound UUID Buyback Ledger, escalating recovery fees,
 * ~30-day wall-clock decay back to 1.0x baseline, 24-hour exclusive reclaim windows,
 * and strict theft guardrails as specified in design-v2.md Section 2.2.
 */
object BuybackLedgerManager {

    data class BuybackEntry(
        val itemUuid: UUID,
        val itemStack: ItemStack,
        val registeredOwnerUuid: UUID,
        val registeredOwnerName: String,
        var deathCount: Int,
        val deathTimestampEpochSeconds: Long,
        val baseCost: Long,
        var catalystDiscount: Double = 0.0
    )

    sealed class ReclaimResult {
        data class Success(
            val item: ItemStack,
            val pricePaid: Long,
            val wasPublicPurchase: Boolean,
            val claimSubsidySaved: Long
        ) : ReclaimResult()

        object ItemNotFound : ReclaimResult()

        data class LockedExclusiveToOwner(
            val ownerName: String,
            val secondsRemaining: Long
        ) : ReclaimResult()

        data class InsufficientFunds(
            val priceRequired: Long,
            val currentBalance: Long
        ) : ReclaimResult()
    }

    private val ledger = ConcurrentHashMap<UUID, BuybackEntry>()

    /**
     * Registers a lost unique item into the buyback ledger upon player death.
     * Enforces theft protection:
     * - If a thief/looter died holding an item stolen without a formal P2P trade,
     *   it routes exclusively back to the original registered owner.
     * - The item's cumulative death_count only increments when the registered owner is the one who died.
     */
    fun recordDeathDrop(deceased: ServerPlayer, stack: ItemStack): BuybackEntry? {
        if (!RecoveryItemClassifier.isBuybackCandidate(stack)) {
            return null
        }

        val existingUuid = stack.get(ModDataComponents.ITEM_UUID.get()) ?: UUID.randomUUID()
        val registeredOwnerUuidComponent = stack.get(ModDataComponents.REGISTERED_OWNER_UUID.get())
        val registeredOwnerNameComponent = stack.get(ModDataComponents.REGISTERED_OWNER.get())

        val isUnowned = registeredOwnerUuidComponent == null && registeredOwnerNameComponent == null
        val currentDeathCount = stack.get(ModDataComponents.DEATH_COUNT.get()) ?: 0
        val nowEpoch = System.currentTimeMillis() / 1000L

        val targetOwnerUuid: UUID
        val targetOwnerName: String
        val updatedDeathCount: Int

        if (isUnowned) {
            // First time this item is registered: deceased is the natural owner
            targetOwnerUuid = deceased.uuid
            targetOwnerName = deceased.gameProfile.name
            updatedDeathCount = 1

            // Initialize origin creator metadata
            ItemProvenance.initializeItemOrigin(stack, deceased, isCrafted = false)
        } else {
            // Item was previously owned
            val isOriginalOwner = (registeredOwnerUuidComponent != null && deceased.uuid == registeredOwnerUuidComponent) ||
                    deceased.gameProfile.name.equals(registeredOwnerNameComponent, ignoreCase = true)

            if (isOriginalOwner) {
                // Legitimate owner died: increment death count
                targetOwnerUuid = deceased.uuid
                targetOwnerName = deceased.gameProfile.name
                updatedDeathCount = currentDeathCount + 1
            } else {
                // THEFT PROTECTION: A thief/looter died with stolen gear!
                // Route exclusively back to original registered owner
                targetOwnerUuid = registeredOwnerUuidComponent ?: deceased.uuid
                targetOwnerName = registeredOwnerNameComponent ?: "Unknown"

                // Thief deaths DO NOT penalize the original owner's death count!
                updatedDeathCount = currentDeathCount
            }
        }

        // Update item data components
        stack.set(ModDataComponents.ITEM_UUID.get(), existingUuid)
        stack.set(ModDataComponents.REGISTERED_OWNER_UUID.get(), targetOwnerUuid)
        stack.set(ModDataComponents.REGISTERED_OWNER.get(), targetOwnerName)
        stack.set(ModDataComponents.DEATH_COUNT.get(), updatedDeathCount)
        stack.set(ModDataComponents.LAST_DEATH_TIMESTAMP.get(), nowEpoch)

        ItemProvenance.updateLore(stack)

        val baseValuation = CoreValuationEngine.calculateBaseValue(stack)

        val entry = BuybackEntry(
            itemUuid = existingUuid,
            itemStack = stack.copy(),
            registeredOwnerUuid = targetOwnerUuid,
            registeredOwnerName = targetOwnerName,
            deathCount = updatedDeathCount,
            deathTimestampEpochSeconds = nowEpoch,
            baseCost = baseValuation
        )

        ledger[existingUuid] = entry
        return entry
    }

    /**
     * Calculates the buyback price considering:
     * 1. Owner's cumulative death_count escalation multiplier
     * 2. Smooth ~30-day wall-clock decay back to 1.0x baseline core value
     * 3. Contiguous claim municipal subsidies (if accessing from a safe claim)
     * 4. Public listing 1.25x markup (if purchased by a non-owner after 24h)
     */
    fun calculatePrice(
        entry: BuybackEntry,
        currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L,
        isOwner: Boolean = true,
        buyer: ServerPlayer? = null
    ): Long {
        val escalationStep = SmpConfig.COMMON.baseBuybackEscalationStep.get()
        val escalatedMultiplier = 1.0 + (entry.deathCount * escalationStep)

        // Smooth decay over wall-clock time (~30 days default)
        val decayPeriodSeconds = SmpConfig.COMMON.wallClockDecayDays.get() * 86400.0
        val elapsedSeconds = max(0.0, (currentTimeEpochSeconds - entry.deathTimestampEpochSeconds).toDouble())
        val decayRatio = (1.0 - (elapsedSeconds / decayPeriodSeconds)).coerceIn(0.0, 1.0)

        // Multiplier smoothly sags toward 1.0x baseline as decayRatio -> 0
        val effectiveMultiplier = 1.0 + ((escalatedMultiplier - 1.0) * decayRatio)
        var rawPrice = (entry.baseCost * effectiveMultiplier).toLong()

        // Catalyst discount
        if (entry.catalystDiscount > 0.0) {
            val discount = (rawPrice * entry.catalystDiscount).toLong()
            rawPrice = (rawPrice - discount).coerceAtLeast(10L)
        }

        // Apply municipal claim subsidy if buyer is inside a contiguous claim
        if (buyer != null) {
            val (subsidizedPrice, _) = SafeRecoveryManager.applyMunicipalSubsidy(rawPrice, buyer)
            rawPrice = subsidizedPrice
        }

        // If purchased by another player after 24h, apply 1.25x public listing markup
        if (!isOwner) {
            val markup = SmpConfig.COMMON.publicListingMarkup.get()
            return (rawPrice * markup).toLong().coerceAtLeast(10L)
        }

        return rawPrice.coerceAtLeast(10L)
    }

    /**
     * Checks if the 24-hour exclusive owner reclaim window has expired.
     */
    fun isPubliclyListed(entry: BuybackEntry, currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val exclusiveWindow = SmpConfig.COMMON.exclusiveOwnerWindowSeconds.get()
        return (currentTimeEpochSeconds - entry.deathTimestampEpochSeconds) >= exclusiveWindow
    }

    /**
     * Returns remaining seconds in the 24-hour exclusive owner window.
     */
    fun getRemainingExclusiveSeconds(entry: BuybackEntry, currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): Long {
        val exclusiveWindow = SmpConfig.COMMON.exclusiveOwnerWindowSeconds.get()
        val elapsed = currentTimeEpochSeconds - entry.deathTimestampEpochSeconds
        return max(0L, exclusiveWindow - elapsed)
    }

    /**
     * Determines whether the specified player is the registered owner of this entry.
     */
    fun isOwner(entry: BuybackEntry, player: Player): Boolean {
        return entry.registeredOwnerUuid == player.uuid ||
                entry.registeredOwnerName.equals(player.gameProfile.name, ignoreCase = true)
    }

    /**
     * Reclaims an item from the buyback ledger.
     * Connects directly to digital balance debits via EconomyManager.
     * Enforces the 24h exclusive window and 1.25x public listing markup.
     */
    fun reclaimItem(itemUuid: UUID, player: ServerPlayer): ReclaimResult {
        val entry = ledger[itemUuid] ?: return ReclaimResult.ItemNotFound
        val now = System.currentTimeMillis() / 1000L
        val isRegisteredOwner = isOwner(entry, player)

        // 24-Hour Exclusive Window check
        if (!isRegisteredOwner && !isPubliclyListed(entry, now)) {
            val remaining = getRemainingExclusiveSeconds(entry, now)
            return ReclaimResult.LockedExclusiveToOwner(entry.registeredOwnerName, remaining)
        }

        // Calculate price with municipal subsidy and public markup if applicable
        val finalPrice = calculatePrice(entry, now, isOwner = isRegisteredOwner, buyer = player)

        // Verify digital balance
        val currentBalance = EconomyManager.getBalance(player)
        if (currentBalance < finalPrice) {
            return ReclaimResult.InsufficientFunds(finalPrice, currentBalance)
        }

        // Debit digital balance with audit record
        val debitResult = dev.rmoseley.smpmod.economy.EconomyManager.debit(
            player,
            finalPrice,
            dev.rmoseley.smpmod.economy.TransactionType.RECOVERY_BUYBACK,
            "Reclaimed ${entry.itemStack.hoverName.string} (UUID: ${entry.itemUuid})"
        )
        if (debitResult !is dev.rmoseley.smpmod.economy.TransactionResult.Success) {
            return ReclaimResult.InsufficientFunds(finalPrice, currentBalance)
        }

        // Remove from ledger
        ledger.remove(itemUuid)
        val recoveredStack = entry.itemStack.copy()

        // If purchased by a public buyer, formally transfer registered ownership
        if (!isRegisteredOwner) {
            ItemProvenance.transferOwnership(recoveredStack, player, "Public Buyback Acquisition")
        }

        // Deliver item to player inventory or drop safely
        if (!player.inventory.add(recoveredStack)) {
            player.drop(recoveredStack, false)
        }

        // Calculate municipal savings
        val unsubsidizedPrice = calculatePrice(entry, now, isOwner = isRegisteredOwner, buyer = null)
        val subsidySaved = max(0L, unsubsidizedPrice - finalPrice)

        return ReclaimResult.Success(
            item = recoveredStack,
            pricePaid = finalPrice,
            wasPublicPurchase = !isRegisteredOwner,
            claimSubsidySaved = subsidySaved
        )
    }

    fun getEntry(itemUuid: UUID): BuybackEntry? = ledger[itemUuid]

    fun getEntriesForPlayer(player: Player): List<BuybackEntry> {
        return ledger.values.filter { isOwner(it, player) }
    }

    fun getEntriesForPlayerName(playerName: String): List<BuybackEntry> {
        return ledger.values.filter { it.registeredOwnerName.equals(playerName, ignoreCase = true) }
    }

    fun getAllPublicEntries(currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): List<BuybackEntry> {
        return ledger.values.filter { isPubliclyListed(it, currentTimeEpochSeconds) }
    }

    fun clearLedger() {
        ledger.clear()
    }
}
