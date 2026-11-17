package dev.rmoseley.smpmod.registry

import dev.rmoseley.smpmod.SmpMod
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Deferred register for SMP Mod Suite items.
 */
object ModItems {
    val REGISTRY: DeferredRegister.Items =
        DeferredRegister.createItems(SmpMod.MOD_ID)

    val WARDED_LANTERN: DeferredItem<BlockItem> =
        REGISTRY.registerSimpleBlockItem("warded_lantern", ModBlocks.WARDED_LANTERN, Item.Properties().rarity(Rarity.RARE))

    val RECOVERY_CATALYST: DeferredItem<Item> =
        REGISTRY.registerItem("recovery_catalyst", { properties -> Item(properties) }, Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16))

    val ENCAPSULATED_VILLAGER: DeferredItem<Item> =
        REGISTRY.registerItem("encapsulated_villager", { properties -> dev.rmoseley.smpmod.world.EncapsulatedVillagerItem(properties) }, Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
}
