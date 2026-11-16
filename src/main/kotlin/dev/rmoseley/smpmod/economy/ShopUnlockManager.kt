package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * Manages per-block natural harvest shop unlocks, discount tiers,
 * whitelisted non-precious building catalogs, and purchase transactions.
 *
 * Implements anti-laundering guardrails and cross-subsystem synergies:
 * - Contiguous town claim development discounts (up to 20% via SettlementTier).
 * - Repurposed village objective hub discounts.
 * - Support role efficiency discounts.
 */
object ShopUnlockManager {

    // Default harvest threshold to unlock purchasing non-precious building blocks
    const val DEFAULT_UNLOCK_THRESHOLD: Int = 100

    /**
     * Catalog of approved non-precious building materials with base unit prices.
     * High-tier equipment, ores, rare catalysts, and precious blocks are strictly excluded.
     */
    private val BUILDING_BLOCK_CATALOG: Map<Block, Long> = mapOf(
        // Stone & Deepslate Variants
        Blocks.STONE to 2L,
        Blocks.COBBLESTONE to 1L,
        Blocks.SMOOTH_STONE to 2L,
        Blocks.STONE_BRICKS to 3L,
        Blocks.MOSSY_STONE_BRICKS to 3L,
        Blocks.ANDESITE to 2L,
        Blocks.POLISHED_ANDESITE to 3L,
        Blocks.DIORITE to 2L,
        Blocks.POLISHED_DIORITE to 3L,
        Blocks.GRANITE to 2L,
        Blocks.POLISHED_GRANITE to 3L,
        Blocks.DEEPSLATE to 2L,
        Blocks.COBBLED_DEEPSLATE to 1L,
        Blocks.POLISHED_DEEPSLATE to 3L,
        Blocks.DEEPSLATE_BRICKS to 3L,
        Blocks.DEEPSLATE_TILES to 3L,
        Blocks.CHISELED_DEEPSLATE to 4L,
        Blocks.TUFF to 2L,
        Blocks.POLISHED_TUFF to 3L,
        Blocks.TUFF_BRICKS to 3L,

        // Sandstones & Bricks
        Blocks.SANDSTONE to 2L,
        Blocks.SMOOTH_SANDSTONE to 3L,
        Blocks.CUT_SANDSTONE to 3L,
        Blocks.RED_SANDSTONE to 2L,
        Blocks.SMOOTH_RED_SANDSTONE to 3L,
        Blocks.BRICKS to 4L,
        Blocks.MUD_BRICKS to 3L,

        // Timber & Planks
        Blocks.OAK_PLANKS to 2L,
        Blocks.SPRUCE_PLANKS to 2L,
        Blocks.BIRCH_PLANKS to 2L,
        Blocks.JUNGLE_PLANKS to 2L,
        Blocks.ACACIA_PLANKS to 2L,
        Blocks.DARK_OAK_PLANKS to 2L,
        Blocks.MANGROVE_PLANKS to 2L,
        Blocks.CHERRY_PLANKS to 2L,
        Blocks.BAMBOO_PLANKS to 2L,
        Blocks.CRIMSON_PLANKS to 3L,
        Blocks.WARPED_PLANKS to 3L,
        Blocks.OAK_LOG to 5L,
        Blocks.SPRUCE_LOG to 5L,
        Blocks.BIRCH_LOG to 5L,
        Blocks.JUNGLE_LOG to 5L,
        Blocks.ACACIA_LOG to 5L,
        Blocks.DARK_OAK_LOG to 5L,
        Blocks.MANGROVE_LOG to 5L,
        Blocks.CHERRY_LOG to 5L,

        // Architectural, Terracotta & Glass
        Blocks.TERRACOTTA to 3L,
        Blocks.WHITE_TERRACOTTA to 4L,
        Blocks.ORANGE_TERRACOTTA to 4L,
        Blocks.LIGHT_GRAY_TERRACOTTA to 4L,
        Blocks.BROWN_TERRACOTTA to 4L,
        Blocks.RED_TERRACOTTA to 4L,
        Blocks.GLASS to 2L,
        Blocks.TINTED_GLASS to 5L,

        // Dimension / Structural Stones
        Blocks.BLACKSTONE to 2L,
        Blocks.POLISHED_BLACKSTONE to 3L,
        Blocks.POLISHED_BLACKSTONE_BRICKS to 4L,
        Blocks.BASALT to 2L,
        Blocks.SMOOTH_BASALT to 3L,
        Blocks.QUARTZ_BLOCK to 8L,
        Blocks.SMOOTH_QUARTZ to 10L,
        Blocks.PRISMARINE to 6L,
        Blocks.PRISMARINE_BRICKS to 8L,
        Blocks.DARK_PRISMARINE to 10L,
        Blocks.END_STONE to 4L,
        Blocks.END_STONE_BRICKS to 5L,
        Blocks.PURPUR_BLOCK to 6L,
        Blocks.PURPUR_PILLAR to 8L
    )

    /**
     * Strict exclusion check ensuring precious ores and exploit items can never be purchased.
     */
    fun isPreciousOrExcluded(block: Block): Boolean {
        val key = BuiltInRegistries.BLOCK.getKey(block).path
        if (key.contains("ore") || key.contains("raw_")) return true
        if (key.contains("diamond") || key.contains("netherite") || key.contains("ancient_debris")) return true
        if (key.contains("emerald") || key.contains("gold_block") || key.contains("iron_block")) return true
        if (key.contains("spawner") || key.contains("beacon") || key.contains("conduit")) return true
        if (key.contains("command") || key.contains("structure") || key.contains("barrier")) return true
        if (block == Blocks.BEDROCK || block == Blocks.END_PORTAL_FRAME || block == Blocks.DRAGON_EGG) return true
        return false
    }

    /**
     * Records a natural harvest event for a specific block.
     */
    fun recordNaturalHarvest(player: Player, block: Block) {
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        val attachment = ModAttachmentTypes.PLAYER_NATURAL_HARVEST.get()
        val currentData = player.getData(attachment)
        val currentCount = currentData.counts[blockId] ?: 0
        val updatedCounts = currentData.counts.toMutableMap()
        val newCount = currentCount + 1
        updatedCounts[blockId] = newCount
        player.setData(attachment, ModAttachmentTypes.NaturalHarvestData(updatedCounts))

        // Check if unlocking milestone announcement is warranted
        if (newCount == DEFAULT_UNLOCK_THRESHOLD) {
            player.sendSystemMessage(
                Component.literal("§6[Shop Unlock] §fYou have naturally harvested $newCount §e${block.name.string}§f! Now unlocked in the Building Shop.")
            )
        }
    }

    /**
     * Gets the total natural harvest count recorded for a player and block.
     */
    fun getHarvestCount(player: Player, block: Block): Int {
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        val attachment = ModAttachmentTypes.PLAYER_NATURAL_HARVEST.get()
        return player.getData(attachment).counts[blockId] ?: 0
    }

    /**
     * Returns the unlock tier achieved by a player for a specific block.
     */
    fun getUnlockTier(player: Player, block: Block, requiredThreshold: Int = DEFAULT_UNLOCK_THRESHOLD): ShopUnlockTier {
        val count = getHarvestCount(player, block)
        return when {
            count >= (requiredThreshold * ShopUnlockTier.TIER_3_MASTER.requiredMultiplier).toInt() -> ShopUnlockTier.TIER_3_MASTER
            count >= (requiredThreshold * ShopUnlockTier.TIER_2_JOURNEYMAN.requiredMultiplier).toInt() -> ShopUnlockTier.TIER_2_JOURNEYMAN
            count >= requiredThreshold -> ShopUnlockTier.TIER_1_UNLOCKED
            else -> ShopUnlockTier.LOCKED
        }
    }

    /**
     * Checks if a block is unlocked for purchasing by the player.
     */
    fun isBlockUnlocked(player: Player, block: Block, requiredThreshold: Int = DEFAULT_UNLOCK_THRESHOLD): Boolean {
        return getHarvestCount(player, block) >= requiredThreshold
    }

    /**
     * Computes the final unit price for a building block incorporating all cross-system discounts:
     * 1. Natural harvest unlock tier discount (up to 25%)
     * 2. Contiguous town claim development tier discount (up to 20%)
     * 3. Support role efficiency discount (up to 15%)
     * Capped at an aggregate maximum 50% discount to maintain economic balance.
     */
    fun calculateUnitPrice(player: Player, block: Block): Long {
        val basePrice = BUILDING_BLOCK_CATALOG[block] ?: return -1L

        // 1. Natural harvest tier discount
        val tier = getUnlockTier(player, block)
        val harvestDiscount = tier.discountPercent

        // 2. Contiguous claim settlement tier discount
        val level = player.level()
        val chunkPos = ChunkPos(player.blockPosition())
        val cluster = ClaimManager.getContiguousCluster(level, chunkPos)
        val claimDiscount = if (cluster != null && cluster.isMember(player.uuid)) {
            cluster.clusterTier.shopDiscountPercent
        } else {
            0.0
        }

        // 3. Support role affinity discount
        val supportAttr = player.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)?.value ?: 1.0
        val supportDiscount = ((supportAttr - 1.0) * 0.15).coerceIn(0.0, 0.15)

        // 4. Gatherer XP track discount
        val gathererDiscount = getGathererDiscountRatio(player)

        // Aggregate discounts capped at 50%
        val totalDiscount = (harvestDiscount + claimDiscount + supportDiscount + gathererDiscount).coerceAtMost(0.50)
        val finalPrice = (basePrice * (1.0 - totalDiscount)).toLong()
        return finalPrice.coerceAtLeast(1L)
    }

    /**
     * Calculates shop discount ratio derived directly from the player's continuous Gatherer Playstyle XP ratio.
     */
    fun getGathererDiscountRatio(player: Player): Double {
        val xpData = player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())
        val totalXp = xpData.combatXp + xpData.builderXp + xpData.gathererXp + xpData.explorerXp
        if (totalXp <= 0.0) return 0.0
        val gathererRatio = (xpData.gathererXp / totalXp).coerceIn(0.0, 1.0)
        val maxDiscount = dev.rmoseley.smpmod.config.SmpConfig.COMMON.gathererShopMaxDiscount.get()
        return (gathererRatio * maxDiscount).coerceIn(0.0, maxDiscount)
    }

    /**
     * Executes a building material shop purchase.
     */
    fun purchaseBuildingBlock(player: ServerPlayer, block: Block, count: Int): ShopPurchaseResult {
        if (count <= 0) {
            return ShopPurchaseResult.Failure("Purchase quantity must be positive")
        }

        if (isPreciousOrExcluded(block)) {
            return ShopPurchaseResult.Failure("Precious ores, equipment, and rare items cannot be purchased in the building shop")
        }

        if (!BUILDING_BLOCK_CATALOG.containsKey(block)) {
            return ShopPurchaseResult.Failure("${block.name.string} is not registered in the building material catalog")
        }

        if (!isBlockUnlocked(player, block)) {
            val countCurrent = getHarvestCount(player, block)
            return ShopPurchaseResult.Failure(
                "Locked! Naturally harvest ${DEFAULT_UNLOCK_THRESHOLD - countCurrent} more ${block.name.string} to unlock"
            )
        }

        val unitPrice = calculateUnitPrice(player, block)
        val totalCost = unitPrice * count
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()

        val debitResult = EconomyManager.debit(
            player,
            totalCost,
            TransactionType.SHOP_PURCHASE,
            "Building Shop: $count x $blockId"
        )

        if (debitResult !is TransactionResult.Success) {
            return ShopPurchaseResult.Failure(
                "Insufficient funds (requires $totalCost coins, current: ${EconomyManager.getBalance(player)})"
            )
        }

        // Distribute purchased items to player
        var remainingToGive = count
        while (remainingToGive > 0) {
            val batch = remainingToGive.coerceAtMost(64)
            val stack = ItemStack(block.asItem(), batch)
            if (!player.inventory.add(stack)) {
                player.drop(stack, false)
            }
            remainingToGive -= batch
        }

        val basePrice = BUILDING_BLOCK_CATALOG[block] ?: unitPrice
        val discountPercent = if (basePrice > 0) ((basePrice - unitPrice).toDouble() / basePrice) * 100.0 else 0.0

        player.sendSystemMessage(
            Component.literal("§a[Building Shop] §fPurchased $count §e${block.name.string}§f for §6$totalCost coins§f (unit: $unitPrice coins).")
        )

        return ShopPurchaseResult.Success(
            blockId = blockId,
            count = count,
            unitPrice = unitPrice,
            totalCost = totalCost,
            discountPercentApplied = discountPercent,
            remainingBalance = debitResult.newBalance
        )
    }

    /**
     * Returns the list of all available building block options in the catalog.
     */
    fun getCatalog(): Map<Block, Long> = BUILDING_BLOCK_CATALOG
}
