package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.claims.ClaimManager
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import kotlin.math.min

/**
 * Acoustic environment hooks and proximity audio integration.
 * Provides:
 * 1. Town Alarm Bell high-priority acoustic broadcast & defender alert.
 * 2. Wool / curtain acoustic occlusion & footstep dampening calculator.
 * 3. Cavern reverb & Nether thermal distortion acoustic modeling for voice chat integration.
 */
object VoiceAcousticHooks {

    data class AcousticProfile(
        val dampeningFactor: Double,      // 0.0 (clear) to 0.85 (muffled by wool/curtains)
        val reverbIntensity: Double,      // 0.0 (dry) to 1.0 (cavernous echo)
        val isNetherDistorted: Boolean
    )

    /**
     * Triggered when a town or settlement bell is struck.
     * Broadcasts an acoustic alarm up to 96 blocks away and alerts residents.
     */
    fun triggerTownAlarm(level: ServerLevel, bellPos: BlockPos, ringer: Player) {
        val chunkPos = ChunkPos(bellPos)
        val cluster = ClaimManager.getContiguousCluster(level, chunkPos)
        val settlementName = cluster?.clusterTierName ?: "Regional"

        // Sound broadcast across 96 blocks
        val broadcastRadiusSq = 96.0 * 96.0

        for (player in level.players()) {
            val distSq = player.distanceToSqr(bellPos.x + 0.5, bellPos.y + 0.5, bellPos.z + 0.5)
            if (distSq <= broadcastRadiusSq) {
                // Play echoing resonant bell chime
                player.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5f, 0.9f)

                player.sendSystemMessage(
                    Component.literal(
                        "§c[$settlementName Alarm] §eBell rung at §f[${bellPos.x}, ${bellPos.y}, ${bellPos.z}] §eby §b${ringer.gameProfile.name}§e! Assemble defenders."
                    )
                )
            }
        }

        // Highlight hostile intruders within 32 blocks of the bell
        val hostiles = level.getEntitiesOfClass(
            Monster::class.java,
            net.minecraft.world.phys.AABB(bellPos).inflate(32.0)
        )
        for (mob in hostiles) {
            mob.addEffect(MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true))
        }

        level.sendParticles(
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            bellPos.x + 0.5, bellPos.y + 1.2, bellPos.z + 0.5,
            20, 0.4, 0.6, 0.4, 0.05
        )
    }

    /**
     * Calculates the acoustic dampening factor around a position based on wool, carpets, and banners.
     * Returns a dampening score between 0.0 (open air, no dampening) and 0.85 (heavily muffled).
     */
    fun calculateAcousticDampening(level: Level, pos: BlockPos): Double {
        var woolBlocks = 0

        for (x in -2..2) {
            for (y in -1..2) {
                for (z in -2..2) {
                    val p = pos.offset(x, y, z)
                    val state = level.getBlockState(p)
                    if (state.`is`(BlockTags.WOOL) ||
                        state.`is`(BlockTags.WOOL_CARPETS) ||
                        state.`is`(BlockTags.BANNERS)
                    ) {
                        woolBlocks++
                    }
                }
            }
        }

        val dampening = (woolBlocks * 0.08).coerceIn(0.0, 0.85)
        return dampening
    }

    /**
     * Evaluates full acoustic profile for a player's current location.
     */
    fun evaluateAcousticProfile(player: ServerPlayer): AcousticProfile {
        val level = player.level()
        val pos = player.blockPosition()

        val dampening = calculateAcousticDampening(level, pos)
        val isNether = level.dimension() == Level.NETHER

        // Cavern reverb calculation: deep caves (< Y: 0) without sky light have high reverb
        val isDeepCave = pos.y < 0 && !level.canSeeSky(pos)
        val rawReverb = if (isDeepCave) 0.75 else if (!level.canSeeSky(pos) && pos.y < 60) 0.35 else 0.05
        val netReverb = (rawReverb * (1.0 - dampening)).coerceIn(0.0, 1.0)

        return AcousticProfile(
            dampeningFactor = dampening,
            reverbIntensity = netReverb,
            isNetherDistorted = isNether
        )
    }
}
