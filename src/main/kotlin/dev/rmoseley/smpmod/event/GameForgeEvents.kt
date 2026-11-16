package dev.rmoseley.smpmod.event

import dev.rmoseley.smpmod.claims.BuilderFlightController
import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.economy.BulkStoreManager
import dev.rmoseley.smpmod.economy.ShopUnlockManager
import dev.rmoseley.smpmod.recovery.BuybackLedgerManager
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import dev.rmoseley.smpmod.world.ElytraFlightManager
import dev.rmoseley.smpmod.world.NetherHazardHandler
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

/**
 * Global game event listeners enforcing core systems and closed dependencies.
 */
object GameForgeEvents {

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val chunkPos = ChunkPos(event.pos)

        // Sustain builder flight inside claims and track cluster leveling
        if (ClaimManager.isChunkClaimedBy(player.level(), chunkPos, player)) {
            ClaimManager.recordBlockPlacement(player.level(), chunkPos, event.placedBlock.block)
            BuilderFlightController.recordBlockPlacement(player)
            PlaystyleXpTracker.awardBuilderXp(player, event.placedBlock.block, 1, 5)
        }
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val player = event.player
        val block = event.state.block

        // In production, player_placed metadata tag confirms whether block was natural
        val isNaturalHarvest = true
        if (isNaturalHarvest) {
            ShopUnlockManager.recordNaturalHarvest(player, block)
            val isDeep = event.pos.y < 0
            PlaystyleXpTracker.awardGathererXp(player, 1.0, isDeep)
        } else {
            ClaimManager.recordBlockBreak(player.level(), ChunkPos(event.pos))
        }
    }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return

        // Intercept inventory items to route between UUID Buyback and Bulk Store
        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue

            if (BulkStoreManager.isBulkStoreCandidate(stack)) {
                BulkStoreManager.deposit(stack)
            } else {
                BuybackLedgerManager.recordDeathDrop(player, stack)
            }
        }
    }

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Pre) {
        val target = event.entity
        val source = event.source.entity

        // Tank mitigation
        if (target is Player) {
            val mitigationAttr = target.getAttribute(ModEntityAttributes.ROLE_TANK_MITIGATION)
            val mitigation = mitigationAttr?.value ?: 0.0
            if (mitigation > 0.0) {
                val reduced = event.newDamage * (1.0 - mitigation.coerceAtMost(0.75))
                event.newDamage = reduced.toFloat()
            }

            if (target is ServerPlayer) {
                BuilderFlightController.onCombatEngaged(target)
            }
        }

        // DPS burst multiplier
        if (source is Player) {
            val dpsAttr = source.getAttribute(ModEntityAttributes.ROLE_DPS_MULTIPLIER)
            val dpsMultiplier = dpsAttr?.value ?: 1.0
            if (dpsMultiplier > 1.0) {
                event.newDamage = (event.newDamage * dpsMultiplier).toFloat()
            }
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return
        NetherHazardHandler.checkNetherPlayer(player)
        BuilderFlightController.onTickPlayer(player)
    }

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val player = event.entity as? ServerPlayer ?: return

        // Cancel mid-air firework rocket boosting to enforce momentum glider flight
        if (event.itemStack.`is`(Items.FIREWORK_ROCKET)) {
            if (ElytraFlightManager.shouldCancelRocketBoost(player)) {
                event.isCanceled = true
            }
        }
    }
}
