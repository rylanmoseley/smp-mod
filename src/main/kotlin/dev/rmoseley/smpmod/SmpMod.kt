package dev.rmoseley.smpmod

import dev.rmoseley.smpmod.config.SmpConfig
import dev.rmoseley.smpmod.event.CommonModEvents
import dev.rmoseley.smpmod.event.GameForgeEvents
import dev.rmoseley.smpmod.registry.ModAttachmentTypes
import dev.rmoseley.smpmod.registry.ModBlocks
import dev.rmoseley.smpmod.registry.ModDataComponents
import dev.rmoseley.smpmod.registry.ModEntityAttributes
import dev.rmoseley.smpmod.registry.ModItems
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * Main entrypoint for the SMP Mod Suite.
 * Re-engineers survival loops to maximize cooperative multiplayer, fair recovery,
 * emergent role derivation, and construction-driven progression.
 */
@Mod(SmpMod.MOD_ID)
class SmpMod(modContainer: ModContainer) {
    init {
        LOGGER.info("Initializing SMP Mod Suite ($MOD_ID)...")

        // Register configuration specification with NeoForge
        modContainer.registerConfig(ModConfig.Type.COMMON, SmpConfig.SPEC)

        MOD_BUS.addListener(::onCommonSetup)
        MOD_BUS.addListener(::onClientSetup)

        // Register deferred registries to the mod-specific event bus
        ModBlocks.REGISTRY.register(MOD_BUS)
        ModItems.REGISTRY.register(MOD_BUS)
        ModDataComponents.REGISTRY.register(MOD_BUS)
        ModAttachmentTypes.REGISTRY.register(MOD_BUS)
        ModEntityAttributes.REGISTRY.register(MOD_BUS)

        // Register mod bus event listeners
        MOD_BUS.register(CommonModEvents)

        // Register game event listeners on the global NeoForge bus
        NeoForge.EVENT_BUS.register(GameForgeEvents)
        NeoForge.EVENT_BUS.register(dev.rmoseley.smpmod.roles.RoleCombatEvents)

        LOGGER.info("SMP Mod Suite initialized successfully.")
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        dev.rmoseley.smpmod.advancements.ModAdvancements.init()
        dev.rmoseley.smpmod.recovery.CuriosIntegration.init()
        LOGGER.info("SMP Mod Suite common setup complete.")
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.info("SMP Mod Suite client setup complete.")
    }

    companion object {
        const val MOD_ID: String = "smpmod"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }
}
