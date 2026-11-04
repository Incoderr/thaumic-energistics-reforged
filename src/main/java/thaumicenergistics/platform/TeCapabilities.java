package thaumicenergistics.platform;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class TeCapabilities {
    private static final List<Supplier<? extends BlockEntityType<? extends IInWorldGridNodeHost>>> GRID_HOSTS = new ArrayList<>();

    private TeCapabilities() {
    }

    public static void gridHost(Supplier<? extends BlockEntityType<? extends IInWorldGridNodeHost>> type) {
        GRID_HOSTS.add(type);
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterCapabilitiesEvent.class, TeCapabilities::onRegister);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends IInWorldGridNodeHost>> type : GRID_HOSTS) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type.get(), (host, context) -> host);
        }
    }
}
