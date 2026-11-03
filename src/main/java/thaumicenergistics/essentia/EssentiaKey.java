package thaumicenergistics.essentia;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;

public final class EssentiaKey extends AEKey {
    public static final MapCodec<EssentiaKey> MAP_CODEC = Aspect.CODEC.fieldOf("aspect").xmap(EssentiaKey::of, EssentiaKey::getAspect);
    public static final Codec<EssentiaKey> CODEC = MAP_CODEC.codec();

    private static final Map<Aspect, EssentiaKey> CACHE = new ConcurrentHashMap<>();

    private final Aspect aspect;

    private EssentiaKey(Aspect aspect) {
        this.aspect = aspect;
    }

    public static EssentiaKey of(Aspect aspect) {
        return CACHE.computeIfAbsent(aspect, EssentiaKey::new);
    }

    public static boolean is(@Nullable AEKey key) {
        return key instanceof EssentiaKey;
    }

    public static @Nullable Aspect aspectOf(@Nullable AEKey key) {
        return key instanceof EssentiaKey essentia ? essentia.aspect : null;
    }

    public static EssentiaKey fromPacket(RegistryFriendlyByteBuf buffer) {
        return of(Aspect.STREAM_CODEC.decode(buffer));
    }

    public Aspect getAspect() {
        return aspect;
    }

    @Override
    public AEKeyType getType() {
        return EssentiaKeyType.TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public CompoundTag toTag(HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    @Override
    public Object getPrimaryKey() {
        return aspect;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", aspect.getTag());
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf buffer) {
        Aspect.STREAM_CODEC.encode(buffer, aspect);
    }

    @Override
    protected Component computeDisplayName() {
        return aspect.getDescription();
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
    }

    @Override
    public boolean hasComponents() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EssentiaKey key && key.aspect.equals(aspect);
    }

    @Override
    public int hashCode() {
        return aspect.hashCode();
    }

    @Override
    public String toString() {
        return "EssentiaKey{" + aspect.getTag() + "}";
    }
}
