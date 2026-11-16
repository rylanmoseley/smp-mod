package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.ElderGuardian
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tracks Combat XP evaluated by:
 * 1. Encounter speed (Time-To-Kill / combat tempo)
 * 2. Hit accuracy (hit precision, crits, and low whiff rate)
 * 3. Mob tier scaling
 * 4. Chunk / radius grinder decay (diminishing returns on repetitive kills)
 */
object CombatXpTracker {

    data class EncounterState(
        val targetUuid: UUID,
        val startTimeMs: Long,
        var lastHitTimeMs: Long,
        var hits: Int = 0,
        var crits: Int = 0,
        var whiffs: Int = 0,
        var damageDealt: Double = 0.0
    )

    data class AccuracyStats(
        var totalSwings: Long = 0L,
        var successfulHits: Long = 0L,
        var whiffs: Long = 0L,
        var lastSwingMs: Long = 0L
    )

    // Player UUID -> Target Entity UUID -> Encounter State
    private val activeEncounters = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, EncounterState>>()

    // Player UUID -> Accuracy Stats
    private val playerAccuracy = ConcurrentHashMap<UUID, AccuracyStats>()

    // Chunk Pos -> Timestamps of mob deaths in recent window
    private val chunkKillHistory = ConcurrentHashMap<ChunkPos, CopyOnWriteArrayList<Long>>()

    private const val RECENT_WINDOW_MS = 300_000L // 5 minutes

    /**
     * Records an arm swing / attack attempt that hit no entity (whiff).
     */
    fun recordWhiff(player: Player) {
        val stats = playerAccuracy.computeIfAbsent(player.uuid) { AccuracyStats() }
        stats.totalSwings++
        stats.whiffs++
        stats.lastSwingMs = System.currentTimeMillis()

        // Also record to any ongoing active encounter within 10m
        activeEncounters[player.uuid]?.values?.forEach { encounter ->
            if (System.currentTimeMillis() - encounter.lastHitTimeMs < 5000L) {
                encounter.whiffs++
            }
        }
    }

    /**
     * Records a successful attack landing on an entity.
     */
    fun recordHit(player: Player, target: LivingEntity, damage: Double, isCrit: Boolean) {
        val now = System.currentTimeMillis()
        val stats = playerAccuracy.computeIfAbsent(player.uuid) { AccuracyStats() }
        stats.totalSwings++
        stats.successfulHits++
        stats.lastSwingMs = now

        val encounters = activeEncounters.computeIfAbsent(player.uuid) { ConcurrentHashMap() }
        val encounter = encounters.computeIfAbsent(target.uuid) {
            EncounterState(
                targetUuid = target.uuid,
                startTimeMs = now,
                lastHitTimeMs = now
            )
        }

        encounter.lastHitTimeMs = now
        encounter.hits++
        if (isCrit) {
            encounter.crits++
        }
        encounter.damageDealt += damage
    }

    /**
     * Evaluates and awards Combat XP upon an entity's death.
     */
    fun onEntityDeath(player: Player, victim: LivingEntity) {
        val encounters = activeEncounters[player.uuid]
        val encounter = encounters?.remove(victim.uuid)
        val now = System.currentTimeMillis()

        val ttkSeconds = if (encounter != null) {
            ((now - encounter.startTimeMs) / 1000.0).coerceAtLeast(0.5)
        } else {
            3.0 // Fallback estimate if kill was instant or untracked
        }

        val hits = encounter?.hits ?: 1
        val crits = encounter?.crits ?: 0
        val whiffs = encounter?.whiffs ?: 0

        // 1. Evaluate Mob Tier
        val mobTier = evaluateMobTier(victim)
        val baseTierXp = when (mobTier) {
            5 -> 120.0 // Ender Dragon
            4 -> 60.0  // Warden, Wither, Elder Guardian
            3 -> 35.0  // Dark-zone elites (destroys_light_on_aggro)
            2 -> 20.0  // Dangerous hostiles (Blaze, Enderman, Evoker, Brute, etc.)
            else -> 10.0 // Standard hostiles
        }

        // 2. Encounter Speed (Tempo) Factor
        val speedWeight = SmpConfig.COMMON.combatXpSpeedWeight.get()
        // Fast combat tempo yields high multiplier; slow corner-cheesing yields minimal
        val speedFactor = (8.0 / ttkSeconds).coerceIn(0.4, 2.8)

        // 3. Accuracy & Critical Hit Factor
        val accuracyWeight = SmpConfig.COMMON.combatXpAccuracyWeight.get()
        val accuracyRatio = (hits.toDouble() / (hits + whiffs).coerceAtLeast(1)).coerceIn(0.2, 1.0)
        val critRatio = (crits.toDouble() / hits.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val accuracyFactor = (0.6 + 0.4 * accuracyRatio) * (1.0 + (critRatio * (accuracyWeight - 1.0)))

        // 4. Chunk / Radius Anti-Grinder Decay
        val grinderDecay = calculateGrinderDecay(victim.blockPosition())

        // Record this kill in chunk history
        recordChunkKill(victim.blockPosition())

        // Combined Combat XP Calculation
        val finalXp = baseTierXp * speedFactor * speedWeight * accuracyFactor * grinderDecay
        PlaystyleXpTracker.incrementCombatXp(player, finalXp)
    }

    fun evaluateMobTier(entity: LivingEntity): Int {
        if (entity is EnderDragon) return 5
        if (entity is Warden || entity is WitherBoss || entity is ElderGuardian) return 4

        // Dark-zone elite trait: destroys_light_on_aggro
        val destroysLight = entity.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        if (destroysLight != null && destroysLight.value > 0.5) {
            return 3
        }

        val name = entity.type.descriptionId.lowercase()
        if (name.contains("blaze") || name.contains("enderman") || name.contains("evoker") ||
            name.contains("piglin_brute") || name.contains("ravager") || name.contains("guardian") ||
            name.contains("vindicator") || name.contains("shulker") || name.contains("breeze") ||
            name.contains("bogged") || name.contains("witch")
        ) {
            return 2
        }

        return 1
    }

    /**
     * Calculates chunk and radius decay to penalize automated mob drop chutes,
     * spawner farms, and static AFK traps.
     */
    fun calculateGrinderDecay(pos: BlockPos): Double {
        val centerChunk = ChunkPos(pos)
        val radius = SmpConfig.COMMON.grinderChunkDecayRadius.get()
        val now = System.currentTimeMillis()
        val cutoff = now - RECENT_WINDOW_MS

        var totalRecentKills = 0
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val chunk = ChunkPos(centerChunk.x + dx, centerChunk.z + dz)
                val timestamps = chunkKillHistory[chunk] ?: continue
                totalRecentKills += timestamps.count { it >= cutoff }
            }
        }

        val threshold = SmpConfig.COMMON.grinderKillsDecayThreshold.get()
        if (totalRecentKills <= threshold) {
            return 1.0
        }

        // Steep decay curve: 1.0 / (1.0 + 0.25 * (kills - threshold))
        val excessKills = totalRecentKills - threshold
        return (1.0 / (1.0 + 0.25 * excessKills)).coerceIn(0.05, 1.0)
    }

    private fun recordChunkKill(pos: BlockPos) {
        val chunk = ChunkPos(pos)
        val now = System.currentTimeMillis()
        val list = chunkKillHistory.computeIfAbsent(chunk) { CopyOnWriteArrayList() }
        list.add(now)

        // Prune entries older than window
        val cutoff = now - RECENT_WINDOW_MS
        list.removeIf { it < cutoff }
    }

    fun cleanExpiredEncounters() {
        val now = System.currentTimeMillis()
        val expireThreshold = 30_000L // 30 seconds idle
        activeEncounters.values.forEach { map ->
            map.values.removeIf { now - it.lastHitTimeMs > expireThreshold }
        }
    }
}
