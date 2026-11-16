package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.config.SmpConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.monster.piglin.AbstractPiglin
import net.minecraft.world.entity.vehicle.AbstractMinecart
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Enforces Nether transit hazards and dimensional restrictions strictly adhering to design-v2.md:
 * 1. Nether roof access completely blocked (teleporting players down, cancelling roof building).
 * 2. Nether highway transit hazards:
 *    - Thermal track/ice instability (ice boats melting, rail friction instabilities).
 *    - Barrier-breaching Piglin/Ghast aggro targeting transit infrastructure.
 *    - Anti-solo sprint penalty (punishing solo high-speed commuters; shared resistance in convoys).
 */
object NetherHazardHandler {

    // Persistent tracking of solo sprint duration per player UUID
    private val soloSprintTicks = ConcurrentHashMap<UUID, Int>()

    // Tracking for vehicle thermal instability alerts to avoid spamming
    private val lastVehicleWarningTick = ConcurrentHashMap<UUID, Long>()

    /**
     * Periodic check executed during player tick in the Nether.
     */
    fun checkNetherPlayer(player: ServerPlayer) {
        if (player.level().dimension() != Level.NETHER) {
            soloSprintTicks.remove(player.uuid)
            return
        }

        val roofLimit = SmpConfig.COMMON.netherRoofYLimit.get()

        // 1. Nether roof barrier enforcement: teleport players down
        if (player.y >= roofLimit && !player.isCreative && !player.isSpectator) {
            val safeY = (roofLimit - 7).toDouble().coerceAtLeast(64.0)
            player.teleportTo(player.x, safeY, player.z)
            player.displayClientMessage(
                Component.literal("§cDimensional thermal turbulence bars Nether roof transit!"),
                true
            )
            player.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true))
            return
        }

        // 2. Anti-solo sprint penalty
        handleAntiSoloSprint(player)

        // 3. Vehicle transit hazards (ice boats melting & rail friction instabilities)
        handleVehicleHazards(player)
    }

    /**
     * Validates whether a block placement attempt in the Nether should be cancelled.
     * Cancels any building on or above the Nether roof.
     */
    fun shouldCancelRoofBuilding(level: Level, pos: BlockPos, player: ServerPlayer?): Boolean {
        if (level.dimension() != Level.NETHER) return false
        val roofLimit = SmpConfig.COMMON.netherRoofYLimit.get()

        if (pos.y >= roofLimit) {
            if (player != null && !player.isCreative) {
                player.displayClientMessage(
                    Component.literal("§cDimensional turbulence prevents construction on the Nether roof!"),
                    true
                )
            }
            return true
        }
        return false
    }

    /**
     * Validates whether an Ender Pearl or chorus fruit teleportation target is on the Nether roof.
     */
    fun shouldCancelRoofTeleport(level: Level, targetPos: Vec3, player: ServerPlayer?): Boolean {
        if (level.dimension() != Level.NETHER) return false
        val roofLimit = SmpConfig.COMMON.netherRoofYLimit.get()

        if (targetPos.y >= roofLimit) {
            if (player != null && !player.isCreative) {
                player.displayClientMessage(
                    Component.literal("§cThermal barrier deflects teleportation above Nether bedrock ceiling!"),
                    true
                )
            }
            return true
        }
        return false
    }

    /**
     * Enforces the anti-solo sprint penalty.
     * Solo high-speed sprinting commuters suffer thermal exhaustion, slowness, and heat damage.
     * Cooperative convoys (another player within convoy radius) nullify the penalty entirely.
     */
    private fun handleAntiSoloSprint(player: ServerPlayer) {
        if (player.isCreative || player.isSpectator) return

        if (!player.isSprinting) {
            // Sprint stopped; decay exhaustion
            val current = soloSprintTicks[player.uuid] ?: 0
            if (current > 0) {
                soloSprintTicks[player.uuid] = (current - 2).coerceAtLeast(0)
            }
            return
        }

        val serverLevel = player.serverLevel()
        val convoyRadius = SmpConfig.COMMON.netherConvoyRadius.get()
        val convoyRadiusSq = convoyRadius * convoyRadius

        // Search for nearby players in the Nether or pack animal caravans to form a cooperative transit convoy
        val nearbyConvoy = serverLevel.players().any { other ->
            other != player && !other.isSpectator && other.distanceToSqr(player) <= convoyRadiusSq
        } || CaravanTransitManager.isInCaravan(player)

        if (nearbyConvoy) {
            // Convoy active: shared thermal resistance
            val wasExhausted = (soloSprintTicks[player.uuid] ?: 0) > 40
            soloSprintTicks[player.uuid] = 0
            if (wasExhausted) {
                player.displayClientMessage(
                    Component.literal("§aNether Convoy Active: Thermal resistance shared with fellow commuters."),
                    true
                )
            }
        } else {
            // Solo commuter: thermal winds cause escalating exhaustion
            val ticks = (soloSprintTicks[player.uuid] ?: 0) + 1
            soloSprintTicks[player.uuid] = ticks

            val thresholdTicks = SmpConfig.COMMON.netherSoloSprintExhaustionSeconds.get() * 20

            if (ticks >= thresholdTicks) {
                // Drain food exhaustion rapidly
                player.causeFoodExhaustion(0.08f)

                // Apply thermal slowness
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true))

                // Periodic thermal damage tick every 2 seconds
                if (ticks % 40 == 0) {
                    player.hurt(player.damageSources().hotFloor(), 1.0f)
                    serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        player.x, player.y + 0.5, player.z,
                        5, 0.2, 0.2, 0.2, 0.02
                    )
                    serverLevel.playSound(
                        null, player.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                        0.5f, 1.2f
                    )
                }

                player.displayClientMessage(
                    Component.literal("§cThermal winds punish solo sprint! [Convoy required to traverse Nether safely]"),
                    true
                )
            }
        }
    }

    /**
     * Enforces thermal track instability:
     * - Ice boats melting under extreme thermal friction.
     * - Rail friction instabilities (sparks, shudder, speed wobble).
     */
    private fun handleVehicleHazards(player: ServerPlayer) {
        val vehicle = player.vehicle ?: return
        val serverLevel = player.serverLevel()
        val currentTick = serverLevel.gameTime

        // 1. Ice Boat Thermal Instability
        if (vehicle is Boat) {
            val underPos = vehicle.blockPosition().below()
            val state = serverLevel.getBlockState(underPos)

            if (isIceBlock(state)) {
                // Thermal friction melts the ice track into basalt or steam
                serverLevel.setBlockAndUpdate(underPos, Blocks.BASALT.defaultBlockState())
                serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    vehicle.x, vehicle.y, vehicle.z,
                    15, 0.3, 0.1, 0.3, 0.05
                )
                serverLevel.playSound(
                    null, underPos,
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                    0.8f, 1.0f
                )

                // Overheat boat: apply fire ticks and eject passenger
                vehicle.remainingFireTicks = 100
                player.hurt(player.damageSources().hotFloor(), 2.0f)
                player.stopRiding()

                player.displayClientMessage(
                    Component.literal("§cThermal friction melted ice undercraft! Ice boats cannot withstand Nether heat."),
                    true
                )
            }
        }

        // 2. Rail Friction Instability
        if (vehicle is AbstractMinecart) {
            val railPos = vehicle.blockPosition()
            val state = serverLevel.getBlockState(railPos)

            if (isRailBlock(state)) {
                val speedSq = vehicle.deltaMovement.horizontalDistanceSqr()
                // If traveling at high rail speed (> 0.2 approx 4 m/s)
                if (speedSq > 0.04) {
                    // Thermal sparks and speed wobble
                    serverLevel.sendParticles(
                        ParticleTypes.LAVA,
                        vehicle.x, vehicle.y + 0.1, vehicle.z,
                        3, 0.2, 0.1, 0.2, 0.05
                    )

                    // Deceleration from thermal rail friction
                    vehicle.deltaMovement = vehicle.deltaMovement.scale(0.85)

                    val lastWarning = lastVehicleWarningTick[player.uuid] ?: 0L
                    if (currentTick - lastWarning > 60L) {
                        lastVehicleWarningTick[player.uuid] = currentTick
                        player.displayClientMessage(
                            Component.literal("§6Rail thermal friction causing track instability! Slow down to avoid derailment."),
                            true
                        )
                        serverLevel.playSound(
                            null, railPos,
                            SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL,
                            0.6f, 1.5f
                        )
                    }
                }
            }
        }
    }

    /**
     * Barrier-breaching Ghast and Piglin aggro targeting transit infrastructure and commuters.
     */
    fun handleNetherMobAggro(mob: Mob) {
        if (mob.level().dimension() != Level.NETHER) return

        // Check if mob is near transit corridors or high-speed commuters
        if (mob is Ghast || mob is AbstractPiglin) {
            // Expand follow range for transit interception
            val followAttr = mob.getAttribute(Attributes.FOLLOW_RANGE)
            if (followAttr != null && followAttr.baseValue < 48.0) {
                followAttr.baseValue = 48.0
            }

            // If mob has no target, scan for nearby commuters in vehicles or sprinting
            if (mob.target == null) {
                val level = mob.level() as? ServerLevel ?: return
                val commuter = level.players().firstOrNull { p ->
                    p.distanceToSqr(mob) <= 48.0 * 48.0 &&
                            !p.isCreative && !p.isSpectator &&
                            (p.vehicle != null || p.isSprinting)
                }

                if (commuter != null) {
                    mob.target = commuter
                }
            }
        }
    }

    private fun isIceBlock(state: BlockState): Boolean {
        return state.`is`(Blocks.ICE) ||
                state.`is`(Blocks.PACKED_ICE) ||
                state.`is`(Blocks.BLUE_ICE) ||
                state.`is`(Blocks.FROSTED_ICE)
    }

    private fun isRailBlock(state: BlockState): Boolean {
        return state.`is`(Blocks.RAIL) ||
                state.`is`(Blocks.POWERED_RAIL) ||
                state.`is`(Blocks.DETECTOR_RAIL) ||
                state.`is`(Blocks.ACTIVATOR_RAIL)
    }
}
