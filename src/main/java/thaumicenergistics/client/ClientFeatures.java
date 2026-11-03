package thaumicenergistics.client;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumicenergistics.client.essentia.ClientEssentia;
import thaumicenergistics.client.workbench.EssentiaCellWorkbenchScreen;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class ClientFeatures {
    private ClientFeatures() {
    }

    public static void setup(FMLClientSetupEvent event) {
        ClientEssentia.setup(event);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WorkbenchFeature.MENU.get(), EssentiaCellWorkbenchScreen::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
