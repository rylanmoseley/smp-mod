package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import java.util.UUID

/**
 * Manages item provenance tracking and formatted Item Lore.
 * Preserves origin creator and current registered owner.
 */
object ItemProvenance {

    fun initializeItemOrigin(stack: ItemStack, creator: Player, isCrafted: Boolean = true) {
        val existingUuid = stack.get(ModDataComponents.ITEM_UUID.get())
        if (existingUuid == null) {
            stack.set(ModDataComponents.ITEM_UUID.get(), UUID.randomUUID())
        }

        val creatorName = creator.gameProfile.name
        val originPrefix = if (isCrafted) "Crafted by" else "Looted by"
        val originString = "$originPrefix $creatorName"

        stack.set(ModDataComponents.ORIGIN_CREATOR.get(), originString)
        stack.set(ModDataComponents.REGISTERED_OWNER.get(), creatorName)
        updateLore(stack)
    }

    fun transferOwnership(stack: ItemStack, newOwner: Player) {
        val newOwnerName = newOwner.gameProfile.name
        stack.set(ModDataComponents.REGISTERED_OWNER.get(), newOwnerName)
        updateLore(stack)
    }

    fun updateLore(stack: ItemStack) {
        val origin = stack.get(ModDataComponents.ORIGIN_CREATOR.get()) ?: return
        val owner = stack.get(ModDataComponents.REGISTERED_OWNER.get()) ?: return
        val deathCount = stack.get(ModDataComponents.DEATH_COUNT.get()) ?: 0

        val loreLines = mutableListOf<Component>()
        loreLines.add(Component.literal("§7$origin"))
        loreLines.add(Component.literal("§aOwned by §f$owner"))
        if (deathCount > 0) {
            loreLines.add(Component.literal("§cDeath Recovery Count: §e$deathCount"))
        }

        stack.set(DataComponents.LORE, ItemLore(loreLines))
    }
}
