package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.claims.VarietyScorer
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block

/**
 * Tracks and awards Playstyle XP across the four continuous survival tracks:
 * Combat, Builder, Gatherer, and Explorer.
 *
 * Automatically updates dynamic role profiles and entity attributes whenever XP changes.
 */
object PlaystyleXpTracker {

    fun incrementCombatXp(player: Player, amount: Double) {
        if (amount <= 0.0) return
        incrementXp(player, combat = amount)
    }

    fun incrementBuilderXp(player: Player, amount: Double) {
        if (amount <= 0.0) return
        incrementXp(player, builder = amount)
    }

    fun incrementGathererXp(player: Player, amount: Double) {
        if (amount <= 0.0) return
        incrementXp(player, gatherer = amount)
    }

    fun incrementExplorerXp(player: Player, amount: Double) {
        if (amount <= 0.0) return
        incrementXp(player, explorer = amount)
    }

    fun getCombatXp(player: Player): Double {
        return player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()).combatXp
    }

    fun getExplorerXp(player: Player): Double {
        return player.getData(ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()).explorerXp
    }

    // --- Backward Compatible Helper Methods ---

    fun awardCombatXp(player: Player, mobTier: Int, timeToKillSeconds: Double, wasCritical: Boolean) {
        val speedWeight = SmpConfig.COMMON.combatXpSpeedWeight.get()
        val accuracyWeight = SmpConfig.COMMON.combatXpAccuracyWeight.get()

        val speedFactor = (10.0 / timeToKillSeconds.coerceAtLeast(1.0)).coerceIn(0.5, 3.0)
        val critBonus = if (wasCritical) accuracyWeight else 1.0

        val gainedXp = (mobTier * 10.0) * speedFactor * critBonus * speedWeight
        incrementCombatXp(player, gainedXp)
    }

    fun awardBuilderXp(player: Player, block: Block, currentCountOfBlock: Int, uniquePaletteCount: Int) {
        val score = VarietyScorer.calculateBlockPlacementScore(block, currentCountOfBlock, uniquePaletteCount)
        incrementBuilderXp(player, score)
    }

    fun awardBuilderXpDirect(player: Player, score: Double) {
        incrementBuilderXp(player, score)
    }

    fun deductBuilderXpDirect(player: Player, score: Double) {
        val attachment = ModAttachmentTypes.PLAYER_PLAYSTYLE_XP.get()
        val current = player.getData(attachment)
        val updated = ModAttachmentTypes.PlaystyleXpData(
            combatXp = current.combatXp,
            builderXp = (current.builderXp - score).coerceAtLeast(0.0),
            gathererXp = current.gathererXp,
            explorerXp = current.explorerXp
        )
        player.setData(attachment, updated)
        RoleCalculator.applyRoleAttributes(player)
    }

    fun awardGathererXp(player: Player, baseResourceValue: Double, isDeepCave: Boolean) {
        val caveMultiplier = if (isDeepCave) SmpConfig.COMMON.deepCaveVeinDensityMultiplier.get() else 1.0
        val gainedXp = baseResourceValue * caveMultiplier
        incrementGathererXp(player, gainedXp)
    }

    fun awardExplorerXp(player: Player, gainedXp: Double = 15.0) {
        incrementExplorerXp(player, gainedXp)
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
        val oldProfile = RoleCalculator.calculateProfile(player)

        val updated = ModAttachmentTypes.PlaystyleXpData(
            combatXp = current.combatXp + combat,
            builderXp = current.builderXp + builder,
            gathererXp = current.gathererXp + gatherer,
            explorerXp = current.explorerXp + explorer
        )
        player.setData(attachment, updated)

        // Dynamically recompute and apply role attributes
        RoleCalculator.applyRoleAttributes(player)

        // Check if role identity transitioned
        val newProfile = RoleCalculator.calculateProfile(player)
        if (oldProfile.primaryRole != newProfile.primaryRole && newProfile.primaryRole != RoleCalculator.Role.HYBRID) {
            player.sendSystemMessage(
                Component.literal("§6[Combat Identity] §fYour playstyle has evolved! You are now a ${newProfile.primaryRole.colorCode}${newProfile.getFormattedTitle()}§f.")
            )
        }
    }
}
