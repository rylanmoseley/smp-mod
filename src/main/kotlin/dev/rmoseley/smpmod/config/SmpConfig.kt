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

    class Common(builder: ModConfigSpec.Builder) {
        // --- 1. Economy & Commerce ---
        val starterAdvancementPayout: ModConfigSpec.IntValue
        val repeatableHarvestThreshold: ModConfigSpec.IntValue
        val repeatableHarvestPayout: ModConfigSpec.IntValue
        val p2pTradeTaxPercent: ModConfigSpec.DoubleValue

        // --- 2. Death Recovery & UUID Buyback ---
        val baseBuybackEscalationStep: ModConfigSpec.DoubleValue
        val exclusiveOwnerWindowSeconds: ModConfigSpec.LongValue
        val publicListingMarkup: ModConfigSpec.DoubleValue
        val wallClockDecayDays: ModConfigSpec.IntValue

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
        val lightSnuffDurationSeconds: ModConfigSpec.IntValue
        val wildernessLightDestroyChance: ModConfigSpec.DoubleValue
        val dungeonLightDestroyChance: ModConfigSpec.DoubleValue

        // --- 6. Roles & Emergent Combat ---
        val combatXpSpeedWeight: ModConfigSpec.DoubleValue
        val combatXpAccuracyWeight: ModConfigSpec.DoubleValue

        init {
            builder.push("Economy")
            starterAdvancementPayout = builder
                .comment("Currency awarded upon completing onboarding milestone advancements")
                .defineInRange("starterAdvancementPayout", 100, 0, 100000)
            repeatableHarvestThreshold = builder
                .comment("Number of natural block harvests required per repeatable currency payout")
                .defineInRange("repeatableHarvestThreshold", 500, 50, 10000)
            repeatableHarvestPayout = builder
                .comment("Currency awarded per repeatable harvest cycle completion")
                .defineInRange("repeatableHarvestPayout", 25, 1, 10000)
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
                .defineInRange("chunksPerBuilderLevel", 2, 1, 10)
            maxBaseClaimChunks = builder
                .comment("Base claimable chunks awarded to any new player")
                .defineInRange("maxBaseClaimChunks", 4, 1, 64)
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
            lightSnuffDurationSeconds = builder
                .comment("Duration in seconds that light-snuff aura extinguishes lights within contiguous claims (20-30s)")
                .defineInRange("lightSnuffDurationSeconds", 25, 5, 120)
            wildernessLightDestroyChance = builder
                .comment("Spawn probability of destroys_light_on_aggro mobs in wild caves")
                .defineInRange("wildernessLightDestroyChance", 0.05, 0.0, 1.0)
            dungeonLightDestroyChance = builder
                .comment("Spawn probability of destroys_light_on_aggro mobs inside dungeons/structures")
                .defineInRange("dungeonLightDestroyChance", 0.35, 0.0, 1.0)
            builder.pop()

            builder.push("RolesAndXP")
            combatXpSpeedWeight = builder
                .comment("Weight applied to time-to-kill speed in Combat XP calculation")
                .defineInRange("combatXpSpeedWeight", 1.2, 0.1, 5.0)
            combatXpAccuracyWeight = builder
                .comment("Weight applied to hit precision/crits in Combat XP calculation")
                .defineInRange("combatXpAccuracyWeight", 1.5, 0.1, 5.0)
            builder.pop()
        }
    }
}
