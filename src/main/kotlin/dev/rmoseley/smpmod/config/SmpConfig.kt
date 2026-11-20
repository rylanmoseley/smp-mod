package dev.rmoseley.smpmod.config

import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Configuration specification for SMP Mod Suite tuning constants.
 * Aligns directly with Section 7 of design-v2.md.
 */
object SmpConfig {
    val SPEC: ModConfigSpec
    val COMMON: Common

    init {
        val builder = ModConfigSpec.Builder()
        COMMON = Common(builder)
        SPEC = builder.build()
    }

    /**
     * Safely retrieves a configuration value, returning its default if the config spec has not yet loaded.
     */
    fun <T> ModConfigSpec.ConfigValue<T>.getSafe(): T {
        return if (SPEC.isLoaded) this.get() else this.default
    }

    class Common(builder: ModConfigSpec.Builder) {
        // --- 1. Economy & Commerce ---
        val starterAdvancementPayout: ModConfigSpec.IntValue
        val repeatableHarvestThreshold: ModConfigSpec.IntValue
        val repeatableHarvestPayout: ModConfigSpec.IntValue
        val constructionDividendThreshold: ModConfigSpec.IntValue
        val constructionDividendPayout: ModConfigSpec.IntValue
        val p2pTradeTaxPercent: ModConfigSpec.DoubleValue

        // --- 2. Death Recovery & UUID Buyback ---
        val baseBuybackEscalationStep: ModConfigSpec.DoubleValue
        val exclusiveOwnerWindowSeconds: ModConfigSpec.LongValue
        val publicListingMarkup: ModConfigSpec.DoubleValue
        val wallClockDecayDays: ModConfigSpec.IntValue
        val claimRecoverySubsidyPercent: ModConfigSpec.DoubleValue
        val supportCatalystReductionStep: ModConfigSpec.IntValue

        // --- 3. Anti-AFK & Sourcing ---
        val grinderChunkDecayRadius: ModConfigSpec.IntValue
        val deepCaveVeinDensityMultiplier: ModConfigSpec.DoubleValue
        val dungeonTomeDropMultiplier: ModConfigSpec.DoubleValue

        // --- 4. Contiguous Land Claims & Builder Flight ---
        val chunksPerBuilderLevel: ModConfigSpec.IntValue
        val maxBaseClaimChunks: ModConfigSpec.IntValue
        val varietyPaletteWeightBase: ModConfigSpec.DoubleValue
        val builderFlightGraceSeconds: ModConfigSpec.IntValue

        // --- 5. World, AI, & Elytra ---
        val elytraExplorerChunkMilestone: ModConfigSpec.IntValue
        val elytraMultiBiomeMilestone: ModConfigSpec.IntValue
        val lightSnuffDurationSeconds: ModConfigSpec.IntValue
        val wildernessLightDestroyChance: ModConfigSpec.DoubleValue
        val dungeonLightDestroyChance: ModConfigSpec.DoubleValue
        val netherRoofYLimit: ModConfigSpec.IntValue
        val netherConvoyRadius: ModConfigSpec.DoubleValue
        val netherSoloSprintExhaustionSeconds: ModConfigSpec.IntValue
        val biomeMobilityThreshold: ModConfigSpec.IntValue
        val biomeMobilityMaxBonus: ModConfigSpec.DoubleValue
        val roadMobilityBonus: ModConfigSpec.DoubleValue
        val wardedFlareDurationSeconds: ModConfigSpec.IntValue
        val villageSanctuaryRadius: ModConfigSpec.DoubleValue

        // --- 6. Roles & Emergent Combat ---
        val combatXpSpeedWeight: ModConfigSpec.DoubleValue
        val combatXpAccuracyWeight: ModConfigSpec.DoubleValue
        val specialistThresholdRatio: ModConfigSpec.DoubleValue
        val dpsPunishBonusMultiplier: ModConfigSpec.DoubleValue
        val dpsExecuteThresholdPercent: ModConfigSpec.DoubleValue
        val tankThreatRadius: ModConfigSpec.IntValue
        val tankKineticBarrierReduction: ModConfigSpec.DoubleValue
        val supportAuraRadius: ModConfigSpec.DoubleValue
        val supportHealAmount: ModConfigSpec.DoubleValue
        val scoutTrapDetectionRadius: ModConfigSpec.IntValue
        val dragonStaggerDurationSeconds: ModConfigSpec.IntValue
        val grinderKillsDecayThreshold: ModConfigSpec.IntValue
        val gathererShopMaxDiscount: ModConfigSpec.DoubleValue

        // --- 7. Multiplayer Sleep ---
        val playersSleepingPercentage: ModConfigSpec.IntValue
        val announceSunrise: ModConfigSpec.BooleanValue
        val announceSleep: ModConfigSpec.BooleanValue
        val enableSanctuaryRestBuffs: ModConfigSpec.BooleanValue

        // --- 8. Discord Webhook Integration ---
        val discordWebhookUrl: ModConfigSpec.ConfigValue<String>
        val discordServerName: ModConfigSpec.ConfigValue<String>
        val discordNotifyTownMilestones: ModConfigSpec.BooleanValue
        val discordNotifyDragonVictories: ModConfigSpec.BooleanValue
        val discordNotifyPublicSalvage: ModConfigSpec.BooleanValue
        val discordNotifyVillagerCrimes: ModConfigSpec.BooleanValue
        val discordNotifyAdvancements: ModConfigSpec.BooleanValue

        init {
            builder.push("Economy")
            starterAdvancementPayout = builder
                .comment("Currency awarded upon completing onboarding milestone advancements")
                .defineInRange("starterAdvancementPayout", 100, 0, 100000)
            repeatableHarvestThreshold = builder
                .comment("Number of natural block harvests required per repeatable currency payout")
                .defineInRange("repeatableHarvestThreshold", 200, 25, 10000)
            repeatableHarvestPayout = builder
                .comment("Currency awarded per repeatable harvest cycle completion")
                .defineInRange("repeatableHarvestPayout", 35, 1, 10000)
            constructionDividendThreshold = builder
                .comment("Number of verified placed blocks within claimed territory per construction dividend")
                .defineInRange("constructionDividendThreshold", 100, 10, 10000)
            constructionDividendPayout = builder
                .comment("Base currency awarded per construction dividend milestone")
                .defineInRange("constructionDividendPayout", 25, 1, 10000)
            p2pTradeTaxPercent = builder
                .comment("Transaction fee percentage applied to formal P2P shop transfers (0.0 to 1.0)")
                .defineInRange("p2pTradeTaxPercent", 0.05, 0.0, 1.0)
            builder.pop()

            builder.push("RecoveryAndBuyback")
            baseBuybackEscalationStep = builder
                .comment("Escalating multiplier increment per cumulative death on an owned item (e.g., 0.2 = +20% per death)")
                .defineInRange("baseBuybackEscalationStep", 0.25, 0.01, 5.0)
            exclusiveOwnerWindowSeconds = builder
                .comment("Exclusive reclaim window duration for registered owner before public listing (default 24h = 86400s)")
                .defineInRange("exclusiveOwnerWindowSeconds", 86400L, 60L, 604800L)
            publicListingMarkup = builder
                .comment("Public listing buyback price multiplier after the exclusive window passes (default 1.25x)")
                .defineInRange("publicListingMarkup", 1.25, 1.0, 5.0)
            wallClockDecayDays = builder
                .comment("Days required for escalated buyback fees to smoothly decay back to 1.0x baseline")
                .defineInRange("wallClockDecayDays", 30, 1, 365)
            claimRecoverySubsidyPercent = builder
                .comment("Municipal recovery subsidy percentage when accessing buyback terminals inside contiguous claims (0.0 to 0.5)")
                .defineInRange("claimRecoverySubsidyPercent", 0.15, 0.0, 0.5)
            supportCatalystReductionStep = builder
                .comment("Number of cumulative death increments subtracted per Support recovery catalyst applied")
                .defineInRange("supportCatalystReductionStep", 1, 1, 10)
            builder.pop()

            builder.push("AntiAFK")
            grinderChunkDecayRadius = builder
                .comment("Radius in chunks where repetitive mob kills begin suffering steep diminishing XP/drop returns")
                .defineInRange("grinderChunkDecayRadius", 3, 1, 16)
            deepCaveVeinDensityMultiplier = builder
                .comment("Ore vein generation multiplier in deep caves to reward active exploration")
                .defineInRange("deepCaveVeinDensityMultiplier", 1.5, 1.0, 5.0)
            dungeonTomeDropMultiplier = builder
                .comment("High-tier enchantment tome drop weight in active dungeons/structures")
                .defineInRange("dungeonTomeDropMultiplier", 2.0, 1.0, 10.0)
            builder.pop()

            builder.push("ClaimsAndBuilding")
            chunksPerBuilderLevel = builder
                .comment("Max claimable contiguous chunks unlocked per Builder Playstyle level")
                .defineInRange("chunksPerBuilderLevel", 3, 1, 10)
            maxBaseClaimChunks = builder
                .comment("Base claimable chunks awarded to any new player")
                .defineInRange("maxBaseClaimChunks", 9, 1, 64)
            varietyPaletteWeightBase = builder
                .comment("Base multiplier for variety-weighted block placement diversity")
                .defineInRange("varietyPaletteWeightBase", 1.0, 0.1, 10.0)
            builderFlightGraceSeconds = builder
                .comment("Temporary building flight grace duration sustained by continuous block placement (default 5s)")
                .defineInRange("builderFlightGraceSeconds", 5, 1, 30)
            builder.pop()

            builder.push("WorldAndAI")
            elytraExplorerChunkMilestone = builder
                .comment("Number of unique chunks mapped/discovered required to deploy Elytra glider flight")
                .defineInRange("elytraExplorerChunkMilestone", 500, 10, 50000)
            elytraMultiBiomeMilestone = builder
                .comment("Number of unique biomes mapped required to deploy Elytra glider flight")
                .defineInRange("elytraMultiBiomeMilestone", 5, 1, 50)
            lightSnuffDurationSeconds = builder
                .comment("Duration in seconds that light-snuff aura extinguishes lights within contiguous claims (20-30s)")
                .defineInRange("lightSnuffDurationSeconds", 25, 5, 120)
            wildernessLightDestroyChance = builder
                .comment("Spawn probability of destroys_light_on_aggro mobs in wild caves")
                .defineInRange("wildernessLightDestroyChance", 0.05, 0.0, 1.0)
            dungeonLightDestroyChance = builder
                .comment("Spawn probability of destroys_light_on_aggro mobs inside dungeons/structures")
                .defineInRange("dungeonLightDestroyChance", 0.35, 0.0, 1.0)
            netherRoofYLimit = builder
                .comment("Y-coordinate limit above which Nether transit and building is completely blocked (default 127)")
                .defineInRange("netherRoofYLimit", 127, 64, 255)
            netherConvoyRadius = builder
                .comment("Distance in blocks within which fellow players mitigate Nether solo sprint exhaustion (default 20m)")
                .defineInRange("netherConvoyRadius", 20.0, 5.0, 64.0)
            netherSoloSprintExhaustionSeconds = builder
                .comment("Seconds of continuous solo sprinting in Nether before thermal exhaustion kicks in")
                .defineInRange("netherSoloSprintExhaustionSeconds", 3, 1, 30)
            biomeMobilityThreshold = builder
                .comment("Number of unique chunks discovered per biome mobility tier")
                .defineInRange("biomeMobilityThreshold", 20, 5, 200)
            biomeMobilityMaxBonus = builder
                .comment("Maximum movement speed bonus attainable in fully explored biomes (e.g. 0.20 = +20%)")
                .defineInRange("biomeMobilityMaxBonus", 0.20, 0.0, 1.0)
            roadMobilityBonus = builder
                .comment("Additional movement speed bonus when traversing paved roads in explored biomes (default +10%)")
                .defineInRange("roadMobilityBonus", 0.10, 0.0, 0.5)
            wardedFlareDurationSeconds = builder
                .comment("Duration in seconds for Support role Warded Light flares (default 300s = 5m)")
                .defineInRange("wardedFlareDurationSeconds", 300, 30, 3600)
            villageSanctuaryRadius = builder
                .comment("Sanctuary radius around village centers for safe rest stops and mob suppression (default 48m)")
                .defineInRange("villageSanctuaryRadius", 48.0, 16.0, 128.0)
            builder.pop()

            builder.push("RolesAndXP")
            combatXpSpeedWeight = builder
                .comment("Weight applied to time-to-kill speed in Combat XP calculation")
                .defineInRange("combatXpSpeedWeight", 1.2, 0.1, 5.0)
            combatXpAccuracyWeight = builder
                .comment("Weight applied to hit precision/crits in Combat XP calculation")
                .defineInRange("combatXpAccuracyWeight", 1.5, 0.1, 5.0)
            specialistThresholdRatio = builder
                .comment("Playstyle XP ratio threshold above which a player becomes a dedicated Role Specialist")
                .defineInRange("specialistThresholdRatio", 0.40, 0.25, 0.80)
            dpsPunishBonusMultiplier = builder
                .comment("Damage multiplier bonus granted to DPS during boss vulnerability / execute punish windows")
                .defineInRange("dpsPunishBonusMultiplier", 1.8, 1.1, 4.0)
            dpsExecuteThresholdPercent = builder
                .comment("Target health percentage threshold below which execution punish activates (default 0.35 = 35% HP)")
                .defineInRange("dpsExecuteThresholdPercent", 0.35, 0.1, 0.6)
            tankThreatRadius = builder
                .comment("Radius in blocks for Tank taunt pulse pulling dark-zone elites off lights and allies")
                .defineInRange("tankThreatRadius", 16, 4, 64)
            tankKineticBarrierReduction = builder
                .comment("Percentage AoE and projectile damage reduction provided by Tank kinetic barrier to allies behind (default 75%)")
                .defineInRange("tankKineticBarrierReduction", 0.75, 0.2, 0.95)
            supportAuraRadius = builder
                .comment("Base radius in blocks for Support role healing pulse and aura buff radiation")
                .defineInRange("supportAuraRadius", 8.0, 2.0, 32.0)
            supportHealAmount = builder
                .comment("Base health points restored per Support sustain pulse (default 1.5 HP = 0.75 hearts)")
                .defineInRange("supportHealAmount", 1.5, 0.5, 10.0)
            scoutTrapDetectionRadius = builder
                .comment("Sensory radar radius in blocks for Scout dark-zone trap and threat detection")
                .defineInRange("scoutTrapDetectionRadius", 24, 8, 64)
            dragonStaggerDurationSeconds = builder
                .comment("Duration in seconds that the Ender Dragon remains staggered/stunned after a Tank dive intercept")
                .defineInRange("dragonStaggerDurationSeconds", 6, 1, 20)
            grinderKillsDecayThreshold = builder
                .comment("Number of mob kills in the same chunk/radius before anti-grinder XP decay kicks in")
                .defineInRange("grinderKillsDecayThreshold", 5, 1, 50)
            gathererShopMaxDiscount = builder
                .comment("Maximum shop discount ratio attainable through high Gatherer XP specialization (default 0.25 = 25%)")
                .defineInRange("gathererShopMaxDiscount", 0.25, 0.0, 0.50)
            builder.pop()

            builder.push("MultiplayerSleep")
            playersSleepingPercentage = builder
                .comment("Percentage of players in bed required to pass the night (default 1 for 1-player sleep, 100 for vanilla)")
                .defineInRange("playersSleepingPercentage", 1, 0, 100)
            announceSunrise = builder
                .comment("Broadcast sunrise announcement to the realm when morning arrives")
                .define("announceSunrise", true)
            announceSleep = builder
                .comment("Broadcast sleep progress message when a player enters bed")
                .define("announceSleep", true)
            enableSanctuaryRestBuffs = builder
                .comment("Award Well Rested regeneration and haste buffs when waking up in claimed towns or village sanctuaries")
                .define("enableSanctuaryRestBuffs", true)
            builder.pop()

            builder.push("Discord")
            discordWebhookUrl = builder
                .comment("Discord Webhook URL for server event announcements (leave empty to disable)")
                .define("discordWebhookUrl", "")
            discordServerName = builder
                .comment("Server name displayed on Discord embed footers")
                .define("discordServerName", "SMP Realm")
            discordNotifyTownMilestones = builder
                .comment("Send Discord alerts for Fortified Town and Metropolis foundings")
                .define("notifyTownMilestones", true)
            discordNotifyDragonVictories = builder
                .comment("Send Discord alerts for cooperative Ender Dragon victories")
                .define("notifyDragonVictories", true)
            discordNotifyPublicSalvage = builder
                .comment("Send Discord alerts when 24h unclaimed salvage drops hit the public buyback market")
                .define("notifyPublicSalvage", true)
            discordNotifyVillagerCrimes = builder
                .comment("Send Discord crime reports when a player murders a villager")
                .define("notifyVillagerCrimes", true)
            discordNotifyAdvancements = builder
                .comment("Send Discord alerts for Master and Legendary advancement completions")
                .define("notifyAdvancements", true)
            builder.pop()
        }
    }
}
