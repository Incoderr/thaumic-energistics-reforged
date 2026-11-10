package thaumicenergistics.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record BackpackData(GlobalPos link, ItemStack facade) {
    public static final Codec<BackpackData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GlobalPos.CODEC.fieldOf("wifiBackpackKey").forGetter(BackpackData::link),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("facade", ItemStack.EMPTY).forGetter(BackpackData::facade)
    ).apply(instance, BackpackData::new));

    public BackpackSkin skin() {
        return GolemBackpackFeature.skinOf(facade);
    }

    public record Client(int state, int skin) {
        public static final int NO_WIFI = 0;
        public static final int IN_RANGE = 1;
        public static final int OUT_OF_RANGE = 2;
        public static final Client NONE = new Client(NO_WIFI, 0);
        public static final StreamCodec<ByteBuf, Client> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Client::state,
            ByteBufCodecs.VAR_INT, Client::skin,
            Client::new
        );

        public boolean hasBackpack() {
            return state != NO_WIFI;
        }

        public boolean inRange() {
            return state == IN_RANGE;
        }
    }
}
