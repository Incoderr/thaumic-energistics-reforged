package thaumicenergistics.client;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumicenergistics.client.essentia.ClientEssentia;
import thaumicenergistics.client.terminal.EssentiaTerminalScreen;
import thaumicenergistics.client.workbench.EssentiaCellWorkbenchScreen;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class ClientFeatures {
    private ClientFeatures() {
    }

    public static void setup(FMLClientSetupEvent event) {
        ClientEssentia.setup(event);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WorkbenchFeature.MENU.get(), EssentiaCellWorkbenchScreen::new);
        event.register(TerminalFeature.MENU.get(), EssentiaTerminalScreen::new);
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new StaticItemColor(AEColor.TRANSPARENT), TerminalFeature.TERMINAL.get());
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
