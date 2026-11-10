package thaumicenergistics.act;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumicenergistics.ThaumicEnergistics;

public record ActFillGridPayload(List<List<ItemStack>> slots) implements CustomPacketPayload {
    public static final int GRID_SIZE = 9;
    public static final int MAX_OPTIONS = 64;
    public static final Type<ActFillGridPayload> TYPE = new Type<>(ThaumicEnergistics.id("act_fill_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActFillGridPayload> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OPTIONS)).apply(ByteBufCodecs.list(GRID_SIZE)), ActFillGridPayload::slots,
        ActFillGridPayload::new
    );

    @Override
    public Type<ActFillGridPayload> type() {
        return TYPE;
    }

    public static void handle(ActFillGridPayload payload, Player player) {
        if (player.containerMenu instanceof ArcaneCraftingTermMenu menu && menu.stillValid(player)) {
            menu.fillGrid(player, payload.slots());
        }
    }
}
