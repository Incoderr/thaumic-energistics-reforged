package thaumicenergistics.client;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import appeng.init.client.InitScreens;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.assembler.AssemblerFeature;
import thaumicenergistics.client.assembler.ArcaneAssemblerScreen;
import thaumicenergistics.buses.BusesFeature;
import thaumicenergistics.client.act.ArcaneCraftingTermScreen;
import thaumicenergistics.client.buses.EssentiaBusScreen;
import thaumicenergistics.client.emitter.EssentiaLevelEmitterScreen;
import thaumicenergistics.emitter.EmitterFeature;
import thaumicenergistics.client.essentia.ClientEssentia;
import thaumicenergistics.client.monitor.EssentiaMonitorRenderer;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;
import thaumicenergistics.monitor.MonitorFeature;
import thaumicenergistics.provider.ProviderBlockEntity;
import thaumicenergistics.provider.ProviderFeature;
import thaumicenergistics.client.terminal.EssentiaTerminalScreen;
import thaumicenergistics.client.workbench.EssentiaCellWorkbenchScreen;
import thaumicenergistics.client.visrelay.VisRelayInterfaceRenderer;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.client.knowledge.KnowledgeInscriberScreen;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.visrelay.VisRelayFeature;
import thaumicenergistics.visrelay.VisRelayInterfacePart;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class ClientFeatures {
    private ClientFeatures() {
    }

    public static void setup(FMLClientSetupEvent event) {
        ClientEssentia.setup(event);
        EssentiaStorageMonitorPart.renderer = new EssentiaMonitorRenderer();
        VisRelayInterfacePart.renderer = new VisRelayInterfaceRenderer();
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WorkbenchFeature.MENU.get(), EssentiaCellWorkbenchScreen::new);
        event.register(TerminalFeature.MENU.get(), EssentiaTerminalScreen::new);
        event.register(BusesFeature.MENU.get(), EssentiaBusScreen::new);
        event.register(EmitterFeature.MENU.get(), EssentiaLevelEmitterScreen::new);
        InitScreens.register(event, ActFeature.MENU.get(), ArcaneCraftingTermScreen::new, ArcaneCraftingTermScreen.STYLE);
        event.register(KnowledgeFeature.MENU.get(), KnowledgeInscriberScreen::new);
        event.register(AssemblerFeature.MENU.get(), ArcaneAssemblerScreen::new);
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new StaticItemColor(AEColor.TRANSPARENT), TerminalFeature.TERMINAL.get());
        event.register(new StaticItemColor(AEColor.TRANSPARENT), BusesFeature.IMPORT_BUS.get(), BusesFeature.EXPORT_BUS.get(), BusesFeature.STORAGE_BUS.get());
        event.register(new StaticItemColor(AEColor.TRANSPARENT), MonitorFeature.STORAGE_MONITOR.get(), MonitorFeature.CONVERSION_MONITOR.get());
        event.register(new StaticItemColor(AEColor.TRANSPARENT), ActFeature.TERMINAL.get(), VisRelayFeature.INTERFACE.get());
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof ProviderBlockEntity provider) {
                return provider.isActive() ? provider.getColor().mediumVariant : 0;
            }
            return AEColor.TRANSPARENT.mediumVariant;
        }, ProviderFeature.ESSENTIA_PROVIDER.get(), ProviderFeature.INFUSION_PROVIDER.get());
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
