package dev.rmoseley.smpmod.server

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component

/**
 * Commands for displaying server-wide stat rankings:
 * /leaderboard <category> [page]
 * /top <category> [page]
 */
object LeaderboardCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val categorySuggestions = LeaderboardManager.LeaderboardCategory.entries.map { it.id }

        val leaderboardCommand = Commands.literal("leaderboard")
            .then(
                Commands.argument("category", StringArgumentType.word())
                    .suggests { _, builder -> SharedSuggestionProvider.suggest(categorySuggestions, builder) }
                    .executes { ctx -> showLeaderboard(ctx, 1) }
                    .then(
                        Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes { ctx -> showLeaderboard(ctx, IntegerArgumentType.getInteger(ctx, "page")) }
                    )
            )
            .executes { ctx -> showCategoryList(ctx) }

        val topCommand = Commands.literal("top")
            .then(
                Commands.argument("category", StringArgumentType.word())
                    .suggests { _, builder -> SharedSuggestionProvider.suggest(categorySuggestions, builder) }
                    .executes { ctx -> showLeaderboard(ctx, 1) }
                    .then(
                        Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes { ctx -> showLeaderboard(ctx, IntegerArgumentType.getInteger(ctx, "page")) }
                    )
            )
            .executes { ctx -> showCategoryList(ctx) }

        dispatcher.register(leaderboardCommand)
        dispatcher.register(topCommand)
    }

    private fun showCategoryList(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSuccess(
            {
                Component.literal("§6=== Realm Leaderboards ===\n" +
                    "§7Usage: §f/leaderboard <category> [page]\n" +
                    "§eAvailable Categories:\n" +
                    LeaderboardManager.LeaderboardCategory.entries.joinToString("\n") { cat ->
                        " §6${cat.icon} §b${cat.id.padEnd(12)} §7— §f${cat.displayName}"
                    }
                )
            },
            false
        )
        return 1
    }

    private fun showLeaderboard(ctx: CommandContext<CommandSourceStack>, page: Int): Int {
        val categoryStr = StringArgumentType.getString(ctx, "category")
        val category = LeaderboardManager.LeaderboardCategory.fromString(categoryStr)
        if (category == null) {
            ctx.source.sendFailure(
                Component.literal("§cUnknown leaderboard category: '$categoryStr'. Use §6/leaderboard §cto view valid options.")
            )
            return 0
        }

        // Refresh online player stats before rendering
        LeaderboardManager.refreshOnlinePlayers(ctx.source.server)

        val (rankings, totalPages) = LeaderboardManager.getRankings(category, page)
        if (rankings.isEmpty()) {
            ctx.source.sendSuccess(
                { Component.literal("§eNo player entries recorded yet for ${category.displayName}.") },
                false
            )
            return 1
        }

        val header = Component.literal(
            "§6=== ${category.icon} Leaderboard: §e${category.displayName} §7(Page $page/$totalPages) ==="
        )
        ctx.source.sendSuccess({ header }, false)

        for ((rank, entry) in rankings) {
            val rankBadge = when (rank) {
                1 -> "§6[#1]"
                2 -> "§f[#2]"
                3 -> "§c[#3]"
                else -> "§7[#$rank]"
            }
            val formattedValue = LeaderboardManager.formatStatValue(category, entry)
            ctx.source.sendSuccess(
                { Component.literal(" $rankBadge §b${entry.name.padEnd(16)} §7— §a$formattedValue") },
                false
            )
        }

        if (page < totalPages) {
            ctx.source.sendSuccess(
                { Component.literal("§8Type §6/leaderboard ${category.id} ${page + 1} §8for next page.") },
                false
            )
        }

        return 1
    }
}
