package dev.rmoseley.smpmod.economy

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

/**
 * Commands for interacting with the Community Bounty Board:
 * /bounty list
 * /bounty accept <id>
 * /bounty status
 * /bounty claim <id>
 * /bounty abandon <id>
 */
object BountyCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("bounty")
                .then(
                    Commands.literal("list")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException
                            val contracts = BountyContractManager.getAvailableContracts()

                            ctx.source.sendSuccess(
                                { Component.literal("§6=== Community Bounty & Contract Board ===") },
                                false
                            )
                            for (c in contracts) {
                                ctx.source.sendSuccess(
                                    {
                                        Component.literal(
                                            "§e${c.title} §7(ID: §b${c.contractId}§7)\n" +
                                                    " §7Goal: ${c.description}\n" +
                                                    " §7Reward: §a${c.currencyReward} coins §7| §d+${c.roleXpReward.toInt()} Role XP"
                                        )
                                    },
                                    false
                                )
                            }
                            1
                        }
                )
                .then(
                    Commands.literal("status")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException
                            val active = BountyContractManager.getPlayerContracts(player.uuid)

                            if (active.isEmpty()) {
                                ctx.source.sendSuccess(
                                    { Component.literal("§7You have no active bounty contracts. Use §6/bounty list §7to browse.") },
                                    false
                                )
                                return@executes 1
                            }

                            ctx.source.sendSuccess(
                                { Component.literal("§6=== Your Active Bounty Contracts (${active.size}/2) ===") },
                                false
                            )
                            for (p in active) {
                                val contract = BountyContractManager.getContractById(p.contractId) ?: continue
                                val isDone = p.currentProgress >= contract.targetQuantity
                                val status = if (isDone) "§a[READY TO CLAIM: /bounty claim ${contract.contractId}]" else "§e(${p.currentProgress}/${contract.targetQuantity})"
                                ctx.source.sendSuccess(
                                    { Component.literal("§e${contract.title} $status\n §7${contract.description}") },
                                    false
                                )
                            }
                            1
                        }
                )
                .then(
                    Commands.literal("accept")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .executes { ctx ->
                                    val player = ctx.source.playerOrException
                                    val id = StringArgumentType.getString(ctx, "id")
                                    val (success, msg) = BountyContractManager.acceptContract(player, id)
                                    if (success) {
                                        ctx.source.sendSuccess({ Component.literal("§a$msg") }, false)
                                        1
                                    } else {
                                        ctx.source.sendFailure(Component.literal("§c$msg"))
                                        0
                                    }
                                }
                        )
                )
                .then(
                    Commands.literal("claim")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .executes { ctx ->
                                    val player = ctx.source.playerOrException
                                    val id = StringArgumentType.getString(ctx, "id")
                                    val (success, msg) = BountyContractManager.claimReward(player, id)
                                    if (success) {
                                        ctx.source.sendSuccess({ Component.literal("§a$msg") }, false)
                                        1
                                    } else {
                                        ctx.source.sendFailure(Component.literal("§c$msg"))
                                        0
                                    }
                                }
                        )
                )
                .then(
                    Commands.literal("abandon")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .executes { ctx ->
                                    val player = ctx.source.playerOrException
                                    val id = StringArgumentType.getString(ctx, "id")
                                    if (BountyContractManager.abandonContract(player, id)) {
                                        ctx.source.sendSuccess({ Component.literal("§eAbandoned contract: $id.") }, false)
                                        1
                                    } else {
                                        ctx.source.sendFailure(Component.literal("§cContract $id not found in your active list."))
                                        0
                                    }
                                }
                        )
                )
        )
    }
}
