package dev.rmoseley.smpmod.economy

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.EnchantedBookItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentInstance
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enforces anti-AFK supply controls and active-play resource redistribution.
 *
 * Nerfs & Suppressions:
 * 1. Automated iron golem farms (passive / repetitive kills drop zero iron/poppies).
 * 2. Static mob-drop XP chutes & mob grinders (steep diminishing returns on repetitive chunk kills).
 * 3. Villager trading halls (repurposed into regional landmarks, safe havens, and objective hubs).
 *
 * Active Redistribution Channels:
 * 1. Deep-cave ore vein buffs (bonus yield & Gatherer XP at y < 0).
 * 2. Active dungeon loot tables (enriched with high-tier enchantment tomes like Mending, Unbreaking, Protection).
 */
object AntiAfkSupplyManager {

    // Track iron golem deaths per chunk coordinate: "dim:chunkX:chunkZ" -> timestamps
    private val golemDeathLog = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    // Track general hostile mob deaths per chunk: "dim:chunkX:chunkZ" -> timestamps
    private val chunkMobDeathLog = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    private fun cleanOldEntries(queue: ConcurrentLinkedQueue<Long>, maxAgeSeconds: Long, now: Long) {
        while (true) {
            val oldest = queue.peek() ?: break
            if (now - oldest > maxAgeSeconds) {
                queue.poll()
            } else {
                break
            }
        }
    }

    /**
     * Determines whether iron golem drops should be suppressed (automated iron farm nerf).
     * Suppresses drops if killed passively (fire, lava, fall, suffocation) or if chunk kill rate is high.
     */
    fun shouldSuppressIronGolemDrops(golem: IronGolem, damageSource: DamageSource): Boolean {
        val killer = damageSource.entity
        val level = golem.level()
        val chunkPos = ChunkPos(golem.blockPosition())
        val key = "${level.dimension().location()}:${chunkPos.x}:${chunkPos.z}"
        val now = System.currentTimeMillis() / 1000L

        // If killed by environmental or non-player means (e.g. lava blade, campfire, suffocator) -> suppress!
        if (killer !is Player) {
            return true
        }

        // Check chunk kill density within 3 minutes (180s)
        val queue = golemDeathLog.computeIfAbsent(key) { ConcurrentLinkedQueue() }
        cleanOldEntries(queue, 180L, now)
        queue.add(now)

        // If more than 2 iron golems have died in this chunk in 3 minutes -> automated farm detected!
        return queue.size > 2
    }

    /**
     * Calculates drop rate decay for hostile mob grinder kills within a chunk radius.
     * Repetitive kills in the same area or passive deaths suffer steep decay.
     */
    fun calculateGrinderDropDecay(mob: LivingEntity, damageSource: DamageSource): Double {
        val killer = damageSource.entity

        // If mob died from passive non-player damage (fall damage chute, campfire, trident killer) -> 95% suppression
        if (killer !is Player) {
            return 0.05
        }

        val level = mob.level()
        val centerChunk = ChunkPos(mob.blockPosition())
        val radius = SmpConfig.COMMON.grinderChunkDecayRadius.get()
        val now = System.currentTimeMillis() / 1000L
        val dim = level.dimension().location().toString()

        var totalKillsInRadius = 0

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val key = "$dim:${centerChunk.x + dx}:${centerChunk.z + dz}"
                val queue = chunkMobDeathLog[key] ?: continue
                cleanOldEntries(queue, 60L, now)
                totalKillsInRadius += queue.size
            }
        }

        // Record this kill in the center chunk
        val centerKey = "$dim:${centerChunk.x}:${centerChunk.z}"
        val queue = chunkMobDeathLog.computeIfAbsent(centerKey) { ConcurrentLinkedQueue() }
        queue.add(now)

        // Normal combat tempo allows up to 10 kills per minute across the radius.
        // Beyond that, drops decay steeply toward a minimum 5% floor.
        if (totalKillsInRadius > 10) {
            val excess = totalKillsInRadius - 10
            return (1.0 - (excess * 0.05)).coerceIn(0.05, 1.0)
        }

        return 1.0
    }

    /**
     * Intercepts villager interaction to deactivate automated trading halls.
     * Repurposes villages into regional objective hubs, safe rest stops, and lore landmarks.
     */
    fun onVillagerInteract(player: Player, villager: Villager, hand: InteractionHand): InteractionResult {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS

        // Send landmark notice instead of opening exploitative vanilla trading GUI
        if (!player.level().isClientSide) {
            player.sendSystemMessage(
                Component.literal("§e[Village Landmark] §7This village is a protected regional rest stop and objective hub. Villager trading halls are deactivated. High-tier tomes and resources are found in deep caves and active dungeons!")
            )
        }

        return InteractionResult.SUCCESS
    }

    /**
     * Deep-cave ore vein buff: when mining natural ores at y < 0, awards bonus yield
     * and extra Gatherer XP to incentivize active spelunking over passive farms.
     */
    fun onNaturalOreHarvest(player: Player, block: Block, pos: BlockPos): List<ItemStack> {
        if (pos.y >= 0) return emptyList()

        val blockKey = BuiltInRegistries.BLOCK.getKey(block).path
        val isOre = blockKey.contains("ore") || blockKey.contains("deepslate_")

        if (!isOre) return emptyList()

        val multiplier = SmpConfig.COMMON.deepCaveVeinDensityMultiplier.get()
        val bonusDrops = mutableListOf<ItemStack>()

        // 50% chance for an extra primary ore drop
        if (player.level().random.nextDouble() < 0.50 * (multiplier - 1.0).coerceAtLeast(0.0)) {
            val itemDrop = block.asItem()
            if (itemDrop != Items.AIR) {
                bonusDrops.add(ItemStack(itemDrop, 1))
            }
        }

        // Award deep-cave Gatherer XP bonus
        PlaystyleXpTracker.awardGathererXp(player, 5.0 * multiplier, isDeepCave = true)
        return bonusDrops
    }

    /**
     * Enriches active dungeon loot containers with high-tier enchantment tomes
     * (Mending, Unbreaking III, Protection IV, etc.) and rare catalysts.
     */
    fun enrichDungeonLoot(
        level: Level,
        lootTableId: ResourceLocation,
        generatedLoot: MutableList<ItemStack>
    ) {
        val path = lootTableId.path
        val isDungeonTable = path.contains("simple_dungeon") ||
                path.contains("trial_chambers") ||
                path.contains("ancient_city") ||
                path.contains("abandoned_mineshaft") ||
                path.contains("stronghold") ||
                path.contains("bastion") ||
                path.contains("nether_bridge")

        if (!isDungeonTable) return

        val multiplier = SmpConfig.COMMON.dungeonTomeDropMultiplier.get()
        val random = level.random

        // Chance to inject high-tier enchantment tomes based on multiplier
        if (random.nextDouble() < 0.40 * multiplier.coerceIn(1.0, 3.0)) {
            val registry = level.registryAccess().registry(Registries.ENCHANTMENT).orElse(null)
            if (registry != null) {
                val candidateKeys = listOf(
                    Enchantments.MENDING to 1,
                    Enchantments.UNBREAKING to 3,
                    Enchantments.PROTECTION to 4,
                    Enchantments.FORTUNE to 3,
                    Enchantments.EFFICIENCY to 5,
                    Enchantments.SHARPNESS to 5
                )

                val selected = candidateKeys[random.nextInt(candidateKeys.size)]
                val holder = registry.getHolder(selected.first).orElse(null)

                if (holder != null) {
                    val enchantedBook = EnchantedBookItem.createForEnchantment(
                        EnchantmentInstance(holder, selected.second)
                    )
                    generatedLoot.add(enchantedBook)
                }
            }
        }
    }
}
