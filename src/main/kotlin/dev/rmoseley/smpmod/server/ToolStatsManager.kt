package dev.rmoseley.smpmod.server

import dev.rmoseley.smpmod.registry.ModDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.DiggerItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShearsItem
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.TridentItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Tracks persistent raw usage metrics on tools and weapons adhering to Vanilla Tweaks "Tool Stats" behavior:
 * - Blocks Mined on diggers, shears, and hoes
 * - Hostiles Slain & Player Kills on melee weapons, bows, crossbows, and tridents
 * - Fish Caught on fishing rods
 * Displayed dynamically on item tooltips.
 */
object ToolStatsManager {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    /**
     * Determines whether an item qualifies as a trackable tool or weapon.
     */
    fun isTrackableTool(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        return item is DiggerItem || item is SwordItem || item is BowItem ||
                item is CrossbowItem || item is TridentItem || item is ShearsItem ||
                item is FishingRodItem || stack.isDamageableItem
    }

    /**
     * Records a broken block on the tool held in hand.
     */
    fun onBlockBroken(player: Player, stack: ItemStack) {
        if (!isTrackableTool(stack)) return

        val current = stack.get(ModDataComponents.TOOL_STATS.get()) ?: ModDataComponents.ToolStatsData()
        val updated = current.copy(blocksMined = current.blocksMined + 1L)
        stack.set(ModDataComponents.TOOL_STATS.get(), updated)
    }

    /**
     * Records an entity elimination on the weapon or tool used.
     */
    fun onEntityKilled(killer: Player, stack: ItemStack, victim: LivingEntity) {
        if (!isTrackableTool(stack)) return

        val current = stack.get(ModDataComponents.TOOL_STATS.get()) ?: ModDataComponents.ToolStatsData()
        val updated = if (victim is Player) {
            current.copy(playerKills = current.playerKills + 1L)
        } else if (victim is Monster || victim is Mob) {
            current.copy(mobsSlain = current.mobsSlain + 1L)
        } else {
            current
        }

        if (updated != current) {
            stack.set(ModDataComponents.TOOL_STATS.get(), updated)
        }
    }

    /**
     * Records a caught fish on the fishing rod used.
     */
    fun onFishCaught(player: Player, stack: ItemStack) {
        if (stack.item !is FishingRodItem && !stack.isDamageableItem) return

        val current = stack.get(ModDataComponents.TOOL_STATS.get()) ?: ModDataComponents.ToolStatsData()
        val updated = current.copy(fishCaught = current.fishCaught + 1L)
        stack.set(ModDataComponents.TOOL_STATS.get(), updated)
    }

    /**
     * Injects formatted tool statistics into the item tooltip.
     */
    fun appendTooltip(stack: ItemStack, tooltipComponents: MutableList<Component>) {
        val stats = stack.get(ModDataComponents.TOOL_STATS.get()) ?: return
        if (stats.blocksMined == 0L && stats.mobsSlain == 0L && stats.playerKills == 0L && stats.fishCaught == 0L) {
            return
        }

        tooltipComponents.add(Component.literal("§8§m----------------"))
        tooltipComponents.add(Component.literal("§6§l⛏ Tool Statistics"))
        if (stats.blocksMined > 0L) {
            tooltipComponents.add(Component.literal("§7  Blocks Mined: §f${numberFormat.format(stats.blocksMined)}"))
        }
        if (stats.mobsSlain > 0L) {
            tooltipComponents.add(Component.literal("§7  Hostiles Slain: §c${numberFormat.format(stats.mobsSlain)}"))
        }
        if (stats.playerKills > 0L) {
            tooltipComponents.add(Component.literal("§7  Player Kills: §4${numberFormat.format(stats.playerKills)}"))
        }
        if (stats.fishCaught > 0L) {
            tooltipComponents.add(Component.literal("§7  Fish Caught: §b${numberFormat.format(stats.fishCaught)}"))
        }
    }
}
