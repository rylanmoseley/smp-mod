package dev.rmoseley.smpmod.event

import dev.rmoseley.smpmod.registry.ModEntityAttributes
import net.minecraft.world.entity.EntityType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent

/**
 * Mod-bus event listeners for attribute modifications and setup.
 */
object CommonModEvents {

    @SubscribeEvent
    fun onEntityAttributeModification(event: EntityAttributeModificationEvent) {
        // Attach emergent role attributes to players
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_DPS_MULTIPLIER)
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_TANK_MITIGATION)
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_SUPPORT_EFFICIENCY)
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_SCOUT_AGILITY)
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_ARMOR_PENETRATION)
        event.add(EntityType.PLAYER, ModEntityAttributes.ROLE_THREAT_GENERATION)

        // Attach light destroy attribute to hostile mobs
        val hostiles = listOf(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.WITHER_SKELETON,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.CAVE_SPIDER
        )
        for (type in hostiles) {
            event.add(type, ModEntityAttributes.DESTROYS_LIGHT_ON_AGGRO)
        }
    }
}
