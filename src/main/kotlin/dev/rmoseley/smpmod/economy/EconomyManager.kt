package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.world.entity.player.Player

/**
 * Manages digital non-material player currency.
 * Balances cannot be dropped, burned, or lost on death.
 */
object EconomyManager {

    fun getBalance(player: Player): Long {
        return player.getData(ModAttachmentTypes.PLAYER_ECONOMY.get())
    }

    fun credit(player: Player, amount: Long): Long {
        if (amount <= 0) return getBalance(player)
        val newBalance = getBalance(player) + amount
        player.setData(ModAttachmentTypes.PLAYER_ECONOMY.get(), newBalance)
        return newBalance
    }

    fun debit(player: Player, amount: Long): Boolean {
        if (amount <= 0) return true
        val current = getBalance(player)
        if (current < amount) return false
        player.setData(ModAttachmentTypes.PLAYER_ECONOMY.get(), current - amount)
        return true
    }

    /**
     * Executes a peer-to-peer digital currency transfer applying the configured tax rate.
     */
    fun transfer(from: Player, to: Player, amount: Long): Boolean {
        if (amount <= 0) return false
        if (getBalance(from) < amount) return false

        val taxRate = SmpConfig.COMMON.p2pTradeTaxPercent.get()
        val taxAmount = (amount * taxRate).toLong()
        val receivedAmount = amount - taxAmount

        debit(from, amount)
        credit(to, receivedAmount)
        return true
    }
}
