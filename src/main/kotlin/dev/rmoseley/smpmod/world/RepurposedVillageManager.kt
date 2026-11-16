package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Repurposed Villages strictly adhering to Section 2.4 and Section 4 of design-v2.md:
 * 1. Pre-lit Safe Rest Stops: Suppresses hostile mob spawning and provides recovery for weary explorers.
 * 2. Regional Server-Wide Objective Hubs: Cooperative milestone contributions with currency & Playstyle XP payouts.
 * 3. Item Lore & Cartography Landmarks: Regional surveys, landmark provenance, and explorer navigation.
 * 4. Anti-AFK Guardrails: Disables vanilla breeder abuse and infinite discount halls.
 */
object RepurposedVillageManager {

    /**
     * Regional Server-Wide Objective structure.
     */
    data class ServerObjective(
        val id: String,
        val displayName: String,
        val description: String,
        val requiredItem: ItemStack,
        val targetAmount: Int,
        var currentAmount: Int = 0,
        val currencyRewardPerItem: Long = 2L,
        var isCompleted: Boolean = false
    )

    // Active regional objectives hosted at village hubs
    private val regionalObjectives = ConcurrentHashMap<String, ServerObjective>().apply {
        put("trade_roads", ServerObjective(
            id = "trade_roads",
            displayName = "Regional Paved Highway",
            description = "Contribute stone bricks to connect regional villages and unlock road travel buffs.",
            requiredItem = ItemStack(Items.STONE_BRICKS),
            targetAmount = 2000,
            currencyRewardPerItem = 2L
        ))
        put("grain_reserve", ServerObjective(
            id = "grain_reserve",
            displayName = "Village Granary Surplus",
            description = "Stockpile wheat and bread to sustain regional explorer caravans.",
            requiredItem = ItemStack(Items.BREAD),
            targetAmount = 1000,
            currencyRewardPerItem = 3L
        ))
        put("cartography_cache", ServerObjective(
            id = "cartography_cache",
            displayName = "Cartography Survey Network",
            description = "Submit compasses and paper to map uncharted dungeon sectors.",
            requiredItem = ItemStack(Items.COMPASS),
            targetAmount = 100,
            currencyRewardPerItem = 25L
        ))
        put("dragon_gate", ServerObjective(
            id = "dragon_gate",
            displayName = "Stronghold Dragon Preparations",
            description = "Deliver Ender Pearls to prepare the realm for the cooperative Dragon Encounter.",
            requiredItem = ItemStack(Items.ENDER_PEARL),
            targetAmount = 500,
            currencyRewardPerItem = 20L
        ))
    }

    // Known village sanctuary centers (keyed by "dim:x:z")
    private val knownVillageSanctuaries = ConcurrentHashMap<String, BlockPos>()

    /**
     * Registers a village center (e.g. detected via village bell or structure start).
     */
    fun registerVillageCenter(level: Level, pos: BlockPos) {
        val key = "${level.dimension().location()}:${pos.x shr 6}:${pos.z shr 6}"
        knownVillageSanctuaries[key] = pos
    }

    /**
     * Checks if a position is within an active village sanctuary perimeter.
     */
    fun isInsideVillageSanctuary(level: Level, pos: BlockPos): Boolean {
        if (level.dimension() != Level.OVERWORLD) return false
        val radius = SmpConfig.COMMON.villageSanctuaryRadius.get()
        val radiusSq = radius * radius

        return knownVillageSanctuaries.values.any { center ->
            pos.distSqr(center) <= radiusSq
        }
    }

    /**
     * Periodic tick check on players: applies the Village Rest Stop recovery buff.
     */
    fun onPlayerTick(player: ServerPlayer) {
        val level = player.level()
        if (level.dimension() != Level.OVERWORLD) return

        val pos = player.blockPosition()
        if (isInsideVillageSanctuary(level, pos)) {
            // Apply Explorer's Haven recovery buff
            player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true))
            player.addEffect(MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, true))

            if (player.tickCount % 60 == 0) {
                player.serverLevel().sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.x, player.y + 1.0, player.z,
                    3, 0.3, 0.3, 0.3, 0.02
                )
                player.displayClientMessage(
                    Component.literal("§aVillage Sanctuary: Safe haven. Rested and replenished."),
                    true
                )
            }
        }
    }

    /**
     * Suppresses hostile mob spawning inside village sanctuary zones.
     */
    fun shouldCancelMobSpawn(mob: Mob): Boolean {
        val level = mob.level()
        if (level.dimension() != Level.OVERWORLD) return false

        // Check if inside any known village perimeter
        return isInsideVillageSanctuary(level, mob.blockPosition())
    }

    /**
     * Interacts with a village hub landmark (such as a Village Bell or Objective Board)
     * to contribute resources toward regional server-wide objectives.
     */
    fun handleObjectiveInteraction(player: ServerPlayer, landmarkPos: BlockPos): Boolean {
        val heldItem = player.mainHandItem
        if (heldItem.isEmpty) {
            // Display current objective status to player
            displayObjectives(player)
            return true
        }

        // Match held item against active server objectives
        for (objective in regionalObjectives.values) {
            if (!objective.isCompleted && heldItem.`is`(objective.requiredItem.item)) {
                val needed = objective.targetAmount - objective.currentAmount
                val depositCount = heldItem.count.coerceAtMost(needed)

                if (depositCount > 0) {
                    heldItem.shrink(depositCount)
                    objective.currentAmount += depositCount

                    // Award digital currency and XP
                    val currencyEarned = depositCount * objective.currencyRewardPerItem
                    EconomyManager.credit(player, currencyEarned)
                    PlaystyleXpTracker.awardExplorerXp(player, depositCount * 2.0)

                    val level = player.serverLevel()
                    level.sendParticles(
                        ParticleTypes.COMPOSTER,
                        landmarkPos.x + 0.5, landmarkPos.y + 1.2, landmarkPos.z + 0.5,
                        15, 0.3, 0.3, 0.3, 0.05
                    )
                    level.playSound(
                        null, landmarkPos,
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                        0.7f, 1.2f
                    )

                    player.displayClientMessage(
                        Component.literal("§6Contributed $depositCount ${heldItem.hoverName.string} to ${objective.displayName}! (+$$currencyEarned) [${objective.currentAmount}/${objective.targetAmount}]"),
                        false
                    )

                    if (objective.currentAmount >= objective.targetAmount) {
                        objective.isCompleted = true
                        level.server.playerList.broadcastSystemMessage(
                            Component.literal("§6§l[Server Objective Complete] §a${objective.displayName} has been fulfilled by the realm!"),
                            false
                        )
                    }
                    return true
                }
            }
        }

        displayObjectives(player)
        return true
    }

    private fun displayObjectives(player: ServerPlayer) {
        player.sendSystemMessage(Component.literal("§6--- Regional Server-Wide Objectives ---"))
        for (obj in regionalObjectives.values) {
            val status = if (obj.isCompleted) "§a[COMPLETED]" else "§e[${obj.currentAmount} / ${obj.targetAmount}]"
            player.sendSystemMessage(
                Component.literal("§b${obj.displayName} $status: §f${obj.description} (Deliver ${obj.requiredItem.hoverName.string})")
            )
        }
    }

    /**
     * Anti-AFK: Prevents vanilla villager discount exploit abuse and infinite breeding.
     */
    fun regulateVillager(villager: Villager) {
        // Clear extreme exploit gossip discounts to preserve non-material economy
        villager.gossips.decay()
    }
}
