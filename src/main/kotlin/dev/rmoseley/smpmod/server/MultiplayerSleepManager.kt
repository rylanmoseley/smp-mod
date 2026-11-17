package dev.rmoseley.smpmod.server

import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.world.RepurposedVillageManager
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.Level

/**
 * Manages multiplayer dynamic sleep mechanics adhering to Vanilla Tweaks behavior:
 * - Dynamic percentage sleep (default 1-player sleep threshold)
 * - Sleep progression and sunrise realm announcements
 * - Storm and thunderstorm weather clearing upon night pass
 * - Sanctuary "Well Rested" recovery buffs when awakening in claimed settlements or village hubs.
 */
object MultiplayerSleepManager {

    /**
     * Enforces configured sleeping percentage gamerule across all server levels.
     */
    fun syncGamerule(server: MinecraftServer) {
        val percentage = SmpConfig.COMMON.playersSleepingPercentage.get()
        for (level in server.allLevels) {
            val rule = level.gameRules.getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE)
            if (rule.get() != percentage) {
                rule.set(percentage, server)
            }
        }
    }

    /**
     * Triggered when a player enters a bed to rest.
     */
    fun onPlayerEnterBed(player: ServerPlayer) {
        val level = player.serverLevel()
        if (level.dimension() != Level.OVERWORLD) return

        player.persistentData.putBoolean("smpmod:is_sleeping", true)

        if (SmpConfig.COMMON.announceSleep.get()) {
            val totalPlayers = level.players().size
            val sleepingPlayers = level.players().count { it.isSleeping } + 1

            level.server.playerList.broadcastSystemMessage(
                Component.literal("§e[Rest] §b${player.gameProfile.name} §fis resting in bed. (§a$sleepingPlayers§7/§a$totalPlayers §fasleep). Night is passing..."),
                false
            )
        }
    }

    /**
     * Triggered when night passes and the server advances time to sunrise.
     */
    fun onSleepFinished(level: ServerLevel) {
        if (level.dimension() != Level.OVERWORLD) return

        // 1. Clear stormy and rainy weather upon waking
        level.setWeatherParameters(0, 0, false, false)

        // 2. Announce sunrise to the realm
        if (SmpConfig.COMMON.announceSunrise.get()) {
            level.server.playerList.broadcastSystemMessage(
                Component.literal("§6🌅 [Sunrise] §fThe morning sun rises upon the realm. Good morning!"),
                false
            )
        }

        // 3. Award Sanctuary Well Rested buffs to players who were sleeping in claimed towns or villages
        if (SmpConfig.COMMON.enableSanctuaryRestBuffs.get()) {
            for (player in level.players()) {
                val wasSleeping = player.persistentData.getBoolean("smpmod:is_sleeping")
                if (wasSleeping) {
                    player.persistentData.putBoolean("smpmod:is_sleeping", false)

                    val pos = player.blockPosition()
                    val inTown = ClaimManager.getContiguousCluster(level, ChunkPos(pos)) != null
                    val inVillage = RepurposedVillageManager.isInsideVillageSanctuary(level, pos)

                    if (inTown || inVillage) {
                        player.addEffect(MobEffectInstance(MobEffects.DIG_SPEED, 2400, 0, false, false, true)) // Haste I for 2 mins
                        player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 800, 0, false, false, true)) // Regen I for 40s
                        player.addEffect(MobEffectInstance(MobEffects.SATURATION, 600, 0, false, false, true))

                        level.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            player.x, player.y + 0.5, player.z,
                            15, 0.4, 0.4, 0.4, 0.05
                        )
                        player.sendSystemMessage(
                            Component.literal("§a[Well Rested] §fYou awaken inside a fortified sanctuary feeling energized! (Haste I & Regeneration active)")
                        )
                    }
                }
            }
        }
    }

    /**
     * Cleans up sleep state when player leaves bed prematurely.
     */
    fun onPlayerLeaveBed(player: ServerPlayer) {
        player.persistentData.putBoolean("smpmod:is_sleeping", false)
    }
}
