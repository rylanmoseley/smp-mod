package dev.rmoseley.smpmod.world

import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import dev.rmoseley.smpmod.roles.RoleCalculator
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.StructureTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.max

/**
 * Manages the Living Villager Overhaul:
 * 1. Spawning Restriction: Villagers only spawn in natural villages (or via Explorer relocation).
 * 2. Reproduction Suppression: Villagers do not reproduce; baby villagers and breeding AI are blocked.
 * 3. Explorer Relocation: Seasoned Explorers (Scouts or 100+ Explorer XP) sneak-interact to encapsulate
 *    villagers into tradable spawn eggs with trade offers and levels intact.
 * 4. Mobility Restocking & Fair Pricing: Villagers require an open 3x3 roaming footprint (>=4 walkable blocks).
 *    Confining them in 1x1 booths halts trade restocking and imposes a +10 emerald distress markup.
 * 5. Permanent Villager Slayer Penalty: Slaying villagers permanently curses the player with bad luck
 *    and brands them realm-wide, imposing a permanent +5 to +30 emerald surcharge on all trades.
 */
object VillagerLivingManager {

    private const val CRAMPED_EMERALD_MARKUP = 10
    private const val BASE_SLAYER_MARKUP_PER_KILL = 5
    private const val MAX_SLAYER_MARKUP = 30
    private const val MIN_WALKABLE_BLOCKS_FOR_MOBILITY = 4

    /**
     * Enforces natural village spawning restrictions.
     * Only allows adult villagers inside natural village structures or those spawned from an Encapsulated item.
     */
    fun shouldCancelSpawn(villager: Villager): Boolean {
        // Relocated villagers deployed from Explorer eggs are always permitted
        if (villager.persistentData.getBoolean("smpmod:relocated_villager")) {
            return false
        }

        // Reproduction suppression: baby villagers are strictly forbidden
        if (villager.isBaby || villager.age < 0) {
            return true
        }

        val level = villager.level() as? ServerLevel ?: return false
        val pos = villager.blockPosition()

        // Check if inside natural village structure or known sanctuary
        val inStructure = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid
        val inSanctuary = RepurposedVillageManager.isInsideVillageSanctuary(level, pos)

        return !inStructure && !inSanctuary
    }

    /**
     * Prevents any villager breeding or baby entity generation.
     */
    fun shouldCancelBabySpawn(parentA: Mob?, parentB: Mob?, child: LivingEntity?): Boolean {
        return parentA is Villager || parentB is Villager || child is Villager
    }

    /**
     * Evaluates a villager's mobility based on the surrounding 3x3 footprint at feet level.
     * Cramped if there are fewer than 4 accessible walkable blocks (e.g. 1x1 trading cells or boat/cart captivity).
     */
    fun isVillagerCramped(villager: Villager): Boolean {
        if (villager.isPassenger) return true // Boat or minecart lock

        val level = villager.level()
        val pos = villager.blockPosition()
        var walkableCount = 0

        for (dx in -1..1) {
            for (dz in -1..1) {
                val feetPos = pos.offset(dx, 0, dz)
                val headPos = feetPos.above()
                val floorPos = feetPos.below()

                val feetPassable = level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty
                val headPassable = level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty
                val floorSturdy = level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP)

                if (feetPassable && headPassable && floorSturdy) {
                    walkableCount++
                }
            }
        }

        return walkableCount < MIN_WALKABLE_BLOCKS_FOR_MOBILITY
    }

    /**
     * Periodic tick update for villagers:
     * - Suppresses breeding food and mating memories.
     * - Enforces restock freeze and visual distress particles for cramped merchants.
     */
    fun onVillagerTick(villager: Villager) {
        val level = villager.level() as? ServerLevel ?: return

        // 1. Suppress baby villagers and breeding behavior
        if (villager.isBaby) {
            villager.discard()
            return
        }

        if (villager.tickCount % 40 == 0) {
            // Strip breeding food items from internal inventory
            val inv = villager.inventory
            for (i in 0 until inv.containerSize) {
                val item = inv.getItem(i)
                if (item.`is`(Items.BREAD) || item.`is`(Items.CARROT) || item.`is`(Items.POTATO) ||
                    item.`is`(Items.BEETROOT) || item.`is`(Items.WHEAT) || item.`is`(Items.WHEAT_SEEDS)
                ) {
                    inv.setItem(i, ItemStack.EMPTY)
                }
            }
            // Erase breeding target memory
            villager.brain.eraseMemory(MemoryModuleType.BREED_TARGET)
        }

        // 2. Mobility restock regulation
        val cramped = isVillagerCramped(villager)
        val wasCramped = villager.persistentData.getBoolean("smpmod:cramped")

        if (cramped) {
            villager.persistentData.putBoolean("smpmod:cramped", true)

            // Freeze exhausted trades: prevent resetting uses while confined
            val exhausted = villager.offers.mapIndexedNotNull { index, offer ->
                if (offer.isOutOfStock || offer.uses >= offer.maxUses) index else null
            }
            if (exhausted.isNotEmpty()) {
                villager.persistentData.putIntArray("smpmod:exhausted_indices", exhausted.toIntArray())
            }

            // Lock any tracked exhausted offers back to out-of-stock if vanilla attempted to restock
            val lockedIndices = villager.persistentData.getIntArray("smpmod:exhausted_indices")
            for (idx in lockedIndices) {
                if (idx in 0 until villager.offers.size) {
                    val offer = villager.offers[idx]
                    offer.setToOutOfStock()
                }
            }

            // Visual distress indication
            if (villager.tickCount % 60 == 0) {
                level.sendParticles(
                    ParticleTypes.SMOKE,
                    villager.x, villager.y + 1.2, villager.z,
                    2, 0.1, 0.1, 0.1, 0.01
                )
            }
        } else {
            if (wasCramped) {
                // Restored mobility: clear distress state and re-enable natural restocks
                villager.persistentData.putBoolean("smpmod:cramped", false)
                villager.persistentData.remove("smpmod:exhausted_indices")

                level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    villager.x, villager.y + 1.2, villager.z,
                    5, 0.2, 0.2, 0.2, 0.02
                )
            }
        }
    }

    /**
     * Intercepts player interaction with villagers:
     * - Sneak-interact: Allows Explorers to encapsulate villagers into tradable spawn eggs.
     * - Regular interact: Modulates trade prices based on mobility status and player slayer record.
     */
    fun onVillagerInteract(player: ServerPlayer, villager: Villager, hand: InteractionHand): InteractionResult {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS

        // =====================================================================
        // 1. Explorer Acquisition (Sneak + Right-Click)
        // =====================================================================
        if (player.isShiftKeyDown) {
            val profile = RoleCalculator.calculateProfile(player)
            val isExplorer = profile.primaryRole == RoleCalculator.Role.SCOUT ||
                profile.secondaryRole == RoleCalculator.Role.SCOUT ||
                PlaystyleXpTracker.getExplorerXp(player) >= 100.0

            if (!isExplorer) {
                player.sendSystemMessage(
                    Component.literal("§c[Explorer Specialization Required] §7Only seasoned Explorers (Scout role or 100+ Explorer XP) have the field equipment to encapsulate and relocate villagers.")
                )
                return InteractionResult.SUCCESS
            }

            val eggStack = EncapsulatedVillagerItem.createFromVillager(villager, player)
            val added = player.inventory.add(eggStack)
            if (!added) {
                player.drop(eggStack, false)
            }

            val level = player.serverLevel()
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f)
            level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0f, 0.8f)
            level.sendParticles(
                ParticleTypes.PORTAL,
                villager.x, villager.y + 1.0, villager.z,
                25, 0.3, 0.5, 0.3, 0.1
            )

            PlaystyleXpTracker.awardExplorerXp(player, 25.0)
            villager.discard()

            player.sendSystemMessage(
                Component.literal("§6[Villager Acquired] §fEncapsulated ${villager.villagerData.profession.name()} villager into a tradable specimen! You can deploy or sell them to other players.")
            )
            return InteractionResult.SUCCESS
        }

        // =====================================================================
        // 2. Mobility Pricing Regulation & Restock Notification
        // =====================================================================
        val cramped = isVillagerCramped(villager)
        val milestones = player.getData(ModAttachmentTypes.PLAYER_MILESTONES)
        val slayerMarkup = (milestones.villagerKills * BASE_SLAYER_MARKUP_PER_KILL).coerceAtMost(MAX_SLAYER_MARKUP)

        for (offer in villager.offers) {
            var additionalDiff = 0

            // Cramped captivity surcharge (+10 emeralds)
            if (cramped) {
                additionalDiff += CRAMPED_EMERALD_MARKUP
            }

            // Permanent villager slayer surcharge (+5 to +30 emeralds)
            if (slayerMarkup > 0) {
                additionalDiff += slayerMarkup
            }

            offer.specialPriceDiff = additionalDiff
        }

        if (cramped) {
            player.sendSystemMessage(
                Component.literal("§c[Distressed Merchant] §7This villager is cramped in a tight stall (<4 walkable blocks)! Trade restocking is halted and a +$CRAMPED_EMERALD_MARKUP emerald captivity markup applies.")
            )
        }

        if (slayerMarkup > 0) {
            player.sendSystemMessage(
                Component.literal("§4§l[Villager Slayer Surcharge] §cMerchants realm-wide charge you +$slayerMarkup emeralds per trade for slaying innocent villagers (Kills: ${milestones.villagerKills})!")
            )
        }

        return InteractionResult.PASS
    }

    /**
     * Triggered when a player kills an innocent villager.
     * Records the kill permanently, curses the player with Bad Omen & Unluck, and alerts the realm.
     */
    fun onVillagerKilled(killer: ServerPlayer, villager: Villager) {
        val milestones = killer.getData(ModAttachmentTypes.PLAYER_MILESTONES)
        val updatedKills = milestones.villagerKills + 1
        killer.setData(ModAttachmentTypes.PLAYER_MILESTONES, milestones.copy(villagerKills = updatedKills))

        // Apply severe debuffs (Unluck and Bad Omen)
        killer.addEffect(MobEffectInstance(MobEffects.UNLUCK, 72000, (updatedKills - 1).coerceIn(0, 2), false, true, true))
        killer.addEffect(MobEffectInstance(MobEffects.BAD_OMEN, 72000, 0, false, true, true))

        val level = killer.serverLevel()
        level.playSound(null, killer.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8f, 1.8f)

        val totalMarkup = (updatedKills * BASE_SLAYER_MARKUP_PER_KILL).coerceAtMost(MAX_SLAYER_MARKUP)
        killer.sendSystemMessage(
            Component.literal("§4§l[CRIME AGAINST CIVILIZATION] §cYou murdered an innocent villager! Villager kills: $updatedKills.")
        )
        killer.sendSystemMessage(
            Component.literal("§cYou are branded a Villager Slayer across the entire realm. All villager trades permanently cost +$totalMarkup emeralds, and you are cursed with Unluck!")
        )
    }

    /**
     * Periodic check on players to ensure Villager Slayers remain cursed with Unluck.
     */
    fun onPlayerTick(player: ServerPlayer) {
        if (player.tickCount % 120 == 0) {
            val kills = player.getData(ModAttachmentTypes.PLAYER_MILESTONES).villagerKills
            if (kills > 0) {
                val amplifier = (kills - 1).coerceIn(0, 2)
                if (!player.hasEffect(MobEffects.UNLUCK)) {
                    player.addEffect(MobEffectInstance(MobEffects.UNLUCK, 600, amplifier, false, false, true))
                }
            }
        }
    }
}
