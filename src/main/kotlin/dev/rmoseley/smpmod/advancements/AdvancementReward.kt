package dev.rmoseley.smpmod.advancements

import net.minecraft.resources.ResourceLocation

enum class AdvancementTier(
    val displayName: String,
    val colorCode: String
) {
    NOVICE("Novice", "§a"),
    JOURNEYMAN("Journeyman", "§e"),
    MASTER("Master", "§6"),
    LEGENDARY("Legendary", "§d")
}

enum class AdvancementCategory(
    val id: String,
    val displayName: String,
    val description: String
) {
    ECONOMY("economy", "Frontier Commerce", "Trade, P2P exchange, bulk storage, and non-AFK wealth creation"),
    EXPLORATION("exploration", "Wilds & Expeditions", "Biome mapping, cavern depths, hazards, and aerial transit"),
    SETTLEMENT("settlement", "Towns & Architecture", "Land claims, contiguous development, variety aesthetics, and civic upgrades"),
    COMBAT("combat", "Dark-Zone Trials", "Elite encounters, roles coordination, threat mitigation, and boss raids"),
    ARTIFICING("artificing", "Artificing & Provenance", "Masterwork gear, catalytic refinement, lore, and safe recovery")
}

data class AdvancementReward(
    val advancementId: ResourceLocation,
    val category: AdvancementCategory,
    val title: String,
    val description: String,
    val tier: AdvancementTier,
    val currencyPayout: Long = 0L,
    val combatXp: Double = 0.0,
    val builderXp: Double = 0.0,
    val gathererXp: Double = 0.0,
    val explorerXp: Double = 0.0,
    val recoveryCatalystCount: Int = 0,
    val broadcastServer: Boolean = false
)
