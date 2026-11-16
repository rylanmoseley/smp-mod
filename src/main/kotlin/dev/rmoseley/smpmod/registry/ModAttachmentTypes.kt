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
                .build()
        })

    // 4. Player Explorer Progression (discovered unique chunks count)
    val PLAYER_DISCOVERED_CHUNKS: DeferredHolder<AttachmentType<*>, AttachmentType<Int>> =
        REGISTRY.register("player_discovered_chunks", Supplier {
            AttachmentType.builder(Supplier { 0 })
                .serialize(Codec.INT)
                .build()
        })
}
