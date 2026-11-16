package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages digital non-material player currency.
 * Balances cannot be dropped, burned, or lost on death.
 *
 * Implements strict transaction safety, audit logging, atomic P2P transfers
 * with configurable server tax rates, and persistent player data attachments.
 */
object EconomyManager {

    // Lock striping by player UUID to guarantee thread-safe atomic balance updates
    private val playerLocks = ConcurrentHashMap<UUID, Any>()

    private fun getLock(uuid: UUID): Any {
        return playerLocks.computeIfAbsent(uuid) { Any() }
    }

    /**
     * Retrieves the current digital balance for the specified player.
     */
    fun getBalance(player: Player): Long {
        return player.getData(ModAttachmentTypes.PLAYER_ECONOMY.get())
    }

    /**
     * Directly writes the balance to the player's persistent data attachment.
     */
    private fun setBalance(player: Player, newBalance: Long) {
        player.setData(ModAttachmentTypes.PLAYER_ECONOMY.get(), newBalance.coerceAtLeast(0L))
    }

    /**
     * Credits digital currency to a player with full transaction safety and audit tracking.
     */
    fun credit(
        player: Player,
        amount: Long,
        type: TransactionType = TransactionType.CREDIT,
        note: String = ""
    ): TransactionResult {
        if (amount <= 0L) {
            return TransactionResult.Failure("Credit amount must be greater than zero", getBalance(player))
        }

        synchronized(getLock(player.uuid)) {
            val current = getBalance(player)
            val updated = current + amount
            setBalance(player, updated)

            val record = TransactionRecord(
                type = type,
                playerUuid = player.uuid,
                playerName = player.gameProfile.name,
                amount = amount,
                taxDeducted = 0L,
                balanceAfter = updated,
                note = note
            )
            EconomyAuditLedger.record(record)
            return TransactionResult.Success(record)
        }
    }

    /**
     * Backward-compatible simple credit helper returning the new balance.
     */
    fun credit(player: Player, amount: Long): Long {
        val result = credit(player, amount, TransactionType.CREDIT, "")
        return if (result is TransactionResult.Success) result.newBalance else getBalance(player)
    }

    /**
     * Debits digital currency from a player with overdraft protection and audit recording.
     */
    fun debit(
        player: Player,
        amount: Long,
        type: TransactionType = TransactionType.DEBIT,
        note: String = ""
    ): TransactionResult {
        if (amount <= 0L) {
            return TransactionResult.Failure("Debit amount must be greater than zero", getBalance(player))
        }

        synchronized(getLock(player.uuid)) {
            val current = getBalance(player)
            if (current < amount) {
                return TransactionResult.Failure("Insufficient funds (current: $current, required: $amount)", current)
            }

            val updated = current - amount
            setBalance(player, updated)

            val record = TransactionRecord(
                type = type,
                playerUuid = player.uuid,
                playerName = player.gameProfile.name,
                amount = amount,
                taxDeducted = 0L,
                balanceAfter = updated,
                note = note
            )
            EconomyAuditLedger.record(record)
            return TransactionResult.Success(record)
        }
    }

    /**
     * Backward-compatible simple debit helper returning boolean success.
     */
    fun debit(player: Player, amount: Long): Boolean {
        if (amount <= 0L) return true
        val result = debit(player, amount, TransactionType.DEBIT, "")
        return result is TransactionResult.Success
    }

    /**
     * Executes an atomic peer-to-peer digital currency transfer applying the configured server tax rate.
     * Prevents deadlocks via deterministic lock acquisition ordering.
     */
    fun transfer(
        from: Player,
        to: Player,
        amount: Long,
        note: String = "P2P Transfer"
    ): TransactionResult {
        if (amount <= 0L) {
            return TransactionResult.Failure("Transfer amount must be greater than zero", getBalance(from))
        }
        if (from.uuid == to.uuid) {
            return TransactionResult.Failure("Cannot transfer currency to oneself", getBalance(from))
        }

        // Deterministic lock acquisition order to guarantee deadlock avoidance
        val lockFirst = if (from.uuid < to.uuid) from.uuid else to.uuid
        val lockSecond = if (from.uuid < to.uuid) to.uuid else from.uuid

        synchronized(getLock(lockFirst)) {
            synchronized(getLock(lockSecond)) {
                val fromBalance = getBalance(from)
                if (fromBalance < amount) {
                    return TransactionResult.Failure(
                        "Insufficient funds for transfer (balance: $fromBalance, requested: $amount)",
                        fromBalance
                    )
                }

                val taxRate = SmpConfig.COMMON.p2pTradeTaxPercent.get().coerceIn(0.0, 1.0)
                val taxAmount = (amount * taxRate).toLong()
                val netReceived = amount - taxAmount

                val newFromBalance = fromBalance - amount
                val newToBalance = getBalance(to) + netReceived

                setBalance(from, newFromBalance)
                setBalance(to, newToBalance)

                val record = TransactionRecord(
                    type = TransactionType.P2P_TRANSFER,
                    playerUuid = from.uuid,
                    playerName = from.gameProfile.name,
                    targetPlayerUuid = to.uuid,
                    targetPlayerName = to.gameProfile.name,
                    amount = amount,
                    taxDeducted = taxAmount,
                    balanceAfter = newFromBalance,
                    note = note
                )
                EconomyAuditLedger.record(record)

                // Also record the server tax if collected
                if (taxAmount > 0L) {
                    EconomyAuditLedger.record(
                        TransactionRecord(
                            type = TransactionType.SERVER_TAX,
                            playerUuid = from.uuid,
                            playerName = from.gameProfile.name,
                            targetPlayerUuid = to.uuid,
                            targetPlayerName = to.gameProfile.name,
                            amount = taxAmount,
                            taxDeducted = taxAmount,
                            balanceAfter = newFromBalance,
                            note = "Server Tax (${(taxRate * 100).toInt()}%) on P2P transfer"
                        )
                    )
                }

                return TransactionResult.Success(record)
            }
        }
    }

    /**
     * Simple boolean transfer helper for external callers.
     */
    fun transfer(from: Player, to: Player, amount: Long): Boolean {
        val result = transfer(from, to, amount, "P2P Transfer")
        return result is TransactionResult.Success
    }
}
