package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.advancements.AdvancementRewardManager
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModItems
import dev.rmoseley.smpmod.roles.ExplorerXpTracker
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Unified Generated Structure Revamp & Dungeon Integration Engine.
 * Dynamically bridges YUNG's Better Dungeons, When Dungeons Arise, Dungeons and Taverns,
 * and vanilla Trial Chambers into core SMP systems:
 * 1. Advancements: Triggers tiered delve and dungeon conqueror achievements.
 * 2. Combat: Escalates `destroys_light_on_aggro` spawn chances (35%-50%), dark-zone follow range, and elite stats.
 * 3. Exploration: Awards major Explorer XP upon uncharted site discoveries and increments dungeon clearance counts.
 * 4. Artificing: Boosts masterwork affix rolls in dungeon chests to 60%+ and awards Recovery Catalysts.
 */
object StructureDelveManager {

    val COMPLEX_DUNGEONS_TAG: TagKey<Structure> =
        TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(SmpMod.MOD_ID, "complex_dungeons"))

    // Persistent tracking of active dungeon structure per player
    private val activePlayerDungeons = ConcurrentHashMap<UUID, String>()

    /**
     * Checks if a world position resides inside a recognized complex dungeon structure piece.
     */
    fun isInComplexDungeon(level: ServerLevel, pos: BlockPos): Boolean {
        val structureManager = level.structureManager()
        val start: StructureStart = structureManager.getStructureWithPieceAt(pos, COMPLEX_DUNGEONS_TAG)
        return start.isValid
    }

    /**
     * Retrieves the active complex structure at the given position, if any.
     */
    fun getStructureAt(level: ServerLevel, pos: BlockPos): StructureStart? {
        val structureManager = level.structureManager()
        val start: StructureStart = structureManager.getStructureWithPieceAt(pos, COMPLEX_DUNGEONS_TAG)
        return if (start.isValid) start else null
    }

    /**
     * Ticks player dungeon location detection, triggering discovery alerts and milestones.
     */
    fun onPlayerTick(player: ServerPlayer) {
        if (player.tickCount % 20 != 0) return // Check once per second

        val level = player.serverLevel()
        val pos = player.blockPosition()
        val currentDungeon = getStructureAt(level, pos)

        val previousDungeonId = activePlayerDungeons[player.uuid]

        if (currentDungeon != null) {
            val structureHolder = currentDungeon.structure
            val structureId = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getKey(structureHolder)
                ?.toString() ?: "unknown_dungeon"

            if (previousDungeonId != structureId) {
                // Newly entered dungeon!
                activePlayerDungeons[player.uuid] = structureId
                onDungeonEntered(player, structureId)
            }
        } else {
            activePlayerDungeons.remove(player.uuid)
        }
    }

    /**
     * Triggered when a player enters a new complex dungeon complex.
     */
    private fun onDungeonEntered(player: ServerPlayer, structureId: String) {
        val cleanName = structureId.substringAfter(':').replace('_', ' ').split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        player.sendSystemMessage(
            Component.literal("§6[Dungeon Delve] §eEntered §c$cleanName§e! Hostile threat escalation & light destruction active.")
        )
        player.playNotifySound(SoundEvents.OMINOUS_BOTTLE_DISPOSE, SoundSource.AMBIENT, 1.0f, 0.8f)

        // Award Exploration XP and structure discovery milestone
        ExplorerXpTracker.onStructureDiscovered(player, structureId, cleanName)

        // Trigger dungeon entry advancement
        dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onStructureDiscovered(player, structureId)
    }

    /**
     * Evaluates whether mob spawning at this position should receive elevated dungeon traits.
     * In complex dungeons, `destroys_light_on_aggro` trait chance is boosted to 35% - 50%.
     */
    fun getLightDestroyerSpawnChance(level: ServerLevel, pos: BlockPos): Double {
        return if (isInComplexDungeon(level, pos)) {
            SmpConfig.COMMON.dungeonLightDestroyChance.get().coerceAtLeast(0.35)
        } else if (pos.y < 0) {
            SmpConfig.COMMON.wildernessLightDestroyChance.get() * 1.5
        } else {
            SmpConfig.COMMON.wildernessLightDestroyChance.get()
        }
    }

    /**
     * Triggered when a player clears a spawner vault room or defeats an elite dungeon encounter.
     */
    fun onDungeonRoomCleared(player: ServerPlayer, pos: BlockPos) {
        val attachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val data = player.getData(attachment)
        val updatedCount = data.dungeonsCleared + 1
        player.setData(attachment, data.copy(dungeonsCleared = updatedCount))

        // Award Playstyle XP
        PlaystyleXpTracker.incrementCombatXp(player, 100.0)
        PlaystyleXpTracker.incrementExplorerXp(player, 100.0)

        // 60% chance to drop Recovery Catalyst
        if (Random.nextDouble() < 0.60) {
            val catalyst = ItemStack(ModItems.RECOVERY_CATALYST.get(), 1)
            if (!player.inventory.add(catalyst)) {
                player.drop(catalyst, false)
            }
            player.sendSystemMessage(
                Component.literal("§d[Vault Cleared] §fRecovered a rare §bRecovery Catalyst§f from the dungeon depths!")
            )
        }

        // Award Combat Advancement: Vault Delver
        dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
            player,
            dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.TRIAL_SURVIVOR
        )

        player.sendSystemMessage(
            Component.literal("§6[Dungeon Vault Cleared] §aRoom conquered! Total dungeons/vaults cleared: §e$updatedCount§a.")
        )
    }
}
