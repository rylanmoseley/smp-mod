package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.registry.ModDataComponents
import dev.rmoseley.smpmod.roles.RoleCalculator
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import kotlin.random.Random

/**
 * Lightweight Masterwork Affixes applied to crafted and dungeon-looted gear.
 * Conforms to design-v2.md Section 6.3: "Item Variation: Included in lightweight form...
 * so unique gear carries real identity worth recovering through buybacks and trading."
 */
enum class MasterworkAffix(
    val displayName: String,
    val description: String,
    val colorCode: String = "§6"
) {
    KEEN("Keen", "+10% Critical Strike Damage", "§c"),
    STURDY("Sturdy", "+25% Enhanced Durability", "§9"),
    BALANCED("Balanced", "+0.2 Attack Speed & -15% Whiff Window", "§e"),
    WARDED("Warded", "+8% Dark-Zone Mob Resistance", "§b"),
    AERODYNAMIC("Aerodynamic", "+10% Elytra Glide Efficiency", "§a"),
    TITANIC("Titanic", "+10% Knockback Resistance & +5% Armor", "§7"),
    PRECISE("Precise", "+15% Combat Hit Precision & Tempo XP", "§d");

    companion object {
        fun fromId(id: String?): MasterworkAffix? {
            if (id == null) return null
            return entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
        }

        /**
         * Rolls an affix when an item is forged or looted from a deep dungeon.
         * Probability scales with the creator's Builder / Gatherer progression or dungeon tier.
         */
        fun rollForCraft(creator: Player): MasterworkAffix? {
            val profile = RoleCalculator.calculateProfile(creator)
            val combinedMastery = profile.tankRatio + profile.supportRatio

            // Base 25% chance, scaling up to 75% for dedicated Builder/Gatherer craftsmen
            val rollChance = 0.25 + (combinedMastery * 0.50)
            if (Random.nextDouble() > rollChance) return null

            return entries.random()
        }

        fun rollForDungeon(): MasterworkAffix? {
            // 60% chance for dungeon loot
            if (Random.nextDouble() > 0.60) return null
            return entries.random()
        }

        fun applyAffix(stack: ItemStack, affix: MasterworkAffix) {
            stack.set(ModDataComponents.MASTERWORK_AFFIX.get(), affix.name)
            ItemProvenance.updateLore(stack)
        }

        fun getAffix(stack: ItemStack): MasterworkAffix? {
            val name = stack.get(ModDataComponents.MASTERWORK_AFFIX.get())
            return fromId(name)
        }
    }
}
