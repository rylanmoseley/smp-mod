package dev.rmoseley.smpmod.advancements

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.economy.TransactionType
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModItems
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry and dispatch manager for deep advancement rewards.
 * Hooks into AdvancementEarnEvent, validates lifetime one-time claims via player attachments,
 * and distributes digital currency, playstyle XP, and tangible artifacts.
 */
object AdvancementRewardManager {

    private val rewards = ConcurrentHashMap<ResourceLocation, AdvancementReward>()
    private val rewardsByPath = ConcurrentHashMap<String, AdvancementReward>()

    fun register(reward: AdvancementReward) {
        rewards[reward.advancementId] = reward
        rewardsByPath[reward.advancementId.path] = reward
    }

    fun registerAll(rewardList: Collection<AdvancementReward>) {
        rewardList.forEach { register(it) }
    }

    fun getReward(id: ResourceLocation): AdvancementReward? {
        return rewards[id] ?: rewardsByPath[id.path]
    }

    fun getAllRewards(): Collection<AdvancementReward> = rewards.values

    fun getRewardsByCategory(category: AdvancementCategory): List<AdvancementReward> {
        return rewards.values.filter { it.category == category }
    }

    /**
     * Processes advancement completion for a player, distributing configured rewards.
     */
    fun onAdvancementEarned(player: ServerPlayer, advancementId: ResourceLocation) {
        val reward = getReward(advancementId) ?: return

        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val currentData = player.getData(attachment)

        val claimKey = advancementId.toString()
        if (currentData.claimedMilestones.contains(claimKey) || currentData.claimedMilestones.contains(advancementId.path)) {
            return // Already claimed
        }

        // Mark as claimed
        val updatedClaimed = currentData.claimedMilestones.toMutableSet()
        updatedClaimed.add(claimKey)
        updatedClaimed.add(advancementId.path)
        player.setData(attachment, currentData.copy(claimedMilestones = updatedClaimed))

        // 1. Award Digital Currency
        if (reward.currencyPayout > 0L) {
            EconomyManager.credit(
                player,
                reward.currencyPayout,
                TransactionType.MILESTONE_STARTER,
                "Advancement: ${reward.title}"
            )
        }

        // 2. Award Playstyle Role XP
        if (reward.combatXp > 0.0) PlaystyleXpTracker.incrementCombatXp(player, reward.combatXp)
        if (reward.builderXp > 0.0) PlaystyleXpTracker.incrementBuilderXp(player, reward.builderXp)
        if (reward.gathererXp > 0.0) PlaystyleXpTracker.incrementGathererXp(player, reward.gathererXp)
        if (reward.explorerXp > 0.0) PlaystyleXpTracker.incrementExplorerXp(player, reward.explorerXp)

        // 3. Award Tangible Items (Recovery Catalysts)
        if (reward.recoveryCatalystCount > 0) {
            val catalystStack = ItemStack(ModItems.RECOVERY_CATALYST.get(), reward.recoveryCatalystCount)
            if (!player.inventory.add(catalystStack)) {
                player.drop(catalystStack, false)
            }
        }

        // 4. Player Notification
        val tierPrefix = "${reward.tier.colorCode}[${reward.tier.displayName}]"
        player.sendSystemMessage(
            Component.literal("§6★ $tierPrefix §eAdvancement Unlocked: §f${reward.title}")
        )
        val rewardSummary = buildString {
            if (reward.currencyPayout > 0L) append("§a+${reward.currencyPayout} coins ")
            if (reward.combatXp > 0.0) append("§c+${reward.combatXp.toInt()} Combat XP ")
            if (reward.builderXp > 0.0) append("§9+${reward.builderXp.toInt()} Builder XP ")
            if (reward.gathererXp > 0.0) append("§e+${reward.gathererXp.toInt()} Gatherer XP ")
            if (reward.explorerXp > 0.0) append("§b+${reward.explorerXp.toInt()} Explorer XP ")
            if (reward.recoveryCatalystCount > 0) append("§d+${reward.recoveryCatalystCount}x Catalyst ")
        }
        if (rewardSummary.isNotEmpty()) {
            player.sendSystemMessage(Component.literal("   §7Rewards: $rewardSummary"))
        }

        // 5. Server-wide broadcast for Master & Legendary achievements
        if (reward.broadcastServer) {
            val server = player.server
            server.playerList.broadcastSystemMessage(
                Component.literal("§6🏆 §b${player.gameProfile.name} §7has achieved $tierPrefix §6${reward.title}§7!"),
                false
            )
            dev.rmoseley.smpmod.server.DiscordWebhookDispatcher.notifyAdvancement(
                player.gameProfile.name,
                reward.title,
                reward.tier.displayName,
                rewardSummary
            )
        }
    }
}
