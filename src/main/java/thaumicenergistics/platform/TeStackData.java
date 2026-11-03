package thaumicenergistics.platform;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumicenergistics.ThaumicEnergistics;

public final class TeStackData {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThaumicEnergistics.MODID);

    private TeStackData() {
    }

    public static <T> TeStackKey<T> register(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new TeStackKey<>(COMPONENTS.registerComponentType(name, builder -> builder.persistent(codec).networkSynchronized(streamCodec)));
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
