package dev.rmoseley.smpmod.registry

import dev.rmoseley.smpmod.SmpMod
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/**
 * Custom entity attributes for the SMP Mod Suite.
 */
object ModEntityAttributes {
    val REGISTRY: DeferredRegister<Attribute> =
        DeferredRegister.create(Registries.ATTRIBUTE, SmpMod.MOD_ID)

    /**
     * Trait flag: when aggroed, this mob actively targets and extinguishes nearby light sources
     * (breaks in wild/dungeons, snuffs temporarily inside claims).
     */
    val DESTROYS_LIGHT_ON_AGGRO: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("destroys_light_on_aggro", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.destroys_light_on_aggro",
                0.0,
                0.0,
                1.0
            ).setSyncable(true)
        })

    /**
     * Emergent combat role scaling attributes for players.
     */
    val ROLE_DPS_MULTIPLIER: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_dps_multiplier", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_dps_multiplier",
                1.0,
                1.0,
                5.0
            ).setSyncable(true)
        })

    val ROLE_TANK_MITIGATION: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_tank_mitigation", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_tank_mitigation",
                0.0,
                0.0,
                0.75
            ).setSyncable(true)
        })

    val ROLE_SUPPORT_EFFICIENCY: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_support_efficiency", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_support_efficiency",
                1.0,
                1.0,
                3.0
            ).setSyncable(true)
        })

    val ROLE_SCOUT_AGILITY: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_scout_agility", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_scout_agility",
                1.0,
                1.0,
                2.5
            ).setSyncable(true)
        })

    val ROLE_ARMOR_PENETRATION: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_armor_penetration", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_armor_penetration",
                0.0,
                0.0,
                1.0
            ).setSyncable(true)
        })

    val ROLE_THREAT_GENERATION: DeferredHolder<Attribute, Attribute> =
        REGISTRY.register("role_threat_generation", Supplier {
            RangedAttribute(
                "attribute.name.smpmod.role_threat_generation",
                1.0,
                1.0,
                5.0
            ).setSyncable(true)
        })
}
