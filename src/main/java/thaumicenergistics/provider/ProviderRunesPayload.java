package thaumicenergistics.provider;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import thaumcraft.fx.Fx;
import thaumicenergistics.ThaumicEnergistics;

public record ProviderRunesPayload(BlockPos pos, int color) implements CustomPacketPayload {
    public static final Type<ProviderRunesPayload> TYPE = new Type<>(ThaumicEnergistics.id("provider_runes"));
    public static final StreamCodec<ByteBuf, ProviderRunesPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ProviderRunesPayload::pos,
        ByteBufCodecs.INT, ProviderRunesPayload::color,
        ProviderRunesPayload::new
    );

    @Override
    public Type<ProviderRunesPayload> type() {
        return TYPE;
    }

    public static void handle(ProviderRunesPayload payload, Player player) {
        float red = (payload.color() >> 16 & 0xFF) / 255.0F;
        float green = (payload.color() >> 8 & 0xFF) / 255.0F;
        float blue = (payload.color() & 0xFF) / 255.0F;
        BlockPos pos = payload.pos();
        for (int index = 0; index < 5; index++) {
            Fx.get().blockRunes(player.level(), pos.getX(), pos.getY(), pos.getZ(), red, green, blue, 15, -0.1F);
        }
        for (int index = 0; index < 5; index++) {
            Fx.get().blockRunes(player.level(), pos.getX(), pos.getY(), pos.getZ(), red, green, blue, 15, 0.1F);
        }
    }
}
