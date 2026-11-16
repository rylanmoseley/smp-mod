package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Manages server bulk store routing for standard stackable items dropped on death,
 * dynamic baseline market rates, and bulk liquidation/purchasing to feed server commerce.
 */
object BulkStoreManager {

    private const val TARGET_STOCK_BASELINE: Double = 1000.0

    // Item ID -> Stock Count in server bulk reserve
    private val inventory = ConcurrentHashMap<String, Long>()

    /**
     * Whitelist of designated high-value stackable exceptions that MUST route
     * to the UUID Buyback Ledger rather than being liquidated into the bulk store.
     */
    private val HIGH_VALUE_EXCEPTIONS = setOf(
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
        Items.TRIAL_KEY,
        Items.OMINOUS_TRIAL_KEY
    )

    /**
     * Baseline market valuations for common stackable items.
     */
    private val BASELINE_RATES = mapOf(
        // Common construction & mundane items
        "minecraft:cobblestone" to 1L,
        "minecraft:dirt" to 1L,
        "minecraft:gravel" to 1L,
        "minecraft:sand" to 1L,
        "minecraft:netherrack" to 1L,
        "minecraft:granite" to 2L,
        "minecraft:diorite" to 2L,
        "minecraft:andesite" to 2L,
        "minecraft:deepslate" to 2L,
        "minecraft:tuff" to 2L,

        // Common mob drops & consumables
        "minecraft:arrow" to 2L,
        "minecraft:bone" to 2L,
        "minecraft:string" to 2L,
        "minecraft:rotten_flesh" to 1L,
        "minecraft:spider_eye" to 2L,
        "minecraft:gunpowder" to 3L,
        "minecraft:feather" to 2L,
        "minecraft:leather" to 3L,
        "minecraft:slime_ball" to 4L,

        // Crops & provisions
        "minecraft:wheat" to 2L,
        "minecraft:carrot" to 2L,
        "minecraft:potato" to 2L,
        "minecraft:beetroot" to 2L,
        "minecraft:bread" to 3L,

        // Common minerals & fuels
        "minecraft:coal" to 3L,
        "minecraft:charcoal" to 2L,
        "minecraft:raw_copper" to 2L,
        "minecraft:copper_ingot" to 3L,
        "minecraft:raw_iron" to 4L,
        "minecraft:iron_ingot" to 6L,
        "minecraft:redstone" to 4L,
        "minecraft:lapis_lazuli" to 4L,
        "minecraft:raw_gold" to 6L,
        "minecraft:gold_ingot" to 8L,
        "minecraft:blaze_rod" to 6L,
        "minecraft:ender_pearl" to 6L
    )

    /**
     * Determines whether an item should route to the bulk store or the unique UUID buyback ledger.
     * Named, enchanted, damageable tools, armor, weapons, and rare catalysts route to Buybacks.
     * Standard mundane stackables route to Bulk Store.
     */
    fun isBulkStoreCandidate(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.isEnchanted) return false
        if (stack.has(DataComponents.CUSTOM_NAME)) return false
        if (stack.isDamageableItem) return false
        if (HIGH_VALUE_EXCEPTIONS.contains(stack.item)) return false
        return stack.maxStackSize > 1
    }

    /**
     * Deposits standard stackables into the bulk store (e.g. from player death).
     */
    fun deposit(stack: ItemStack) {
        if (stack.isEmpty) return
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        inventory.compute(itemId) { _, current -> (current ?: 0L) + stack.count }
    }

    /**
     * Directly adjusts stock count for a given item ID.
     */
    fun addStock(itemId: String, amount: Long) {
        if (amount <= 0) return
        inventory.compute(itemId) { _, current -> (current ?: 0L) + amount }
    }

    /**
     * Returns current reserve stock for the given item.
     */
    fun getStock(itemId: String): Long {
        return inventory[itemId] ?: 0L
    }

    /**
     * Returns baseline market value for an item.
     */
    fun getBasePrice(itemId: String): Long {
        return BASELINE_RATES[itemId] ?: 1L
    }

    /**
     * Calculates the dynamic buy price (server selling to player) based on current reserve stock.
     * High stock reduces price; scarcity elevates price.
     */
    fun getDynamicBuyPrice(itemId: String): Long {
        val base = getBasePrice(itemId)
        val currentStock = getStock(itemId).toDouble()
        val stockRatio = currentStock / TARGET_STOCK_BASELINE

        // Price multiplier scales from 2.0x (empty stock) down to 0.70x (abundant stock)
        val multiplier = 0.70 + (1.30 / (1.0 + (stockRatio * 0.8)))
        return max(1L, (base * multiplier).toLong())
    }

    /**
     * Calculates dynamic liquidation payout (player selling to server).
     * High stock lowers liquidation payouts; low stock increases liquidation payouts.
     */
    fun getDynamicLiquidationPayout(itemId: String): Long {
        val base = getBasePrice(itemId)
        val currentStock = getStock(itemId).toDouble()
        val stockRatio = currentStock / TARGET_STOCK_BASELINE

        // Liquidation multiplier scales from 0.80x (scarce) down to 0.25x (oversaturated)
        val multiplier = 0.25 + (0.55 / (1.0 + (stockRatio * 1.2)))
        return max(1L, (base * multiplier).toLong())
    }

    /**
     * Liquidates bulk stackable goods from player inventory into the server bulk store.
     * Incorporates Support role efficiency bonus (+25% bonus payout).
     */
    fun liquidate(player: ServerPlayer, stack: ItemStack): LiquidationResult {
        if (stack.isEmpty) {
            return LiquidationResult.Failure("Cannot liquidate an empty item stack")
        }

        if (!isBulkStoreCandidate(stack)) {
            return LiquidationResult.Failure("Item is unique, enchanted, damaged, or a high-value catalyst; cannot bulk liquidate")
        }

        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        val unitPayout = getDynamicLiquidationPayout(itemId)
        val count = stack.count

        // Support role efficiency bonus
        val supportAttr = player.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)?.value ?: 1.0
        val supportBonusMultiplier = ((supportAttr - 1.0) * 0.25).coerceIn(0.0, 0.50)

        val baseTotalPayout = unitPayout * count
        val supportBonus = (baseTotalPayout * supportBonusMultiplier).toLong()
        val finalPayout = baseTotalPayout + supportBonus

        val creditResult = EconomyManager.credit(
            player,
            finalPayout,
            TransactionType.BULK_LIQUIDATION,
            "Bulk Liquidation: $count x $itemId"
        )

        // Transfer into bulk inventory
        addStock(itemId, count.toLong())

        // Remove item from player
        stack.shrink(count)

        player.sendSystemMessage(
            Component.literal("§6[Bulk Store] §fLiquidated $count §e${stack.hoverName.string}§f for §a+$finalPayout coins§f (unit: $unitPayout, Support bonus: +$supportBonus).")
        )

        val updatedBalance = if (creditResult is TransactionResult.Success) creditResult.record.balanceAfter else EconomyManager.getBalance(player)

        return LiquidationResult.Success(
            itemId = itemId,
            count = count,
            unitPayout = unitPayout,
            totalPayout = finalPayout,
            supportBonusAmount = supportBonus,
            updatedBalance = updatedBalance
        )
    }

    /**
     * Purchases bulk items from the server store.
     */
    fun purchaseBulk(player: ServerPlayer, itemId: String, count: Int): BulkPurchaseResult {
        if (count <= 0) {
            return BulkPurchaseResult.Failure("Purchase quantity must be positive")
        }

        val currentStock = getStock(itemId)
        if (currentStock < count) {
            return BulkPurchaseResult.Failure("Insufficient server stock (available: $currentStock, requested: $count)")
        }

        val itemResource = ResourceLocation.tryParse(itemId)
            ?: return BulkPurchaseResult.Failure("Invalid item identifier: $itemId")
        val item = BuiltInRegistries.ITEM.get(itemResource)
        if (item == Items.AIR) {
            return BulkPurchaseResult.Failure("Unknown item: $itemId")
        }

        val unitPrice = getDynamicBuyPrice(itemId)
        val rawTotal = unitPrice * count
        val gathererDiscount = ShopUnlockManager.getGathererDiscountRatio(player)
        val totalCost = (rawTotal * (1.0 - gathererDiscount)).toLong().coerceAtLeast(1L)

        val debitResult = EconomyManager.debit(
            player,
            totalCost,
            TransactionType.BULK_PURCHASE,
            "Bulk Purchase: $count x $itemId"
        )

        if (debitResult !is TransactionResult.Success) {
            return BulkPurchaseResult.Failure(
                "Insufficient funds (requires $totalCost coins, current: ${EconomyManager.getBalance(player)})"
            )
        }

        // Deduct from bulk stock
        inventory.compute(itemId) { _, curr -> (curr ?: 0L) - count }

        // Distribute purchased items to player
        var remainingToGive = count
        while (remainingToGive > 0) {
            val batch = remainingToGive.coerceAtMost(item.defaultInstance.maxStackSize)
            val stack = ItemStack(item, batch)
            if (!player.inventory.add(stack)) {
                player.drop(stack, false)
            }
            remainingToGive -= batch
        }

        player.sendSystemMessage(
            Component.literal("§a[Bulk Store] §fPurchased $count §e${item.description.string}§f for §6$totalCost coins§f.")
        )

        return BulkPurchaseResult.Success(
            itemId = itemId,
            count = count,
            unitPrice = unitPrice,
            totalCost = totalCost,
            remainingBalance = debitResult.newBalance
        )
    }

    /**
     * Snapshot of the entire bulk inventory for UI / admin inspection.
     */
    fun getAllStock(): Map<String, Long> = inventory.toMap()
}
