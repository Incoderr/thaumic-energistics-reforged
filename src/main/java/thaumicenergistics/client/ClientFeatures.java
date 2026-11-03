package thaumicenergistics.client;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumicenergistics.client.essentia.ClientEssentia;

public final class ClientFeatures {
    private ClientFeatures() {
    }

    public static void setup(FMLClientSetupEvent event) {
        ClientEssentia.setup(event);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
