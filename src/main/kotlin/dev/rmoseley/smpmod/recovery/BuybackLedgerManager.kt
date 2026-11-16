package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Manages the UUID Buyback Ledger, escalating recovery fees, wall-clock decay,
 * and the 24-hour exclusive reclaim window before public listing.
 */
object BuybackLedgerManager {

    data class BuybackEntry(
        val itemUuid: UUID,
        val itemStack: ItemStack,
        val registeredOwnerUuid: UUID,
        val registeredOwnerName: String,
        val deathCount: Int,
        val deathTimestampEpochSeconds: Long,
        val baseCost: Long
    )

    private val ledger = ConcurrentHashMap<UUID, BuybackEntry>()

    /**
     * Registers a lost item into the buyback ledger upon player death.
     * Enforces theft protection: if a thief died with a stolen item, it routes back
     * to the original registered owner and the death count does NOT increment.
     */
    fun recordDeathDrop(deceased: Player, stack: ItemStack, estimatedBaseCost: Long = 100L): BuybackEntry {
        val currentOwnerName = stack.get(ModDataComponents.REGISTERED_OWNER.get()) ?: deceased.gameProfile.name
        val registeredOwnerUuid = deceased.uuid // Defaults to current deceased if not previously tracked
        val existingUuid = stack.get(ModDataComponents.ITEM_UUID.get()) ?: UUID.randomUUID()

        val isOriginalOwner = deceased.gameProfile.name.equals(currentOwnerName, ignoreCase = true)
        val currentDeathCount = stack.get(ModDataComponents.DEATH_COUNT.get()) ?: 0

        // Increment death_count only if the original registered owner was the one who died
        val updatedDeathCount = if (isOriginalOwner) currentDeathCount + 1 else currentDeathCount
        val nowEpoch = System.currentTimeMillis() / 1000L

        // Update item data components
        stack.set(ModDataComponents.ITEM_UUID.get(), existingUuid)
        stack.set(ModDataComponents.REGISTERED_OWNER.get(), currentOwnerName)
        stack.set(ModDataComponents.DEATH_COUNT.get(), updatedDeathCount)
        stack.set(ModDataComponents.LAST_DEATH_TIMESTAMP.get(), nowEpoch)

        val entry = BuybackEntry(
            itemUuid = existingUuid,
            itemStack = stack.copy(),
            registeredOwnerUuid = registeredOwnerUuid,
            registeredOwnerName = currentOwnerName,
            deathCount = updatedDeathCount,
            deathTimestampEpochSeconds = nowEpoch,
            baseCost = estimatedBaseCost
        )

        ledger[existingUuid] = entry
        return entry
    }

    /**
     * Calculates the current buyback price considering cumulative death multiplier
     * and the ~30-day wall-clock decay back toward 1.0x.
     */
    fun calculatePrice(entry: BuybackEntry, currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): Long {
        val escalationStep = SmpConfig.COMMON.baseBuybackEscalationStep.get()
        val escalatedMultiplier = 1.0 + (entry.deathCount * escalationStep)

        // Smooth decay over wall-clock time
        val decayPeriodSeconds = SmpConfig.COMMON.wallClockDecayDays.get() * 86400.0
        val elapsedSeconds = max(0.0, (currentTimeEpochSeconds - entry.deathTimestampEpochSeconds).toDouble())
        val decayRatio = (1.0 - (elapsedSeconds / decayPeriodSeconds)).coerceIn(0.0, 1.0)

        // Multiplier scales from escalated down to 1.0x baseline as decayRatio -> 0
        val effectiveMultiplier = 1.0 + ((escalatedMultiplier - 1.0) * decayRatio)
        return (entry.baseCost * effectiveMultiplier).toLong()
    }

    /**
     * Checks if the 24-hour exclusive reclaim window has expired.
     */
    fun isPubliclyListed(entry: BuybackEntry, currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val exclusiveWindow = SmpConfig.COMMON.exclusiveOwnerWindowSeconds.get()
        return (currentTimeEpochSeconds - entry.deathTimestampEpochSeconds) > exclusiveWindow
    }

    /**
     * Returns purchase price for a specific buyer.
     * Original owner gets calculated price.
     * Other players can only purchase after 24h, paying the 1.25x markup.
     */
    fun getPurchasePriceForPlayer(
        entry: BuybackEntry,
        buyer: Player,
        currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): Long? {
        val isOwner = buyer.gameProfile.name.equals(entry.registeredOwnerName, ignoreCase = true)
        val basePrice = calculatePrice(entry, currentTimeEpochSeconds)

        if (isOwner) {
            return basePrice
        }

        // Public listing check
        if (!isPubliclyListed(entry, currentTimeEpochSeconds)) {
            return null // Locked to owner
        }

        val markup = SmpConfig.COMMON.publicListingMarkup.get()
        return (basePrice * markup).toLong()
    }

    fun claimItem(itemUuid: UUID): ItemStack? {
        val entry = ledger.remove(itemUuid)
        return entry?.itemStack
    }

    fun getEntriesForPlayer(playerName: String): List<BuybackEntry> {
        return ledger.values.filter { it.registeredOwnerName.equals(playerName, ignoreCase = true) }
    }

    fun getAllPublicEntries(currentTimeEpochSeconds: Long = System.currentTimeMillis() / 1000L): List<BuybackEntry> {
        return ledger.values.filter { isPubliclyListed(it, currentTimeEpochSeconds) }
    }
}
