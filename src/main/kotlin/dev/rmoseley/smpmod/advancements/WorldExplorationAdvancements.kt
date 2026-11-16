package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.world.BiomeMobilityManager
import dev.rmoseley.smpmod.world.CaravanTransitManager
import dev.rmoseley.smpmod.world.ElytraFlightManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level

/**
 * World & Navigation Advancement Tree and Rewards Registry.
 *
 * Implements a deep, tiered progression spanning Novice to Legendary tiers:
 * - exploration/root: Wilds & Expeditions (Root Tab)
 * - exploration/into_the_wilds: Frontier Scout (Novice)
 * - exploration/deep_caverns: Abyss Descender (Novice)
 * - exploration/dungeon_delver: Deep Dungeon Delver (Journeyman)
 * - exploration/nether_convoy: Thermal Convoy (Journeyman)
 * - exploration/highway_caravan: Highway Caravan Master (Journeyman)
 * - exploration/master_cartographer: Realm Cartographer (Master)
 * - exploration/momentum_glider: Master of the Winds (Master)
 * - exploration/abyssal_sovereign: Horizon Pioneer (Legendary)
 */
object WorldExplorationAdvancements {

    val ROOT_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/root")

    val INTO_THE_WILDS_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/into_the_wilds")

    val DEEP_CAVERNS_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/deep_caverns")

    val DUNGEON_DELVER_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/dungeon_delver")

    val NETHER_CONVOY_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/nether_convoy")

    val HIGHWAY_CARAVAN_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/highway_caravan")

    val MASTER_CARTOGRAPHER_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/master_cartographer")

    val MOMENTUM_GLIDER_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/momentum_glider")

    val ALPINE_SUMMIT_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/alpine_summit")

    val ABYSSAL_SOVEREIGN_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "exploration/abyssal_sovereign")

    /**
     * Registers all World Exploration advancement rewards with the central AdvancementRewardManager.
     */
    fun register() {
        // 1. Root Tab: Wilds & Expeditions
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = ROOT_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Wilds & Expeditions",
                description = "Step beyond the safety of settlement borders to explore the uncharted frontier.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 0L,
                explorerXp = 0.0
            )
        )

        // 2. Novice: Frontier Scout
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = INTO_THE_WILDS_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Frontier Scout",
                description = "Venture into the wilderness and discover your first unique biome.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 50L,
                explorerXp = 30.0
            )
        )

        // 3. Novice: Abyss Descender
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = DEEP_CAVERNS_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Abyss Descender",
                description = "Descend below elevation Y: 0 into the perilous deepslate caverns.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 75L,
                explorerXp = 40.0
            )
        )

        // 4. Journeyman: Deep Dungeon Delver
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = DUNGEON_DELVER_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Deep Dungeon Delver",
                description = "Infiltrate and explore an uncharted subterranean dungeon complex or ancient trial chamber.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 200L,
                explorerXp = 80.0,
                recoveryCatalystCount = 1
            )
        )

        // 5. Journeyman: Thermal Convoy
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = NETHER_CONVOY_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Thermal Convoy",
                description = "Traverse the treacherous Nether in an organized caravan convoy without roof transit.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 150L,
                explorerXp = 75.0
            )
        )

        // 6. Journeyman: Highway Caravan Master
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = HIGHWAY_CARAVAN_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Highway Caravan Master",
                description = "Lead pack animals or an organized convoy along a recognized paved highway.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 150L,
                explorerXp = 60.0
            )
        )

        // 7. Master: Realm Cartographer
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = MASTER_CARTOGRAPHER_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Realm Cartographer",
                description = "Map 250+ unique chunks spanning across at least 10 diverse biomes.",
                tier = AdvancementTier.MASTER,
                currencyPayout = 350L,
                explorerXp = 150.0,
                recoveryCatalystCount = 1
            )
        )

        // 8. Master: Master of the Winds
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = MOMENTUM_GLIDER_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Master of the Winds",
                description = "Unlock and deploy aerodynamic Elytra glider flight via exploration milestones.",
                tier = AdvancementTier.MASTER,
                currencyPayout = 400L,
                explorerXp = 180.0,
                recoveryCatalystCount = 1,
                broadcastServer = true
            )
        )

        // 9. Journeyman: Peak of the World (Tectonic Alpine Summit)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = ALPINE_SUMMIT_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Peak of the World",
                description = "Scale an alpine summit above Y: 220 in Tectonic mountain terrain and launch an unboosted momentum glide.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 250L,
                explorerXp = 100.0,
                recoveryCatalystCount = 1
            )
        )

        // 10. Legendary: Horizon Pioneer
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = ABYSSAL_SOVEREIGN_ID,
                category = AdvancementCategory.EXPLORATION,
                title = "Horizon Pioneer",
                description = "Map 1,000+ unique chunks across the frontier and discover ancient deep structures.",
                tier = AdvancementTier.LEGENDARY,
                currencyPayout = 1000L,
                explorerXp = 300.0,
                recoveryCatalystCount = 3,
                broadcastServer = true
            )
        )

        SmpMod.LOGGER.info("Registered 10 World Exploration Advancement Rewards (Novice -> Legendary).")
    }

    fun onAlpineGliderLaunch(player: ServerPlayer, altitude: Double) {
        if (altitude >= 200.0) {
            grantAdvancement(player, ALPINE_SUMMIT_ID)
        }
    }

    /**
     * Programmatically awards an advancement to a player if not already completed.
     */
    fun grantAdvancement(player: ServerPlayer, advancementId: ResourceLocation) {
        val server = player.server
        val holder = server.advancements.get(advancementId) ?: return
        val progress = player.advancements.getOrStartProgress(holder)
        if (!progress.isDone) {
            val remaining = progress.remainingCriteria.toList()
            for (crit in remaining) {
                player.advancements.award(holder, crit)
            }
        }
    }

    /**
     * Checks exploration milestones upon new chunk or biome discovery.
     */
    fun onExplorationProgress(player: ServerPlayer, totalChunks: Int, totalBiomes: Int) {
        if (totalBiomes >= 1) {
            grantAdvancement(player, INTO_THE_WILDS_ID)
        }
        if (totalChunks >= 250 && totalBiomes >= 10) {
            grantAdvancement(player, MASTER_CARTOGRAPHER_ID)
        }
        if (totalChunks >= 1000) {
            grantAdvancement(player, ABYSSAL_SOVEREIGN_ID)
        }
    }

    /**
     * Evaluates cavern depth milestones when descending below Y: 0.
     */
    fun onCavernDepthCheck(player: ServerPlayer) {
        if (player.level().dimension() == Level.OVERWORLD && player.blockY < 0) {
            grantAdvancement(player, DEEP_CAVERNS_ID)
        }
    }

    /**
     * Evaluates caravan transit milestones in the Nether or on Overworld paved highways.
     */
    fun onCaravanTransitCheck(player: ServerPlayer) {
        val level = player.level()
        val inCaravan = CaravanTransitManager.isInCaravan(player)
        if (!inCaravan) return

        // Nether Convoy (must be in Nether and below bedrock roof Y: 128)
        if (level.dimension() == Level.NETHER && player.blockY < 128) {
            grantAdvancement(player, NETHER_CONVOY_ID)
        }

        // Highway Caravan Master (must be on recognized road/rail in Overworld)
        if (level.dimension() == Level.OVERWORLD && BiomeMobilityManager.isStandingOnRoad(player)) {
            grantAdvancement(player, HIGHWAY_CARAVAN_ID)
        }
    }

    /**
     * Evaluates momentum glider deployment milestone when player deploys Elytra flight.
     */
    fun onGliderDeployed(player: ServerPlayer) {
        if (player.isFallFlying && ElytraFlightManager.canDeployElytra(player)) {
            grantAdvancement(player, MOMENTUM_GLIDER_ID)
        }
    }

    /**
     * Evaluates deep structure and dungeon discoveries for delve milestones.
     */
    fun onStructureDiscovered(player: ServerPlayer, structureKey: String) {
        // Deep Dungeon Delver milestone
        grantAdvancement(player, DUNGEON_DELVER_ID)

        val totalChunks = BiomeMobilityManager.getUniqueChunksExplored(player)
        if (totalChunks >= 1000 || structureKey.contains("ancient_city") || structureKey.contains("trial_chambers")) {
            if (totalChunks >= 250) {
                grantAdvancement(player, ABYSSAL_SOVEREIGN_ID)
            }
        }
    }
}
