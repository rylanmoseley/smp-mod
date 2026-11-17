package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import kotlin.math.abs

/**
 * Manages server liquidity faucets:
 * 1. One-time onboarding starter milestones.
 * 2. Repeatable active-play milestone cycles (every Y natural blocks harvested, dungeons cleared).
 * 3. Anti-AFK verified active playtime stipends.
 */
object ProgressionFaucetManager {

    // Number of active, non-AFK ticks required for an active playtime stipend (15 minutes = 18000 ticks)
    const val ACTIVE_PLAY_INTERVAL_TICKS: Long = 18000L
    const val ACTIVE_PLAY_STIPEND_PAYOUT: Long = 50L

    // Idle threshold before non-AFK accumulation halts (3000 ticks = 2.5 minutes of inactivity)
    const val IDLE_THRESHOLD_TICKS: Long = 3000L

    /**
     * Attempts to award a one-time starter progression milestone.
     * Returns true if successfully claimed, false if already completed.
     */
    fun awardStarterMilestone(player: ServerPlayer, milestone: StarterMilestone): Boolean {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val currentData = player.getData(attachment)

        if (currentData.claimedMilestones.contains(milestone.id)) {
            return false // Already claimed
        }

        val payout = SmpConfig.COMMON.starterAdvancementPayout.get().toLong().coerceAtLeast(milestone.defaultPayout)
        val updatedClaimed = currentData.claimedMilestones.toMutableSet()
        updatedClaimed.add(milestone.id)

        player.setData(attachment, currentData.copy(claimedMilestones = updatedClaimed))

        // Credit digital non-material balance
        EconomyManager.credit(
            player,
            payout,
            TransactionType.MILESTONE_STARTER,
            "Starter Milestone: ${milestone.displayName}"
        )

        player.sendSystemMessage(
            Component.literal("§6[Milestone] §fCompleted '§e${milestone.displayName}§f'! Received §a+$payout coins§f.")
        )
        return true
    }

    /**
     * Records a verified natural block harvest, advancing the repeatable harvest milestone cycle.
     */
    fun onNaturalBlockHarvest(player: Player) {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val currentData = player.getData(attachment)
        val newHarvestTotal = currentData.totalNaturalBlocksHarvested + 1L

        player.setData(attachment, currentData.copy(totalNaturalBlocksHarvested = newHarvestTotal))

        if (player is ServerPlayer) {
            dev.rmoseley.smpmod.advancements.EconomyAdvancements.awardFirstHarvest(player)
        }

        // Check if repeatable milestone cycle threshold is reached
        val threshold = SmpConfig.COMMON.repeatableHarvestThreshold.get().toLong()
        if (threshold > 0L && newHarvestTotal % threshold == 0L) {
            val payout = SmpConfig.COMMON.repeatableHarvestPayout.get().toLong()
            EconomyManager.credit(
                player,
                payout,
                TransactionType.MILESTONE_REPEATABLE_HARVEST,
                "Repeatable Harvest Cycle Milestone ($newHarvestTotal blocks)"
            )

            // Synergize with Gatherer XP
            PlaystyleXpTracker.awardGathererXp(player, 15.0, false)

            player.sendSystemMessage(
                Component.literal("§a[Harvest Milestone] §fCompleted $newHarvestTotal natural harvests! Rewarded §e+$payout coins§f and bonus Gatherer XP.")
            )
        }
    }

    /**
     * Records a verified construction block placement within claimed territory,
     * advancing recurring construction dividends for active builders.
     */
    fun onConstructionBlockPlaced(player: Player, varietyScore: Double, block: net.minecraft.world.level.block.Block) {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val currentData = player.getData(attachment)
        val newConstructedTotal = currentData.totalBlocksConstructed + 1L

        player.setData(attachment, currentData.copy(totalBlocksConstructed = newConstructedTotal))

        // Every 25 placed blocks in a claim earns a recurring construction dividend
        val threshold = 25L
        if (newConstructedTotal % threshold == 0L) {
            val bonusFromVariety = (varietyScore * 5.0).coerceIn(0.0, 25.0).toLong()
            val payout = 25L + bonusFromVariety

            EconomyManager.credit(
                player,
                payout,
                TransactionType.CONSTRUCTION_DIVIDEND,
                "Recurring Construction Dividend ($newConstructedTotal blocks placed)"
            )

            // Grant bonus Builder XP
            PlaystyleXpTracker.incrementBuilderXp(player, 15.0)

            player.sendSystemMessage(
                Component.literal("§9[Construction Dividend] §fCompleted milestone ($newConstructedTotal blocks placed)! Rewarded §a+$payout coins§f and bonus Builder XP.")
            )
        }
    }

    /**
     * Records completion/clearing of a dungeon or boss encounter.
     */
    fun onDungeonCleared(player: ServerPlayer, dungeonType: String, basePayout: Long = 150L) {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val currentData = player.getData(attachment)
        val newDungeonCount = currentData.dungeonsCleared + 1

        player.setData(attachment, currentData.copy(dungeonsCleared = newDungeonCount))

        EconomyManager.credit(
            player,
            basePayout,
            TransactionType.MILESTONE_DUNGEON,
            "Dungeon Clear: $dungeonType (#$newDungeonCount)"
        )

        PlaystyleXpTracker.awardCombatXp(player, mobTier = 3, timeToKillSeconds = 12.0, wasCritical = true)
        PlaystyleXpTracker.awardGathererXp(player, 25.0, true)

        player.sendSystemMessage(
            Component.literal("§c[Dungeon Cleared] §fConquered $dungeonType! Rewarded §e+$basePayout coins§f.")
        )
    }

    /**
     * Ticked periodically for each player to track active non-AFK playtime.
     * Evaluates positional and look delta. Standing motionless halts accumulation.
     */
    fun onPlayerTick(player: ServerPlayer) {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val data = player.getData(attachment)

        val posX = player.x
        val posY = player.y
        val posZ = player.z
        val yaw = player.yRot
        val pitch = player.xRot

        val distSq = (posX - data.lastActiveCheckX) * (posX - data.lastActiveCheckX) +
                (posY - data.lastActiveCheckY) * (posY - data.lastActiveCheckY) +
                (posZ - data.lastActiveCheckZ) * (posZ - data.lastActiveCheckZ)
        val rotDelta = abs(yaw - data.lastActiveYaw) + abs(pitch - data.lastActivePitch)

        val isActivelyPlaying = distSq > 0.0025 || rotDelta > 1.5f

        val updatedIdleTicks: Long
        var updatedActiveTicks = data.activePlaytimeTicks

        if (isActivelyPlaying) {
            updatedIdleTicks = 0L
            updatedActiveTicks += 1L
        } else {
            updatedIdleTicks = data.idleTicks + 1L
            // If idle exceeds threshold, player is deemed AFK: active ticks freeze
            if (updatedIdleTicks < IDLE_THRESHOLD_TICKS) {
                updatedActiveTicks += 1L
            }
        }

        // Check if an active non-AFK playtime block milestone has elapsed
        if (updatedActiveTicks >= ACTIVE_PLAY_INTERVAL_TICKS) {
            updatedActiveTicks = 0L
            EconomyManager.credit(
                player,
                ACTIVE_PLAY_STIPEND_PAYOUT,
                TransactionType.ACTIVE_PLAY_STIPEND,
                "Active Non-AFK Playtime Stipend (15 min)"
            )
            player.sendSystemMessage(
                Component.literal("§2[Active Play] §fAwarded §a+$ACTIVE_PLAY_STIPEND_PAYOUT coins§f for active non-AFK server participation.")
            )
        }

        player.setData(
            attachment,
            data.copy(
                activePlaytimeTicks = updatedActiveTicks,
                idleTicks = updatedIdleTicks,
                lastActiveCheckX = posX,
                lastActiveCheckY = posY,
                lastActiveCheckZ = posZ,
                lastActiveYaw = yaw,
                lastActivePitch = pitch
            )
        )
    }

    /**
     * Returns total natural harvests recorded for the player.
     */
    fun getTotalNaturalHarvests(player: Player): Long {
        return player.getData(ModAttachmentTypes.PLAYER_MILESTONES.get()).totalNaturalBlocksHarvested
    }

    /**
     * Returns total dungeons cleared recorded for the player.
     */
    fun getDungeonsCleared(player: Player): Int {
        return player.getData(ModAttachmentTypes.PLAYER_MILESTONES.get()).dungeonsCleared
    }
}
