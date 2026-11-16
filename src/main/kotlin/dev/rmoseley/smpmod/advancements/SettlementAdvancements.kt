package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.claims.ContiguousCluster
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
    val TOWN_STATUS: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/town_status")
    val CIVIC_RENAISSANCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "settlement/civic_renaissance")
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
            description = "Establish a contiguous settlement cluster of 2 to 3 claimed chunks",
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
            advancementId = TOWN_STATUS,
            category = AdvancementCategory.SETTLEMENT,
            title = "Fortified Town",
            description = "Expand a contiguous settlement to 9 or more chunks, reaching Town tier",
            tier = AdvancementTier.MASTER,
            currencyPayout = 400L,
            builderXp = 150.0,
            recoveryCatalystCount = 1,
            broadcastServer = true
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
            advancementId = GRAND_METROPOLIS,
            category = AdvancementCategory.SETTLEMENT,
            title = "Regional Metropolis",
            description = "Expand a contiguous settlement to 16 or more chunks reaching Metropolis tier with rich palette variety",
            tier = AdvancementTier.LEGENDARY,
            currencyPayout = 1000L,
            builderXp = 350.0,
            recoveryCatalystCount = 3,
            broadcastServer = true
        )
    )

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
        if (size >= 2) {
            award(player, CONTIGUOUS_HAMLET)
        }
        if (cluster.blockCountsByType.size >= 5) {
            award(player, PALETTE_DIVERSITY)
        }
        if (size >= 9) {
            award(player, TOWN_STATUS)
        }
        if (size >= 16 && cluster.blockCountsByType.size >= 5) {
            award(player, GRAND_METROPOLIS)
        }
    }

    /**
     * Evaluates palette diversity when blocks are placed.
     */
    fun checkPaletteDiversity(player: ServerPlayer, uniquePaletteCount: Int) {
        if (uniquePaletteCount >= 5) {
            award(player, PALETTE_DIVERSITY)
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
}
