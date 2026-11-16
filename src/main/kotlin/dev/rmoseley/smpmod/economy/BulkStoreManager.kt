package dev.rmoseley.smpmod.economy

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages server bulk store routing for standard stackable items dropped on death.
 * Feeds player/server commerce at baseline market rates without cluttering unique buybacks.
 */
object BulkStoreManager {

    private val inventory = ConcurrentHashMap<String, Long>()

    /**
     * Determines whether an item should route to the bulk store or the unique UUID buyback ledger.
     * Named, enchanted, tools, armor, weapons, and rare catalysts route to Buybacks.
     * Standard stackables route to Bulk Store.
     */
    fun isBulkStoreCandidate(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.isEnchanted) return false
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) return false
        if (stack.isDamageableItem) return false
        return stack.maxStackSize > 1
    }

    fun deposit(stack: ItemStack) {
        if (stack.isEmpty) return
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        inventory.compute(itemId) { _, current -> (current ?: 0L) + stack.count }
    }

    fun getStock(itemId: String): Long {
        return inventory[itemId] ?: 0L
    }
}
