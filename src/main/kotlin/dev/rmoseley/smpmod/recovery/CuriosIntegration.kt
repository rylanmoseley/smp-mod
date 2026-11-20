package dev.rmoseley.smpmod.recovery

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.economy.BulkStoreManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.neoforge.common.NeoForge
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.event.CurioDropsEvent

/**
 * Robust integration with Curios API (NeoForge 1.21.1) to guarantee that all equipped
 * accessories, charms, and cosmetic curios are safely routed to the Ownership-Bound UUID
 * Buyback Ledger upon player death, preventing loss into the world.
 */
object CuriosIntegration {

    val isCuriosLoaded: Boolean by lazy {
        ModList.get().isLoaded("curios")
    }

    fun init() {
        if (isCuriosLoaded) {
            SmpMod.LOGGER.info("Curios API detected. Initializing Curios Buyback & Recovery Integration...")
            NeoForge.EVENT_BUS.register(CuriosEventListener)
        }
    }

    /**
     * Extracts and clears all equipped and cosmetic Curios items from the player's slots
     * on death, securely registering them into the Buyback Ledger or Bulk Store.
     */
    fun processPlayerDeathCurios(
        player: ServerPlayer,
        onBuyback: (ItemStack) -> Unit,
        onBulk: (ItemStack) -> Unit
    ): Int {
        if (!isCuriosLoaded) return 0
        var count = 0
        try {
            val curiosInventoryOpt = CuriosApi.getCuriosInventory(player)
            if (curiosInventoryOpt.isPresent) {
                val handler = curiosInventoryOpt.get()

                // 1. Process equipped functional curios
                val equipped = handler.equippedCurios
                for (i in 0 until equipped.slots) {
                    val stack = equipped.getStackInSlot(i)
                    if (!stack.isEmpty) {
                        val destination = RecoveryItemClassifier.classify(stack)
                        when (destination) {
                            RecoveryItemClassifier.RoutingDestination.BULK_STORE -> onBulk(stack.copy())
                            else -> onBuyback(stack.copy())
                        }
                        equipped.setStackInSlot(i, ItemStack.EMPTY)
                        count++
                    }
                }

                // 2. Process cosmetic curios if any
                for ((_, stacksHandler) in handler.curios) {
                    if (stacksHandler.hasCosmetic()) {
                        val cosmeticStacks = stacksHandler.cosmeticStacks
                        for (i in 0 until cosmeticStacks.slots) {
                            val stack = cosmeticStacks.getStackInSlot(i)
                            if (!stack.isEmpty) {
                                onBuyback(stack.copy())
                                cosmeticStacks.setStackInSlot(i, ItemStack.EMPTY)
                                count++
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            SmpMod.LOGGER.error("Failed to process Curios on player death: ${t.message}", t)
        }
        return count
    }

    /**
     * Checks if an ItemStack is registered as a Curio accessory.
     */
    fun isCurioItem(stack: ItemStack): Boolean {
        if (!isCuriosLoaded || stack.isEmpty) return false
        return try {
            CuriosApi.getCurio(stack).isPresent
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Event listener intercepting any Curio drop entities that might be generated on death.
     */
    object CuriosEventListener {
        @SubscribeEvent
        fun onCurioDrops(event: CurioDropsEvent) {
            val player = event.entity as? ServerPlayer ?: return
            val dropsIterator = event.drops.iterator()
            var recoveredCount = 0
            while (dropsIterator.hasNext()) {
                val itemEntity: ItemEntity = dropsIterator.next()
                val stack = itemEntity.item
                if (stack.isEmpty) continue

                val destination = RecoveryItemClassifier.classify(stack)
                if (destination == RecoveryItemClassifier.RoutingDestination.BULK_STORE) {
                    BulkStoreManager.deposit(stack.copy())
                } else {
                    BuybackLedgerManager.recordDeathDrop(player, stack.copy())
                }
                dropsIterator.remove()
                recoveredCount++
            }
            if (recoveredCount > 0) {
                SmpMod.LOGGER.info("Secured $recoveredCount dropped curio(s) into Buyback Ledger for ${player.gameProfile.name}.")
            }
        }
    }
}
