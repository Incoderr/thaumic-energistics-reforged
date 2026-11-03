package thaumicenergistics.cells;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.aspect.Aspect;

public record EssentiaCellData(List<Entry> stored, List<Aspect> partition) {
    public static final EssentiaCellData EMPTY = new EssentiaCellData(List.of(), List.of());

    public static final Codec<EssentiaCellData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Entry.CODEC.listOf().optionalFieldOf("stored", List.of()).forGetter(EssentiaCellData::stored),
        Aspect.CODEC.listOf().optionalFieldOf("partition", List.of()).forGetter(EssentiaCellData::partition)
    ).apply(instance, EssentiaCellData::new));

    public static final StreamCodec<ByteBuf, EssentiaCellData> STREAM_CODEC = StreamCodec.composite(
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), EssentiaCellData::stored,
        Aspect.STREAM_CODEC.apply(ByteBufCodecs.list()), EssentiaCellData::partition,
        EssentiaCellData::new
    );

    public EssentiaCellData {
        stored = List.copyOf(stored);
        partition = List.copyOf(partition);
    }

    public record Entry(Aspect aspect, long amount) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Aspect.CODEC.fieldOf("aspect").forGetter(Entry::aspect),
            Codec.LONG.fieldOf("amount").forGetter(Entry::amount)
        ).apply(instance, Entry::new));

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            Aspect.STREAM_CODEC, Entry::aspect,
            ByteBufCodecs.VAR_LONG, Entry::amount,
            Entry::new
        );
    }
}
