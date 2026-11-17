package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.claims.CivicUpgrade
import dev.rmoseley.smpmod.claims.ContiguousCluster
import dev.rmoseley.smpmod.claims.SettlementTier
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Progression advancements and rewards for the Settlement & Architecture track.
 * Encompasses frontier land claiming, contiguous settlement clustering, architectural variety,
 * cadence flight mastery, fortified town status, municipal civic upgrades, and regional metropolis scale.
 */
object SettlementAdvancements {

    val ROOT: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/root")
    val FIRST_HOMESTEAD: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/first_homestead")
    val CONTIGUOUS_HAMLET: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/contiguous_hamlet")
    val PALETTE_DIVERSITY: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/palette_diversity")
    val BUILDER_FLIGHT_CADENCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/builder_flight_cadence")
    val HIGHWAY_ENGINEER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/highway_engineer")
    val COOPERATIVE_CLUSTER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/cooperative_cluster")
    val TOWN_STATUS: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/town_status")
    val PALETTE_VIRTUOSO: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/palette_virtuoso")
    val CIVIC_RENAISSANCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/civic_renaissance")
    val WARDED_BASTION: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/warded_bastion")
    val GRAND_METROPOLIS: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/grand_metropolis")

    val REWARDS: List<AdvancementReward> = listOf(
        AdvancementReward(
            advancementId = ROOT,
            category = AdvancementCategory.SETTLEMENT,
            title = "Towns & Architecture",
            description = "Root tab for land claims, contiguous settlements, and architectural wonders",
            tier = AdvancementTier.NOVICE,
            currencyPayout = 0L,
            builderXp = 0.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = FIRST_HOMESTEAD,
            category = AdvancementCategory.SETTLEMENT,
            title = "Claiming the Frontier",
            description = "Stake your claim on the wilderness by claiming your very first chunk",
            tier = AdvancementTier.NOVICE,
            currencyPayout = 100L,
            builderXp = 40.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = CONTIGUOUS_HAMLET,
            category = AdvancementCategory.SETTLEMENT,
            title = "Hamlet Emergence",
            description = "Establish a contiguous settlement cluster of 4 to 8 claimed chunks",
            tier = AdvancementTier.NOVICE,
            currencyPayout = 100L,
            builderXp = 50.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = PALETTE_DIVERSITY,
            category = AdvancementCategory.SETTLEMENT,
            title = "Master Mason",
            description = "Construct structures utilizing 5 or more distinct structural block palettes in a cluster",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 150L,
            builderXp = 80.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = BUILDER_FLIGHT_CADENCE,
            category = AdvancementCategory.SETTLEMENT,
            title = "Aerial Architect",
            description = "Maintain continuous cadence-based builder flight for over 60 seconds",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 200L,
            builderXp = 100.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = HIGHWAY_ENGINEER,
            category = AdvancementCategory.SETTLEMENT,
            title = "Artery of Civilization",
            description = "Construct or lay down 64+ paved road or rail blocks connecting claimed settlement territory",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 200L,
            builderXp = 80.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = COOPERATIVE_CLUSTER,
            category = AdvancementCategory.SETTLEMENT,
            title = "Commonwealth Assembly",
            description = "Establish a contiguous settlement cluster shared and co-developed by 3 or more town residents",
            tier = AdvancementTier.JOURNEYMAN,
            currencyPayout = 200L,
            builderXp = 90.0,
            recoveryCatalystCount = 0,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = TOWN_STATUS,
            category = AdvancementCategory.SETTLEMENT,
            title = "Fortified Town",
            description = "Expand a contiguous settlement to 19 or more chunks, reaching Town tier",
            tier = AdvancementTier.MASTER,
            currencyPayout = 400L,
            builderXp = 150.0,
            recoveryCatalystCount = 1,
            broadcastServer = true
        ),
        AdvancementReward(
            advancementId = PALETTE_VIRTUOSO,
            category = AdvancementCategory.SETTLEMENT,
            title = "Architectural Polymath",
            description = "Construct a contiguous settlement cluster utilizing 12 or more unique structural block palettes",
            tier = AdvancementTier.MASTER,
            currencyPayout = 350L,
            builderXp = 150.0,
            recoveryCatalystCount = 1,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = CIVIC_RENAISSANCE,
            category = AdvancementCategory.SETTLEMENT,
            title = "Municipal Renaissance",
            description = "Unlock 2 or more civic upgrades such as Paved Highways or Warded Streetlights",
            tier = AdvancementTier.MASTER,
            currencyPayout = 500L,
            builderXp = 200.0,
            recoveryCatalystCount = 2,
            broadcastServer = false
        ),
        AdvancementReward(
            advancementId = WARDED_BASTION,
            category = AdvancementCategory.SETTLEMENT,
            title = "Fortress of Sanctuary",
            description = "Fortify a town settlement cluster with both Warded Streetlights and Municipal Quarantine civic upgrades",
            tier = AdvancementTier.MASTER,
            currencyPayout = 400L,
            builderXp = 160.0,
            recoveryCatalystCount = 1,
            broadcastServer = true
        ),
        AdvancementReward(
            advancementId = GRAND_METROPOLIS,
            category = AdvancementCategory.SETTLEMENT,
            title = "Regional Metropolis",
            description = "Expand a contiguous settlement to 37 or more chunks reaching Metropolis tier with rich palette variety",
            tier = AdvancementTier.LEGENDARY,
            currencyPayout = 1000L,
            builderXp = 350.0,
            recoveryCatalystCount = 3,
            broadcastServer = true
        )
    )

    private val playerRoadBlocksPlaced = java.util.concurrent.ConcurrentHashMap<java.util.UUID, java.util.concurrent.atomic.AtomicInteger>()

    fun register() {
        REWARDS.forEach { AdvancementRewardManager.register(it) }
    }

    /**
     * Helper to award a settlement advancement to a player, triggering its criteria completion.
     */
    fun award(player: ServerPlayer, advancementId: ResourceLocation) {
        val advancementHolder = player.server.advancements.get(advancementId) ?: return
        val progress = player.advancements.getOrStartProgress(advancementHolder)
        if (!progress.isDone) {
            for (criterion in progress.remainingCriteria) {
                player.advancements.award(advancementHolder, criterion)
            }
        }
    }

    /**
     * Checks claim topology and contiguous clusters for advancement triggers.
     */
    fun checkClaimMilestones(player: ServerPlayer) {
        val level = player.serverLevel()
        val totalClaims = ClaimManager.provider.getAllClaimedChunks(level, player.uuid)
        if (totalClaims.isNotEmpty()) {
            award(player, FIRST_HOMESTEAD)
        }

        val clusters = ClaimManager.getAllContiguousClusters(level, player.uuid)
        for (cluster in clusters) {
            checkClusterProgression(player, cluster)
        }
    }

    /**
     * Checks a specific contiguous cluster's tier and variety for advancement triggers.
     */
    fun checkClusterProgression(player: ServerPlayer, cluster: ContiguousCluster) {
        val size = cluster.chunks.size
        if (size >= SettlementTier.HAMLET.minChunks) {
            award(player, CONTIGUOUS_HAMLET)
        }
        if (cluster.blockCountsByType.size >= 5) {
            award(player, PALETTE_DIVERSITY)
        }
        if (cluster.blockCountsByType.size >= 12) {
            award(player, PALETTE_VIRTUOSO)
        }
        if (size >= SettlementTier.TOWN.minChunks) {
            award(player, TOWN_STATUS)
        }
        if (size >= SettlementTier.METROPOLIS.minChunks && cluster.blockCountsByType.size >= 5) {
            award(player, GRAND_METROPOLIS)
        }
        val residentCount = cluster.members.size + 1
        if (residentCount >= 3) {
            award(player, COOPERATIVE_CLUSTER)
        }
    }

    /**
     * Evaluates palette diversity when blocks are placed.
     */
    fun checkPaletteDiversity(player: ServerPlayer, uniquePaletteCount: Int) {
        if (uniquePaletteCount >= 5) {
            award(player, PALETTE_DIVERSITY)
        }
        if (uniquePaletteCount >= 12) {
            award(player, PALETTE_VIRTUOSO)
        }
    }

    /**
     * Evaluates builder flight cadence duration.
     */
    fun checkFlightCadence(player: ServerPlayer, continuousFlightSeconds: Double) {
        if (continuousFlightSeconds >= 60.0) {
            award(player, BUILDER_FLIGHT_CADENCE)
        }
    }

    /**
     * Evaluates active civic upgrade milestones.
     */
    fun checkCivicUpgrades(player: ServerPlayer, activeUpgradeCount: Int) {
        if (activeUpgradeCount >= 2) {
            award(player, CIVIC_RENAISSANCE)
        }
    }

    fun checkHighwayBlockPlaced(player: ServerPlayer) {
        val total = playerRoadBlocksPlaced.computeIfAbsent(player.uuid) { java.util.concurrent.atomic.AtomicInteger(0) }.incrementAndGet()
        if (total >= 64) {
            award(player, HIGHWAY_ENGINEER)
        }
    }

    fun checkWardedBastion(player: ServerPlayer, activeUpgrades: Set<CivicUpgrade>) {
        if (activeUpgrades.contains(CivicUpgrade.WARDED_STREETLIGHTS) && activeUpgrades.contains(CivicUpgrade.MUNICIPAL_QUARANTINE)) {
            award(player, WARDED_BASTION)
        }
    }
}
