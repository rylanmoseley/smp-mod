package dev.rmoseley.smpmod.registry

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.rmoseley.smpmod.SmpMod
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Data structures attached to players and level chunks for persistent state.
 */
object ModAttachmentTypes {
    val REGISTRY: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmpMod.MOD_ID)

    // 1. Non-material Economy balance
    val PLAYER_ECONOMY: DeferredHolder<AttachmentType<*>, AttachmentType<Long>> =
        REGISTRY.register("player_economy", Supplier {
            AttachmentType.builder(Supplier { 0L })
                .serialize(Codec.LONG)
                .copyOnDeath()
                .build()
        })

    // 2. Playstyle XP Data Structure
    data class PlaystyleXpData(
        val combatXp: Double = 0.0,
        val builderXp: Double = 0.0,
        val gathererXp: Double = 0.0,
        val explorerXp: Double = 0.0
    ) {
        companion object {
            val CODEC: Codec<PlaystyleXpData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.DOUBLE.optionalFieldOf("combatXp", 0.0).forGetter(PlaystyleXpData::combatXp),
                    Codec.DOUBLE.optionalFieldOf("builderXp", 0.0).forGetter(PlaystyleXpData::builderXp),
                    Codec.DOUBLE.optionalFieldOf("gathererXp", 0.0).forGetter(PlaystyleXpData::gathererXp),
                    Codec.DOUBLE.optionalFieldOf("explorerXp", 0.0).forGetter(PlaystyleXpData::explorerXp)
                ).apply(instance, ::PlaystyleXpData)
            }
        }
    }

    val PLAYER_PLAYSTYLE_XP: DeferredHolder<AttachmentType<*>, AttachmentType<PlaystyleXpData>> =
        REGISTRY.register("player_playstyle_xp", Supplier {
            AttachmentType.builder(Supplier { PlaystyleXpData() })
                .serialize(PlaystyleXpData.CODEC)
                .copyOnDeath()
                .build()
        })

    // 3. Natural Harvest Unlocks (Map of Resource Location String to harvest count)
    data class NaturalHarvestData(
        val counts: Map<String, Int> = emptyMap()
    ) {
        companion object {
            val CODEC: Codec<NaturalHarvestData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                        .optionalFieldOf("counts", emptyMap())
                        .forGetter(NaturalHarvestData::counts)
                ).apply(instance, ::NaturalHarvestData)
            }
        }
    }

    val PLAYER_NATURAL_HARVEST: DeferredHolder<AttachmentType<*>, AttachmentType<NaturalHarvestData>> =
        REGISTRY.register("player_natural_harvest", Supplier {
            AttachmentType.builder(Supplier { NaturalHarvestData() })
                .serialize(NaturalHarvestData.CODEC)
                .copyOnDeath()
                .build()
        })

    // 4. Player Explorer Progression (discovered unique chunks count)
    val PLAYER_DISCOVERED_CHUNKS: DeferredHolder<AttachmentType<*>, AttachmentType<Int>> =
        REGISTRY.register("player_discovered_chunks", Supplier {
            AttachmentType.builder(Supplier { 0 })
                .serialize(Codec.INT)
                .copyOnDeath()
                .build()
        })

    // 5. Player Biome Exploration Progression (unique chunks per biome & visited coordinates)
    data class PlayerBiomeExplorationData(
        val discoveredChunkCoords: List<Long> = emptyList(),
        val biomeChunkCounts: Map<String, Int> = emptyMap()
    ) {
        companion object {
            val CODEC: Codec<PlayerBiomeExplorationData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.list(Codec.LONG).optionalFieldOf("discoveredChunkCoords", emptyList())
                        .forGetter(PlayerBiomeExplorationData::discoveredChunkCoords),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                        .optionalFieldOf("biomeChunkCounts", emptyMap())
                        .forGetter(PlayerBiomeExplorationData::biomeChunkCounts)
                ).apply(instance, ::PlayerBiomeExplorationData)
            }
        }
    }

    val PLAYER_BIOME_EXPLORATION: DeferredHolder<AttachmentType<*>, AttachmentType<PlayerBiomeExplorationData>> =
        REGISTRY.register("player_biome_exploration", Supplier {
            AttachmentType.builder(Supplier { PlayerBiomeExplorationData() })
                .serialize(PlayerBiomeExplorationData.CODEC)
                .copyOnDeath()
                .build()
        })

    // 6. Placed Block Chunk Tracking (Anti-laundering player_placed guardrail)
    data class PlacedBlockChunkData(
        val placedPositions: Set<Long> = emptySet()
    ) {
        companion object {
            val LONG_SET_CODEC: Codec<Set<Long>> = Codec.LONG.listOf().xmap({ it.toSet() }, { it.toList() })
            val CODEC: Codec<PlacedBlockChunkData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    LONG_SET_CODEC.optionalFieldOf("placedPositions", emptySet())
                        .forGetter(PlacedBlockChunkData::placedPositions)
                ).apply(instance, ::PlacedBlockChunkData)
            }
        }
    }

    val CHUNK_PLACED_BLOCKS: DeferredHolder<AttachmentType<*>, AttachmentType<PlacedBlockChunkData>> =
        REGISTRY.register("chunk_placed_blocks", Supplier {
            AttachmentType.builder(Supplier { PlacedBlockChunkData() })
                .serialize(PlacedBlockChunkData.CODEC)
                .build()
        })

    // 7. Player Progression Milestones & Non-AFK Active Playtime
    data class PlayerMilestoneData(
        val claimedMilestones: Set<String> = emptySet(),
        val totalNaturalBlocksHarvested: Long = 0L,
        val dungeonsCleared: Int = 0,
        val activePlaytimeTicks: Long = 0L,
        val lastActiveCheckX: Double = 0.0,
        val lastActiveCheckY: Double = 0.0,
        val lastActiveCheckZ: Double = 0.0,
        val lastActiveYaw: Float = 0.0f,
        val lastActivePitch: Float = 0.0f,
        val idleTicks: Long = 0L
    ) {
        companion object {
            val STRING_SET_CODEC: Codec<Set<String>> = Codec.STRING.listOf().xmap({ it.toSet() }, { it.toList() })
            val CODEC: Codec<PlayerMilestoneData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    STRING_SET_CODEC.optionalFieldOf("claimedMilestones", emptySet()).forGetter(PlayerMilestoneData::claimedMilestones),
                    Codec.LONG.optionalFieldOf("totalNaturalBlocksHarvested", 0L).forGetter(PlayerMilestoneData::totalNaturalBlocksHarvested),
                    Codec.INT.optionalFieldOf("dungeonsCleared", 0).forGetter(PlayerMilestoneData::dungeonsCleared),
                    Codec.LONG.optionalFieldOf("activePlaytimeTicks", 0L).forGetter(PlayerMilestoneData::activePlaytimeTicks),
                    Codec.DOUBLE.optionalFieldOf("lastActiveCheckX", 0.0).forGetter(PlayerMilestoneData::lastActiveCheckX),
                    Codec.DOUBLE.optionalFieldOf("lastActiveCheckY", 0.0).forGetter(PlayerMilestoneData::lastActiveCheckY),
                    Codec.DOUBLE.optionalFieldOf("lastActiveCheckZ", 0.0).forGetter(PlayerMilestoneData::lastActiveCheckZ),
                    Codec.FLOAT.optionalFieldOf("lastActiveYaw", 0.0f).forGetter(PlayerMilestoneData::lastActiveYaw),
                    Codec.FLOAT.optionalFieldOf("lastActivePitch", 0.0f).forGetter(PlayerMilestoneData::lastActivePitch),
                    Codec.LONG.optionalFieldOf("idleTicks", 0L).forGetter(PlayerMilestoneData::idleTicks)
                ).apply(instance, ::PlayerMilestoneData)
            }
        }
    }

    val PLAYER_MILESTONES: DeferredHolder<AttachmentType<*>, AttachmentType<PlayerMilestoneData>> =
        REGISTRY.register("player_milestones", Supplier {
            AttachmentType.builder(Supplier { PlayerMilestoneData() })
                .serialize(PlayerMilestoneData.CODEC)
                .copyOnDeath()
                .build()
        })
}
