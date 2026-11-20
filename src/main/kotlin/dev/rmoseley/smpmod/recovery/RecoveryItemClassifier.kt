package dev.rmoseley.smpmod.recovery

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.DiggerItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.TieredItem
import net.minecraft.world.item.TridentItem

/**
 * Strict classifier routing dropped death items between the Ownership-Bound UUID Buyback Ledger
 * and the BulkStoreManager, in adherence to design-v2.md Section 2.2.
 *
 * Buyback Ledger Candidates:
 *  - Weapons, armor, tools, and shields (damageable / equipment)
 *  - Any item with a custom name
 *  - Any enchanted item (or containing stored enchantments)
 *  - Whitelisted high-value stackable exceptions (Netherite, catalysts, rare artifacts)
 *
 * Bulk Store Candidates:
 *  - Mundane stackables (maxStackSize > 1) not meeting any of the above criteria.
 */
object RecoveryItemClassifier {

    /**
     * High-value stackable exceptions that must route to the UUID Buyback Ledger
     * rather than being liquidated into the bulk store.
     */
    private val HIGH_VALUE_STACKABLE_EXCEPTIONS = setOf(
        Items.NETHERITE_INGOT,
        Items.NETHERITE_BLOCK,
        Items.NETHERITE_SCRAP,
        Items.ANCIENT_DEBRIS,
        Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
        Items.TOTEM_OF_UNDYING,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.ELYTRA,
        Items.HEAVY_CORE,
        Items.NETHER_STAR,
        Items.BEACON,
        Items.DRAGON_EGG,
        Items.DRAGON_BREATH,
        Items.CONDUIT,
        Items.HEART_OF_THE_SEA,
        Items.OMINOUS_BOTTLE,
        Items.OMINOUS_TRIAL_KEY,
        Items.TRIAL_KEY,
        Items.WITHER_SKELETON_SKULL,
        Items.ECHO_SHARD
    )

    enum class RoutingDestination {
        BUYBACK_LEDGER,
        BULK_STORE,
        IGNORED
    }

    /**
     * Determines whether an item routes to UUID Buyback or Bulk Store.
     */
    fun classify(stack: ItemStack): RoutingDestination {
        if (stack.isEmpty) return RoutingDestination.IGNORED

        if (isBuybackCandidate(stack)) {
            return RoutingDestination.BUYBACK_LEDGER
        }

        if (isBulkStoreCandidate(stack)) {
            return RoutingDestination.BULK_STORE
        }

        // Single un-damageable, un-enchanted mundane items also default to buyback or bulk store
        return if (stack.maxStackSize > 1) RoutingDestination.BULK_STORE else RoutingDestination.BUYBACK_LEDGER
    }

    /**
     * Returns true if the item belongs in the unique ownership-bound UUID Buyback ledger.
     */
    fun isBuybackCandidate(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false

        // 1. Damageable gear (weapons, armor, tools, shields, bows)
        if (stack.isDamageableItem) return true
        if (isEquipmentItem(stack)) return true

        // 2. Named items
        if (stack.has(DataComponents.CUSTOM_NAME)) return true

        // 3. Enchanted items or enchanted books
        if (stack.isEnchanted) return true
        if (stack.has(DataComponents.ENCHANTMENTS) || stack.has(DataComponents.STORED_ENCHANTMENTS)) return true

        // 4. Designated high-value stackables & artifacts
        if (HIGH_VALUE_STACKABLE_EXCEPTIONS.contains(stack.item)) return true

        // 5. Recovery / Repair catalysts
        if (RecoveryCatalystManager.isCatalyst(stack)) return true

        // 6. Curio accessory items
        if (CuriosIntegration.isCurioItem(stack)) return true

        return false
    }

    /**
     * Returns true if the item is a standard mundane stackable destined for Bulk Store.
     */
    fun isBulkStoreCandidate(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (isBuybackCandidate(stack)) return false
        return stack.maxStackSize > 1
    }

    private fun isEquipmentItem(stack: ItemStack): Boolean {
        val item = stack.item
        return item is ArmorItem ||
                item is SwordItem ||
                item is DiggerItem ||
                item is TieredItem ||
                item is BowItem ||
                item is CrossbowItem ||
                item is TridentItem ||
                item is ShieldItem ||
                item == Items.ELYTRA
    }
}
