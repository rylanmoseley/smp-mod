package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

/**
 * Handles Spatial Light AI, Dark-Zone Escalation, and Scout Threat Detection strictly adhering to design-v2.md:
 * - Un-aggroed Overworld mobs actively avoid illuminated corridors (lit roads & claimed towns).
 * - Mobs operating/spawning in darkness receive escalated stats, AI pathfinding aggression, and varied traits.
 * - Scout combat role provides dark-zone threat and trap detection.
 */
object SpatialLightAI {

    const val DEFAULT_LIGHT_AVOIDANCE_THRESHOLD: Int = 8

    /**
     * Injects the light avoidance goal into qualifying Overworld hostile mobs.
     */
    fun attachLightAvoidanceAI(mob: PathfinderMob) {
        if (mob.level().dimension() != Level.OVERWORLD) return
        mob.goalSelector.addGoal(3, AvoidLightGoal(mob, DEFAULT_LIGHT_AVOIDANCE_THRESHOLD))
    }

    /**
     * Applies dark-zone stat escalation and aggression scaling to mobs operating or spawning in darkness.
     */
    fun escalateDarkZoneMob(mob: Mob) {
        val level = mob.level()
        val pos = mob.blockPosition()
        val blockLight = level.getBrightness(LightLayer.BLOCK, pos)

        // Only escalate in deep darkness (light level 0 in caves, dungeons, or night)
        if (blockLight > 0) return

        val isDeepCave = pos.y < 0
        val isUnderground = pos.y < 50

        if (!isUnderground && level.isDay) return

        // 1. Enhanced AI pathfinding aggression (double follow range)
        val followAttr = mob.getAttribute(Attributes.FOLLOW_RANGE)
        if (followAttr != null && followAttr.baseValue < 40.0) {
            followAttr.baseValue = 40.0
        }

        // 2. Escalated combat statistics
        val healthMultiplier = if (isDeepCave) 1.50 else 1.35
        val damageMultiplier = if (isDeepCave) 1.40 else 1.25

        val maxHealthAttr = mob.getAttribute(Attributes.MAX_HEALTH)
        if (maxHealthAttr != null) {
            val original = maxHealthAttr.baseValue
            maxHealthAttr.baseValue = original * healthMultiplier
            mob.health = maxHealthAttr.baseValue.toFloat()
        }

        val attackAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE)
        if (attackAttr != null) {
            attackAttr.baseValue = attackAttr.baseValue * damageMultiplier
        }

        val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED)
        if (speedAttr != null) {
            speedAttr.baseValue = speedAttr.baseValue * 1.15
        }

        val knockbackAttr = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
        if (knockbackAttr != null) {
            knockbackAttr.baseValue = (knockbackAttr.baseValue + 0.25).coerceAtMost(1.0)
        }

        mob.addTag("smpmod_dark_zone_escalated")
    }

    /**
     * Scout Role Threat Radar: detects hidden dark-zone elites and illuminates them for the party.
     */
    fun performScoutThreatDetection(player: ServerPlayer) {
        val scoutAgility = player.getAttribute(ModEntityAttributes.ROLE_SCOUT_AGILITY)?.value ?: 1.0
        if (scoutAgility <= 1.05) return

        val serverLevel = player.serverLevel()
        val scanRadius = 24.0
        val scanRadiusSq = scanRadius * scanRadius

        val threats = serverLevel.getEntitiesOfClass(Mob::class.java, player.boundingBox.inflate(scanRadius)) { mob ->
            mob.isAlive &&
                    mob.distanceToSqr(player) <= scanRadiusSq &&
                    (mob.tags.contains("smpmod_dark_zone_escalated") || LightMobAIHandler.hasDestroysLightTrait(mob))
        }

        if (threats.isNotEmpty()) {
            for (threat in threats) {
                // Highlight dark threats with glowing effect for the Scout
                threat.addEffect(MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false))
            }

            if (player.tickCount % 40 == 0) {
                val count = threats.size
                player.displayClientMessage(
                    Component.literal("§e[Scout Radar] Detected $count dark-zone threat(s) lurking in shadow!"),
                    true
                )
            }
        }
    }

    /**
     * Custom AI Goal: Un-aggroed Overworld mobs actively avoid illuminated corridors,
     * lit roads, and claimed settlements, retreating toward shadow.
     */
    class AvoidLightGoal(
        private val mob: PathfinderMob,
        private val lightThreshold: Int = 8
    ) : Goal() {

        private var targetPos: Vec3? = null

        init {
            flags = EnumSet.of(Flag.MOVE)
        }

        override fun canUse(): Boolean {
            // Only un-aggroed mobs avoid light; once aggroed, combat takes priority
            if (mob.target != null) return false

            val level = mob.level()
            if (level.dimension() != Level.OVERWORLD) return false

            val currentPos = mob.blockPosition()
            val currentBlockLight = level.getBrightness(LightLayer.BLOCK, currentPos)

            // Also check if inside a contiguous claim or on a paved road
            val inClaim = ClaimManager.getClaimOwner(level, ChunkPos(currentPos)) != null

            if (currentBlockLight < lightThreshold && !inClaim) {
                return false
            }

            // Find a darker retreat position
            val darkTarget = findDarkRetreatPosition() ?: return false
            targetPos = Vec3(darkTarget.x + 0.5, darkTarget.y.toDouble(), darkTarget.z + 0.5)
            return true
        }

        override fun canContinueToUse(): Boolean {
            return mob.target == null && !mob.navigation.isDone && targetPos != null
        }

        override fun start() {
            targetPos?.let {
                mob.navigation.moveTo(it.x, it.y, it.z, 1.2)
            }
        }

        override fun stop() {
            targetPos = null
            mob.navigation.stop()
        }

        private fun findDarkRetreatPosition(): BlockPos? {
            val level = mob.level()
            val mobPos = mob.blockPosition()
            var bestPos: BlockPos? = null
            var lowestLight = lightThreshold

            // Sample random positions within 8 blocks
            for (i in 0 until 12) {
                val randomOffset = DefaultRandomPos.getPos(mob, 8, 4) ?: continue
                val testPos = BlockPos(randomOffset.x.toInt(), randomOffset.y.toInt(), randomOffset.z.toInt())

                val testBlockLight = level.getBrightness(LightLayer.BLOCK, testPos)
                val testInClaim = ClaimManager.getClaimOwner(level, ChunkPos(testPos)) != null

                if (!testInClaim && testBlockLight < lowestLight) {
                    lowestLight = testBlockLight
                    bestPos = testPos
                    if (lowestLight <= 2) break
                }
            }

            return bestPos
        }
    }
}
