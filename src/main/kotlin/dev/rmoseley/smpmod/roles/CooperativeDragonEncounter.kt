package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.economy.EconomyManager
import dev.rmoseley.smpmod.recovery.ItemProvenance
import dev.rmoseley.smpmod.world.WardedLightManager
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Centerpiece Cooperative Endgame Boss Encounter:
 * Upgraded Ender Dragon encounter integrating all four combat roles:
 * - Tanks: Absorb dive and breath phases; blocking dive staggers dragon into vulnerability.
 * - Scouts: Navigate high pillars, disarm caged crystals, and disrupt healing tethers.
 * - Support: Deploys Warded Light flares to neutralize acid clouds and maintains team sustain.
 * - DPS: Unleashes massive burst execution damage during stagger and perch punish windows.
 */
object CooperativeDragonEncounter {

    data class EncounterStats(
        var dragonUuid: UUID? = null,
        var staggerExpiryTick: Long = 0L,
        var isTetherStunned: Boolean = false,
        val tankDiveIntercepts: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap(),
        val scoutCrystalsShattered: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap(),
        val dpsPunishDamage: ConcurrentHashMap<UUID, Double> = ConcurrentHashMap(),
        val supportHealingProvided: ConcurrentHashMap<UUID, Double> = ConcurrentHashMap()
    )

    private val currentEncounter = EncounterStats()

    /**
     * Checks if the Ender Dragon is currently in an active Punish / Vulnerability Window.
     */
    fun isDragonInPunishWindow(dragon: EnderDragon): Boolean {
        val level = dragon.level()
        val currentTick = level.gameTime

        // 1. Staggered from Tank dive intercept or Scout crystal backlash
        if (currentTick < currentEncounter.staggerExpiryTick) {
            return true
        }

        // 2. Perched at the central bedrock fountain
        val phase = dragon.phaseManager.currentPhase.phase
        if (phase == EnderDragonPhase.SITTING_FLAMING ||
            phase == EnderDragonPhase.SITTING_ATTACKING ||
            phase == EnderDragonPhase.SITTING_SCANNING
        ) {
            return true
        }

        return false
    }

    fun getActiveDragon(level: ServerLevel): EnderDragon? {
        val uuid = level.dragonFight?.dragonUUID ?: currentEncounter.dragonUuid
        if (uuid != null) {
            val entity = level.getEntity(uuid)
            if (entity is EnderDragon) return entity
        }
        val dragons = level.getEntitiesOfClass(EnderDragon::class.java, AABB(-256.0, 0.0, -256.0, 256.0, 256.0, 256.0))
        return dragons.firstOrNull()
    }

    /**
     * Called on each server tick in the End dimension to orchestrate encounter mechanics.
     */
    fun onEndLevelTick(level: ServerLevel) {
        if (level.dimension() != Level.END) return

        val dragon = getActiveDragon(level) ?: return
        currentEncounter.dragonUuid = dragon.uuid

        // 1. Support Warded Flare Acid Neutralization
        // Dispel purple dragon breath acid clouds near Warded Flares
        val breathClouds = level.getEntitiesOfClass(AreaEffectCloud::class.java, dragon.boundingBox.inflate(64.0))
        for (cloud in breathClouds) {
            val cloudPos = cloud.blockPosition()
            if (WardedLightManager.isWarded(level, cloudPos) || isNearAnyWardedFlare(level, cloudPos, 10.0)) {
                // Neutralize acid cloud
                cloud.discard()
                level.sendParticles(
                    ParticleTypes.GLOW,
                    cloud.x, cloud.y + 0.5, cloud.z,
                    15, 0.5, 0.2, 0.5, 0.05
                )
            }
        }

        // 2. If staggered, immobilize dragon and display reeling particles
        val currentTick = level.gameTime
        if (currentTick < currentEncounter.staggerExpiryTick) {
            dragon.deltaMovement = dragon.deltaMovement.scale(0.1)
            level.sendParticles(
                ParticleTypes.CRIT,
                dragon.x, dragon.y + 2.0, dragon.z,
                8, 1.5, 0.5, 1.5, 0.1
            )
        }
    }

    /**
     * Tank Cooperative Mechanic:
     * When the Dragon charges/dives (CHARGING_PLAYER) and hits a blocking Tank.
     */
    fun onDragonDiveHitPlayer(dragon: EnderDragon, player: ServerPlayer, rawDamage: Float): Boolean {
        val phase = dragon.phaseManager.currentPhase.phase
        val isDivePhase = phase == EnderDragonPhase.CHARGING_PLAYER || dragon.deltaMovement.lengthSqr() > 1.2

        if (isDivePhase && player.isBlocking && TankRoleHandler.isTankSpecialist(player)) {
            val level = player.serverLevel()
            val staggerSeconds = SmpConfig.COMMON.dragonStaggerDurationSeconds.get()
            currentEncounter.staggerExpiryTick = level.gameTime + (staggerSeconds * 20L)

            // Record Tank achievement
            val count = currentEncounter.tankDiveIntercepts.compute(player.uuid) { _, curr -> (curr ?: 0) + 1 } ?: 1
            dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
                player,
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.TANK_DIVE_INTERCEPT
            )

            // Broadcast stagger announcement to all players in the End
            val raidMsg = Component.literal(
                "§6[Raid Alert] §9Tank §f${player.name.string} §6intercepted Dragon Dive! Dragon STAGGERED for ${staggerSeconds}s! §c(DPS Punish Window Open!)"
            )
            level.players().forEach { it.sendSystemMessage(raidMsg) }

            // Stagger visuals and sound
            level.sendParticles(
                ParticleTypes.FLASH,
                player.x, player.y + 1.5, player.z,
                2, 0.0, 0.0, 0.0, 0.0
            )
            level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                dragon.x, dragon.y + 1.0, dragon.z,
                1, 0.0, 0.0, 0.0, 0.0
            )
            level.playSound(
                null, player.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                1.5f, 0.8f
            )
            level.playSound(
                null, dragon.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE,
                1.5f, 0.6f
            )

            // Stagger dragon momentum
            dragon.deltaMovement = dragon.deltaMovement.scale(-0.4)
            return true // Intercepted successfully
        }

        return false
    }

    /**
     * Scout Cooperative Mechanic:
     * When a player destroys an End Crystal on a pillar.
     */
    fun onEndCrystalDestroyed(player: ServerPlayer, crystal: EndCrystal) {
        val level = player.serverLevel()
        if (level.dimension() != Level.END) return

        val isScout = ScoutRoleHandler.isScoutSpecialist(player)
        if (isScout) {
            val count = currentEncounter.scoutCrystalsShattered.compute(player.uuid) { _, curr -> (curr ?: 0) + 1 } ?: 1

            // Check if crystal was actively tethered to the dragon
            val dragon = getActiveDragon(level)
            val isTethered = dragon != null && (dragon.nearestCrystal == crystal || crystal.beamTarget != null)

            if (dragon != null && isTethered) {
                // Backlash shockwave staggers dragon
                val staggerSeconds = 5
                currentEncounter.staggerExpiryTick = level.gameTime + (staggerSeconds * 20L)

                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
                    player,
                    dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.SCOUT_CRYSTAL_NEUTRALIZER
                )

                val alert = Component.literal(
                    "§6[Raid Alert] §bScout §f${player.name.string} §6shattered healing crystal! Backlash stuns Dragon! §c(Punish Window Open!)"
                )
                level.players().forEach { it.sendSystemMessage(alert) }

                dragon.hurt(level.damageSources().magic(), 25.0f)
            } else {
                player.displayClientMessage(
                    Component.literal("§b[Scout] End Crystal safely dismantled without explosive recoil."),
                    true
                )
            }
        }
    }

    /**
     * DPS Punish Window Damage Accounting:
     * Records bonus punish damage dealt to the dragon during vulnerability phases.
     */
    fun recordDpsPunishDamage(player: ServerPlayer, damage: Double) {
        currentEncounter.dpsPunishDamage.compute(player.uuid) { _, curr -> (curr ?: 0.0) + damage }
        dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
            player,
            dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.DPS_EXECUTE_STRIKE
        )
    }

    /**
     * Support Healing Accounting during the dragon encounter.
     */
    fun recordSupportHealing(player: ServerPlayer, healedAmount: Double) {
        val total = currentEncounter.supportHealingProvided.compute(player.uuid) { _, curr -> (curr ?: 0.0) + healedAmount } ?: 0.0
        if (total >= 20.0) {
            dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
                player,
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.SUPPORT_REJUVENATION
            )
        }
    }

    /**
     * Called when the Ender Dragon is slain in the End dimension.
     * Evaluates all role contributions, awards Playstyle XP & currency,
     * and permanently logs combat history onto players' held items.
     */
    fun onDragonDefeated(level: ServerLevel, dragon: EnderDragon) {
        val playersInEnd = level.players().filter { it.distanceToSqr(dragon) < 256.0 * 256.0 }
        if (playersInEnd.isEmpty()) return

        val victoryBanner = Component.literal(
            "§d========================================\n" +
                    "§6★ COOPERATIVE ENDGAME VICTORY: ENDER DRAGON SLAIN! ★\n" +
                    "§d========================================"
        )
        playersInEnd.forEach { it.sendSystemMessage(victoryBanner) }

        // Dispatch Discord Webhook announcement
        dev.rmoseley.smpmod.server.DiscordWebhookDispatcher.notifyDragonDefeated(playersInEnd.map { it.gameProfile.name })

        for (player in playersInEnd) {
            val profile = RoleCalculator.calculateProfile(player)
            val heldItem = player.mainHandItem

            // Award Vanguard of the End Legendary Advancement
            dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
                player,
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.COOPERATIVE_DRAGON_TRIUMPH
            )

            // 1. Award 2500 XP to player's primary playstyle track
            when (profile.primaryRole) {
                RoleCalculator.Role.DPS -> PlaystyleXpTracker.incrementCombatXp(player, 2500.0)
                RoleCalculator.Role.TANK -> PlaystyleXpTracker.incrementBuilderXp(player, 2500.0)
                RoleCalculator.Role.SUPPORT -> PlaystyleXpTracker.incrementGathererXp(player, 2500.0)
                RoleCalculator.Role.SCOUT -> PlaystyleXpTracker.incrementExplorerXp(player, 2500.0)
                RoleCalculator.Role.HYBRID -> {
                    PlaystyleXpTracker.incrementCombatXp(player, 625.0)
                    PlaystyleXpTracker.incrementBuilderXp(player, 625.0)
                    PlaystyleXpTracker.incrementGathererXp(player, 625.0)
                    PlaystyleXpTracker.incrementExplorerXp(player, 625.0)
                }
            }

            // 2. Award 500 digital non-material currency
            EconomyManager.credit(player, 500L)

            // 3. Log combat achievement onto item lore
            if (!heldItem.isEmpty) {
                val achievement = when (profile.primaryRole) {
                    RoleCalculator.Role.DPS -> {
                        val punishDmg = currentEncounter.dpsPunishDamage[player.uuid]?.toInt() ?: 250
                        "Dragon Slayer - Punish Burst: $punishDmg dmg"
                    }
                    RoleCalculator.Role.TANK -> {
                        val intercepts = currentEncounter.tankDiveIntercepts[player.uuid] ?: 1
                        "Dragon Bulwark - Intercepted $intercepts Dives"
                    }
                    RoleCalculator.Role.SUPPORT -> {
                        val healing = currentEncounter.supportHealingProvided[player.uuid]?.toInt() ?: 120
                        "Sanctuary Pillar - Restored $healing Raid HP"
                    }
                    RoleCalculator.Role.SCOUT -> {
                        val crystals = currentEncounter.scoutCrystalsShattered[player.uuid] ?: 2
                        "End Vanguard - Shattered $crystals Crystals"
                    }
                    RoleCalculator.Role.HYBRID -> "Dragon Raid Victor"
                }

                ItemProvenance.addCombatHistory(heldItem, achievement)
                player.sendSystemMessage(
                    Component.literal("§a[Provenance Lore] Logged accomplishment onto ${heldItem.hoverName.string}: §d$achievement")
                )
            }
        }

        // Reset encounter stats
        currentEncounter.tankDiveIntercepts.clear()
        currentEncounter.scoutCrystalsShattered.clear()
        currentEncounter.dpsPunishDamage.clear()
        currentEncounter.supportHealingProvided.clear()
        currentEncounter.staggerExpiryTick = 0L
    }

    private fun isNearAnyWardedFlare(level: Level, pos: net.minecraft.core.BlockPos, radius: Double): Boolean {
        for (dx in -radius.toInt()..radius.toInt() step 3) {
            for (dz in -radius.toInt()..radius.toInt() step 3) {
                val checkPos = pos.offset(dx, 0, dz)
                if (WardedLightManager.isWarded(level, checkPos)) {
                    return true
                }
            }
        }
        return false
    }
}
