package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.claims.CivicUpgrade
import dev.rmoseley.smpmod.claims.TownTreasuryManager
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.entity.vehicle.AbstractMinecart
import net.minecraft.world.level.block.BaseRailBlock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Caravan & Highway Transit mechanics.
 * Conforms to design-v2.md Section 4 (World, Survival & Exploration):
 * Incentivizes organized group movement, road networks, pack animal caravans,
 * and railway infrastructure while mitigating Nether solo-exhaustion.
 */
object CaravanTransitManager {

    private val activeCaravanPlayers = ConcurrentHashMap<UUID, Long>()

    /**
     * Ticks caravan detection for an active player.
     */
    fun onPlayerTick(player: ServerPlayer) {
        if (player.isSpectator) return

        val inCaravan = evaluateCaravanStatus(player)
        if (inCaravan) {
            activeCaravanPlayers[player.uuid] = System.currentTimeMillis()
            applyCaravanBonuses(player)
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onCaravanTransitCheck(player)
        } else {
            activeCaravanPlayers.remove(player.uuid)
        }
    }

    fun isInCaravan(player: ServerPlayer): Boolean {
        val lastSeen = activeCaravanPlayers[player.uuid] ?: return false
        return (System.currentTimeMillis() - lastSeen) < 3000L
    }

    /**
     * Determines whether the player qualifies as being part of an active caravan.
     * Criteria:
     * 1. Accompanied by another non-spectator player within 24 blocks, OR
     * 2. Leading or riding a pack animal (Llama, Donkey, Mule, Horse, Camel), OR
     * 3. Riding a minecart on rails.
     */
    private fun evaluateCaravanStatus(player: ServerPlayer): Boolean {
        val level = player.serverLevel()

        // 1. Minecart transit
        val vehicle = player.vehicle
        if (vehicle is AbstractMinecart) {
            return true
        }

        // 2. Pack animal mount or leashed beasts
        if (vehicle is AbstractHorse || vehicle is Camel) {
            return true
        }

        val nearbyAnimals = level.getEntitiesOfClass(
            AbstractHorse::class.java,
            player.boundingBox.inflate(12.0)
        ) { it.isLeashed && it.leashHolder == player }
        if (nearbyAnimals.isNotEmpty()) return true

        val nearbyLlamas = level.getEntitiesOfClass(
            Llama::class.java,
            player.boundingBox.inflate(12.0)
        ) { (it.isLeashed && it.leashHolder == player) || it.inCaravan() }
        if (nearbyLlamas.isNotEmpty()) return true

        // 3. Multi-player group traversal within 24 blocks
        val nearbyPlayers = level.players().any { other ->
            other != player && !other.isSpectator && other.distanceToSqr(player) <= 576.0 // 24^2
        }

        return nearbyPlayers
    }

    /**
     * Applies speed boosts, reduced exhaustion, and particles to players in caravans on roads.
     */
    private fun applyCaravanBonuses(player: ServerPlayer) {
        val isOnRoad = BiomeMobilityManager.isStandingOnRoad(player)
        val isOnRail = player.level().getBlockState(player.blockPosition()).block is BaseRailBlock ||
                player.level().getBlockState(player.blockPosition().below()).block is BaseRailBlock

        if (isOnRoad || isOnRail) {
            // Check for Town Civic Paved Highway Network upgrade
            val hasTownHighwayUpgrade = TownTreasuryManager.hasUpgradeAt(
                player.level(),
                player.blockPosition(),
                CivicUpgrade.PAVED_HIGHWAY_NETWORK
            )

            val amplifier = if (hasTownHighwayUpgrade) 1 else 0 // Speed II vs Speed I
            player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, amplifier, false, false, false))

            // Mitigate food exhaustion (half exhaustion drain during caravan travel)
            if (player.tickCount % 20 == 0 && player.foodData.needsFood()) {
                player.causeFoodExhaustion(-0.1f)
            }

            // Occasional caravan trail particles
            if (player.tickCount % 20 == 0) {
                val serverLevel = player.serverLevel()
                serverLevel.sendParticles(
                    ParticleTypes.COMPOSTER,
                    player.x, player.y + 0.1, player.z,
                    3, 0.2, 0.1, 0.2, 0.02
                )
            }
        }
    }
}
