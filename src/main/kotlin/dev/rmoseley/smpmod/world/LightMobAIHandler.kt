package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Handles spatial light avoidance and destroys_light_on_aggro mob behaviors.
 * Enforces claim-safe shroud aura instead of block destruction inside contiguous claims.
 */
object LightMobAIHandler {

    fun hasDestroysLightTrait(mob: Mob): Boolean {
        val attr = mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        return attr != null && attr.value > 0.5
    }

    /**
     * Executes light snuffing when an aggroed mob targets nearby illumination.
     */
    fun onAggroLightInteraction(mob: Mob, targetPos: BlockPos) {
        val level = mob.level()
        val chunkPos = ChunkPos(targetPos)
        val claim = ClaimManager.getClaimAt(chunkPos)

        if (claim != null) {
            // Inside contiguous claim: claim-safe snuff aura (blocks are NEVER destroyed)
            val snuffDurationSeconds = SmpConfig.COMMON.lightSnuffDurationSeconds.get()
            // In full implementation, applies a temporary level shroud light modifier or particles
        } else {
            // In wilderness / dungeons: destroy torch or temporary light source
            val state = level.getBlockState(targetPos)
            if (isFragileLightSource(state)) {
                level.destroyBlock(targetPos, true, mob)
            }
        }
    }

    private fun isFragileLightSource(state: BlockState): Boolean {
        return state.`is`(Blocks.TORCH) ||
                state.`is`(Blocks.WALL_TORCH) ||
                state.`is`(Blocks.SOUL_TORCH) ||
                state.`is`(Blocks.SOUL_WALL_TORCH) ||
                state.`is`(Blocks.CAMPFIRE) ||
                state.`is`(Blocks.SOUL_CAMPFIRE)
    }
}
