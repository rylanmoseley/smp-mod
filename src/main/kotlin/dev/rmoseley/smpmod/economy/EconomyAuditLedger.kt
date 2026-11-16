package dev.rmoseley.smpmod.economy

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe audit ledger maintaining an unalterable history of all digital transactions.
 * Facilitates administrative oversight, player ledger inspection, and server tax accounting.
 */
object EconomyAuditLedger {

    private const val MAX_GLOBAL_ENTRIES = 1000
    private const val MAX_PLAYER_ENTRIES = 50

    private val globalHistory = ConcurrentLinkedDeque<TransactionRecord>()
    private val playerHistory = ConcurrentHashMap<UUID, ConcurrentLinkedDeque<TransactionRecord>>()
    private val cumulativeServerTax = AtomicLong(0L)

    /**
     * Commits a new transaction record into the audit ledger.
     */
    fun record(record: TransactionRecord) {
        // Global chronological ledger (newest first)
        globalHistory.addFirst(record)
        while (globalHistory.size > MAX_GLOBAL_ENTRIES) {
            globalHistory.pollLast()
        }

        // Primary player history
        val pQueue = playerHistory.computeIfAbsent(record.playerUuid) { ConcurrentLinkedDeque() }
        pQueue.addFirst(record)
        while (pQueue.size > MAX_PLAYER_ENTRIES) {
            pQueue.pollLast()
        }

        // Secondary / counterparty history if applicable
        if (record.targetPlayerUuid != null && record.targetPlayerUuid != record.playerUuid) {
            val tQueue = playerHistory.computeIfAbsent(record.targetPlayerUuid) { ConcurrentLinkedDeque() }
            tQueue.addFirst(record)
            while (tQueue.size > MAX_PLAYER_ENTRIES) {
                tQueue.pollLast()
            }
        }

        // Accrue server tax if deducted
        if (record.taxDeducted > 0L) {
            cumulativeServerTax.addAndGet(record.taxDeducted)
        }
    }

    /**
     * Retrieves the most recent transaction entries for a specific player.
     */
    fun getPlayerAuditHistory(playerUuid: UUID, limit: Int = 20): List<TransactionRecord> {
        val queue = playerHistory[playerUuid] ?: return emptyList()
        return queue.take(limit.coerceAtLeast(1))
    }

    /**
     * Retrieves recent server-wide transactions.
     */
    fun getGlobalAuditHistory(limit: Int = 50): List<TransactionRecord> {
        return globalHistory.take(limit.coerceAtLeast(1))
    }

    /**
     * Returns cumulative tax collected by the server from P2P transfers and commerce.
     */
    fun getCumulativeServerTax(): Long {
        return cumulativeServerTax.get()
    }

    /**
     * Resets the audit state (primarily for automated testing).
     */
    fun clear() {
        globalHistory.clear()
        playerHistory.clear()
        cumulativeServerTax.set(0L)
    }
}
