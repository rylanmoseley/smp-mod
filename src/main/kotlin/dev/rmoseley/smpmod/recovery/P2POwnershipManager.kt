package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages formal Peer-to-Peer (P2P) trading and ownership handover.
 * Completing a trade transfers registered ownership, updates Item Lore provenance,
 * and transfers non-material digital currency applying server transaction taxes.
 */
object P2POwnershipManager {

    data class TradeOffer(
        val offerId: UUID = UUID.randomUUID(),
        val sellerUuid: UUID,
        val sellerName: String,
        val buyerUuid: UUID,
        val buyerName: String,
        val itemStack: ItemStack,
        val price: Long,
        val timestampEpochSeconds: Long = System.currentTimeMillis() / 1000L
    )

    sealed class P2PTradeResult {
        data class Success(val item: ItemStack, val price: Long, val taxDeducted: Long) : P2PTradeResult()
        data class Failure(val reason: String) : P2PTradeResult()
    }

    // Pending trade offers mapped by buyer UUID
    private val pendingOffers = ConcurrentHashMap<UUID, TradeOffer>()

    /**
     * Creates a formal P2P trade offer for the item in the seller's main hand.
     */
    fun createOffer(seller: ServerPlayer, buyer: ServerPlayer, price: Long): P2PTradeResult {
        if (price <= 0) {
            return P2PTradeResult.Failure("Trade price must be greater than zero.")
        }

        if (seller.uuid == buyer.uuid) {
            return P2PTradeResult.Failure("Cannot trade an item to yourself.")
        }

        val heldItem = seller.mainHandItem
        if (heldItem.isEmpty) {
            return P2PTradeResult.Failure("Must hold an item in main hand to trade.")
        }

        val buyerBalance = EconomyManager.getBalance(buyer)
        if (buyerBalance < price) {
            return P2PTradeResult.Failure("${buyer.gameProfile.name} does not have sufficient digital funds (has $buyerBalance, requires $price).")
        }

        // Ensure item has origin initialized if not already
        if (heldItem.get(ModDataComponents.ORIGIN_CREATOR.get()) == null) {
            ItemProvenance.initializeItemOrigin(heldItem, seller, isCrafted = false)
        }

        val offer = TradeOffer(
            sellerUuid = seller.uuid,
            sellerName = seller.gameProfile.name,
            buyerUuid = buyer.uuid,
            buyerName = buyer.gameProfile.name,
            itemStack = heldItem.copy(),
            price = price
        )

        pendingOffers[buyer.uuid] = offer

        seller.sendSystemMessage(Component.literal("§aOffer sent to ${buyer.gameProfile.name} for §e$price §acoins."))
        buyer.sendSystemMessage(Component.literal("§e${seller.gameProfile.name} offered to sell you §b${heldItem.hoverName.string} §efor §a$price §ecoins. Type §6/p2p accept §eto purchase."))

        return P2PTradeResult.Success(heldItem, price, 0L)
    }

    /**
     * Buyer accepts a pending trade offer, executing payment and ownership transfer.
     */
    fun acceptOffer(buyer: ServerPlayer): P2PTradeResult {
        val offer = pendingOffers.remove(buyer.uuid)
            ?: return P2PTradeResult.Failure("You have no pending P2P trade offers.")

        val server = buyer.server
        val seller = server.playerList.getPlayer(offer.sellerUuid)
            ?: return P2PTradeResult.Failure("The seller (${offer.sellerName}) is no longer online.")

        // Verify seller still holds the item in inventory
        val sellerInventory = seller.inventory
        var matchingSlot = -1
        for (i in 0 until sellerInventory.containerSize) {
            val s = sellerInventory.getItem(i)
            if (ItemStack.isSameItemSameComponents(s, offer.itemStack)) {
                matchingSlot = i
                break
            }
        }

        if (matchingSlot == -1) {
            return P2PTradeResult.Failure("Seller no longer has the offered item in their inventory.")
        }

        // Check buyer balance
        if (EconomyManager.getBalance(buyer) < offer.price) {
            return P2PTradeResult.Failure("Insufficient digital currency to complete this purchase.")
        }

        // Execute currency transfer with tax
        val transferSuccess = EconomyManager.transfer(buyer, seller, offer.price)
        if (!transferSuccess) {
            return P2PTradeResult.Failure("Currency transaction failed.")
        }

        // Remove item from seller
        val soldStack = sellerInventory.removeItem(matchingSlot, 1)

        // Formally transfer ownership and update provenance lore
        ItemProvenance.transferOwnership(soldStack, buyer, "P2P Sale")

        // Deliver item to buyer
        if (!buyer.inventory.add(soldStack)) {
            buyer.drop(soldStack, false)
        }

        seller.sendSystemMessage(Component.literal("§aSuccessfully sold §b${soldStack.hoverName.string} §ato ${buyer.gameProfile.name} for §e${offer.price} §acoins."))
        buyer.sendSystemMessage(Component.literal("§aSuccessfully purchased §b${soldStack.hoverName.string} §afrom ${seller.gameProfile.name}. Ownership formally registered to you!"))

        return P2PTradeResult.Success(soldStack, offer.price, 0L)
    }

    /**
     * Directly executes an atomic P2P trade without requiring interactive offer/acceptance.
     * Useful for automated stalls, trading menus, or direct commands.
     */
    fun executeDirectTrade(seller: ServerPlayer, buyer: ServerPlayer, stack: ItemStack, price: Long): P2PTradeResult {
        if (price <= 0) return P2PTradeResult.Failure("Price must be positive.")
        if (stack.isEmpty) return P2PTradeResult.Failure("Item is empty.")
        if (EconomyManager.getBalance(buyer) < price) {
            return P2PTradeResult.Failure("Buyer has insufficient funds.")
        }

        // Initialize origin if needed
        if (stack.get(ModDataComponents.ORIGIN_CREATOR.get()) == null) {
            ItemProvenance.initializeItemOrigin(stack, seller, isCrafted = false)
        }

        // Transfer funds
        val paid = EconomyManager.transfer(buyer, seller, price)
        if (!paid) return P2PTradeResult.Failure("Payment transfer failed.")

        // Formal ownership transfer
        ItemProvenance.transferOwnership(stack, buyer, "P2P Trade")

        return P2PTradeResult.Success(stack, price, 0L)
    }

    fun getPendingOffer(buyerUuid: UUID): TradeOffer? = pendingOffers[buyerUuid]

    fun cancelOffer(buyerUuid: UUID): Boolean = pendingOffers.remove(buyerUuid) != null
}
