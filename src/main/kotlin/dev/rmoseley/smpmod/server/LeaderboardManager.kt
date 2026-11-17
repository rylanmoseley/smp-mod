package dev.rmoseley.smpmod.server

import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages server-wide stat leaderboards across economic, playstyle, and survival categories.
 * Caches player metrics and provides paginated ranking lists for in-game query.
 */
object LeaderboardManager {

    data class PlayerLeaderboardEntry(
        val uuid: UUID,
        val name: String,
        val balance: Long,
        val combatXp: Double,
        val builderXp: Double,
        val gathererXp: Double,
        val explorerXp: Double,
        val playtimeSeconds: Long,
        val dungeonsCleared: Int,
        val blocksConstructed: Long,
        val naturalBlocksHarvested: Long,
        val villagerKills: Int
    )

    enum class LeaderboardCategory(
        val id: String,
        val displayName: String,
        val icon: String,
        val unit: String
    ) {
        WEALTH("wealth", "Wealth & Treasury", "💰", "coins"),
        COMBAT("combat", "Combat Proficiency", "⚔", "XP"),
        BUILDER("builder", "Architecture & Building", "🏛", "XP"),
        GATHERER("gatherer", "Resource Harvesting", "⛏", "XP"),
        EXPLORER("explorer", "Wilderness Exploration", "🧭", "XP"),
        PLAYTIME("playtime", "Active Survival Playtime", "⌛", "hours"),
        DUNGEONS("dungeons", "Dungeon Delves Cleared", "🗝", "dungeons"),
        CONSTRUCTION("construction", "Blocks Constructed", "🧱", "blocks"),
        HARVEST("harvest", "Natural Blocks Harvested", "🌾", "blocks");

        companion object {
            fun fromString(str: String): LeaderboardCategory? =
                entries.firstOrNull { it.id.equals(str, ignoreCase = true) || it.name.equals(str, ignoreCase = true) }
        }
    }

    private val playerCache = ConcurrentHashMap<UUID, PlayerLeaderboardEntry>()
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    /**
     * Updates or records stats for an active player.
     */
    fun updatePlayer(player: ServerPlayer) {
        val uuid = player.uuid
        val name = player.gameProfile.name
        val balance = EconomyManager.getBalance(player)

        val xpAttachment = ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()
        val xpData = player.getData(xpAttachment)

        val milestoneAttachment = ModAttachmentTypes.PLAYER_MILESTONES.get()
        val milestoneData = player.getData(milestoneAttachment)

        val entry = PlayerLeaderboardEntry(
            uuid = uuid,
            name = name,
            balance = balance,
            combatXp = xpData.combatXp,
            builderXp = xpData.builderXp,
            gathererXp = xpData.gathererXp,
            explorerXp = xpData.explorerXp,
            playtimeSeconds = milestoneData.activePlaytimeTicks / 20L,
            dungeonsCleared = milestoneData.dungeonsCleared,
            blocksConstructed = milestoneData.totalBlocksConstructed,
            naturalBlocksHarvested = milestoneData.totalNaturalBlocksHarvested,
            villagerKills = milestoneData.villagerKills
        )

        playerCache[uuid] = entry
    }

    /**
     * Refreshes stats for all online players from the server.
     */
    fun refreshOnlinePlayers(server: MinecraftServer) {
        for (player in server.playerList.players) {
            updatePlayer(player)
        }
    }

    /**
     * Returns a sorted, paginated list of entries for a given category.
     */
    fun getRankings(category: LeaderboardCategory, page: Int = 1, pageSize: Int = 8): Pair<List<Pair<Int, PlayerLeaderboardEntry>>, Int> {
        val sorted = when (category) {
            LeaderboardCategory.WEALTH -> playerCache.values.sortedByDescending { it.balance }
            LeaderboardCategory.COMBAT -> playerCache.values.sortedByDescending { it.combatXp }
            LeaderboardCategory.BUILDER -> playerCache.values.sortedByDescending { it.builderXp }
            LeaderboardCategory.GATHERER -> playerCache.values.sortedByDescending { it.gathererXp }
            LeaderboardCategory.EXPLORER -> playerCache.values.sortedByDescending { it.explorerXp }
            LeaderboardCategory.PLAYTIME -> playerCache.values.sortedByDescending { it.playtimeSeconds }
            LeaderboardCategory.DUNGEONS -> playerCache.values.sortedByDescending { it.dungeonsCleared }
            LeaderboardCategory.CONSTRUCTION -> playerCache.values.sortedByDescending { it.blocksConstructed }
            LeaderboardCategory.HARVEST -> playerCache.values.sortedByDescending { it.naturalBlocksHarvested }
        }

        val totalPages = ((sorted.size - 1) / pageSize + 1).coerceAtLeast(1)
        val validPage = page.coerceIn(1, totalPages)
        val startIndex = (validPage - 1) * pageSize

        val pagedEntries = sorted.drop(startIndex).take(pageSize).mapIndexed { index, entry ->
            Pair(startIndex + index + 1, entry)
        }

        return Pair(pagedEntries, totalPages)
    }

    /**
     * Formats a player's stat value for display in the leaderboard.
     */
    fun formatStatValue(category: LeaderboardCategory, entry: PlayerLeaderboardEntry): String {
        return when (category) {
            LeaderboardCategory.WEALTH -> "${numberFormat.format(entry.balance)} coins"
            LeaderboardCategory.COMBAT -> "${numberFormat.format(entry.combatXp.toLong())} Combat XP"
            LeaderboardCategory.BUILDER -> "${numberFormat.format(entry.builderXp.toLong())} Builder XP"
            LeaderboardCategory.GATHERER -> "${numberFormat.format(entry.gathererXp.toLong())} Gatherer XP"
            LeaderboardCategory.EXPLORER -> "${numberFormat.format(entry.explorerXp.toLong())} Explorer XP"
            LeaderboardCategory.PLAYTIME -> {
                val hours = entry.playtimeSeconds / 3600
                val minutes = (entry.playtimeSeconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
            LeaderboardCategory.DUNGEONS -> "${numberFormat.format(entry.dungeonsCleared)} cleared"
            LeaderboardCategory.CONSTRUCTION -> "${numberFormat.format(entry.blocksConstructed)} placed"
            LeaderboardCategory.HARVEST -> "${numberFormat.format(entry.naturalBlocksHarvested)} harvested"
        }
    }
}
