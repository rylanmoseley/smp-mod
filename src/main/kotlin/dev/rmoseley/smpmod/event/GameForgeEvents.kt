package dev.rmoseley.smpmod.event

import dev.rmoseley.smpmod.claims.BuilderFlightController
import dev.rmoseley.smpmod.claims.ClaimManager
import dev.rmoseley.smpmod.economy.AntiAfkSupplyManager
import dev.rmoseley.smpmod.economy.BulkStoreManager
import dev.rmoseley.smpmod.economy.PlacedBlockTracker
import dev.rmoseley.smpmod.economy.ProgressionFaucetManager
import dev.rmoseley.smpmod.economy.ShopUnlockManager
import dev.rmoseley.smpmod.economy.StarterMilestone
import dev.rmoseley.smpmod.recovery.BuybackLedgerManager
import dev.rmoseley.smpmod.recovery.BuybackSavedData
import dev.rmoseley.smpmod.recovery.ItemProvenance
import dev.rmoseley.smpmod.recovery.RecoveryCommands
import dev.rmoseley.smpmod.recovery.RecoveryItemClassifier
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import dev.rmoseley.smpmod.roles.PlaystyleXpTracker
import dev.rmoseley.smpmod.world.BiomeMobilityManager
import dev.rmoseley.smpmod.world.ElytraFlightManager
import dev.rmoseley.smpmod.world.LightMobAIHandler
import dev.rmoseley.smpmod.world.LightSnuffManager
import dev.rmoseley.smpmod.world.NetherHazardHandler
import dev.rmoseley.smpmod.world.RepurposedVillageManager
import dev.rmoseley.smpmod.world.SpatialLightAI
import dev.rmoseley.smpmod.world.WardedLightManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.EntityTeleportEvent
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent
import net.neoforged.neoforge.event.entity.player.AdvancementEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

/**
 * Global game event listeners enforcing core systems, closed dependencies,
 * and World & AI subsystem integration.
 */
object GameForgeEvents {

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val level = player.level()

        // 1. Nether Roof Construction Lockout
        if (NetherHazardHandler.shouldCancelRoofBuilding(level, event.pos, player)) {
            event.isCanceled = true
            return
        }

        // 2. Track placed block coordinate for anti-laundering guardrail
        PlacedBlockTracker.markPlaced(level, event.pos)

        val chunkPos = ChunkPos(event.pos)

        // 3. Sustain builder flight inside claims, track cluster leveling, and award recurring construction dividends
        if (ClaimManager.isChunkClaimedBy(level, chunkPos, player)) {
            val xpScore = ClaimManager.recordBlockPlacement(player, level, chunkPos, event.placedBlock.block)
            BuilderFlightController.recordBlockPlacement(player, event.pos)
            PlaystyleXpTracker.awardBuilderXpDirect(player, xpScore)
            ProgressionFaucetManager.onConstructionBlockPlaced(player, xpScore, event.placedBlock.block)
        }

        // 4. Register village sanctuary if placing landmark bell
        if (event.placedBlock.`is`(Blocks.BELL)) {
            RepurposedVillageManager.registerVillageCenter(level, event.pos)
        }

        // 5. Track highway and railway engineering
        val placedState = event.placedBlock
        if (dev.rmoseley.smpmod.world.BiomeMobilityManager.isRoadBlock(placedState) || placedState.block is net.minecraft.world.level.block.BaseRailBlock) {
            (player as? ServerPlayer)?.let {
                dev.rmoseley.smpmod.advancements.SettlementAdvancements.checkHighwayBlockPlaced(it)
            }
        }
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val player = event.player
        val block = event.state.block
        val level = player.level()
        val chunkPos = ChunkPos(event.pos)

        // Tool Statistics: track blocks mined on active tool
        val toolStack = player.mainHandItem
        dev.rmoseley.smpmod.server.ToolStatsManager.onBlockBroken(player, toolStack)

        // Inside claims: decrement cluster net blocks and Builder XP to prevent exploit
        if (ClaimManager.isChunkClaimed(level, chunkPos)) {
            val deductedXp = ClaimManager.recordBlockBreak(player, level, chunkPos, block)
            PlaystyleXpTracker.deductBuilderXpDirect(player, deductedXp)
            PlacedBlockTracker.unmarkPlaced(level, event.pos)
        } else {
            // Anti-laundering guardrail: breaking player-placed, shop-bought, or Silk-Touched blocks never counts
            val isNaturalHarvest = PlacedBlockTracker.isNaturalHarvest(player, level, event.pos)
            if (isNaturalHarvest) {
                ShopUnlockManager.recordNaturalHarvest(player, block)
                ProgressionFaucetManager.onNaturalBlockHarvest(player)

                val isDeep = event.pos.y < 0
                if (isDeep) {
                    val bonusDrops = AntiAfkSupplyManager.onNaturalOreHarvest(player, block, event.pos)
                    for (drop in bonusDrops) {
                        Block.popResource(level, event.pos, drop)
                    }
                    (player as? ServerPlayer)?.let { serverPlayer ->
                        dev.rmoseley.smpmod.advancements.EconomyAdvancements.recordDeepOreHarvest(serverPlayer)
                    }
                }
                (player as? ServerPlayer)?.let { serverPlayer ->
                    dev.rmoseley.smpmod.roles.GathererXpTracker.onBlockHarvested(
                        serverPlayer,
                        event.pos,
                        event.state,
                        isPlayerPlaced = false
                    )
                    dev.rmoseley.smpmod.economy.BountyContractManager.recordProgress(
                        serverPlayer,
                        dev.rmoseley.smpmod.economy.BountyType.GATHERER_HARVEST,
                        1
                    )
                }
            }
        }

        // Track dungeon spawner destruction for combat milestones
        if (block == Blocks.SPAWNER) {
            (player as? ServerPlayer)?.let {
                dev.rmoseley.smpmod.advancements.CombatRaidAdvancements.onDungeonSpawnerDestroyed(it)
            }
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        val entity = event.entity
        val killer = event.source.entity as? ServerPlayer

        // Track weapon/tool statistics
        if (killer != null) {
            val weaponStack = killer.mainHandItem
            dev.rmoseley.smpmod.server.ToolStatsManager.onEntityKilled(killer, weaponStack, entity)
        }

        // Villager homicide penalty & curse branding
        if (entity is Villager && killer != null) {
            dev.rmoseley.smpmod.world.VillagerLivingManager.onVillagerKilled(killer, entity)
        }

        val player = entity as? ServerPlayer ?: return

        var buybackCount = 0
        var bulkStoreCount = 0

        // Intercept inventory items to route between UUID Buyback and Bulk Store
        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue

            val destination = RecoveryItemClassifier.classify(stack)
            when (destination) {
                RecoveryItemClassifier.RoutingDestination.BULK_STORE -> {
                    BulkStoreManager.deposit(stack.copy())
                    bulkStoreCount += stack.count
                    player.inventory.setItem(i, ItemStack.EMPTY)
                }
                RecoveryItemClassifier.RoutingDestination.BUYBACK_LEDGER -> {
                    BuybackLedgerManager.recordDeathDrop(player, stack.copy())
                    buybackCount++
                    player.inventory.setItem(i, ItemStack.EMPTY)
                }
                RecoveryItemClassifier.RoutingDestination.IGNORED -> {}
            }
        }

        if (buybackCount > 0 || bulkStoreCount > 0) {
            player.sendSystemMessage(
                Component.literal("§6[Recovery] §eDeath Drops Secured: §b$buybackCount gear item(s) §ein Buyback Ledger, §a$bulkStoreCount §emundane stackable(s) routed to Bulk Store.")
            )
            player.sendSystemMessage(
                Component.literal("§7Use §6/buyback list §7inside claimed settlements to safely reclaim your gear!")
            )
        }
    }

    @SubscribeEvent
    fun onItemCrafted(event: PlayerEvent.ItemCraftedEvent) {
        val player = event.entity
        val crafted = event.crafting
        if (RecoveryItemClassifier.isBuybackCandidate(crafted)) {
            ItemProvenance.initializeItemOrigin(crafted, player, isCrafted = true)
            if (player is ServerPlayer) {
                dev.rmoseley.smpmod.advancements.ArtificingRecoveryAdvancements.onItemCrafted(player, crafted)
            }
        }
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        RecoveryCommands.register(event.dispatcher)
        dev.rmoseley.smpmod.claims.TownCommands.register(event.dispatcher)
        dev.rmoseley.smpmod.economy.BountyCommands.register(event.dispatcher)
        dev.rmoseley.smpmod.economy.WealthCommands.register(event.dispatcher)
        dev.rmoseley.smpmod.roles.RoleCommands.register(event.dispatcher)
        dev.rmoseley.smpmod.server.LeaderboardCommands.register(event.dispatcher)
    }

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Pre) {
        val target = event.entity
        val source = event.source.entity

        // Builder flight revocation on combat engagement (roles handled in RoleCombatEvents)
        if (target is ServerPlayer) {
            BuilderFlightController.onCombatEngaged(target)
        }
        if (source is ServerPlayer) {
            BuilderFlightController.onCombatEngaged(source)
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return

        // 1. Nether hazards & anti-solo sprint
        NetherHazardHandler.checkNetherPlayer(player)

        // 2. Builder flight controller
        BuilderFlightController.onTickPlayer(player)

        // 3. Pooled territorial health regeneration in contiguous claims
        ClaimManager.applyTerritorialRegen(player)

        // 4. Overworld Biome Mobility & road progression
        BiomeMobilityManager.onPlayerTick(player)

        // 5. Elytra Glider Aerodynamics & Nether lockout
        ElytraFlightManager.onPlayerTick(player)

        // 6. Claim-Safe Light Snuff shroud aura
        LightSnuffManager.onPlayerTick(player)

        // 7. Scout Role Threat Detection Radar
        SpatialLightAI.performScoutThreatDetection(player)

        // 8. Repurposed Village Sanctuary Rest Haven
        RepurposedVillageManager.onPlayerTick(player)

        // 9. Progression Faucets & Non-AFK Active Playtime tracking
        ProgressionFaucetManager.onPlayerTick(player)

        // 10. Caravan & Highway Transit
        dev.rmoseley.smpmod.world.CaravanTransitManager.onPlayerTick(player)

        // 11. Complex Dungeon Delve & Structure Discovery
        dev.rmoseley.smpmod.world.StructureDelveManager.onPlayerTick(player)

        // 12. Cavern Depth Milestone (Abyss Descender below Y: 0)
        dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onCavernDepthCheck(player)

        // 13. Subterranean Voyage & Long-Distance Exploration
        if (player.tickCount % 40 == 0) {
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onSubterraneanVoyage(player)
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onPlayerTravelCheck(player)
        }

        // 14. Villager Slayer Curse Maintenance
        dev.rmoseley.smpmod.world.VillagerLivingManager.onPlayerTick(player)

        // 15. Contiguous Settlement Nonresident Tourism & Prosperity Buffs
        val currentChunkPos = ChunkPos(player.blockPosition())
        val settlementCluster = ClaimManager.getContiguousCluster(player.level(), currentChunkPos)
        if (settlementCluster != null) {
            if (!settlementCluster.isMember(player.uuid)) {
                dev.rmoseley.smpmod.claims.TownTreasuryManager.recordVisitorPresence(settlementCluster, player)
            } else {
                dev.rmoseley.smpmod.claims.TownTreasuryManager.applyResidentTourismBuff(player, settlementCluster)
            }
        }

        // 16. Multiplayer Dynamic Sleep Check
        val wasSleeping = player.persistentData.getBoolean("smpmod:is_sleeping")
        val isSleeping = player.isSleeping
        if (isSleeping && !wasSleeping) {
            dev.rmoseley.smpmod.server.MultiplayerSleepManager.onPlayerEnterBed(player)
        } else if (!isSleeping && wasSleeping) {
            dev.rmoseley.smpmod.server.MultiplayerSleepManager.onPlayerLeaveBed(player)
        }

        // 17. Stat Leaderboards & Gamerule sync (every 5 seconds)
        if (player.tickCount % 100 == 0) {
            dev.rmoseley.smpmod.server.LeaderboardManager.updatePlayer(player)
            dev.rmoseley.smpmod.server.MultiplayerSleepManager.syncGamerule(player.server)
        }
    }

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val player = event.entity as? ServerPlayer ?: return

        // Cancel mid-air firework rocket boosting to enforce momentum glider flight
        if (event.itemStack.`is`(Items.FIREWORK_ROCKET)) {
            if (ElytraFlightManager.shouldCancelRocketBoost(player)) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity as? ServerPlayer ?: return
        val pos = event.pos
        val state = player.level().getBlockState(pos)

        // 1. Regional Village Objective Hub interaction via Village Bell & Acoustic Town Alarm
        if (state.`is`(Blocks.BELL)) {
            RepurposedVillageManager.registerVillageCenter(player.level(), pos)
            dev.rmoseley.smpmod.world.VoiceAcousticHooks.triggerTownAlarm(player.serverLevel(), pos, player)
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onSanctuaryBellRung(player)
            if (RepurposedVillageManager.handleObjectiveInteraction(player, pos)) {
                event.isCanceled = true
                return
            }
        }

        // Subterranean Relic / Vault Interaction
        if (state.`is`(Blocks.SUSPICIOUS_SAND) || state.`is`(Blocks.SUSPICIOUS_GRAVEL) || state.`is`(Blocks.VAULT)) {
            dev.rmoseley.smpmod.advancements.WorldExplorationAdvancements.onAncientRelicUncovered(player)
        }

        // 2. Support Role: Empowering Warded Light Flares (sneaking + right click light source)
        if (player.isShiftKeyDown && LightMobAIHandler.isFragileLightSource(state)) {
            if (WardedLightManager.empowerLight(player, pos)) {
                event.isCanceled = true
                return
            }
        }
    }

    @SubscribeEvent
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity

        // 1. Spatial Light Avoidance & Dark-Zone Escalation for hostile mobs
        if (entity is PathfinderMob && entity is Monster) {
            SpatialLightAI.attachLightAvoidanceAI(entity)
            SpatialLightAI.escalateDarkZoneMob(entity)
        }

        // 2. Nether mob barrier-breaching transit aggro
        if (entity is Mob && entity.level().dimension() == net.minecraft.world.level.Level.NETHER) {
            NetherHazardHandler.handleNetherMobAggro(entity)
        }

        // 3. Living Villager spawn filtering & exploit regulation
        if (entity is Villager) {
            if (dev.rmoseley.smpmod.world.VillagerLivingManager.shouldCancelSpawn(entity)) {
                event.isCanceled = true
                return
            }
            RepurposedVillageManager.regulateVillager(entity)
        }
    }

    @SubscribeEvent
    fun onFinalizeSpawn(event: FinalizeSpawnEvent) {
        val entity = event.entity

        // 1. Village Sanctuary spawn suppression: hostile mobs cannot spawn in safe villages
        if (entity is Monster && RepurposedVillageManager.shouldCancelMobSpawn(entity)) {
            event.isCanceled = true
            return
        }

        // 2. Villager spawn regulation: only spawn in natural villages
        if (entity is Villager && dev.rmoseley.smpmod.world.VillagerLivingManager.shouldCancelSpawn(entity)) {
            event.isCanceled = true
            return
        }

        // 3. Roll destroys_light_on_aggro trait for eligible hostile mobs
        if (entity is Mob && entity is Monster) {
            LightMobAIHandler.evaluateSpawnTrait(entity)
        }
    }

    @SubscribeEvent
    fun onBabyEntitySpawn(event: net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent) {
        if (dev.rmoseley.smpmod.world.VillagerLivingManager.shouldCancelBabySpawn(event.parentA, event.parentB, event.child)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEntityTick(event: net.neoforged.neoforge.event.tick.EntityTickEvent.Post) {
        val villager = event.entity as? Villager ?: return
        if (!villager.level().isClientSide) {
            dev.rmoseley.smpmod.world.VillagerLivingManager.onVillagerTick(villager)
        }
    }

    @SubscribeEvent
    fun onEntityTeleport(event: EntityTeleportEvent) {
        val entity = event.entity
        val player = entity as? ServerPlayer

        // Prevent ender pearl or chorus teleportation onto the Nether roof
        if (NetherHazardHandler.shouldCancelRoofTeleport(entity.level(), event.target, player)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLivingDrops(event: LivingDropsEvent) {
        val entity = event.entity

        // 1. Anti-AFK: Suppress iron golem drops from automated iron farms
        if (entity is IronGolem) {
            if (AntiAfkSupplyManager.shouldSuppressIronGolemDrops(entity, event.source)) {
                event.drops.clear()
                return
            }
        }

        // 2. Anti-AFK: Mob grinder drop decay for static mob chutes
        if (entity is Monster) {
            val decayMultiplier = AntiAfkSupplyManager.calculateGrinderDropDecay(entity, event.source)
            if (decayMultiplier <= 0.05) {
                event.drops.clear()
            } else if (decayMultiplier < 1.0) {
                val random = entity.level().random
                val iterator = event.drops.iterator()
                while (iterator.hasNext()) {
                    iterator.next()
                    if (random.nextDouble() > decayMultiplier) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onLivingExperienceDrop(event: LivingExperienceDropEvent) {
        val entity = event.entity
        val player = event.attackingPlayer

        // Passive kills in automated mob grinder chutes yield zero XP
        if (player == null) {
            event.droppedExperience = 0
            return
        }

        if (entity is Monster) {
            val decay = AntiAfkSupplyManager.calculateGrinderDropDecay(entity, entity.damageSources().playerAttack(player))
            if (decay <= 0.05) {
                event.droppedExperience = 0
            } else if (decay < 1.0) {
                event.droppedExperience = (event.droppedExperience * decay).toInt()
            }
        }
    }

    @SubscribeEvent
    fun onVillagerInteract(event: PlayerInteractEvent.EntityInteract) {
        val target = event.target as? Villager ?: return
        val player = event.entity as? ServerPlayer ?: return
        val res = dev.rmoseley.smpmod.world.VillagerLivingManager.onVillagerInteract(player, target, event.hand)
        if (res == InteractionResult.SUCCESS) {
            event.isCanceled = true
            event.cancellationResult = InteractionResult.SUCCESS
        }
    }

    @SubscribeEvent
    fun onAnvilUpdate(event: net.neoforged.neoforge.event.AnvilUpdateEvent) {
        val left = event.left
        if (left.get(dev.rmoseley.smpmod.registry.ModDataComponents.MASTERWORK_AFFIX.get()) != null) {
            val player = event.player as? ServerPlayer ?: return
            dev.rmoseley.smpmod.advancements.ArtificingRecoveryAdvancements.awardRelicRestorer(player)
        }
    }

    @SubscribeEvent
    fun onAdvancementEarn(event: AdvancementEvent.AdvancementEarnEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val advId = event.advancement.id()
        dev.rmoseley.smpmod.advancements.AdvancementRewardManager.onAdvancementEarned(player, advId)

        val advPath = advId.path
        val milestone = StarterMilestone.byId(advPath)
        if (milestone != null) {
            ProgressionFaucetManager.awardStarterMilestone(player, milestone)
        }
    }

    @SubscribeEvent
    fun onItemTooltip(event: net.neoforged.neoforge.event.entity.player.ItemTooltipEvent) {
        dev.rmoseley.smpmod.server.ToolStatsManager.appendTooltip(event.itemStack, event.toolTip)
    }

    @SubscribeEvent
    fun onItemFished(event: net.neoforged.neoforge.event.entity.player.ItemFishedEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val mainRod = player.mainHandItem
        if (mainRod.item is net.minecraft.world.item.FishingRodItem) {
            dev.rmoseley.smpmod.server.ToolStatsManager.onFishCaught(player, mainRod)
        } else {
            val offRod = player.offhandItem
            if (offRod.item is net.minecraft.world.item.FishingRodItem) {
                dev.rmoseley.smpmod.server.ToolStatsManager.onFishCaught(player, offRod)
            }
        }
    }

    @SubscribeEvent
    fun onSleepFinished(event: net.neoforged.neoforge.event.level.SleepFinishedTimeEvent) {
        val serverLevel = event.level as? ServerLevel ?: return
        dev.rmoseley.smpmod.server.MultiplayerSleepManager.onSleepFinished(serverLevel)
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        // Initialize and deserialize persistent Buyback Ledger SavedData
        BuybackSavedData.init(event.server.overworld())
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        // Ensure final dirty state is marked for disk flush on shutdown
        BuybackSavedData.markDirty()
    }

    @SubscribeEvent
    fun onLevelSave(event: LevelEvent.Save) {
        val serverLevel = event.level as? ServerLevel ?: return
        if (serverLevel.dimension() == Level.OVERWORLD) {
            BuybackSavedData.markDirty()
        }
    }
}
