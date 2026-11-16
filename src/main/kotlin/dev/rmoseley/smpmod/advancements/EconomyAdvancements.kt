package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Economy & Commerce Progression Advancements.
 * Defines tiered rewards spanning Novice, Journeyman, Master, and Legendary tiers
 * for Frontier Commerce, rewarding active, non-AFK economic engagement.
 */
object EconomyAdvancements {

    val ROOT = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/root"),
        category = AdvancementCategory.ECONOMY,
        title = "Frontier Commerce",
        description = "Establish trade routes, digital finance, and active market ventures in the frontier.",
        tier = AdvancementTier.NOVICE,
        currencyPayout = 0L
    )

    val FIRST_HARVEST = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/first_harvest"),
        category = AdvancementCategory.ECONOMY,
        title = "Pioneer Prospector",
        description = "Naturally harvest raw survival resources in the wild without laundering placed blocks.",
        tier = AdvancementTier.NOVICE,
        currencyPayout = 50L,
        gathererXp = 25.0
    )

    val FIRST_P2P_TRADE = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/first_p2p_trade"),
        category = AdvancementCategory.ECONOMY,
        title = "Mercantile Handshake",
        description = "Complete your first peer-to-peer digital transaction or player trade.",
        tier = AdvancementTier.NOVICE,
        currencyPayout = 75L,
        gathererXp = 30.0
    )

    val BULK_STORE_SUPPLIER = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/bulk_store_supplier"),
        category = AdvancementCategory.ECONOMY,
        title = "Municipal Supplier",
        description = "Liquidate naturally gathered resources into the central server bulk store to fuel civic reserves.",
        tier = AdvancementTier.JOURNEYMAN,
        currencyPayout = 150L,
        gathererXp = 60.0
    )

    val CONTRACT_COMPLETE = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/contract_complete"),
        category = AdvancementCategory.ECONOMY,
        title = "Mercenary Surveyor",
        description = "Fulfill all objectives for a Community Bounty Board contract and claim your bounty.",
        tier = AdvancementTier.JOURNEYMAN,
        currencyPayout = 200L,
        gathererXp = 75.0
    )

    val BOUNTY_VETERAN = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/bounty_veteran"),
        category = AdvancementCategory.ECONOMY,
        title = "Contract Syndicate",
        description = "Prove professional reliability by fulfilling 5 community bounty board contracts.",
        tier = AdvancementTier.JOURNEYMAN,
        currencyPayout = 200L,
        gathererXp = 80.0,
        recoveryCatalystCount = 1
    )

    val PUBLIC_CONSIGNOR = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/public_consignor"),
        category = AdvancementCategory.ECONOMY,
        title = "Liquidity Provider",
        description = "Consign or purchase an unclaimed salvage item on the public 24-hour buyback market.",
        tier = AdvancementTier.JOURNEYMAN,
        currencyPayout = 150L,
        gathererXp = 60.0
    )

    val CONTRACT_MASTER = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/contract_master"),
        category = AdvancementCategory.ECONOMY,
        title = "Guild Commissioner",
        description = "Establish supreme reliability by completing 10 community bounty contracts.",
        tier = AdvancementTier.MASTER,
        currencyPayout = 500L,
        gathererXp = 150.0,
        broadcastServer = true
    )

    val DEEP_PROSPECTOR = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/deep_prospector"),
        category = AdvancementCategory.ECONOMY,
        title = "Mineral Vein Monopolist",
        description = "Naturally prospect and extract 20+ unplaced deepslate ore veins below Y: 0 in deep caverns.",
        tier = AdvancementTier.MASTER,
        currencyPayout = 400L,
        gathererXp = 150.0,
        recoveryCatalystCount = 1
    )

    val TOWN_PATRON = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/town_patron"),
        category = AdvancementCategory.ECONOMY,
        title = "Civic Benefactor",
        description = "Contribute to civic prosperity by donating 500 or more coins to a municipal town treasury.",
        tier = AdvancementTier.MASTER,
        currencyPayout = 250L,
        builderXp = 100.0,
        recoveryCatalystCount = 1
    )

    val CIVIC_PHILANTHROPIST = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/civic_philanthropist"),
        category = AdvancementCategory.ECONOMY,
        title = "Grand Civic Benefactor",
        description = "Endow communal infrastructure by investing 1,500+ cumulative coins into town treasuries.",
        tier = AdvancementTier.MASTER,
        currencyPayout = 450L,
        builderXp = 180.0,
        recoveryCatalystCount = 2,
        broadcastServer = true
    )

    val WEALTH_TYCOON = AdvancementReward(
        advancementId = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "economy/wealth_tycoon"),
        category = AdvancementCategory.ECONOMY,
        title = "Sovereign Merchant",
        description = "Accumulate an active fortune of 5,000 or more digital coins in your off-inventory balance.",
        tier = AdvancementTier.LEGENDARY,
        currencyPayout = 1000L,
        gathererXp = 250.0,
        recoveryCatalystCount = 3,
        broadcastServer = true
    )

    private val playerContractCompletions = ConcurrentHashMap<UUID, AtomicInteger>()
    private val playerDeepOresHarvested = ConcurrentHashMap<UUID, AtomicInteger>()
    private val playerCumulativeDonations = ConcurrentHashMap<UUID, AtomicInteger>()

    /**
     * Registers all economy rewards with AdvancementRewardManager.
     */
    fun register() {
        AdvancementRewardManager.register(ROOT)
        AdvancementRewardManager.register(FIRST_HARVEST)
        AdvancementRewardManager.register(FIRST_P2P_TRADE)
        AdvancementRewardManager.register(BULK_STORE_SUPPLIER)
        AdvancementRewardManager.register(CONTRACT_COMPLETE)
        AdvancementRewardManager.register(BOUNTY_VETERAN)
        AdvancementRewardManager.register(PUBLIC_CONSIGNOR)
        AdvancementRewardManager.register(CONTRACT_MASTER)
        AdvancementRewardManager.register(DEEP_PROSPECTOR)
        AdvancementRewardManager.register(TOWN_PATRON)
        AdvancementRewardManager.register(CIVIC_PHILANTHROPIST)
        AdvancementRewardManager.register(WEALTH_TYCOON)
    }

    /**
     * Programmatically grants an advancement in the economy tree.
     */
    fun grant(player: ServerPlayer, advancementPath: String) {
        val id = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, advancementPath)
        val holder = player.server.advancements.get(id) ?: return
        val progress = player.advancements.getOrStartProgress(holder)
        if (!progress.isDone) {
            for (criterion in progress.remainingCriteria) {
                player.advancements.award(holder, criterion)
            }
        }
    }

    fun awardFirstHarvest(player: ServerPlayer) {
        grant(player, "economy/first_harvest")
    }

    fun awardFirstP2PTrade(player: ServerPlayer) {
        grant(player, "economy/first_p2p_trade")
    }

    fun awardBulkStoreSupplier(player: ServerPlayer) {
        grant(player, "economy/bulk_store_supplier")
    }

    fun awardContractClaim(player: ServerPlayer) {
        grant(player, "economy/contract_complete")
        val count = playerContractCompletions.computeIfAbsent(player.uuid) { AtomicInteger(0) }.incrementAndGet()
        if (count >= 5) {
            grant(player, "economy/bounty_veteran")
        }
        if (count >= 10) {
            grant(player, "economy/contract_master")
        }
    }

    fun awardPublicMarketTrade(player: ServerPlayer) {
        grant(player, "economy/public_consignor")
    }

    fun recordDeepOreHarvest(player: ServerPlayer) {
        val total = playerDeepOresHarvested.computeIfAbsent(player.uuid) { AtomicInteger(0) }.incrementAndGet()
        if (total >= 20) {
            grant(player, "economy/deep_prospector")
        }
    }

    fun awardTownDonation(player: ServerPlayer, amount: Long) {
        if (amount >= 500L) {
            grant(player, "economy/town_patron")
        }
        val cumulative = playerCumulativeDonations.computeIfAbsent(player.uuid) { AtomicInteger(0) }.addAndGet(amount.toInt())
        if (cumulative >= 1500) {
            grant(player, "economy/civic_philanthropist")
        }
    }

    fun checkWealthTycoon(player: ServerPlayer, currentBalance: Long) {
        if (currentBalance >= 5000L) {
            grant(player, "economy/wealth_tycoon")
        }
    }
}
