package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod

/**
 * Master initializer for all SMP Mod Suite advancement trees and reward registries.
 */
object ModAdvancements {

    fun init() {
        SmpMod.LOGGER.info("Registering SMP Mod Suite Advancement Trees and Reward Registries...")

        EconomyAdvancements.register()
        WorldExplorationAdvancements.register()
        SettlementAdvancements.register()
        CombatRaidAdvancements.register()
        ArtificingRecoveryAdvancements.register()

        SmpMod.LOGGER.info("Registered ${AdvancementRewardManager.getAllRewards().size} Advancement Rewards across 5 progression categories.")
    }
}
