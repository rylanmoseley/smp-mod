package dev.rmoseley.smpmod.registry

import com.mojang.serialization.Codec
import dev.rmoseley.smpmod.SmpMod
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.UUID

/**
 * Data components for persistent item tracking, UUID buybacks, and provenance lore.
 * Native to Minecraft 1.21.1 component architecture.
 */
object ModDataComponents {
    val REGISTRY: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SmpMod.MOD_ID)

    private val UUID_CODEC: Codec<UUID> = Codec.STRING.xmap(UUID::fromString, UUID::toString)

    val ITEM_UUID: DeferredHolder<DataComponentType<*>, DataComponentType<UUID>> =
        REGISTRY.registerComponentType("item_uuid") { builder ->
            builder.persistent(UUID_CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(UUID_CODEC))
        }

    val ORIGIN_CREATOR: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        REGISTRY.registerComponentType("origin_creator") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.STRING))
        }

    val REGISTERED_OWNER: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        REGISTRY.registerComponentType("registered_owner") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.STRING))
        }

    val REGISTERED_OWNER_UUID: DeferredHolder<DataComponentType<*>, DataComponentType<UUID>> =
        REGISTRY.registerComponentType("registered_owner_uuid") { builder ->
            builder.persistent(UUID_CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(UUID_CODEC))
        }

    val CATALYST_REDUCTIONS: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        REGISTRY.registerComponentType("catalyst_reductions") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.INT))
        }

    val DEATH_COUNT: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        REGISTRY.registerComponentType("death_count") { builder ->
            builder.persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.INT))
        }

    val LAST_DEATH_TIMESTAMP: DeferredHolder<DataComponentType<*>, DataComponentType<Long>> =
        REGISTRY.registerComponentType("last_death_timestamp") { builder ->
            builder.persistent(Codec.LONG)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.LONG))
        }

    private val LORE_LIST_CODEC: Codec<List<String>> = Codec.STRING.listOf()

    val PROVENANCE_LORE: DeferredHolder<DataComponentType<*>, DataComponentType<List<String>>> =
        REGISTRY.registerComponentType("provenance_lore") { builder ->
            builder.persistent(LORE_LIST_CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(LORE_LIST_CODEC))
        }

    val MASTERWORK_AFFIX: DeferredHolder<DataComponentType<*>, DataComponentType<String>> =
        REGISTRY.registerComponentType("masterwork_affix") { builder ->
            builder.persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.STRING))
        }
}
