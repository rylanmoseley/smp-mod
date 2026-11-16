package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Combat & Dark-Zone Trials progression advancements.
 * Defines tiered rewards spanning Novice, Journeyman, Master, and Legendary ranks
 * across individual combat milestones and cooperative multi-role raid encounters.
 */
object CombatRaidAdvancements {

    val ROOT: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/root")
    val FIRST_KILL_TEMPO: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/first_kill_tempo")
    val SNUFFER_SLAYER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/snuffer_slayer")
    val TRIAL_SURVIVOR: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/trial_survivor")
    val SCOUT_CRYSTAL_NEUTRALIZER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/scout_crystal_neutralizer")
    val TANK_DIVE_INTERCEPT: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/tank_dive_intercept")
    val DPS_EXECUTE_STRIKE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/dps_execute_strike")
    val SUPPORT_REJUVENATION: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/support_rejuvenation")
    val COOPERATIVE_DRAGON_TRIUMPH: ResourceLocation = ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "combat/cooperative_dragon_triumph")

    fun register() {
        // 1. Root Tab: Dark-Zone Trials
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = ROOT,
                category = AdvancementCategory.COMBAT,
                title = "Dark-Zone Trials",
                description = "Enter the shadows of the dark-zone trials and prove your martial prowess.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 0L,
                combatXp = 0.0
            )
        )

        // 2. Novice: High-Tempo Skirmisher
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = FIRST_KILL_TEMPO,
                category = AdvancementCategory.COMBAT,
                title = "High-Tempo Skirmisher",
                description = "Defeat a hostile mob with fast time-to-kill.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 50L,
                combatXp = 30.0
            )
        )

        // 3. Novice: Beacon Protector
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = SNUFFER_SLAYER,
                category = AdvancementCategory.COMBAT,
                title = "Beacon Protector",
                description = "Defeat an elite mob with destroys_light_on_aggro.",
                tier = AdvancementTier.NOVICE,
                currencyPayout = 100L,
                combatXp = 50.0
            )
        )

        // 4. Journeyman: Vault Delver
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = TRIAL_SURVIVOR,
                category = AdvancementCategory.COMBAT,
                title = "Vault Delver",
                description = "Clear a deep trial chamber or spawner vault.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 200L,
                combatXp = 80.0
            )
        )

        // 5. Journeyman: Spire Infiltrator (Scout Role)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = SCOUT_CRYSTAL_NEUTRALIZER,
                category = AdvancementCategory.COMBAT,
                title = "Spire Infiltrator",
                description = "Scale obsidian heights and shatter an End Crystal tethered to the Ender Dragon.",
                tier = AdvancementTier.JOURNEYMAN,
                currencyPayout = 250L,
                combatXp = 100.0,
                explorerXp = 40.0,
                recoveryCatalystCount = 1
            )
        )

        // 6. Master: Unyielding Aegis (Tank Role)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = TANK_DIVE_INTERCEPT,
                category = AdvancementCategory.COMBAT,
                title = "Unyielding Aegis",
                description = "Intercept an Ender Dragon dive or boss attack using Tank barrier mitigation.",
                tier = AdvancementTier.MASTER,
                currencyPayout = 300L,
                combatXp = 120.0,
                recoveryCatalystCount = 1
            )
        )

        // 7. Master: Executioner's Precision (DPS Role)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = DPS_EXECUTE_STRIKE,
                category = AdvancementCategory.COMBAT,
                title = "Executioner's Precision",
                description = "Deal massive burst execution during a vulnerability stagger window.",
                tier = AdvancementTier.MASTER,
                currencyPayout = 300L,
                combatXp = 120.0,
                recoveryCatalystCount = 1
            )
        )

        // 8. Master: Field Medic Radiance (Support Role)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = SUPPORT_REJUVENATION,
                category = AdvancementCategory.COMBAT,
                title = "Field Medic Radiance",
                description = "Deploy warded lights and sustain team through raid phase.",
                tier = AdvancementTier.MASTER,
                currencyPayout = 350L,
                combatXp = 150.0,
                recoveryCatalystCount = 2
            )
        )

        // 9. Legendary: Vanguard of the End (Cooperative Dragon Triumph)
        AdvancementRewardManager.register(
            AdvancementReward(
                advancementId = COOPERATIVE_DRAGON_TRIUMPH,
                category = AdvancementCategory.COMBAT,
                title = "Vanguard of the End",
                description = "Defeat the Cooperative Ender Dragon centerpiece encounter.",
                tier = AdvancementTier.LEGENDARY,
                currencyPayout = 1000L,
                combatXp = 350.0,
                recoveryCatalystCount = 3,
                broadcastServer = true
            )
        )
    }

    /**
     * Programmatically awards a combat progression advancement to a player.
     * Completes remaining criteria on the server, which fires NeoForge's AdvancementEarnEvent
     * and automatically triggers AdvancementRewardManager dispatch.
     */
    fun grant(player: ServerPlayer, advancementId: ResourceLocation) {
        val serverAdvancements = player.server.advancements
        val holder = serverAdvancements.get(advancementId)
        if (holder != null) {
            val progress = player.advancements.getOrStartProgress(holder)
            if (!progress.isDone) {
                for (criterion in progress.remainingCriteria) {
                    player.advancements.award(holder, criterion)
                }
            }
        } else {
            // Direct fallback if datapacks are unloaded or in mock test harness
            AdvancementRewardManager.onAdvancementEarned(player, advancementId)
        }
    }
}
