package dev.rmoseley.smpmod.recovery

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.rmoseley.smpmod.economy.EconomyManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * Server commands for interacting with the Ownership-Bound UUID Buyback Ledger,
 * public liquidation market, catalyst restoration, and formal P2P trade handovers.
 */
object RecoveryCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // --- /buyback commands ---
        val buybackRoot = Commands.literal("buyback")
            .executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the buyback menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            }
            .then(Commands.literal("gui").executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the buyback menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            })
            .then(Commands.literal("menu").executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the buyback menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            })
            .then(Commands.literal("list").executes { ctx -> listPlayerBuybacks(ctx.source) })
            .then(Commands.literal("public").executes { ctx -> listPublicBuybacks(ctx.source) })
            .then(
                Commands.literal("claim")
                    .then(
                        Commands.argument("uuid", StringArgumentType.word())
                            .executes { ctx ->
                                val uuidStr = StringArgumentType.getString(ctx, "uuid")
                                claimBuyback(ctx.source, uuidStr)
                            }
                    )
            )
            .then(
                Commands.literal("catalyst")
                    .then(
                        Commands.argument("uuid", StringArgumentType.word())
                            .executes { ctx ->
                                val uuidStr = StringArgumentType.getString(ctx, "uuid")
                                applyCatalyst(ctx.source, uuidStr)
                            }
                    )
            )

        // Alias: /recovery
        val recoveryRoot = Commands.literal("recovery")
            .executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the recovery menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            }
            .then(Commands.literal("gui").executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the recovery menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            })
            .then(Commands.literal("menu").executes { ctx ->
                val player = ctx.source.player ?: run {
                    ctx.source.sendFailure(Component.literal("Only players can open the recovery menu."))
                    return@executes 0
                }
                BuybackMenu.open(player)
                1
            })
            .then(Commands.literal("list").executes { ctx -> listPlayerBuybacks(ctx.source) })
            .then(Commands.literal("public").executes { ctx -> listPublicBuybacks(ctx.source) })
            .then(
                Commands.literal("claim")
                    .then(
                        Commands.argument("uuid", StringArgumentType.word())
                            .executes { ctx ->
                                val uuidStr = StringArgumentType.getString(ctx, "uuid")
                                claimBuyback(ctx.source, uuidStr)
                            }
                    )
            )

        // --- /p2p trade commands ---
        val p2pRoot = Commands.literal("p2p")
            .then(
                Commands.literal("sell")
                    .then(
                        Commands.argument("buyer", EntityArgument.player())
                            .then(
                                Commands.argument("price", LongArgumentType.longArg(1L))
                                    .executes { ctx ->
                                        val seller = ctx.source.playerOrException
                                        val buyer = EntityArgument.getPlayer(ctx, "buyer")
                                        val price = LongArgumentType.getLong(ctx, "price")
                                        createP2POffer(seller, buyer, price)
                                    }
                            )
                    )
            )
            .then(Commands.literal("accept").executes { ctx -> acceptP2POffer(ctx.source.playerOrException) })
            .then(Commands.literal("cancel").executes { ctx -> cancelP2POffer(ctx.source.playerOrException) })

        dispatcher.register(buybackRoot)
        dispatcher.register(recoveryRoot)
        dispatcher.register(p2pRoot)
    }

    private fun listPlayerBuybacks(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("Only players can view their buyback ledger."))
            return 0
        }

        val entries = BuybackLedgerManager.getEntriesForPlayer(player)
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.literal("§aYour buyback ledger is empty. No lost items registered!"))
            return 1
        }

        val safety = SafeRecoveryManager.evaluateSafety(player)
        player.sendSystemMessage(Component.literal("§6=== Your Buyback Ledger (${entries.size} items) ==="))
        player.sendSystemMessage(Component.literal(safety.statusMessage))

        val now = System.currentTimeMillis() / 1000L
        for (entry in entries) {
            val price = BuybackLedgerManager.calculatePrice(entry, now, isOwner = true, buyer = player)
            val remainingSec = BuybackLedgerManager.getRemainingExclusiveSeconds(entry, now)
            val windowStatus = if (remainingSec > 0) {
                val hours = remainingSec / 3600
                val mins = (remainingSec % 3600) / 60
                "§eExclusive Window: ${hours}h ${mins}m remaining"
            } else {
                "§cPUBLICLY LISTED (1.25x markup active for other players)"
            }

            player.sendSystemMessage(
                Component.literal("§b• ${entry.itemStack.hoverName.string} §7(UUID: §f${entry.itemUuid.toString().take(8)}§7)")
            )
            player.sendSystemMessage(
                Component.literal("  §7Deaths: §e${entry.deathCount} §7| Base: §f${entry.baseCost} §7| Buyback Fee: §a$price coins")
            )
            player.sendSystemMessage(Component.literal("  $windowStatus"))
            player.sendSystemMessage(Component.literal("  §8Command: §6/buyback claim ${entry.itemUuid}"))
        }

        return entries.size
    }

    private fun listPublicBuybacks(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("Only players can view the public buyback listing."))
            return 0
        }

        val now = System.currentTimeMillis() / 1000L
        val publicEntries = BuybackLedgerManager.getAllPublicEntries(now)

        if (publicEntries.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No items currently listed on the public buyback market."))
            return 1
        }

        player.sendSystemMessage(Component.literal("§6=== Public Buyback Market (${publicEntries.size} items) ==="))
        player.sendSystemMessage(Component.literal("§7(24-hour exclusive owner window expired. Listed at 1.25x price markup)"))

        for (entry in publicEntries) {
            val isOwner = BuybackLedgerManager.isOwner(entry, player)
            val price = BuybackLedgerManager.calculatePrice(entry, now, isOwner = isOwner, buyer = player)
            val ownerTag = if (isOwner) "§a(Your Item - Standard Price)" else "§7(Former Owner: §f${entry.registeredOwnerName}§7)"

            player.sendSystemMessage(
                Component.literal("§b• ${entry.itemStack.hoverName.string} §7[UUID: §f${entry.itemUuid.toString().take(8)}§7] $ownerTag")
            )
            player.sendSystemMessage(
                Component.literal("  §7Purchase Price: §e$price coins §7| §6/buyback claim ${entry.itemUuid}")
            )
        }

        return publicEntries.size
    }

    private fun claimBuyback(source: CommandSourceStack, uuidPrefixOrFull: String): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("Only players can reclaim items."))
            return 0
        }

        val targetUuid = resolveUuid(uuidPrefixOrFull) ?: run {
            player.sendSystemMessage(Component.literal("§cItem not found with UUID: $uuidPrefixOrFull"))
            return 0
        }

        when (val result = BuybackLedgerManager.reclaimItem(targetUuid, player)) {
            is BuybackLedgerManager.ReclaimResult.Success -> {
                val itemName = result.item.hoverName.string
                val subsidyMsg = if (result.claimSubsidySaved > 0) {
                    " §a(Saved §e${result.claimSubsidySaved} coins §avia Municipal Claim Subsidy!)"
                } else ""

                if (result.wasPublicPurchase) {
                    player.sendSystemMessage(
                        Component.literal("§aPurchased §b$itemName §afrom the public listing for §e${result.pricePaid} coins! Ownership formally transferred to you.$subsidyMsg")
                    )
                } else {
                    player.sendSystemMessage(
                        Component.literal("§aSuccessfully recovered §b$itemName §afor §e${result.pricePaid} coins!$subsidyMsg")
                    )
                }
                return 1
            }

            is BuybackLedgerManager.ReclaimResult.InsufficientFunds -> {
                player.sendSystemMessage(
                    Component.literal("§cInsufficient digital balance! Required: §e${result.priceRequired} coins§c, Current: §e${result.currentBalance} coins§c.")
                )
                return 0
            }

            is BuybackLedgerManager.ReclaimResult.LockedExclusiveToOwner -> {
                val hours = result.secondsRemaining / 3600
                val mins = (result.secondsRemaining % 3600) / 60
                player.sendSystemMessage(
                    Component.literal("§cItem is currently locked in the 24-hour exclusive recovery window for §e${result.ownerName}§c. (${hours}h ${mins}m remaining before public listing)")
                )
                return 0
            }

            is BuybackLedgerManager.ReclaimResult.ItemNotFound -> {
                player.sendSystemMessage(Component.literal("§cItem no longer exists in the buyback ledger."))
                return 0
            }
        }
    }

    private fun applyCatalyst(source: CommandSourceStack, uuidPrefixOrFull: String): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("Only players can apply catalysts."))
            return 0
        }

        val targetUuid = resolveUuid(uuidPrefixOrFull) ?: run {
            player.sendSystemMessage(Component.literal("§cItem not found in ledger with UUID: $uuidPrefixOrFull"))
            return 0
        }

        val heldItem = player.mainHandItem
        when (val result = RecoveryCatalystManager.applyCatalystToEntry(targetUuid, player, heldItem)) {
            is RecoveryCatalystManager.CatalystApplicationResult.Success -> {
                player.sendSystemMessage(Component.literal("§a${result.message}"))
                player.sendSystemMessage(Component.literal("§aNew Reclaim Fee: §e${result.newPrice} coins"))
                return 1
            }

            is RecoveryCatalystManager.CatalystApplicationResult.Failure -> {
                player.sendSystemMessage(Component.literal("§c${result.reason}"))
                return 0
            }
        }
    }

    private fun createP2POffer(seller: ServerPlayer, buyer: ServerPlayer, price: Long): Int {
        return when (val result = P2POwnershipManager.createOffer(seller, buyer, price)) {
            is P2POwnershipManager.P2PTradeResult.Success -> 1
            is P2POwnershipManager.P2PTradeResult.Failure -> {
                seller.sendSystemMessage(Component.literal("§cTrade offer failed: ${result.reason}"))
                0
            }
        }
    }

    private fun acceptP2POffer(buyer: ServerPlayer): Int {
        return when (val result = P2POwnershipManager.acceptOffer(buyer)) {
            is P2POwnershipManager.P2PTradeResult.Success -> 1
            is P2POwnershipManager.P2PTradeResult.Failure -> {
                buyer.sendSystemMessage(Component.literal("§cFailed to accept trade: ${result.reason}"))
                0
            }
        }
    }

    private fun cancelP2POffer(player: ServerPlayer): Int {
        val cancelled = P2POwnershipManager.cancelOffer(player.uuid)
        if (cancelled) {
            player.sendSystemMessage(Component.literal("§ePending P2P trade offer cancelled."))
            return 1
        } else {
            player.sendSystemMessage(Component.literal("§7No pending trade offer to cancel."))
            return 0
        }
    }

    private fun resolveUuid(prefixOrFull: String): UUID? {
        try {
            return UUID.fromString(prefixOrFull)
        } catch (_: IllegalArgumentException) {
            // Check by prefix
            val allPublic = BuybackLedgerManager.getAllPublicEntries()
            val match = allPublic.firstOrNull { it.itemUuid.toString().startsWith(prefixOrFull, ignoreCase = true) }
            if (match != null) return match.itemUuid

            // Check across all ledger entries if accessible
            for (player in net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()?.playerList?.players ?: emptyList()) {
                val playerMatch = BuybackLedgerManager.getEntriesForPlayer(player).firstOrNull {
                    it.itemUuid.toString().startsWith(prefixOrFull, ignoreCase = true)
                }
                if (playerMatch != null) return playerMatch.itemUuid
            }
        }
        return null
    }
}
