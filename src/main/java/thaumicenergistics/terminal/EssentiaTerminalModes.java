package thaumicenergistics.terminal;

import appeng.api.config.ViewItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EssentiaTerminalModes(EssentiaSortMode sort, ViewItems view) {
    public static final EssentiaTerminalModes DEFAULT = new EssentiaTerminalModes(EssentiaSortMode.ALPHABETIC, ViewItems.ALL);

    private static final Codec<EssentiaSortMode> SORT_CODEC = Codec.INT.xmap(index -> EssentiaSortMode.values()[Math.floorMod(index, EssentiaSortMode.values().length)], Enum::ordinal);
    private static final Codec<ViewItems> VIEW_CODEC = Codec.INT.xmap(index -> ViewItems.values()[Math.floorMod(index, ViewItems.values().length)], Enum::ordinal);

    public static final Codec<EssentiaTerminalModes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        SORT_CODEC.fieldOf("sort").forGetter(EssentiaTerminalModes::sort),
        VIEW_CODEC.fieldOf("view").forGetter(EssentiaTerminalModes::view)
    ).apply(instance, EssentiaTerminalModes::new));

    public static final StreamCodec<ByteBuf, EssentiaTerminalModes> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(index -> EssentiaSortMode.values()[Math.floorMod(index, EssentiaSortMode.values().length)], Enum::ordinal), EssentiaTerminalModes::sort,
        ByteBufCodecs.VAR_INT.map(index -> ViewItems.values()[Math.floorMod(index, ViewItems.values().length)], Enum::ordinal), EssentiaTerminalModes::view,
        EssentiaTerminalModes::new
    );

    public EssentiaTerminalModes withSort(EssentiaSortMode mode) {
        return new EssentiaTerminalModes(mode, view);
    }

    public EssentiaTerminalModes withView(ViewItems mode) {
        return new EssentiaTerminalModes(sort, mode);
    }
}
