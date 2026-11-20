package dev.rmoseley.smpmod.roles

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * Commands for inspecting emergent survival playstyle metrics, derived combat roles,
 * active attribute affinities, and administrator role XP adjustments.
 */
object RoleCommands {

    private val TRACK_SUGGESTIONS = listOf("combat", "builder", "gatherer", "explorer")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val roleRoot = Commands.literal("role")
            .executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players possess a playstyle profile."))
                    return@executes 0
                }
                displayProfile(ctx.source, player)
            }
            .then(
                Commands.literal("check")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .executes { ctx ->
                                val target = EntityArgument.getPlayer(ctx, "player")
                                displayProfile(ctx.source, target)
                            }
                    )
            )
            .then(
                Commands.literal("addxp")
                    .requires { it.hasPermission(2) }
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("track", StringArgumentType.word())
                                    .suggests { _, builder -> SharedSuggestionProvider.suggest(TRACK_SUGGESTIONS, builder) }
                                    .then(
                                        Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                                            .executes { ctx ->
                                                val target = EntityArgument.getPlayer(ctx, "player")
                                                val track = StringArgumentType.getString(ctx, "track").lowercase()
                                                val amount = DoubleArgumentType.getDouble(ctx, "amount")
                                                adminAddXp(ctx.source, target, track, amount)
                                            }
                                    )
                            )
                    )
            )
            .then(
                Commands.literal("reset")
                    .requires { it.hasPermission(2) }
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .executes { ctx ->
                                val target = EntityArgument.getPlayer(ctx, "player")
                                adminResetXp(ctx.source, target)
                            }
                    )
            )

        // Aliases: /roles and /profile
        val rolesRoot = Commands.literal("roles")
            .executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players possess a playstyle profile."))
                    return@executes 0
                }
                displayProfile(ctx.source, player)
            }
            .then(
                Commands.literal("check")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .executes { ctx ->
                                val target = EntityArgument.getPlayer(ctx, "player")
                                displayProfile(ctx.source, target)
                            }
                    )
            )

        val profileRoot = Commands.literal("profile")
            .executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players possess a playstyle profile."))
                    return@executes 0
                }
                displayProfile(ctx.source, player)
            }
            .then(
                Commands.argument("player", EntityArgument.player())
                    .executes { ctx ->
                        val target = EntityArgument.getPlayer(ctx, "player")
                        displayProfile(ctx.source, target)
                    }
            )

        dispatcher.register(roleRoot)
        dispatcher.register(rolesRoot)
        dispatcher.register(profileRoot)
    }

    private fun displayProfile(source: CommandSourceStack, target: ServerPlayer): Int {
        val profile = RoleCalculator.calculateProfile(target)
        val xpData = target.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get())

        source.sendSuccess({
            Component.literal(buildString {
                append("§6========================================\n")
                append("§6 Playstyle Profile: §f${target.gameProfile.name}\n")
                append("§7 Role Title: §f${profile.getFormattedTitle()} ${profile.primaryRole.colorCode}[${profile.primaryRole.displayName}]\n")
                append("§7 Total Playstyle XP: §e${profile.totalXp.toInt()}\n")
                append("§8----------------------------------------\n")
                append("§c • Combat XP:   §f${xpData.combatXp.toInt()} §7(${(profile.dpsRatio * 100).toInt()}% share | ${(profile.dpsAffinity * 100).toInt()}% affinity)\n")
                append("§9 • Builder XP:  §f${xpData.builderXp.toInt()} §7(${(profile.supportRatio * 100).toInt()}% share | ${(profile.supportAffinity * 100).toInt()}% affinity)\n")
                append("§e • Gatherer XP: §f${xpData.gathererXp.toInt()} §7(${(profile.tankRatio * 100).toInt()}% share | ${(profile.tankAffinity * 100).toInt()}% affinity)\n")
                append("§b • Explorer XP: §f${xpData.explorerXp.toInt()} §7(${(profile.scoutRatio * 100).toInt()}% share | ${(profile.scoutAffinity * 100).toInt()}% affinity)\n")
                append("§8----------------------------------------\n")
                append("§7 Active Role Modifiers:\n")
                append("§c • Attack Damage:     §f+${(profile.dpsAffinity * 150).toInt()}% §7(Armor Pen: +${(profile.dpsAffinity * 45).toInt()}%)\n")
                append("§9 • Damage Mitigation: §f+${(profile.tankAffinity * 50).toInt()}% §7(Threat: ${(1.0 + profile.tankAffinity * 3.0).toInt()}x)\n")
                append("§a • Support Synergy:   §f+${(profile.supportAffinity * 100).toInt()}% consumable potency\n")
                append("§b • Scout Mobility:    §f+${(profile.scoutAffinity * 35).toInt()}% movement speed\n")
                if (profile.isSpecialist(profile.primaryRole)) {
                    append("§6 ★ Specialist Status: Accelerated affinity scaling unlocked!\n")
                }
                append("§6========================================")
            })
        }, false)
        return 1
    }

    private fun adminAddXp(
        source: CommandSourceStack,
        target: ServerPlayer,
        track: String,
        amount: Double
    ): Int {
        when (track) {
            "combat" -> PlaystyleXpTracker.incrementCombatXp(target, amount)
            "builder" -> PlaystyleXpTracker.incrementBuilderXp(target, amount)
            "gatherer" -> PlaystyleXpTracker.incrementGathererXp(target, amount)
            "explorer" -> PlaystyleXpTracker.incrementExplorerXp(target, amount)
            else -> {
                source.sendFailure(Component.literal("§cUnknown track: $track. Expected: combat, builder, gatherer, explorer."))
                return 0
            }
        }
        RoleCalculator.applyRoleAttributes(target)
        source.sendSuccess(
            { Component.literal("§a✔ Awarded §e${amount.toInt()} $track XP §ato §f${target.gameProfile.name}§a.") },
            true
        )
        return 1
    }

    private fun adminResetXp(source: CommandSourceStack, target: ServerPlayer): Int {
        val attachment = ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()
        target.setData(attachment, ModAttachmentTypes.PlaystyleXpData(0.0, 0.0, 0.0, 0.0))
        RoleCalculator.applyRoleAttributes(target)
        source.sendSuccess(
            { Component.literal("§a✔ Successfully reset all playstyle XP for §f${target.gameProfile.name}§a.") },
            true
        )
        return 1
    }
}
