package thaumicenergistics.platform;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import javax.annotation.Nullable;
import thaumicenergistics.ThaumicEnergistics;

public final class TeEntityKey<T> {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumicEnergistics.MODID);

    private final Supplier<AttachmentType<T>> type;

    private TeEntityKey(Supplier<AttachmentType<T>> type) {
        this.type = type;
    }

    public static <T> TeEntityKey<T> persistent(String name, Supplier<T> fallback, Codec<T> codec) {
        return new TeEntityKey<>(ATTACHMENTS.register(name, () -> AttachmentType.builder(fallback).serialize(codec).build()));
    }

    public static <T> TeEntityKey<T> synced(String name, Supplier<T> fallback, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new TeEntityKey<>(ATTACHMENTS.register(name, () -> AttachmentType.builder(fallback).sync(streamCodec).build()));
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    public @Nullable T get(Entity entity) {
        return entity.getExistingDataOrNull(type.get());
    }

    public boolean has(Entity entity) {
        return entity.hasData(type.get());
    }

    public void set(Entity entity, T value) {
        entity.setData(type.get(), value);
    }

    public void remove(Entity entity) {
        entity.removeData(type.get());
    }

    public void sync(Entity entity) {
        entity.syncData(type.get());
    }
}
