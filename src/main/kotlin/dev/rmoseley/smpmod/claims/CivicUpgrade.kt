package dev.rmoseley.smpmod.claims

/**
 * Municipal Civic Upgrades funded through the Town Treasury.
 * Conforms to design-v2.md Section 3 (Land Claims & Settlement Development)
 * and complementary systems architecture.
 */
enum class CivicUpgrade(
    val id: String,
    val displayName: String,
    val cost: Long,
    val requiredTier: SettlementTier,
    val description: String
) {
    PAVED_HIGHWAY_NETWORK(
        "paved_highways",
        "Paved Highway Network",
        500L,
        SettlementTier.SETTLEMENT,
        "Doubles road transit speed buffs (+40% movement speed) within and departing town claims."
    ),
    MUNICIPAL_QUARANTINE(
        "municipal_quarantine",
        "Municipal Quarantine",
        750L,
        SettlementTier.SETTLEMENT,
        "Reduces dark-zone light snuff duration by 50% and suppresses hostile mob aggro speed."
    ),
    TOWN_RECOVERY_SUBSIDY(
        "recovery_subsidy",
        "Town Recovery Subsidy",
        1000L,
        SettlementTier.TOWN,
        "Grants an additional 15% discount on player death buybacks performed within the settlement."
    ),
    WARDED_STREETLIGHTS(
        "warded_streetlights",
        "Warded Streetlights",
        1500L,
        SettlementTier.TOWN,
        "Automatically renders municipal lanterns warded and completely immune to mob snuffing."
    );

    companion object {
        fun fromId(id: String): CivicUpgrade? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
