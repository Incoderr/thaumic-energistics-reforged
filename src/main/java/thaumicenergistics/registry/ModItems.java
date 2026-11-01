package thaumicenergistics.registry;

import net.neoforged.bus.api.IEventBus;

public final class ModItems {
    public static final thaumicenergistics.platform.TeRegistration.Items ITEMS = new thaumicenergistics.platform.TeRegistration.Items();

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
