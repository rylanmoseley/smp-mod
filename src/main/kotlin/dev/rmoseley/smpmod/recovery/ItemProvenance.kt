package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import java.util.UUID

/**
 * Manages item provenance tracking and formatted Item Lore in Minecraft 1.21.1.
 * Preserves the original creator (Crafted by / Looted by), tracks registered owner,
 * and maintains recovery count and formal ownership handover history.
 */
object ItemProvenance {

    /**
     * Initializes provenance metadata when an item is first crafted or naturally looted.
     */
    fun initializeItemOrigin(stack: ItemStack, creator: Player, isCrafted: Boolean = true) {
        val existingUuid = stack.get(ModDataComponents.ITEM_UUID.get())
        if (existingUuid == null) {
            stack.set(ModDataComponents.ITEM_UUID.get(), UUID.randomUUID())
        }

        val creatorName = creator.gameProfile.name
        val creatorUuid = creator.uuid
        val originPrefix = if (isCrafted) "Crafted by" else "Looted by"
        val originString = "$originPrefix $creatorName"

        stack.set(ModDataComponents.ORIGIN_CREATOR.get(), originString)
        stack.set(ModDataComponents.REGISTERED_OWNER.get(), creatorName)
        stack.set(ModDataComponents.REGISTERED_OWNER_UUID.get(), creatorUuid)

        if (stack.get(ModDataComponents.DEATH_COUNT.get()) == null) {
            stack.set(ModDataComponents.DEATH_COUNT.get(), 0)
        }

        val initialHistory = listOf("Origin: $originString")
        stack.set(ModDataComponents.PROVENANCE_LORE.get(), initialHistory)

        updateLore(stack)
    }

    /**
     * Formally transfers registered ownership to a new player upon completing a P2P sale
     * or a public buyback purchase. Preserves the immutable origin creator.
     */
    fun transferOwnership(stack: ItemStack, newOwner: Player, transferContext: String = "P2P Sale") {
        val previousOwner = stack.get(ModDataComponents.REGISTERED_OWNER.get()) ?: "Unknown"
        val newOwnerName = newOwner.gameProfile.name
        val newOwnerUuid = newOwner.uuid

        stack.set(ModDataComponents.REGISTERED_OWNER.get(), newOwnerName)
        stack.set(ModDataComponents.REGISTERED_OWNER_UUID.get(), newOwnerUuid)

        // Append to provenance history
        val currentHistory = stack.get(ModDataComponents.PROVENANCE_LORE.get()) ?: emptyList()
        val updatedHistory = currentHistory.toMutableList()
        updatedHistory.add("Transferred: $previousOwner -> $newOwnerName ($transferContext)")
        stack.set(ModDataComponents.PROVENANCE_LORE.get(), updatedHistory)

        updateLore(stack)
    }

    /**
     * Updates the native Minecraft 1.21.1 DataComponents.LORE with rich provenance components.
     */
    fun updateLore(stack: ItemStack) {
        val origin = stack.get(ModDataComponents.ORIGIN_CREATOR.get()) ?: return
        val owner = stack.get(ModDataComponents.REGISTERED_OWNER.get()) ?: return
        val deathCount = stack.get(ModDataComponents.DEATH_COUNT.get()) ?: 0
        val catalystReductions = stack.get(ModDataComponents.CATALYST_REDUCTIONS.get()) ?: 0
        val history = stack.get(ModDataComponents.PROVENANCE_LORE.get()) ?: emptyList()

        val loreLines = mutableListOf<Component>()

        // 1. Immutable Origin line: Crafted by / Looted by
        loreLines.add(Component.literal("§7$origin"))

        // 2. Current Registered Owner line
        loreLines.add(Component.literal("§aOwned by §f$owner"))

        // 3. Death recovery history
        if (deathCount > 0) {
            loreLines.add(Component.literal("§cDeath Recovery Count: §e$deathCount"))
        }

        // 4. Catalyst restoration history
        if (catalystReductions > 0) {
            loreLines.add(Component.literal("§bCatalyst Restorations: §f$catalystReductions"))
        }

        // 5. Combat Achievement History
        val combatEntries = history.filter { it.startsWith("Combat:") }
        if (combatEntries.isNotEmpty()) {
            loreLines.add(Component.literal("§dCombat History:"))
            for (entry in combatEntries.takeLast(2)) {
                val concise = entry.removePrefix("Combat: ")
                loreLines.add(Component.literal("§d  ★ $concise"))
            }
        }

        // 6. Transfer History (last 2 entries if transfers occurred)
        val transferEntries = history.filter { it.startsWith("Transferred:") }
        if (transferEntries.isNotEmpty()) {
            loreLines.add(Component.literal("§8History:"))
            for (entry in transferEntries.takeLast(2)) {
                val concise = entry.removePrefix("Transferred: ")
                loreLines.add(Component.literal("§8  • $concise"))
            }
        }

        stack.set(DataComponents.LORE, ItemLore(loreLines))
    }

    /**
     * Records a major combat achievement or role accomplishment on the item's persistent provenance lore.
     */
    fun addCombatHistory(stack: ItemStack, achievement: String) {
        val currentHistory = stack.get(ModDataComponents.PROVENANCE_LORE.get()) ?: emptyList()
        val updatedHistory = currentHistory.toMutableList()
        updatedHistory.add("Combat: $achievement")
        stack.set(ModDataComponents.PROVENANCE_LORE.get(), updatedHistory)
        updateLore(stack)
    }

    /**
     * Returns the full provenance ledger history for an item.
     */
    fun getProvenanceHistory(stack: ItemStack): List<String> {
        return stack.get(ModDataComponents.PROVENANCE_LORE.get()) ?: emptyList()
    }
}
