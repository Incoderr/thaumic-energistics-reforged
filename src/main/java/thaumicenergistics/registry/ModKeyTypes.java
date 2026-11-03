package thaumicenergistics.registry;

import appeng.api.stacks.AEKeyType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumicenergistics.ThaumicEnergistics;

public final class ModKeyTypes {
    public static final DeferredRegister<AEKeyType> KEY_TYPES = DeferredRegister.create(AEKeyType.REGISTRY_KEY, ThaumicEnergistics.MODID);

    private ModKeyTypes() {
    }

    public static void register(IEventBus bus) {
        KEY_TYPES.register(bus);
    }
}
