package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles spatial light avoidance and destroys_light_on_aggro mob behaviors strictly adhering to Section 4.3 of design-v2.md:
 * - Low spawn weight in wild caves (5%), high spawn weight in dungeons (35%).
 * - Wilderness & Dungeons: actively targets and breaks temporary light sources.
 * - Inside Contiguous Claims (Anti-Griefing Guardrail): mobs CANNOT break blocks;
 *   instead they apply a temporary 20-30s snuff/shroud aura suppressing illumination.
 *   Duration is further mitigated by the claim cluster's development leveling!
 * - Tank Role Threat Control: Tanks pull aggro off backline lights.
 * - Support Role Warded Lights: Immune to destruction and snuffing.
 */
object LightMobAIHandler {

    data class SnuffAura(
        val position: BlockPos,
        val dimensionId: String,
        val expiresAtEpochMs: Long
    )

    // Keyed by "dim:x:y:z"
    private val activeSnuffAuras = ConcurrentHashMap<String, SnuffAura>()

    private fun makePosKey(dimId: String, pos: BlockPos): String {
        return "$dimId:${pos.x}:${pos.y}:${pos.z}"
    }

    fun hasDestroysLightTrait(mob: Mob): Boolean {
        val attr = mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        return attr != null && attr.value > 0.5
    }

    /**
     * Rolls spawn chance for the destroys_light_on_aggro trait when a mob spawns.
     */
    fun evaluateSpawnTrait(mob: Mob) {
        val level = mob.level()
        val pos = mob.blockPosition()

        val isUnderground = pos.y < 50
        val isDeep = pos.y < 0
        val blockLight = level.getBrightness(LightLayer.BLOCK, pos)

        // Dungeons / deep cave structures have higher probability
        val isDungeon = isDeep && blockLight == 0

        val chance = if (isDungeon) {
            SmpConfig.COMMON.dungeonLightDestroyChance.get()
        } else if (isUnderground && blockLight <= 4) {
            SmpConfig.COMMON.wildernessLightDestroyChance.get()
        } else {
            0.0
        }

        if (level.random.nextDouble() < chance) {
            mob.getAttribute(ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)?.baseValue = 1.0

            if (mob is PathfinderMob) {
                mob.goalSelector.addGoal(2, DestroyLightGoal(mob))
            }
        }
    }

    /**
     * Executes light interaction when an aggroed mob targets nearby illumination.
     */
    fun onAggroLightInteraction(mob: Mob, targetPos: BlockPos) {
        val level = mob.level()

        // 1. Support Role Warded Light Flare immunity
        if (WardedLightManager.isWarded(level, targetPos)) {
            WardedLightManager.onAttemptExtinguishWarded(mob, targetPos)
            return
        }

        val chunkPos = ChunkPos(targetPos)
        val isClaimed = ClaimManager.isChunkClaimed(level, chunkPos)

        if (isClaimed) {
            // 2. Contiguous Claim Anti-Griefing Guardrail: Suppress light output via aura (NEVER destroy blocks)
            val effectiveDuration = ClaimManager.getEffectiveLightSnuffDuration(level, targetPos)
            applyClaimLightSnuff(level, targetPos, effectiveDuration)
            LightSnuffManager.applySnuffAura(level, targetPos, mob)
        } else {
            // 3. Wilderness & Dungeons: actively break temporary light source
            val state = level.getBlockState(targetPos)
            if (isFragileLightSource(state)) {
                level.destroyBlock(targetPos, true, mob)

                val serverLevel = level as? ServerLevel
                serverLevel?.sendParticles(
                    ParticleTypes.SMOKE,
                    targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5,
                    15, 0.3, 0.3, 0.3, 0.05
                )
                serverLevel?.playSound(
                    null, targetPos,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE,
                    1.0f, 0.8f
                )
            }
        }
    }

    fun applyClaimLightSnuff(level: Level, pos: BlockPos, durationSeconds: Int) {
        val dimId = level.dimension().location().toString()
        val key = makePosKey(dimId, pos)
        val expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L)

        activeSnuffAuras[key] = SnuffAura(pos, dimId, expiresAt)

        val serverLevel = level as? ServerLevel ?: return
        serverLevel.sendParticles(
            ParticleTypes.SMOKE,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            12,
            0.2,
            0.2,
            0.2,
            0.02
        )
    }

    fun isPositionSnuffed(level: Level, pos: BlockPos): Boolean {
        val dimId = level.dimension().location().toString()
        val key = makePosKey(dimId, pos)
        val aura = activeSnuffAuras[key] ?: return false

        if (System.currentTimeMillis() > aura.expiresAtEpochMs) {
            activeSnuffAuras.remove(key)
            return false
        }
        return true
    }

    fun pruneExpiredSnuffAuras() {
        val now = System.currentTimeMillis()
        activeSnuffAuras.values.removeIf { now > it.expiresAtEpochMs }
    }

    /**
     * Tank Role Threat Control: Tanks pulling `destroys_light_on_aggro` elites off backline lights.
     */
    fun onTankDamageMob(tankPlayer: ServerPlayer, mob: Mob) {
        if (!hasDestroysLightTrait(mob)) return

        val tankMitigation = tankPlayer.getAttribute(ModEntityAttributes.ROLE_TANK_MITIGATION)?.value ?: 0.0
        if (tankMitigation > 0.10) {
            // Force mob target onto the Tank, interrupting any light-destruction pathing
            mob.target = tankPlayer

            tankPlayer.serverLevel().sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                mob.x, mob.y + mob.eyeHeight, mob.z,
                3, 0.2, 0.2, 0.2, 0.0
            )
        }
    }

    fun isFragileLightSource(state: BlockState): Boolean {
        return state.`is`(Blocks.TORCH) ||
                state.`is`(Blocks.WALL_TORCH) ||
                state.`is`(Blocks.SOUL_TORCH) ||
                state.`is`(Blocks.SOUL_WALL_TORCH) ||
                state.`is`(Blocks.CAMPFIRE) ||
                state.`is`(Blocks.SOUL_CAMPFIRE) ||
                state.`is`(Blocks.LANTERN) ||
                state.`is`(Blocks.SOUL_LANTERN)
    }

    /**
     * Custom AI Goal: When aggroed on a player, actively searches for and destroys nearby light sources.
     */
    class DestroyLightGoal(private val mob: PathfinderMob) : Goal() {

        private var targetLightPos: BlockPos? = null
        private var checkCooldown: Int = 0

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean {
            val target = mob.target
            if (target !is Player) return false

            if (!hasDestroysLightTrait(mob)) return false

            checkCooldown--
            if (checkCooldown > 0) return false
            checkCooldown = 20

            val found = findNearbyLightSource()
            if (found != null) {
                targetLightPos = found
                return true
            }
            return false
        }

        override fun canContinueToUse(): Boolean {
            val lightPos = targetLightPos ?: return false
            return mob.target is Player &&
                    !mob.navigation.isDone &&
                    isFragileLightSource(mob.level().getBlockState(lightPos))
        }

        override fun start() {
            targetLightPos?.let {
                mob.navigation.moveTo(it.x + 0.5, it.y.toDouble(), it.z + 0.5, 1.25)
            }
        }

        override fun tick() {
            val lightPos = targetLightPos ?: return
            mob.lookControl.setLookAt(lightPos.x + 0.5, lightPos.y.toDouble(), lightPos.z + 0.5, 30.0f, 30.0f)

            val distSq = mob.distanceToSqr(lightPos.x + 0.5, lightPos.y.toDouble(), lightPos.z + 0.5)
            if (distSq <= 4.0) {
                onAggroLightInteraction(mob, lightPos)
                stop()
            }
        }

        override fun stop() {
            targetLightPos = null
            mob.navigation.stop()
        }

        private fun findNearbyLightSource(): BlockPos? {
            val level = mob.level()
            val mobPos = mob.blockPosition()
            val searchRadius = 8

            var closestPos: BlockPos? = null
            var closestDistSq = Double.MAX_VALUE

            for (x in -searchRadius..searchRadius) {
                for (y in -2..3) {
                    for (z in -searchRadius..searchRadius) {
                        val testPos = mobPos.offset(x, y, z)
                        val state = level.getBlockState(testPos)

                        if (isFragileLightSource(state)) {
                            val distSq = mob.distanceToSqr(testPos.x + 0.5, testPos.y.toDouble(), testPos.z + 0.5)
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq
                                closestPos = testPos
                            }
                        }
                    }
                }
            }

            return closestPos
        }
    }
}
