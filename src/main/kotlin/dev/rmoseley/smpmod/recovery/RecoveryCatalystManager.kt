package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModDataComponents
import dev.rmoseley.smpmod.roles.RoleCalculator
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import java.util.UUID

/**
 * Manages Recovery and Repair Catalysts crafted and deployed by Support specialists.
 * Catalysts reduce cumulative death_count escalation on buyback items and enable
 * Support players to assist teammates in recovering lost gear at reduced fees.
 */
object RecoveryCatalystManager {

    const val CATALYST_TAG = "smpmod_recovery_catalyst"

    enum class CatalystTier(val reductionAmount: Int, val displayName: String) {
        LESSER(1, "Lesser Recovery Catalyst"),
        STANDARD(2, "Standard Recovery Catalyst"),
        GREATER(3, "Greater Recovery Catalyst")
    }

    sealed class CatalystApplicationResult {
        data class Success(
            val previousDeathCount: Int,
            val newDeathCount: Int,
            val newPrice: Long,
            val message: String
        ) : CatalystApplicationResult()

        data class Failure(val reason: String) : CatalystApplicationResult()
    }

    /**
     * Checks if an ItemStack is an active recovery catalyst.
     */
    fun isCatalyst(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false

        // Check for custom tag or item type
        if (stack.item == Items.ECHO_SHARD) return true

        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null && customData.copyTag().contains(CATALYST_TAG)) {
            return true
        }

        val name = stack.hoverName.string
        return name.contains("Catalyst", ignoreCase = true) && name.contains("Recovery", ignoreCase = true)
    }

    /**
     * Creates a Recovery Catalyst item with Support-crafted provenance lore.
     */
    fun createCatalyst(crafter: Player, tier: CatalystTier = CatalystTier.STANDARD): ItemStack {
        val stack = ItemStack(Items.ECHO_SHARD)
        val profile = RoleCalculator.calculateProfile(crafter)

        // Support role bonus: High support ratio can enhance catalyst tier
        val effectiveReduction = if (profile.supportRatio >= 0.50) {
            tier.reductionAmount + 1
        } else {
            tier.reductionAmount
        }

        val customData = CustomData.EMPTY.update { tag ->
            tag.putBoolean(CATALYST_TAG, true)
            tag.putInt("reduction_amount", effectiveReduction)
            tag.putString("crafted_by", crafter.gameProfile.name)
        }
        stack.set(DataComponents.CUSTOM_DATA, customData)

        // Name and lore
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§b${tier.displayName}"))
        val loreLines = listOf(
            Component.literal("§7Crafted by Support: §f${crafter.gameProfile.name}"),
            Component.literal("§eReduces Buyback Escalation by §a-$effectiveReduction death(s)"),
            Component.literal("§8Apply at a Buyback Terminal or via /buyback catalyst")
        )
        stack.set(DataComponents.LORE, ItemLore(loreLines))

        return stack
    }

    /**
     * Applies a recovery catalyst to a specific item in the buyback ledger.
     * Decrements the item's deathCount, immediately reducing the escalating fee.
     */
    fun applyCatalystToEntry(
        itemUuid: UUID,
        applicator: Player,
        catalystStack: ItemStack
    ): CatalystApplicationResult {
        if (!isCatalyst(catalystStack)) {
            return CatalystApplicationResult.Failure("Held item is not a valid Recovery Catalyst.")
        }

        val entry = BuybackLedgerManager.getEntry(itemUuid)
            ?: return CatalystApplicationResult.Failure("Target item not found in buyback ledger.")

        val customData = catalystStack.get(DataComponents.CUSTOM_DATA)
        var reduction = customData?.copyTag()?.getInt("reduction_amount") ?: SmpConfig.COMMON.supportCatalystReductionStep.get()
        if (reduction <= 0) {
            reduction = SmpConfig.COMMON.supportCatalystReductionStep.get()
        }

        // Support role player applying a catalyst receives affinity bonus
        val profile = RoleCalculator.calculateProfile(applicator)
        if (profile.supportRatio >= 0.35) {
            reduction += 1
        }

        val prevCount = entry.deathCount
        val newCount = (prevCount - reduction).coerceAtLeast(0)

        // Update entry deathCount
        entry.deathCount = newCount
        BuybackLedgerManager.markDirty()

        // Update item component
        val itemStack = entry.itemStack
        itemStack.set(ModDataComponents.DEATH_COUNT.get(), newCount)

        val currentReductions = itemStack.get(ModDataComponents.CATALYST_REDUCTIONS.get()) ?: 0
        itemStack.set(ModDataComponents.CATALYST_REDUCTIONS.get(), currentReductions + reduction)

        ItemProvenance.updateLore(itemStack)

        // Consume catalyst item
        catalystStack.shrink(1)

        val newPrice = BuybackLedgerManager.calculatePrice(entry)
        val isTeammate = !applicator.gameProfile.name.equals(entry.registeredOwnerName, ignoreCase = true)
        val assistMsg = if (isTeammate) {
            " (Support assistance applied on behalf of ${entry.registeredOwnerName})"
        } else ""

        if (applicator is net.minecraft.server.level.ServerPlayer) {
            dev.rmoseley.smpmod.advancements.ArtificingRecoveryAdvancements.onCatalystApplied(applicator)
        }

        return CatalystApplicationResult.Success(
            previousDeathCount = prevCount,
            newDeathCount = newCount,
            newPrice = newPrice,
            message = "Catalyst applied! Escalation reduced from $prevCount to $newCount deaths$assistMsg."
        )
    }
}
