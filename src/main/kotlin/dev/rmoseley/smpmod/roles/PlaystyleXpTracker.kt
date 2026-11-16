package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.claims.VarietyScorer
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block

/**
 * Tracks and awards Playstyle XP across the four survival tracks:
 * Combat, Builder, Gatherer, and Explorer.
 */
object PlaystyleXpTracker {

    fun awardCombatXp(player: Player, mobTier: Int, timeToKillSeconds: Double, wasCritical: Boolean) {
        val speedWeight = SmpConfig.COMMON.combatXpSpeedWeight.get()
        val accuracyWeight = SmpConfig.COMMON.combatXpAccuracyWeight.get()

        val speedFactor = (10.0 / timeToKillSeconds.coerceAtLeast(1.0)).coerceIn(0.5, 3.0)
        val critBonus = if (wasCritical) accuracyWeight else 1.0

        val gainedXp = (mobTier * 10.0) * speedFactor * critBonus * speedWeight
        incrementXp(player, combat = gainedXp)
    }

    fun awardBuilderXp(player: Player, block: Block, currentCountOfBlock: Int, uniquePaletteCount: Int) {
        val score = VarietyScorer.calculateBlockPlacementScore(block, currentCountOfBlock, uniquePaletteCount)
        incrementXp(player, builder = score)
    }

    fun awardGathererXp(player: Player, baseResourceValue: Double, isDeepCave: Boolean) {
        val caveMultiplier = if (isDeepCave) SmpConfig.COMMON.deepCaveVeinDensityMultiplier.get() else 1.0
        val gainedXp = baseResourceValue * caveMultiplier
        incrementXp(player, gatherer = gainedXp)
    }

    fun awardExplorerXp(player: Player, gainedXp: Double = 15.0) {
        incrementXp(player, explorer = gainedXp)
    }

    private fun incrementXp(
        player: Player,
        combat: Double = 0.0,
        builder: Double = 0.0,
        gatherer: Double = 0.0,
        explorer: Double = 0.0
    ) {
        val attachment = ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()
        val current = player.getData(attachment)
        val updated = ModAttachmentTypes.PlaystyleXpData(
            combatXp = current.combatXp + combat,
            builderXp = current.builderXp + builder,
            gathererXp = current.gathererXp + gatherer,
            explorerXp = current.explorerXp + explorer
        )
        player.setData(attachment, updated)
        RoleCalculator.applyRoleAttributes(player)
    }
}
