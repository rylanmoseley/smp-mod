package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.recovery.SafeRecoveryManager
import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

/**
 * Artificing & Recovery progression advancements.
 * Covers masterwork equipment, provenance lore, safe buybacks, alchemical catalysts,
 * public auctions, and heirloom lineage tracking across Novice to Legendary tiers.
 */
object ArtificingRecoveryAdvancements {

    val ROOT: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/root")
    val FIRST_CRAFT_PROVENANCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/first_craft_provenance")
    val MASTERWORK_FORGE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/masterwork_forge")
    val SAFE_BUYBACK: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/safe_buyback")
    val CATALYTIC_REFINEMENT: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/catalytic_refinement")
    val PROVENANCE_HEIRLOOM: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/provenance_heirloom")
    val PUBLIC_AUCTIONEER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/public_auctioneer")
    val PARAGON_ARTIFICER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "artificing/paragon_artificer")

    val REWARDS: List<AdvancementReward> = listOf(
        // 1. Root Tab (Novice)
        AdvancementReward(
            advancementId = ROOT,
            category = AdvancementCategory.ARTIFICING,
            title = "Artificing & Provenance",
            description = "Forge masterwork gear, refine recovery catalysts, and build a lasting lineage.",
            tier = AdvancementTier.NOVICE,
            currencyPayout = 0L,
            combatXp = 0.0,
            builderXp = 0.0,
            gathererXp = 0.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),

        // 2. Maker's Mark (Novice)
        // Reward: +75 coins, +25 Builder XP, +25 Gatherer XP
        AdvancementReward(
            advancementId = FIRST_CRAFT_PROVENANCE,
            category = AdvancementCategory.ARTIFICING,
            title = "Maker's Mark",
            description = "Craft your first unique gear with permanent maker lore.",
            tier = AdvancementTier.NOVICE,
            currencyPayout = 75L,
            combatXp = 0.0,
            builderXp = 25.0,
            gathererXp = 25.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),

        // 3. Masterwork Forging (Journeyman)
        // Reward: +150 coins, +50 Builder XP
        AdvancementReward(
            advancementId = MASTERWORK_FORGE,
            category = AdvancementCategory.ARTIFICING,
            title = "Masterwork Forging",
            description = "Forge or loot an item with a Masterwork Affix such as Keen, Sturdy, Titanic, etc.",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 150L,
            combatXp = 0.0,
            builderXp = 50.0,
            gathererXp = 0.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),

        // 4. Prudent Recovery (Journeyman)
        // Reward: +100 coins, +1 Recovery Catalyst
        AdvancementReward(
            advancementId = SAFE_BUYBACK,
            category = AdvancementCategory.ARTIFICING,
            title = "Prudent Recovery",
            description = "Safely recover lost equipment from a town Buyback Terminal.",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 100L,
            combatXp = 0.0,
            builderXp = 0.0,
            gathererXp = 0.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 1,
            broadcastServer = false
        ),

        // 5. Alchemical Catalyst (Journeyman)
        // Reward: +200 coins, +75 Gatherer XP, +1 Recovery Catalyst
        AdvancementReward(
            advancementId = CATALYTIC_REFINEMENT,
            category = AdvancementCategory.ARTIFICING,
            title = "Alchemical Catalyst",
            description = "Craft and utilize a Recovery Catalyst to discount buyback fees.",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 200L,
            combatXp = 0.0,
            builderXp = 0.0,
            gathererXp = 75.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 1,
            broadcastServer = false
        ),

        // 6. Living Provenance (Master)
        // Reward: +350 coins, +100 Builder XP, +2 Recovery Catalysts
        AdvancementReward(
            advancementId = PROVENANCE_HEIRLOOM,
            category = AdvancementCategory.ARTIFICING,
            title = "Living Provenance",
            description = "Own or trade an heirloom item with 3+ historical ownership transitions.",
            tier = AdvancementTier.MASTER,
            currencyPayout = 350L,
            combatXp = 0.0,
            builderXp = 100.0,
            gathererXp = 0.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 2,
            broadcastServer = false
        ),

        // 7. Grand Appraiser (Master)
        // Reward: +300 coins, +100 Gatherer XP, +1 Recovery Catalyst
        AdvancementReward(
            advancementId = PUBLIC_AUCTIONEER,
            category = AdvancementCategory.ARTIFICING,
            title = "Grand Appraiser",
            description = "Purchase an unclaimed public auction item past the 24h window.",
            tier = AdvancementTier.MASTER,
            currencyPayout = 300L,
            combatXp = 0.0,
            builderXp = 0.0,
            gathererXp = 100.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 1,
            broadcastServer = false
        ),

        // 8. Paragon of the Forge (Legendary)
        // Reward: +1000 coins, +300 Builder XP, +3 Recovery Catalysts, broadcastServer = true
        AdvancementReward(
            advancementId = PARAGON_ARTIFICER,
            category = AdvancementCategory.ARTIFICING,
            title = "Paragon of the Forge",
            description = "Forge a full set of masterwork equipment with pristine provenance.",
            tier = AdvancementTier.LEGENDARY,
            currencyPayout = 1000L,
            combatXp = 0.0,
            builderXp = 300.0,
            gathererXp = 0.0,
            explorerXp = 0.0,
            recoveryCatalystCount = 3,
            broadcastServer = true
        )
    )

    /**
     * Registers all Artificing & Recovery advancement rewards.
     */
    fun register() {
        REWARDS.forEach { AdvancementRewardManager.register(it) }
    }

    /**
     * Programmatically grants an advancement in the artificing tree.
     */
    fun grant(player: ServerPlayer, advancementPath: String) {
        val resLoc = if (advancementPath.contains(":")) {
            ResourceLocation.parse(advancementPath)
        } else {
            ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, advancementPath)
        }
        val holder = player.server.advancements.get(resLoc)
        if (holder != null) {
            val progress = player.advancements.getOrStartProgress(holder)
            if (!progress.isDone) {
                for (criterion in progress.remainingCriteria) {
                    player.advancements.award(holder, criterion)
                }
            }
        } else {
            // Direct fallback when running in tests or before advancement tree is loaded
            AdvancementRewardManager.onAdvancementEarned(player, resLoc)
        }
    }

    fun awardFirstCraft(player: ServerPlayer) {
        grant(player, "artificing/first_craft_provenance")
    }

    fun awardMasterworkForge(player: ServerPlayer) {
        grant(player, "artificing/masterwork_forge")
    }

    fun awardSafeBuyback(player: ServerPlayer) {
        grant(player, "artificing/safe_buyback")
    }

    fun awardCatalyticRefinement(player: ServerPlayer) {
        grant(player, "artificing/catalytic_refinement")
    }

    fun awardProvenanceHeirloom(player: ServerPlayer) {
        grant(player, "artificing/provenance_heirloom")
    }

    fun awardPublicAuctioneer(player: ServerPlayer) {
        grant(player, "artificing/public_auctioneer")
    }

    fun awardParagonArtificer(player: ServerPlayer) {
        grant(player, "artificing/paragon_artificer")
    }

    /**
     * Evaluates if a player is equipped with a complete set of masterwork equipment.
     */
    fun checkParagonArtificer(player: ServerPlayer) {
        val armor = listOf(
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
        )
        val allMasterwork = armor.all { stack ->
            !stack.isEmpty && stack.get(ModDataComponents.MASTERWORK_AFFIX.get()) != null
        }
        if (allMasterwork) {
            awardParagonArtificer(player)
        }
    }

    /**
     * Handles item crafted events for Maker's Mark and Masterwork Forging.
     */
    fun onItemCrafted(player: ServerPlayer, stack: ItemStack) {
        awardFirstCraft(player)
        if (stack.get(ModDataComponents.MASTERWORK_AFFIX.get()) != null) {
            awardMasterworkForge(player)
        }
        checkParagonArtificer(player)
    }

    /**
     * Handles item looted events from deep dungeons or structures.
     */
    fun onItemLooted(player: ServerPlayer, stack: ItemStack) {
        if (stack.get(ModDataComponents.MASTERWORK_AFFIX.get()) != null) {
            awardMasterworkForge(player)
        }
        checkParagonArtificer(player)
    }

    /**
     * Handles buyback reclamation for Prudent Recovery or Grand Appraiser.
     */
    fun onBuybackReclaimed(player: ServerPlayer, wasPublic: Boolean, claimSubsidySaved: Long) {
        if (wasPublic) {
            awardPublicAuctioneer(player)
        } else {
            val safety = SafeRecoveryManager.evaluateSafety(player)
            if (safety.isSafeZone || claimSubsidySaved > 0) {
                awardSafeBuyback(player)
            }
        }
    }

    /**
     * Handles catalyst utilization for Alchemical Catalyst.
     */
    fun onCatalystApplied(player: ServerPlayer) {
        awardCatalyticRefinement(player)
    }

    /**
     * Handles ownership transfers for Living Provenance.
     */
    fun onOwnershipTransferred(player: ServerPlayer, stack: ItemStack) {
        val history = stack.get(ModDataComponents.PROVENANCE_LORE.get()) ?: emptyList()
        val transferCount = history.count { it.startsWith("Transferred:") }
        if (transferCount >= 3) {
            awardProvenanceHeirloom(player)
        }
    }
}
