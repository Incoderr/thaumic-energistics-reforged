package thaumicenergistics.client.essentia;

import appeng.api.client.AEKeyRendering;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaKeyType;

public final class ClientEssentia {
    private ClientEssentia() {
    }

    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AEKeyRendering.register(EssentiaKeyType.TYPE, EssentiaKey.class, new EssentiaKeyRenderHandler()));
    }
}
