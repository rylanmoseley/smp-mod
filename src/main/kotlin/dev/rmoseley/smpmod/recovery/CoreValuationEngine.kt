package dev.rmoseley.smpmod.recovery

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TieredItem
import net.minecraft.world.item.Tiers

/**
 * Baseline Core Valuation Engine implementing Section 7's core valuation table.
 * Computes a deterministic baseline digital currency value for any lost unique item,
 * serving as the 1.0x baseline before death escalation and decay formulas are applied.
 */
object CoreValuationEngine {

    private const val MIN_BASE_VALUE: Long = 50L

    /**
     * Calculates the baseline core value for a given ItemStack.
     */
    fun calculateBaseValue(stack: ItemStack): Long {
        if (stack.isEmpty) return MIN_BASE_VALUE

        var value: Long = 0L

        // 1. Check for specific high-value artifacts and items
        val specificValue = getSpecificItemValue(stack)
        if (specificValue != null) {
            value = specificValue * stack.count
        } else {
            // 2. Evaluate equipment by tier and type
            val tierValue = getEquipmentTierValue(stack)
            val slotMultiplier = getEquipmentSlotMultiplier(stack)
            value = (tierValue * slotMultiplier).toLong()
        }

        // 3. Add enchantment valuation
        val enchantmentValue = calculateEnchantmentValue(stack)
        value += enchantmentValue

        // 4. Custom name emotional/aesthetic bonus
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            value += 50L
        }

        // 5. Catalyst bonus if recognized
        if (RecoveryCatalystManager.isCatalyst(stack)) {
            value += 200L * stack.count
        }

        return value.coerceAtLeast(MIN_BASE_VALUE)
    }

    private fun getSpecificItemValue(stack: ItemStack): Long? {
        return when (stack.item) {
            Items.ELYTRA -> 3000L
            Items.HEAVY_CORE -> 2500L
            Items.NETHER_STAR -> 2000L
            Items.BEACON -> 2200L
            Items.MACE -> 2500L
            Items.TRIDENT -> 1500L
            Items.NETHERITE_BLOCK -> 14400L
            Items.NETHERITE_INGOT -> 1600L
            Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE -> 1200L
            Items.ENCHANTED_GOLDEN_APPLE -> 1000L
            Items.TOTEM_OF_UNDYING -> 800L
            Items.ANCIENT_DEBRIS -> 400L
            Items.NETHERITE_SCRAP -> 350L
            Items.OMINOUS_TRIAL_KEY -> 600L
            Items.TRIAL_KEY -> 300L
            Items.WITHER_SKELETON_SKULL -> 500L
            Items.ECHO_SHARD -> 250L
            Items.DRAGON_EGG -> 5000L
            Items.DRAGON_BREATH -> 100L
            Items.HEART_OF_THE_SEA -> 800L
            Items.CONDUIT -> 1200L
            else -> null
        }
    }

    private fun getEquipmentTierValue(stack: ItemStack): Long {
        val item = stack.item

        if (item is TieredItem) {
            return when (item.tier) {
                Tiers.NETHERITE -> 1200L
                Tiers.DIAMOND -> 400L
                Tiers.IRON -> 100L
                Tiers.GOLD -> 60L
                Tiers.STONE -> 40L
                Tiers.WOOD -> 25L
                else -> 100L
            }
        }

        if (item is ArmorItem) {
            return when (item.material) {
                ArmorMaterials.NETHERITE -> 1400L
                ArmorMaterials.DIAMOND -> 450L
                ArmorMaterials.IRON -> 100L
                ArmorMaterials.GOLD -> 70L
                ArmorMaterials.CHAIN -> 90L
                ArmorMaterials.LEATHER -> 30L
                ArmorMaterials.TURTLE -> 300L
                ArmorMaterials.ARMADILLO -> 150L
                else -> 100L
            }
        }

        // Default weapons/tools like bows, crossbows, shields
        return when (item) {
            Items.CROSSBOW -> 120L
            Items.BOW -> 80L
            Items.SHIELD -> 75L
            Items.FISHING_ROD -> 50L
            Items.SHEARS -> 40L
            Items.FLINT_AND_STEEL -> 30L
            else -> 60L
        }
    }

    private fun getEquipmentSlotMultiplier(stack: ItemStack): Double {
        val item = stack.item
        if (item is ArmorItem) {
            return when (item.type) {
                ArmorItem.Type.CHESTPLATE -> 1.5
                ArmorItem.Type.LEGGINGS -> 1.3
                ArmorItem.Type.HELMET -> 1.1
                ArmorItem.Type.BOOTS -> 1.0
                ArmorItem.Type.BODY -> 1.2
            }
        }
        return 1.0
    }

    private fun calculateEnchantmentValue(stack: ItemStack): Long {
        var enchantTotal = 0L

        val enchantments = stack.get(DataComponents.ENCHANTMENTS)
        if (enchantments != null) {
            for (entry in enchantments.entrySet()) {
                val level = entry.intValue
                // Each enchantment level adds baseline + 50L per level
                enchantTotal += (level * 60L)
            }
        }

        val storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS)
        if (storedEnchantments != null) {
            for (entry in storedEnchantments.entrySet()) {
                val level = entry.intValue
                enchantTotal += (level * 80L)
            }
        }

        return enchantTotal
    }
}
