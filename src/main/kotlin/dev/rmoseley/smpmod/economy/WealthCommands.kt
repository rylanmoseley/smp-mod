package dev.rmoseley.smpmod.economy

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import dev.rmoseley.smpmod.recovery.SafeRecoveryManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/**
 * Commands for checking digital wealth/balance, executing peer-to-peer digital transfers,
 * and administrative balance adjustments.
 */
object WealthCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // --- /wealth command ---
        val wealthRoot = Commands.literal("wealth")
            .executes { ctx -> displayBalance(ctx.source) }
            .then(Commands.literal("top").executes { ctx -> displayTop(ctx.source) })
            .then(
                Commands.literal("pay")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("amount", LongArgumentType.longArg(1L))
                                    .executes { ctx ->
                                        val sender = ctx.source.playerOrException
                                        val target = EntityArgument.getPlayer(ctx, "player")
                                        val amount = LongArgumentType.getLong(ctx, "amount")
                                        executePay(ctx.source, sender, target, amount)
                                    }
                            )
                    )
            )
            .then(
                Commands.literal("give")
                    .requires { it.hasPermission(2) }
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("amount", LongArgumentType.longArg(1L))
                                    .executes { ctx ->
                                        val target = EntityArgument.getPlayer(ctx, "player")
                                        val amount = LongArgumentType.getLong(ctx, "amount")
                                        adminCredit(ctx.source, target, amount)
                                    }
                            )
                    )
            )
            .then(
                Commands.literal("take")
                    .requires { it.hasPermission(2) }
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("amount", LongArgumentType.longArg(1L))
                                    .executes { ctx ->
                                        val target = EntityArgument.getPlayer(ctx, "player")
                                        val amount = LongArgumentType.getLong(ctx, "amount")
                                        adminDebit(ctx.source, target, amount)
                                    }
                            )
                    )
            )

        // --- /balance command and aliases ---
        val balanceRoot = Commands.literal("balance")
            .executes { ctx -> displayBalance(ctx.source) }
            .then(Commands.literal("top").executes { ctx -> displayTop(ctx.source) })
            .then(
                Commands.literal("pay")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("amount", LongArgumentType.longArg(1L))
                                    .executes { ctx ->
                                        val sender = ctx.source.playerOrException
                                        val target = EntityArgument.getPlayer(ctx, "player")
                                        val amount = LongArgumentType.getLong(ctx, "amount")
                                        executePay(ctx.source, sender, target, amount)
                                    }
                            )
                    )
            )

        val balRoot = Commands.literal("bal")
            .executes { ctx -> displayBalance(ctx.source) }
            .then(Commands.literal("top").executes { ctx -> displayTop(ctx.source) })

        val moneyRoot = Commands.literal("money")
            .executes { ctx -> displayBalance(ctx.source) }
            .then(Commands.literal("top").executes { ctx -> displayTop(ctx.source) })

        // Dedicated /pay shortcut
        val payRoot = Commands.literal("pay")
            .then(
                Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("amount", LongArgumentType.longArg(1L))
                            .executes { ctx ->
                                val sender = ctx.source.playerOrException
                                val target = EntityArgument.getPlayer(ctx, "player")
                                val amount = LongArgumentType.getLong(ctx, "amount")
                                executePay(ctx.source, sender, target, amount)
                            }
                    )
            )

        dispatcher.register(wealthRoot)
        dispatcher.register(balanceRoot)
        dispatcher.register(balRoot)
        dispatcher.register(moneyRoot)
        dispatcher.register(payRoot)
    }

    private fun displayBalance(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("Only players possess a personal digital currency balance."))
            return 0
        }

        val balance = EconomyManager.getBalance(player)
        val safety = SafeRecoveryManager.evaluateSafety(player)
        val subsidyPct = (safety.subsidyDiscountPercent * 100).toInt()

        player.sendSystemMessage(Component.literal("§6========================================"))
        player.sendSystemMessage(Component.literal("§6 Digital Wealth: §f${player.gameProfile.name}"))
        player.sendSystemMessage(Component.literal("§7 Current Balance: §a$balance coins"))
        if (subsidyPct > 0) {
            player.sendSystemMessage(Component.literal("§7 Municipal Subsidy: §a$subsidyPct% discount active"))
        } else {
            player.sendSystemMessage(Component.literal("§7 Municipal Subsidy: §7None (Wilderness)"))
        }
        player.sendSystemMessage(Component.literal("§7 Claim Safety: ${safety.statusMessage}"))
        player.sendSystemMessage(Component.literal("§8 Digital coins cannot be dropped, stolen, or lost on death."))
        player.sendSystemMessage(Component.literal("§6========================================"))
        return 1
    }

    private fun displayTop(source: CommandSourceStack): Int {
        source.sendSuccess(
            { Component.literal("§6=== Wealth Rankings ===\n§7Use §e/leaderboard wealth §7or §e/top wealth §7to view top server wealth rankings.") },
            false
        )
        return 1
    }

    private fun executePay(
        source: CommandSourceStack,
        sender: ServerPlayer,
        target: ServerPlayer,
        amount: Long
    ): Int {
        when (val result = EconomyManager.transfer(sender, target, amount, "Direct /pay transfer")) {
            is TransactionResult.Success -> {
                val record = result.record
                sender.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f)
                target.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f)

                val taxNote = if (record.taxDeducted > 0L) {
                    " §7(Server tax: §e${record.taxDeducted} coins§7)"
                } else ""

                sender.sendSystemMessage(
                    Component.literal("§a✔ Sent §e$amount coins §ato §f${target.gameProfile.name}§a!$taxNote New balance: §e${record.balanceAfter} coins§a.")
                )
                target.sendSystemMessage(
                    Component.literal("§a✔ Received §e${record.amount - record.taxDeducted} coins §afrom §f${sender.gameProfile.name}§a!")
                )
                return 1
            }

            is TransactionResult.Failure -> {
                source.sendFailure(Component.literal("§c✖ Payment failed: ${result.reason}"))
                return 0
            }
        }
    }

    private fun adminCredit(source: CommandSourceStack, target: ServerPlayer, amount: Long): Int {
        val result = EconomyManager.credit(
            target,
            amount,
            TransactionType.CREDIT,
            "Admin grant by ${source.textName}"
        )
        return if (result is TransactionResult.Success) {
            source.sendSuccess(
                { Component.literal("§a✔ Successfully credited §e$amount coins §ato §f${target.gameProfile.name}§a. New balance: §e${result.newBalance} coins§a.") },
                true
            )
            target.sendSystemMessage(
                Component.literal("§aAn administrator credited §e$amount coins §ato your digital balance.")
            )
            1
        } else {
            source.sendFailure(Component.literal("§c✖ Failed to credit currency."))
            0
        }
    }

    private fun adminDebit(source: CommandSourceStack, target: ServerPlayer, amount: Long): Int {
        val result = EconomyManager.debit(
            target,
            amount,
            TransactionType.DEBIT,
            "Admin debit by ${source.textName}"
        )
        return if (result is TransactionResult.Success) {
            source.sendSuccess(
                { Component.literal("§a✔ Successfully debited §e$amount coins §afrom §f${target.gameProfile.name}§a. New balance: §e${result.newBalance} coins§a.") },
                true
            )
            target.sendSystemMessage(
                Component.literal("§cAn administrator debited §e$amount coins §afrom your digital balance.")
            )
            1
        } else {
            source.sendFailure(Component.literal("§c✖ Failed to debit currency (insufficient funds)."))
            0
        }
    }
}
