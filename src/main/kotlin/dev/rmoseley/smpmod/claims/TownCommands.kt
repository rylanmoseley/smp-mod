package dev.rmoseley.smpmod.claims

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ChunkPos

/**
 * Commands for interacting with Town Treasuries and Civic Upgrades:
 * /town balance
 * /town deposit <amount>
 * /town withdraw <amount>
 * /town upgrades
 * /town upgrade buy <upgrade_id>
 */
object TownCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("town")
                .then(
                    Commands.literal("balance")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException
                            val treasury = TownTreasuryManager.getTreasury(player.uuid)
                            ctx.source.sendSuccess(
                                {
                                    Component.literal("§6[Town Treasury] §eBalance: §a${treasury.balance} coins §7| Total Taxes: §b${treasury.totalTaxesCollected} coins")
                                },
                                false
                            )
                            1
                        }
                )
                .then(
                    Commands.literal("tourism")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException
                            val chunkPos = ChunkPos(player.blockPosition())
                            val cluster = ClaimManager.getContiguousCluster(player.level(), chunkPos)
                            val ownerUuid = cluster?.ownerUuid ?: player.uuid
                            val tourism = TownTreasuryManager.getTourismRecord(ownerUuid)
                            val prosperous = cluster != null && TownTreasuryManager.isSettlementProsperous(cluster)
                            val statusStr = if (prosperous) "§aACTIVE (Speed & Regen buff)" else "§7INACTIVE"

                            ctx.source.sendSuccess(
                                {
                                    Component.literal("§6[Settlement Tourism] §7Total Nonresident Visitor Time: §e${tourism.totalVisitorMinutes}m §7| Prosperity Status: $statusStr")
                                },
                                false
                            )
                            1
                        }
                )
                .then(
                    Commands.literal("deposit")
                        .then(
                            Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes { ctx ->
                                    val player = ctx.source.playerOrException
                                    val amount = LongArgumentType.getLong(ctx, "amount")
                                    if (TownTreasuryManager.deposit(player, amount)) {
                                        ctx.source.sendSuccess(
                                            { Component.literal("§aDeposited §e$amount coins §ainto the Town Treasury.") },
                                            false
                                        )
                                        1
                                    } else {
                                        ctx.source.sendFailure(Component.literal("§cDeposit failed. Verify sufficient personal funds."))
                                        0
                                    }
                                }
                        )
                )
                .then(
                    Commands.literal("withdraw")
                        .then(
                            Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes { ctx ->
                                    val player = ctx.source.playerOrException
                                    val amount = LongArgumentType.getLong(ctx, "amount")
                                    if (TownTreasuryManager.withdraw(player, amount)) {
                                        ctx.source.sendSuccess(
                                            { Component.literal("§aWithdrew §e$amount coins §afrom the Town Treasury.") },
                                            false
                                        )
                                        1
                                    } else {
                                        ctx.source.sendFailure(Component.literal("§cWithdrawal failed. Insufficient town treasury balance."))
                                        0
                                    }
                                }
                        )
                )
                .then(
                    Commands.literal("upgrades")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException
                            val chunkPos = ChunkPos(player.blockPosition())
                            val cluster = ClaimManager.getContiguousCluster(player.level(), chunkPos)
                            val ownerUuid = cluster?.ownerUuid ?: player.uuid
                            val active = TownTreasuryManager.getActiveUpgrades(ownerUuid)

                            ctx.source.sendSuccess(
                                {
                                    Component.literal("§6=== Town Civic Upgrades (${cluster?.clusterTierName ?: "Personal"}) ===")
                                },
                                false
                            )
                            for (upgrade in CivicUpgrade.entries) {
                                val isUnlocked = active.contains(upgrade)
                                val status = if (isUnlocked) "§a[UNLOCKED]" else "§c[LOCKED - ${upgrade.cost} coins (Req: ${upgrade.requiredTier.displayName})]"
                                ctx.source.sendSuccess(
                                    {
                                        Component.literal("§e${upgrade.displayName} $status\n §7${upgrade.description} (ID: §b${upgrade.id}§7)")
                                    },
                                    false
                                )
                            }
                            1
                        }
                )
                .then(
                    Commands.literal("upgrade")
                        .then(
                            Commands.literal("buy")
                                .then(
                                    Commands.argument("id", StringArgumentType.word())
                                        .executes { ctx ->
                                            val player = ctx.source.playerOrException
                                            val id = StringArgumentType.getString(ctx, "id")
                                            val upgrade = CivicUpgrade.fromId(id)
                                                ?: run {
                                                    ctx.source.sendFailure(Component.literal("§cUnknown upgrade ID: $id. Use /town upgrades to view available options."))
                                                    return@executes 0
                                                }

                                            val chunkPos = ChunkPos(player.blockPosition())
                                            val cluster = ClaimManager.getContiguousCluster(player.level(), chunkPos)
                                                ?: run {
                                                    ctx.source.sendFailure(Component.literal("§cMust stand inside your town's contiguous claim to purchase upgrades."))
                                                    return@executes 0
                                                }

                                            val (success, msg) = TownTreasuryManager.purchaseUpgrade(player, cluster, upgrade)
                                            if (success) {
                                                ctx.source.sendSuccess({ Component.literal("§a$msg") }, true)
                                                1
                                            } else {
                                                ctx.source.sendFailure(Component.literal("§c$msg"))
                                                0
                                            }
                                        }
                                )
                        )
                )
        )
    }
}
