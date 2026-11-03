package thaumicenergistics.essentia;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import thaumicenergistics.ThaumicEnergistics;

public final class EssentiaKeyType extends AEKeyType {
    public static final EssentiaKeyType TYPE = new EssentiaKeyType();
    public static final int ESSENTIA_PER_BYTE = 2;

    private EssentiaKeyType() {
        super(ThaumicEnergistics.id("essentia"), EssentiaKey.class, Component.translatable("thaumicenergistics.key.essentia"));
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return EssentiaKey.MAP_CODEC;
    }

    @Override
    public AEKey readFromPacket(RegistryFriendlyByteBuf buffer) {
        return EssentiaKey.fromPacket(buffer);
    }

    @Override
    public int getAmountPerByte() {
        return ESSENTIA_PER_BYTE;
    }
}
