package dev.rmoseley.smpmod.roles

import dev.rmoseley.smpmod.recovery.RecoveryCatalystManager
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Items
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

/**
 * Global NeoForge event listeners orchestrating the 4 combat roles:
 * DPS burst & armor penetration, Tank frontline mitigation & cover,
 * Support sustain & potion radiance, Scout mobility & threat radar,
 * and the Cooperative Ender Dragon centerpiece encounter.
 */
object RoleCombatEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingIncomingDamage(event: LivingIncomingDamageEvent) {
        val directEntity = event.source.directEntity
        // 1. Tank kinetic barrier projectile absorption
        if (directEntity is Projectile && TankRoleHandler.interceptProjectileWithBarrier(directEntity)) {
            event.isCanceled = true
            return
        }

        // 2. Tank Cooperative Ender Dragon dive interception
        val sourceEntity = event.source.entity
        val victim = event.entity
        if (sourceEntity is EnderDragon && victim is ServerPlayer) {
            if (CooperativeDragonEncounter.onDragonDiveHitPlayer(sourceEntity, victim, event.amount)) {
                // Mitigate dive kinetic damage directly
                val mitigated = TankRoleHandler.processIncomingDamage(victim, event.amount * 0.25f)
                event.amount = mitigated
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    fun onLivingDamage(event: LivingDamageEvent.Pre) {
        val target = event.entity
        val source = event.source.entity

        // 1. Target is a Player: apply Tank frontline mitigation and Scout fall/crash dampening
        if (target is Player) {
            // Tank mitigation
            var mitigated = TankRoleHandler.processIncomingDamage(target, event.newDamage)

            // Ally portable cover from nearby blocking Tank
            mitigated = TankRoleHandler.checkPortableCoverForAlly(target, mitigated)

            // Scout fall damage reduction
            if (event.source.`is`(DamageTypes.FALL)) {
                mitigated = ScoutRoleHandler.processFallDamage(target, mitigated)
            }

            // Scout Elytra collision crash dampening
            if (event.source.`is`(DamageTypes.FLY_INTO_WALL)) {
                mitigated = ScoutRoleHandler.processElytraCrashDamage(target, mitigated)
            }

            // Masterwork Affix: Titanic (+5% Armor / mitigation) & Warded (+8% dark-zone mob resistance)
            val armorAffixes = target.armorSlots.mapNotNull { dev.rmoseley.smpmod.recovery.MasterworkAffix.getAffix(it) }
            if (armorAffixes.contains(dev.rmoseley.smpmod.recovery.MasterworkAffix.TITANIC)) {
                mitigated *= 0.95f
            }
            if (armorAffixes.contains(dev.rmoseley.smpmod.recovery.MasterworkAffix.WARDED) && source is Mob) {
                mitigated *= 0.92f
            }

            event.newDamage = mitigated
        }

        // 2. Source is a Player: apply DPS burst, armor penetration, execution punish, and hit tracking
        if (source is Player) {
            val wasCritical = source.fallDistance > 0.0f &&
                    !source.onGround() &&
                    !source.onClimbable() &&
                    !source.isInWater &&
                    !source.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)

            // Masterwork Affix: Keen (+10% Critical Strike Damage)
            val heldAffix = dev.rmoseley.smpmod.recovery.MasterworkAffix.getAffix(source.mainHandItem)
            var baseAttackDmg = event.newDamage
            if (wasCritical && heldAffix == dev.rmoseley.smpmod.recovery.MasterworkAffix.KEEN) {
                baseAttackDmg *= 1.10f
            }

            // Process DPS burst scaling, armor pen, and execute punish windows
            val updatedDamage = DpsRoleHandler.processAttack(source, target, baseAttackDmg)
            event.newDamage = updatedDamage

            // Record Combat XP hit and accuracy stats (bonus tempo XP if Precise affix)
            val effectiveDmg = if (heldAffix == dev.rmoseley.smpmod.recovery.MasterworkAffix.PRECISE) updatedDamage.toDouble() * 1.15 else updatedDamage.toDouble()
            CombatXpTracker.recordHit(source, target, effectiveDmg, wasCritical)

            // Tank Threat Aggro on attack
            if (source is ServerPlayer && target is Mob) {
                TankRoleHandler.onTankDamageMob(source, target)
            }

            // Record DPS dragon punish window damage
            if (target is EnderDragon && source is ServerPlayer) {
                if (CooperativeDragonEncounter.isDragonInPunishWindow(target)) {
                    CooperativeDragonEncounter.recordDpsPunishDamage(source, updatedDamage.toDouble())
                }
            }
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        val victim = event.entity
        val killer = event.source.entity as? Player

        // 1. Cooperative Ender Dragon Defeat
        if (victim is EnderDragon) {
            val serverLevel = victim.level() as? ServerLevel
            if (serverLevel != null) {
                CooperativeDragonEncounter.onDragonDefeated(serverLevel, victim)
            }
        }

        // 2. Award Combat XP evaluated by speed, accuracy, and anti-grinder decay
        if (killer != null) {
            CombatXpTracker.onEntityDeath(killer, victim)

            if (killer is ServerPlayer && victim is Monster) {
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.grant(
                    killer,
                    dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.FIRST_KILL_TEMPO
                )
                dev.rmoseley.smpmod.economy.BountyContractManager.recordProgress(
                    killer,
                    dev.rmoseley.smpmod.economy.BountyType.COMBAT_ELITE_SLAY,
                    1
                )
                if (victim is Mob && dev.rmoseley.smpmod.world.LightMobAIHandler.hasDestroysLightTrait(victim)) {
                    dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.onSnufferMobKilled(killer)
                    dev.rmoseley.smpmod.economy.BountyContractManager.recordProgress(
                        killer,
                        dev.rmoseley.smpmod.economy.BountyType.COMBAT_ELITE_SLAY,
                        1,
                        "destroys_light"
                    )
                }

                if (victim is net.minecraft.world.entity.raid.Raider) {
                    dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.onRaidDefended(killer)
                }

                val combatXp = PlaystyleXpTracker.getCombatXp(killer)
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.checkCombatXpMilestone(killer, combatXp)
            }
        }
    }

    @SubscribeEvent
    fun onAttackEntity(event: AttackEntityEvent) {
        val target = event.target
        val player = event.entity as? ServerPlayer ?: return

        // Scout End Crystal Sabotage
        if (target is EndCrystal) {
            CooperativeDragonEncounter.onEndCrystalDestroyed(player, target)
        }
    }

    @SubscribeEvent
    fun onArmSwingEmpty(event: PlayerInteractEvent.LeftClickEmpty) {
        // Attack attempt hit no entity/block: record whiff for accuracy evaluation
        CombatXpTracker.recordWhiff(event.entity)
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return

        // 1. Explorer chunk and expedition tracking
        ExplorerXpTracker.onPlayerTick(player)

        // 2. Scout threat radar scan (every 40 ticks = 2s)
        if (player.tickCount % 40 == 0) {
            ScoutRoleHandler.performThreatRadarScan(player)
        }

        // 3. Tank taunt pulse in combat (every 60 ticks = 3s)
        if (player.tickCount % 60 == 0) {
            TankRoleHandler.performTauntPulse(player)
        }

        // 4. Support team sustain pulse (every 80 ticks = 4s)
        if (player.tickCount % 80 == 0) {
            SupportRoleHandler.performSustainPulse(player)
        }
    }

    @SubscribeEvent
    fun onLevelTick(event: LevelTickEvent.Post) {
        val serverLevel = event.level as? ServerLevel ?: return

        // 1. Cooperative Ender Dragon encounter state machine
        CooperativeDragonEncounter.onEndLevelTick(serverLevel)

        // 2. Prune expired combat encounters periodically
        if (serverLevel.gameTime % 200 == 0L) {
            CombatXpTracker.cleanExpiredEncounters()
        }
    }

    @SubscribeEvent
    fun onItemUseFinish(event: LivingEntityUseItemEvent.Finish) {
        val player = event.entity as? ServerPlayer ?: return
        val stack = event.item

        // Support consumable efficiency: radiate beneficial potion effects to nearby allies
        if (stack.`is`(Items.POTION)) {
            val potionContents = stack.get(DataComponents.POTION_CONTENTS)
            potionContents?.allEffects?.forEach { effect ->
                SupportRoleHandler.onPotionConsumed(player, effect)
            }
        }
    }
}
