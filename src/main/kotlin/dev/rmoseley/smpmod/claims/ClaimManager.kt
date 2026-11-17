package dev.rmoseley.smpmod.claims

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.neoforged.fml.ModList
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Manages chunk claims and contiguous settlement cluster development leveling.
 *
 * Core Architectural Rules:
 * 1. Chunk claims CAN be noncontiguous anywhere in the world (outposts, remote mines, portals).
 * 2. Only contiguous chunks sharing orthogonal borders with a given claim form a cluster,
 *    and only contiguous chunks within that specific cluster contribute to that claim's:
 *    - Development Leveling
 *    - Variety-weighted block score
 *    - Pooled territorial defensive buffs (damage reduction, health regen, light snuff reduction)
 *    - Temporary builder flight boundaries
 * 3. Individual Builder XP Isolation: Builder XP is awarded strictly to the builder who placed
 *    the block, preventing passive leeching.
 * 4. Claim Cap Scaling: Player max chunk cap scales with their individual Builder Playstyle Level.
 */
object ClaimManager {

    val provider: IClaimProvider by lazy {
        if (ModList.get().isLoaded("openpartiesandclaims")) {
            SmpMod.LOGGER.info("Open Parties and Claims (OPAC) detected. Using OpacClaimProvider.")
            OpacClaimProvider()
        } else {
            SmpMod.LOGGER.info("Open Parties and Claims not detected. Using InternalClaimProvider fallback.")
            InternalClaimProvider()
        }
    }

    /**
     * Per-chunk construction ledger storing net block counts by block registry ID.
     * Keyed by "dimensionLocation:chunkX:chunkZ".
     */
    data class ChunkConstructionLedger(
        val blockCounts: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
    ) {
        fun addPlacement(blockId: String): Long {
            return blockCounts.compute(blockId) { _, current -> (current ?: 0L) + 1L } ?: 1L
        }

        fun removePlacement(blockId: String): Long {
            return blockCounts.compute(blockId) { _, current ->
                val updated = (current ?: 0L) - 1L
                if (updated <= 0L) null else updated
            } ?: 0L
        }

        fun getCount(blockId: String): Long = blockCounts[blockId] ?: 0L
        fun getTotalNetBlocks(): Long = blockCounts.values.sum()
        fun getUniquePaletteSize(): Int = blockCounts.size
    }

    private val chunkLedgers = ConcurrentHashMap<String, ChunkConstructionLedger>()

    // Combat timestamps per player for out-of-combat territorial regen
    private val lastCombatTimestamps = ConcurrentHashMap<UUID, Long>()

    private fun makeChunkKey(dimId: String, pos: ChunkPos): String {
        return "$dimId:${pos.x}:${pos.z}"
    }

    // =========================================================================
    // 1. Claim Status & Ownership
    // =========================================================================

    fun isChunkClaimed(level: Level, pos: ChunkPos): Boolean {
        return provider.isChunkClaimed(level, pos)
    }

    fun isChunkClaimedBy(level: Level, pos: ChunkPos, player: Player): Boolean {
        return provider.isClaimedBy(level, pos, player)
    }

    fun getClaimOwner(level: Level, pos: ChunkPos): UUID? {
        return provider.getClaimOwner(level, pos)
    }

    // =========================================================================
    // 2. Contiguous Cluster Breadth-First Search (BFS) Partitioning
    // =========================================================================

    /**
     * Identifies the maximal contiguous cluster containing the given position.
     * Uses Breadth-First Search (BFS) over orthogonal chunk neighbors.
     */
    fun getContiguousCluster(level: Level, pos: ChunkPos): ContiguousCluster? {
        val ownerUuid = getClaimOwner(level, pos) ?: return null
        val dimId = level.dimension().location().toString()

        // Retrieve all chunks belonging to this owner or their party in this dimension
        val allOwnedChunks = provider.getAllClaimedChunks(level, ownerUuid)
        if (!allOwnedChunks.contains(pos)) return null

        // BFS flood-fill on orthogonal neighbors (North, South, East, West)
        val clusterChunks = mutableSetOf<ChunkPos>()
        val queue = ArrayDeque<ChunkPos>()
        queue.add(pos)
        clusterChunks.add(pos)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val neighbors = listOf(
                ChunkPos(current.x + 1, current.z),
                ChunkPos(current.x - 1, current.z),
                ChunkPos(current.x, current.z + 1),
                ChunkPos(current.x, current.z - 1)
            )

            for (neighbor in neighbors) {
                if (allOwnedChunks.contains(neighbor) && clusterChunks.add(neighbor)) {
                    queue.add(neighbor)
                }
            }
        }

        return buildClusterFromChunks(dimId, ownerUuid, clusterChunks, provider.getPartyMembers(level, ownerUuid))
    }

    /**
     * Partitions all chunks claimed by an owner into their maximal contiguous settlement clusters.
     */
    fun getAllContiguousClusters(level: Level, ownerUuid: UUID): List<ContiguousCluster> {
        val dimId = level.dimension().location().toString()
        val allChunks = provider.getAllClaimedChunks(level, ownerUuid).toMutableSet()
        val partyMembers = provider.getPartyMembers(level, ownerUuid)
        val clusters = mutableListOf<ContiguousCluster>()

        while (allChunks.isNotEmpty()) {
            val seed = allChunks.first()
            val clusterChunks = mutableSetOf<ChunkPos>()
            val queue = ArrayDeque<ChunkPos>()
            queue.add(seed)
            clusterChunks.add(seed)
            allChunks.remove(seed)

            while (queue.isNotEmpty()) {
                val current = queue.poll()
                val neighbors = listOf(
                    ChunkPos(current.x + 1, current.z),
                    ChunkPos(current.x - 1, current.z),
                    ChunkPos(current.x, current.z + 1),
                    ChunkPos(current.x, current.z - 1)
                )

                for (neighbor in neighbors) {
                    if (allChunks.remove(neighbor)) {
                        clusterChunks.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }

            clusters.add(buildClusterFromChunks(dimId, ownerUuid, clusterChunks, partyMembers))
        }

        return clusters
    }

    private fun buildClusterFromChunks(
        dimId: String,
        ownerUuid: UUID,
        chunks: Set<ChunkPos>,
        partyMembers: Set<UUID>
    ): ContiguousCluster {
        // Aggregate net block counts across all chunks in this contiguous cluster
        val aggregatedCounts = mutableMapOf<String, Long>()
        var totalNetBlocks = 0L

        for (chunk in chunks) {
            val key = makeChunkKey(dimId, chunk)
            val ledger = chunkLedgers[key] ?: continue
            for ((blockId, count) in ledger.blockCounts) {
                if (count > 0L) {
                    aggregatedCounts[blockId] = (aggregatedCounts[blockId] ?: 0L) + count
                    totalNetBlocks += count
                }
            }
        }

        val varietyScore = VarietyScorer.calculateClusterVarietyScore(aggregatedCounts)
        val devLevel = calculateClusterDevelopmentLevel(chunks.size, varietyScore, totalNetBlocks)
        val tier = SettlementTier.fromChunkCount(chunks.size)

        // Territorial buffs scaling with development level and settlement tier
        val defenseMitigation = (devLevel * 0.02 + tier.ordinal * 0.02).coerceAtMost(0.40)
        val snuffReduction = (devLevel * 0.03 + tier.ordinal * 0.03).coerceAtMost(0.60)
        val regenSeconds = max(3, 12 - (devLevel / 2))

        return ContiguousCluster(
            ownerUuid = ownerUuid,
            dimensionId = dimId,
            chunks = chunks,
            developmentLevel = devLevel,
            paletteVarietyScore = varietyScore,
            pooledDefenseMitigation = defenseMitigation,
            pooledHealthRegenSeconds = regenSeconds,
            lightSnuffReductionPercent = snuffReduction,
            netPlacedBlocks = totalNetBlocks,
            blockCountsByType = aggregatedCounts,
            members = partyMembers
        )
    }

    /**
     * Calculates the development level for a contiguous cluster based on chunk count,
     * variety score, and net placed blocks.
     */
    fun calculateClusterDevelopmentLevel(chunkCount: Int, varietyScore: Double, netBlocks: Long): Int {
        val sizeTierBonus = when {
            chunkCount >= SettlementTier.METROPOLIS.minChunks -> 5
            chunkCount >= SettlementTier.TOWN.minChunks -> 4
            chunkCount >= SettlementTier.SETTLEMENT.minChunks -> 3
            chunkCount >= SettlementTier.HAMLET.minChunks -> 2
            else -> 1
        }
        val varietyFactor = (varietyScore / 75.0).toInt()
        val blockFactor = (netBlocks / 150L).toInt()

        return (sizeTierBonus + varietyFactor + blockFactor).coerceIn(1, 25)
    }

    // =========================================================================
    // 3. Construction Ledger & Block Variety Tracking
    // =========================================================================

    /**
     * Records a block placement in the claim and returns the marginal variety XP score.
     */
    fun recordBlockPlacement(player: Player, level: Level, pos: ChunkPos, block: Block): Double {
        val dimId = level.dimension().location().toString()
        val key = makeChunkKey(dimId, pos)
        val ledger = chunkLedgers.computeIfAbsent(key) { ChunkConstructionLedger() }
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()

        // Query existing count and variety across the contiguous cluster before placement
        val cluster = getContiguousCluster(level, pos)
        val currentBlockCount = cluster?.blockCountsByType?.get(blockId)?.toInt() ?: ledger.getCount(blockId).toInt()
        val uniquePaletteCount = cluster?.blockCountsByType?.size ?: ledger.getUniquePaletteSize()

        // Record placement
        ledger.addPlacement(blockId)

        // Calculate variety-weighted score (with +10% bonus during active settlement tourism prosperity)
        val baseScore = VarietyScorer.calculateBlockPlacementScore(block, currentBlockCount, uniquePaletteCount)
        return if (cluster != null && TownTreasuryManager.isSettlementProsperous(cluster)) {
            baseScore * 1.10
        } else {
            baseScore
        }
    }

    /**
     * Records a block break in the claim and returns the variety score deducted.
     */
    fun recordBlockBreak(player: Player, level: Level, pos: ChunkPos, block: Block): Double {
        val dimId = level.dimension().location().toString()
        val key = makeChunkKey(dimId, pos)
        val ledger = chunkLedgers[key] ?: return 0.0
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()

        val cluster = getContiguousCluster(level, pos)
        val currentBlockCount = cluster?.blockCountsByType?.get(blockId)?.toInt() ?: ledger.getCount(blockId).toInt()
        val uniquePaletteCount = cluster?.blockCountsByType?.size ?: ledger.getUniquePaletteSize()

        ledger.removePlacement(blockId)

        return VarietyScorer.calculateBlockPlacementScore(block, currentBlockCount, uniquePaletteCount)
    }

    fun getChunkBlockData(level: Level, pos: ChunkPos): Map<String, Long> {
        val dimId = level.dimension().location().toString()
        val key = makeChunkKey(dimId, pos)
        return chunkLedgers[key]?.blockCounts?.toMap() ?: emptyMap()
    }

    // =========================================================================
    // 4. Builder Playstyle Level & Claim Cap Scaling
    // =========================================================================

    /**
     * Converts raw Builder Playstyle XP into Builder Level.
     * Level 0 at 0 XP, Level 1 at 100 XP, Level 2 at 300 XP, Level 3 at 600 XP, etc.
     */
    fun calculateBuilderLevel(builderXp: Double): Int {
        if (builderXp <= 0.0) return 0
        return floor((-1.0 + sqrt(1.0 + 8.0 * (builderXp / 50.0))) / 2.0).toInt().coerceAtLeast(0)
    }

    fun getBuilderLevel(player: Player): Int {
        val xpData = player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())
        return calculateBuilderLevel(xpData.builderXp)
    }

    /**
     * Computes the maximum number of chunks a player is permitted to claim anywhere in the world.
     * Scales with Builder level: base + (level * chunksPerLevel).
     */
    fun getMaxClaimChunks(player: Player): Int {
        val baseChunks = SmpConfig.COMMON.maxBaseClaimChunks.get()
        val chunksPerLevel = SmpConfig.COMMON.chunksPerBuilderLevel.get()
        val builderLevel = getBuilderLevel(player)

        return baseChunks + (builderLevel * chunksPerLevel)
    }

    fun canPlayerClaimMoreChunks(level: Level, player: Player): Boolean {
        val claimedChunks = provider.getAllClaimedChunks(level, player.uuid).size
        val maxAllowed = getMaxClaimChunks(player)
        return claimedChunks < maxAllowed
    }

    fun getRemainingClaimChunks(level: Level, player: Player): Int {
        val claimedChunks = provider.getAllClaimedChunks(level, player.uuid).size
        val maxAllowed = getMaxClaimChunks(player)
        return max(0, maxAllowed - claimedChunks)
    }

    // =========================================================================
    // 5. Territorial Pooled Buffs & Combat Hooks
    // =========================================================================

    fun recordCombatEngagement(player: Player) {
        lastCombatTimestamps[player.uuid] = System.currentTimeMillis()
    }

    fun isPlayerInCombat(player: Player, combatWindowMs: Long = 10_000L): Boolean {
        val lastCombat = lastCombatTimestamps[player.uuid] ?: return false
        return (System.currentTimeMillis() - lastCombat) < combatWindowMs
    }

    /**
     * Applies territorial health regeneration to a player standing inside their contiguous cluster.
     * Only triggers when out of combat and below maximum health.
     */
    fun applyTerritorialRegen(player: ServerPlayer) {
        val chunkPos = ChunkPos(player.blockPosition())
        if (!isChunkClaimedBy(player.level(), chunkPos, player)) return
        if (player.health >= player.maxHealth) return
        if (isPlayerInCombat(player)) return

        val cluster = getContiguousCluster(player.level(), chunkPos) ?: return
        val regenIntervalTicks = cluster.pooledHealthRegenSeconds * 20

        if (player.tickCount % regenIntervalTicks == 0) {
            player.heal(1.0f) // Heals 1 HP (half heart) per interval
        }
    }

    /**
     * Computes the effective light snuff duration for a position, reduced by the contiguous
     * cluster's development leveling.
     */
    fun getEffectiveLightSnuffDuration(level: Level, pos: BlockPos): Int {
        val chunkPos = ChunkPos(pos)
        val cluster = getContiguousCluster(level, chunkPos)
        val baseDuration = SmpConfig.COMMON.lightSnuffDurationSeconds.get().toDouble()

        if (cluster == null) return baseDuration.toInt()

        val reduction = cluster.lightSnuffReductionPercent
        val effective = baseDuration * (1.0 - reduction)
        return effective.coerceAtLeast(5.0).toInt()
    }

    // =========================================================================
    // 6. Economy & Regional Settlement Perks
    // =========================================================================

    fun getSettlementTierAt(level: Level, pos: ChunkPos): SettlementTier {
        val cluster = getContiguousCluster(level, pos) ?: return SettlementTier.OUTPOST
        return cluster.clusterTier
    }

    fun getPlayerHighestSettlementTier(level: Level, player: Player): SettlementTier {
        val clusters = getAllContiguousClusters(level, player.uuid)
        if (clusters.isEmpty()) return SettlementTier.OUTPOST
        return clusters.maxByOrNull { it.chunks.size }?.clusterTier ?: SettlementTier.OUTPOST
    }

    fun getRegionalShopDiscount(player: Player, level: Level, pos: BlockPos): Double {
        val chunkPos = ChunkPos(pos)
        val cluster = getContiguousCluster(level, chunkPos)
        var discount = if (cluster != null && isChunkClaimedBy(level, chunkPos, player)) {
            cluster.clusterTier.shopDiscountPercent
        } else {
            getPlayerHighestSettlementTier(level, player).shopDiscountPercent
        }

        // Active Tourism Prosperity bonus (+5% discount)
        if (cluster != null && TownTreasuryManager.isSettlementProsperous(cluster)) {
            discount += 0.05
        }

        return discount
    }
}
