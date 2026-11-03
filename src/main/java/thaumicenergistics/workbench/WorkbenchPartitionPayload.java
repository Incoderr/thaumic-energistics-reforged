package thaumicenergistics.workbench;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;

public record WorkbenchPartitionPayload(int containerId, List<Aspect> partition) implements CustomPacketPayload {
    public static final Type<WorkbenchPartitionPayload> TYPE = new Type<>(ThaumicEnergistics.id("workbench_partition"));
    public static final StreamCodec<ByteBuf, WorkbenchPartitionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WorkbenchPartitionPayload::containerId,
        Aspect.STREAM_CODEC.apply(ByteBufCodecs.list()), WorkbenchPartitionPayload::partition,
        WorkbenchPartitionPayload::new
    );

    @Override
    public Type<WorkbenchPartitionPayload> type() {
        return TYPE;
    }

    public static void handle(WorkbenchPartitionPayload payload, Player player) {
        if (player.containerMenu instanceof EssentiaCellWorkbenchMenu menu && menu.containerId == payload.containerId()) {
            menu.setClientPartition(payload.partition());
        }
    }
}
