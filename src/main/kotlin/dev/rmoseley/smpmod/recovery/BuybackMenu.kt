package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.economy.EconomyManager
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import kotlin.math.ceil

/**
 * Interactive chest-based graphical interface for the Ownership-Bound Buyback Ledger
 * and Public Liquidation Market.
 *
 * Server-side container menu fully compatible with vanilla and modded clients.
 * Supports pagination, real-time balance display, dynamic municipal subsidy indicators,
 * and single-click purchase / recovery with direct delivery to player inventory.
 */
class BuybackMenu(
    containerId: Int,
    playerInventory: Inventory,
    val serverPlayer: ServerPlayer,
    val chestContainer: SimpleContainer = SimpleContainer(54)
) : ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, chestContainer, 6) {

    enum class Mode {
        PERSONAL,
        PUBLIC
    }

    var currentMode: Mode = Mode.PERSONAL
    var currentPage: Int = 0

    private val slotEntryMap = mutableMapOf<Int, BuybackLedgerManager.BuybackEntry>()

    init {
        populate()
    }

    override fun stillValid(player: Player): Boolean = true

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
        if (player !is ServerPlayer) return

        // Intercept all shop container slot clicks (0..53)
        if (slotId in 0 until chestContainer.containerSize) {
            handleChestSlotClick(slotId)
            broadcastChanges()
            return
        }

        // Prevent shift-clicking items from player inventory into the shop
        if (clickType == ClickType.QUICK_MOVE) {
            broadcastChanges()
            return
        }

        super.clicked(slotId, button, clickType, player)
    }

    private fun handleChestSlotClick(slotId: Int) {
        when (slotId) {
            SLOT_PREV_PAGE -> {
                if (currentPage > 0) {
                    currentPage--
                    serverPlayer.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f)
                    populate()
                }
            }

            SLOT_NEXT_PAGE -> {
                val totalPages = getTotalPages()
                if (currentPage < totalPages - 1) {
                    currentPage++
                    serverPlayer.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f)
                    populate()
                }
            }

            SLOT_MODE_TOGGLE -> {
                currentMode = if (currentMode == Mode.PERSONAL) Mode.PUBLIC else Mode.PERSONAL
                currentPage = 0
                serverPlayer.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.0f)
                populate()
            }

            SLOT_BALANCE_REFRESH -> {
                serverPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.0f)
                populate()
            }

            in 0 until PAGE_SIZE -> {
                val entry = slotEntryMap[slotId]
                if (entry != null) {
                    handleReclaim(entry)
                }
            }
        }
    }

    private fun handleReclaim(entry: BuybackLedgerManager.BuybackEntry) {
        when (val result = BuybackLedgerManager.reclaimItem(entry.itemUuid, serverPlayer)) {
            is BuybackLedgerManager.ReclaimResult.Success -> {
                serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)
                val itemName = result.item.hoverName.string
                val subsidyNote = if (result.claimSubsidySaved > 0) {
                    " §a(Saved §e${result.claimSubsidySaved} coins §avia Municipal Subsidy!)"
                } else ""

                if (result.wasPublicPurchase) {
                    serverPlayer.sendSystemMessage(
                        Component.literal("§a✔ Purchased §b$itemName §afrom Public Market for §e${result.pricePaid} coins!$subsidyNote")
                    )
                } else {
                    serverPlayer.sendSystemMessage(
                        Component.literal("§a✔ Recovered §b$itemName §afor §e${result.pricePaid} coins!$subsidyNote")
                    )
                }
                populate()
            }

            is BuybackLedgerManager.ReclaimResult.InsufficientFunds -> {
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f)
                serverPlayer.sendSystemMessage(
                    Component.literal("§c✖ Insufficient balance! Required: §e${result.priceRequired} coins §c(You have: §e${result.currentBalance} coins§c)")
                )
                populate()
            }

            is BuybackLedgerManager.ReclaimResult.LockedExclusiveToOwner -> {
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f)
                val hours = result.secondsRemaining / 3600
                val mins = (result.secondsRemaining % 3600) / 60
                serverPlayer.sendSystemMessage(
                    Component.literal("§c✖ Locked: Exclusive to §f${result.ownerName} §cfor another §e${hours}h ${mins}m§c.")
                )
                populate()
            }

            is BuybackLedgerManager.ReclaimResult.ItemNotFound -> {
                serverPlayer.sendSystemMessage(Component.literal("§c✖ Item is no longer available in the ledger."))
                populate()
            }
        }
    }

    private fun getActiveEntries(): List<BuybackLedgerManager.BuybackEntry> {
        val now = System.currentTimeMillis() / 1000L
        return when (currentMode) {
            Mode.PERSONAL -> BuybackLedgerManager.getEntriesForPlayer(serverPlayer)
            Mode.PUBLIC -> BuybackLedgerManager.getAllPublicEntries(now)
        }
    }

    private fun getTotalPages(): Int {
        val size = getActiveEntries().size
        return maxOf(1, ceil(size.toDouble() / PAGE_SIZE).toInt())
    }

    fun populate() {
        slotEntryMap.clear()
        for (i in 0 until chestContainer.containerSize) {
            chestContainer.setItem(i, ItemStack.EMPTY)
        }

        val entries = getActiveEntries()
        val totalPages = getTotalPages()
        currentPage = currentPage.coerceIn(0, totalPages - 1)

        val pageEntries = entries.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
        val now = System.currentTimeMillis() / 1000L

        // Populate item slots (0..44)
        for ((index, entry) in pageEntries.withIndex()) {
            val display = createDisplayItem(entry, now)
            chestContainer.setItem(index, display)
            slotEntryMap[index] = entry
        }

        // Empty state indicator
        if (pageEntries.isEmpty()) {
            val emptyStack = ItemStack(Items.PAPER)
            emptyStack.set(DataComponents.CUSTOM_NAME, Component.literal(
                if (currentMode == Mode.PERSONAL) "§aNo Lost Items Registered" else "§7Public Market is Empty"
            ))
            val emptyLore = if (currentMode == Mode.PERSONAL) {
                listOf(
                    Component.literal("§7Your buyback ledger has no lost items."),
                    Component.literal("§7Items lost on death appear here automatically."),
                    Component.literal("§6▶ Click slot 48 below to browse the Public Market!")
                )
            } else {
                listOf(
                    Component.literal("§7No expired items currently on the public liquidation market."),
                    Component.literal("§7Unclaimed gear enters public sale after 24 hours.")
                )
            }
            emptyStack.set(DataComponents.LORE, ItemLore(emptyLore))
            chestContainer.setItem(SLOT_EMPTY_INDICATOR, emptyStack)
        }

        // Populate bottom navigation toolbar (45..53)
        populateNavigationToolbar(totalPages)

        broadcastChanges()
    }

    private fun createDisplayItem(entry: BuybackLedgerManager.BuybackEntry, now: Long): ItemStack {
        val display = entry.itemStack.copy()
        val isOwner = BuybackLedgerManager.isOwner(entry, serverPlayer)
        val price = BuybackLedgerManager.calculatePrice(entry, now, isOwner = isOwner, buyer = serverPlayer)
        val remainingSec = BuybackLedgerManager.getRemainingExclusiveSeconds(entry, now)

        val existingLore = display.get(DataComponents.LORE)?.lines ?: emptyList()
        val loreList = mutableListOf<Component>()
        loreList.addAll(existingLore)
        loreList.add(Component.literal("§8§m----------------------------------------"))
        loreList.add(Component.literal("§eBuyback Price: §a$price digital coins"))
        loreList.add(Component.literal("§7Registered Owner: §f${entry.registeredOwnerName}"))
        loreList.add(Component.literal("§7Cumulative Deaths: §e${entry.deathCount}"))

        if (remainingSec > 0) {
            val hours = remainingSec / 3600
            val mins = (remainingSec % 3600) / 60
            loreList.add(Component.literal("§bExclusive Window: §f${hours}h ${mins}m remaining"))
        } else {
            loreList.add(Component.literal("§cPublic Market Listing §7(1.25x markup)"))
        }

        if (entry.catalystDiscount > 0.0) {
            val pct = (entry.catalystDiscount * 100).toInt()
            loreList.add(Component.literal("§dRecovery Catalyst: §f-$pct% discount applied"))
        }

        loreList.add(Component.literal("§8§m----------------------------------------"))
        loreList.add(Component.literal("§a▶ CLICK TO RECOVER TO INVENTORY"))

        display.set(DataComponents.LORE, ItemLore(loreList))
        return display
    }

    private fun populateNavigationToolbar(totalPages: Int) {
        // Filler pane
        fun filler(): ItemStack {
            val pane = ItemStack(Items.BLACK_STAINED_GLASS_PANE)
            pane.set(DataComponents.CUSTOM_NAME, Component.literal("§8 "))
            return pane
        }

        // Slot 45: Previous Page
        if (currentPage > 0) {
            val prev = ItemStack(Items.ARROW)
            prev.set(DataComponents.CUSTOM_NAME, Component.literal("§a« Previous Page (${currentPage}/${totalPages})"))
            prev.set(DataComponents.LORE, ItemLore(listOf(Component.literal("§7Click to view previous items"))))
            chestContainer.setItem(SLOT_PREV_PAGE, prev)
        } else {
            val noPrev = ItemStack(Items.GRAY_STAINED_GLASS_PANE)
            noPrev.set(DataComponents.CUSTOM_NAME, Component.literal("§8No Previous Page"))
            chestContainer.setItem(SLOT_PREV_PAGE, noPrev)
        }

        chestContainer.setItem(46, filler())

        // Slot 47: Mode Switcher
        val modeStack = if (currentMode == Mode.PERSONAL) {
            ItemStack(Items.CHEST).apply {
                set(DataComponents.CUSTOM_NAME, Component.literal("§aMode: §eYour Lost Items"))
                set(DataComponents.LORE, ItemLore(listOf(
                    Component.literal("§7Currently showing items bound to your UUID."),
                    Component.literal("§7You have exclusive 24h recovery rights."),
                    Component.literal(" "),
                    Component.literal("§6▶ Click to switch to Public Market")
                )))
            }
        } else {
            ItemStack(Items.ENDER_CHEST).apply {
                set(DataComponents.CUSTOM_NAME, Component.literal("§6Mode: §bPublic Market"))
                set(DataComponents.LORE, ItemLore(listOf(
                    Component.literal("§7Currently showing expired items from all players."),
                    Component.literal("§7Available to purchase with 1.25x markup."),
                    Component.literal(" "),
                    Component.literal("§a▶ Click to switch to Your Items")
                )))
            }
        }
        chestContainer.setItem(SLOT_MODE_TOGGLE, modeStack)

        chestContainer.setItem(48, filler())

        // Slot 49: Player Balance & Subsidy
        val balance = EconomyManager.getBalance(serverPlayer)
        val safety = SafeRecoveryManager.evaluateSafety(serverPlayer)
        val subsidyPct = (safety.subsidyDiscountPercent * 100).toInt()
        val balanceStack = ItemStack(Items.GOLD_INGOT).apply {
            set(DataComponents.CUSTOM_NAME, Component.literal("§6Your Balance: §a$balance coins"))
            set(DataComponents.LORE, ItemLore(listOf(
                Component.literal("§7Municipal Subsidy: ${if (subsidyPct > 0) "§a$subsidyPct% discount" else "§cNone (Wilderness)"}"),
                Component.literal("§7Status: ${safety.statusMessage}"),
                Component.literal(" "),
                Component.literal("§e▶ Click to refresh balance")
            )))
        }
        chestContainer.setItem(SLOT_BALANCE_REFRESH, balanceStack)

        chestContainer.setItem(50, filler())

        // Slot 51: Ledger Info
        val infoStack = ItemStack(Items.BOOK).apply {
            set(DataComponents.CUSTOM_NAME, Component.literal("§eBuyback Ledger Guide"))
            set(DataComponents.LORE, ItemLore(listOf(
                Component.literal("§7• Lost items are preserved in this ledger on death."),
                Component.literal("§7• 24-hour exclusive reclaim window for owner."),
                Component.literal("§7• Fees scale with deaths and decay over ~30 days."),
                Component.literal("§7• Contiguous claims grant municipal subsidies."),
                Component.literal("§7• Recovered items are delivered straight to inventory!")
            )))
        }
        chestContainer.setItem(SLOT_INFO, infoStack)

        chestContainer.setItem(52, filler())

        // Slot 53: Next Page
        if (currentPage < totalPages - 1) {
            val next = ItemStack(Items.ARROW)
            next.set(DataComponents.CUSTOM_NAME, Component.literal("§aNext Page » (${currentPage + 2}/${totalPages})"))
            next.set(DataComponents.LORE, ItemLore(listOf(Component.literal("§7Click to view more items"))))
            chestContainer.setItem(SLOT_NEXT_PAGE, next)
        } else {
            val noNext = ItemStack(Items.GRAY_STAINED_GLASS_PANE)
            noNext.set(DataComponents.CUSTOM_NAME, Component.literal("§8No Next Page"))
            chestContainer.setItem(SLOT_NEXT_PAGE, noNext)
        }
    }

    companion object {
        const val PAGE_SIZE = 45
        const val SLOT_PREV_PAGE = 45
        const val SLOT_MODE_TOGGLE = 47
        const val SLOT_BALANCE_REFRESH = 49
        const val SLOT_INFO = 51
        const val SLOT_NEXT_PAGE = 53
        const val SLOT_EMPTY_INDICATOR = 22

        fun open(player: ServerPlayer) {
            player.openMenu(
                SimpleMenuProvider(
                    { id, inv, p -> BuybackMenu(id, inv, p as ServerPlayer) },
                    Component.literal("Lost Item Buyback Ledger")
                )
            )
        }
    }
}
