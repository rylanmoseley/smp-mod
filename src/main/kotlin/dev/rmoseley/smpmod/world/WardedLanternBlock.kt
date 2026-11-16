package dev.rmoseley.smpmod.world

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LanternBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

/**
 * Permanent Warded Lantern block.
 * Emits light level 15 and is completely immune to destroys_light_on_aggro mob destruction and snuff auras.
 * Crafted by Support specialists and installed by Builders along roads and town walls.
 */
class WardedLanternBlock(properties: Properties) : LanternBlock(properties) {

    companion object {
        fun createProperties(): Properties {
            return BlockBehaviour.Properties.of()
                .lightLevel { 15 }
                .strength(3.5f)
                .sound(SoundType.LANTERN)
                .noOcclusion()
        }

        fun isWardedLantern(state: BlockState): Boolean {
            return state.block is WardedLanternBlock
        }
    }
}
