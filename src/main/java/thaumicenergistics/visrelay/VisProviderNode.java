package thaumicenergistics.visrelay;

import java.util.HashSet;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.vis.VisNet;
import thaumcraft.vis.VisNode;
import thaumcraft.vis.VisRelayBlockEntity;

/**
 * A fake vis net source that pulls vis through its interface.
 * It is never placed in a world.
 */
public final class VisProviderNode extends VisNode {
    /**
     * How far vis can travel from the source.
     */
    private static final int VIS_RANGE = 1;

    /**
     * Tracks which proxies have been visited during a single call chain.
     */
    private static final HashSet<VisProviderNode> VISITED_PROXIES = new HashSet<>();

    /**
     * Reuses the AE2 cable bus block entity type. The node is never attached
     * to a world; the type only has to accept the host block state, since
     * 1.21 validates it in the block entity constructor and new types cannot
     * be created once the registries are frozen.
     */
    private static final BlockEntityType<?> TYPE = AEBlockEntities.CABLE_BUS.get();

    /**
     * The interface associated with this source.
     */
    private @Nullable VisRelayInterfacePart visInterface;

    /**
     * What vis 'channel' does this node respond on?
     */
    private byte attunement = -1;

    public VisProviderNode(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    static VisProviderNode create(VisRelayInterfacePart visInterface, Level level) {
        // TE placed the proxy in front of the interface, but the 1.21 vis net
        // skips nodes at zero distance, so it sits on the bus block instead.
        BlockPos pos = visInterface.getBlockEntity().getBlockPos();
        VisProviderNode node = new VisProviderNode(pos, level.getBlockState(pos));
        node.visInterface = visInterface;
        node.setLevel(level);
        node.updateVisInterface();
        return node;
    }

    /**
     * Attempts to consume vis from the interface.
     */
    @Override
    public int consumeVis(Aspect aspect, int vis) {
        if (visInterface == null || !visInterface.isVisProvider() || visInterface.getRelay() == null) {
            return 0;
        }
        int consumed = 0;
        if (!VISITED_PROXIES.contains(this)) {
            VISITED_PROXIES.add(this);
            consumed = visInterface.consumeVis(aspect, vis);
            VISITED_PROXIES.remove(this);
        }
        return consumed;
    }

    @Override
    public byte getAttunement() {
        return attunement;
    }

    @Override
    public int getRange() {
        return VIS_RANGE;
    }

    @Override
    public boolean isSource() {
        return visInterface != null && visInterface.isVisProvider();
    }

    void invalidate() {
        removeThisNode();
    }

    void updateVisInterface() {
        byte relayAttunement = -1;
        VisRelayBlockEntity relay = visInterface != null ? visInterface.getRelay() : null;
        if (relay != null) {
            relayAttunement = relay.getAttunement();
        }
        if (attunement != relayAttunement) {
            attunement = relayAttunement;
            removeThisNode();
            requestNodeRefresh();
            if (level != null) {
                VisNet.addSource(level, this);
            }
        }
        if (level != null) {
            tickNetwork(level);
        }
    }
}
