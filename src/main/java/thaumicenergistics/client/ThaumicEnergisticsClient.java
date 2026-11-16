package thaumicenergistics.client;

import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import thaumicenergistics.ThaumicEnergistics;

@Mod(value = ThaumicEnergistics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ThaumicEnergistics.MODID, value = Dist.CLIENT)
public final class ThaumicEnergisticsClient {
    public ThaumicEnergisticsClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientPayloadHandlers.register();
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        ClientFeatures.setup(event);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        ClientFeatures.registerScreens(event);
    }

    @SubscribeEvent
    static void registerPartModels(RegisterPartModelsEvent event) {
        ClientFeatures.registerPartModels(event);
    }

    @SubscribeEvent
    static void registerPartRenderers(RegisterPartRendererEvent event) {
        ClientFeatures.registerPartRenderers(event);
    }

    @SubscribeEvent
    static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        ClientFeatures.registerBlockTints(event);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientFeatures.registerRenderers(event);
    }

    @SubscribeEvent
    static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        ClientFeatures.registerSpecialRenderers(event);
    }

    @SubscribeEvent
    static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        ClientFeatures.registerRenderStateModifiers(event);
    }
}
