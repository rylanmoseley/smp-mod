package dev.rmoseley.smpmod.registry

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.world.WardedLanternBlock
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Deferred register for SMP Mod Suite blocks.
 */
object ModBlocks {
    val REGISTRY: DeferredRegister.Blocks =
        DeferredRegister.createBlocks(SmpMod.MOD_ID)

    val WARDED_LANTERN: DeferredBlock<WardedLanternBlock> =
        REGISTRY.registerBlock("warded_lantern", ::WardedLanternBlock, WardedLanternBlock.createProperties())
}
