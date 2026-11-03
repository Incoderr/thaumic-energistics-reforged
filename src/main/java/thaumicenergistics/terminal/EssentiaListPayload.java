package thaumicenergistics.terminal;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;

public record EssentiaListPayload(int containerId, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<EssentiaListPayload> TYPE = new Type<>(ThaumicEnergistics.id("essentia_list"));
    public static final StreamCodec<ByteBuf, EssentiaListPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, EssentiaListPayload::containerId,
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), EssentiaListPayload::entries,
        EssentiaListPayload::new
    );

    @Override
    public Type<EssentiaListPayload> type() {
        return TYPE;
    }

    public static void handle(EssentiaListPayload payload, Player player) {
        if (player.containerMenu instanceof EssentiaTerminalMenu menu && menu.containerId == payload.containerId()) {
            menu.setClientList(payload.entries());
        }
    }

    public record Entry(Aspect aspect, long amount, boolean craftable) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            Aspect.STREAM_CODEC, Entry::aspect,
            ByteBufCodecs.VAR_LONG, Entry::amount,
            ByteBufCodecs.BOOL, Entry::craftable,
            Entry::new
        );
    }
}
