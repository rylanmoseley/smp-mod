package dev.rmoseley.smpmod.world

import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.context.UseOnContext

/**
 * Encapsulated Villager Spawn Egg item acquired by Explorers.
 * Preserves the villager's complete profession, trade offers, level, experience, and custom name.
 * Deployable anywhere by right-clicking on a block.
 */
class EncapsulatedVillagerItem(properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val player = context.player
        val stack = context.itemInHand

        if (!level.isClientSide) {
            val serverLevel = level as ServerLevel
            val clickedPos = context.clickedPos
            val spawnPos = clickedPos.relative(context.clickedFace)

            val villager = EntityType.VILLAGER.create(serverLevel)
            if (villager != null) {
                val customData = stack.get(DataComponents.CUSTOM_DATA)
                if (customData != null) {
                    val tag = customData.copyTag()
                    if (tag.contains("VillagerEntityData")) {
                        villager.load(tag.getCompound("VillagerEntityData"))
                    } else {
                        villager.load(tag)
                    }
                }

                villager.moveTo(
                    spawnPos.x + 0.5,
                    spawnPos.y.toDouble(),
                    spawnPos.z + 0.5,
                    context.rotation,
                    0.0f
                )

                // Tag as active relocated villager
                villager.persistentData.putBoolean("smpmod:relocated_villager", true)
                serverLevel.addFreshEntity(villager)

                serverLevel.playSound(
                    null, spawnPos,
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL,
                    1.0f, 1.0f
                )
                serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    spawnPos.x + 0.5, spawnPos.y + 0.5, spawnPos.z + 0.5,
                    15, 0.4, 0.4, 0.4, 0.05
                )

                if (player == null || !player.isCreative) {
                    stack.shrink(1)
                }

                player?.sendSystemMessage(
                    Component.literal("§a[Villager Deployed] §fDeployed ${villager.villagerData.profession.name()} villager! Ensure they have a 3x3+ open area to roam and restock.")
                )
                return InteractionResult.SUCCESS
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null) {
            val tag = if (customData.copyTag().contains("VillagerEntityData")) {
                customData.copyTag().getCompound("VillagerEntityData")
            } else {
                customData.copyTag()
            }

            val villagerData = tag.getCompound("VillagerData")
            val profRaw = villagerData.getString("profession")
            val profName = if (profRaw.isNotEmpty()) profRaw.substringAfter(":").replaceFirstChar { it.uppercase() } else "Unemployed"
            val level = villagerData.getInt("level").coerceAtLeast(1)

            val offersList = tag.getCompound("Offers").getList("Recipes", 10)
            val tradeCount = offersList.size

            val acquiredBy = customData.copyTag().getString("acquired_by")

            tooltipComponents.add(Component.literal("§6§l[Explorer Specimen] §eEncapsulated Villager"))
            tooltipComponents.add(Component.literal("§7Profession: §b$profName §8(Level $level)"))
            tooltipComponents.add(Component.literal("§7Trades Preserved: §a$tradeCount verified offers"))
            if (acquiredBy.isNotEmpty()) {
                tooltipComponents.add(Component.literal("§7Acquired By: §d$acquiredBy §8(Scout)"))
            }
            tooltipComponents.add(Component.literal("§eRight-click ground to deploy into settlement."))
            tooltipComponents.add(Component.literal("§8Requires 3x3+ free walking area for trade restocks."))
        } else {
            tooltipComponents.add(Component.literal("§7Empty encapsulated specimen."))
        }
    }

    companion object {
        fun createFromVillager(villager: Villager, explorer: Player): ItemStack {
            val itemStack = ItemStack(dev.rmoseley.smpmod.registry.ModItems.ENCAPSULATED_VILLAGER.get())
            val entityTag = CompoundTag()
            villager.saveWithoutId(entityTag)

            val customData = CustomData.EMPTY.update { tag ->
                tag.put("VillagerEntityData", entityTag)
                tag.putString("acquired_by", explorer.gameProfile.name)
                tag.putString("profession", villager.villagerData.profession.name())
                tag.putInt("level", villager.villagerData.level)
            }

            itemStack.set(DataComponents.CUSTOM_DATA, customData)
            val profTitle = villager.villagerData.profession.name().replace("minecraft:", "").replaceFirstChar { it.uppercase() }
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal("§6Encapsulated $profTitle §7(Lvl ${villager.villagerData.level})"))
            return itemStack
        }
    }
}
