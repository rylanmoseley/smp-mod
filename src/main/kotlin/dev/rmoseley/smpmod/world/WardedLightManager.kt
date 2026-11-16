package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Warded Light Flares deployed by players with the Support combat role.
 *
 * Support Role Synergy (Section 5.1 of design-v2.md):
 * Warded lights are strictly immune to both physical block destruction and claim-safe snuffing
 * by `destroys_light_on_aggro` mobs, and emit defensive recoil when targeted.
 */
object WardedLightManager {

    data class WardedFlare(
        val dimension: ResourceLocation,
        val pos: BlockPos,
        val creatorUuid: UUID,
        val expireGameTime: Long
    )

    private val wardedFlares = ConcurrentHashMap<BlockPos, WardedFlare>()

    /**
     * Checks if a light source at the specified position is currently warded.
     */
    fun isWarded(level: Level, pos: BlockPos): Boolean {
        val flare = wardedFlares[pos] ?: return false
        if (flare.dimension != level.dimension().location()) return false

        if (level.gameTime >= flare.expireGameTime) {
            wardedFlares.remove(pos)
            return false
        }
        return true
    }

    /**
     * Empowers a light source into a Warded Light Flare using the player's Support role efficiency.
     */
    fun empowerLight(player: ServerPlayer, pos: BlockPos): Boolean {
        val level = player.serverLevel()
        val state = level.getBlockState(pos)

        if (!isValidLightSource(state)) return false

        val supportEfficiency = player.getAttribute(ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)?.value ?: 1.0
        val baseDuration = SmpConfig.COMMON.wardedFlareDurationSeconds.get()
        val totalDurationSeconds = (baseDuration * supportEfficiency).toLong()
        val expireTime = level.gameTime + (totalDurationSeconds * 20L)

        val flare = WardedFlare(
            dimension = level.dimension().location(),
            pos = pos,
            creatorUuid = player.uuid,
            expireGameTime = expireTime
        )
        wardedFlares[pos] = flare

        // Support empowerment visuals
        level.sendParticles(
            ParticleTypes.GLOW,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            25, 0.4, 0.4, 0.4, 0.05
        )
        level.sendParticles(
            ParticleTypes.END_ROD,
            pos.x + 0.5, pos.y + 0.8, pos.z + 0.5,
            10, 0.2, 0.2, 0.2, 0.02
        )
        level.playSound(
            null, pos,
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
            0.8f, 1.6f
        )

        player.displayClientMessage(
            Component.literal("§b[Support Flare] Light warded against dark snuffing and destruction for ${totalDurationSeconds}s."),
            true
        )
        return true
    }

    /**
     * Triggers defensive recoil when a dark-zone elite tries to extinguish a warded light.
     */
    fun onAttemptExtinguishWarded(mob: Mob, pos: BlockPos) {
        val level = mob.level() as? ServerLevel ?: return

        // Recoil damage and knockback to the hostile mob
        mob.hurt(level.damageSources().magic(), 4.0f)
        val knockbackVec = mob.position().subtract(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5).normalize().scale(0.8)
        mob.push(knockbackVec.x, 0.3, knockbackVec.z)

        // Visual ward flash
        level.sendParticles(
            ParticleTypes.FLASH,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            1, 0.0, 0.0, 0.0, 0.0
        )
        level.sendParticles(
            ParticleTypes.WAX_OFF,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            15, 0.5, 0.5, 0.5, 0.05
        )
        level.playSound(
            null, pos,
            SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS,
            1.0f, 1.2f
        )
    }

    private fun isValidLightSource(state: BlockState): Boolean {
        return state.`is`(Blocks.TORCH) ||
                state.`is`(Blocks.WALL_TORCH) ||
                state.`is`(Blocks.SOUL_TORCH) ||
                state.`is`(Blocks.SOUL_WALL_TORCH) ||
                state.`is`(Blocks.CAMPFIRE) ||
                state.`is`(Blocks.SOUL_CAMPFIRE) ||
                state.`is`(Blocks.LANTERN) ||
                state.`is`(Blocks.SOUL_LANTERN) ||
                state.`is`(Blocks.GLOWSTONE) ||
                state.`is`(Blocks.SEA_LANTERN) ||
                state.`is`(Blocks.OCHRE_FROGLIGHT) ||
                state.`is`(Blocks.PEARLESCENT_FROGLIGHT) ||
                state.`is`(Blocks.VERDANT_FROGLIGHT)
    }
}
