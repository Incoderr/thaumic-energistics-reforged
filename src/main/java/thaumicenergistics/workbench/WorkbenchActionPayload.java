package thaumicenergistics.workbench;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;

public record WorkbenchActionPayload(int action, Optional<Aspect> slotAspect) implements CustomPacketPayload {
    public static final int CLICK_SLOT = 0;
    public static final int CLEAR = 1;
    public static final int PARTITION_TO_CONTENTS = 2;

    public static final Type<WorkbenchActionPayload> TYPE = new Type<>(ThaumicEnergistics.id("workbench_action"));
    public static final StreamCodec<ByteBuf, WorkbenchActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WorkbenchActionPayload::action,
        ByteBufCodecs.optional(Aspect.STREAM_CODEC), WorkbenchActionPayload::slotAspect,
        WorkbenchActionPayload::new
    );

    @Override
    public Type<WorkbenchActionPayload> type() {
        return TYPE;
    }

    public static void handle(WorkbenchActionPayload payload, Player player) {
        if (player.containerMenu instanceof EssentiaCellWorkbenchMenu menu && menu.stillValid(player)) {
            menu.handleAction(payload.action(), payload.slotAspect().orElse(null));
        }
    }
}
