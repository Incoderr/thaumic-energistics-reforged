package thaumicenergistics.provider;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.me.helpers.MachineSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaNetwork;

public abstract class ProviderBlockEntity extends AENetworkedBlockEntity implements IColorableBlockEntity {
    private static final String COLOR_KEY = "TEColor";
    private static final String ATTACHMENT_KEY = "TEAttachSide";
    private static final String FORCED_KEY = "ColorForced";

    private final IActionSource source = new MachineSource(this);
    private @Nullable Direction attachmentSide;
    private boolean colorForced;
    private AEColor color = AEColor.TRANSPARENT;
    private boolean active;

    protected ProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, double idlePower) {
        super(type, pos, state);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(idlePower);
    }

    public boolean isActive() {
        return isClientSide() ? active : getMainNode().isActive();
    }

    @Override
    public AEColor getColor() {
        return color;
    }

    protected IActionSource source() {
        return source;
    }

    protected @Nullable IGrid activeGrid() {
        return getMainNode().isActive() ? getMainNode().getGrid() : null;
    }

    protected long networkAmount(Aspect aspect) {
        IGrid grid = activeGrid();
        return grid == null ? 0 : grid.getStorageService().getCachedInventory().get(EssentiaKey.of(aspect));
    }

    protected int extractFromNetwork(Aspect aspect, int amount, boolean mustMatch) {
        IGrid grid = activeGrid();
        if (grid == null) {
            return 0;
        }
        long available = EssentiaNetwork.extract(grid, aspect, amount, Actionable.SIMULATE, source);
        if (available == 0 || mustMatch && available != amount) {
            return 0;
        }
        return (int) EssentiaNetwork.extract(grid, aspect, available, Actionable.MODULATE, source);
    }

    protected long insertIntoNetwork(Aspect aspect, int amount, Actionable mode) {
        IGrid grid = activeGrid();
        return grid == null ? 0 : EssentiaNetwork.insert(grid, aspect, amount, mode, source);
    }

    protected void onPowerChange(boolean powered) {
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        boolean now = getMainNode().isActive();
        if (now != active) {
            active = now;
            onPowerChange(now);
        }
        checkGridConnectionColor();
        markForUpdate();
    }

    @Override
    public void onReady() {
        super.onReady();
        checkGridConnectionColor();
    }

    @Override
    public void markForUpdate() {
        if (level != null && !level.isClientSide() && !isRemoved()) {
            BlockState state = getBlockState();
            if (state.hasProperty(ProviderBlock.ACTIVE) && state.getValue(ProviderBlock.ACTIVE) != active) {
                level.setBlockAndUpdate(worldPosition, state.setValue(ProviderBlock.ACTIVE, active));
            }
        }
        super.markForUpdate();
    }

    private @Nullable AEColor neighborColor(Direction side) {
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
        return neighbor instanceof IPartHost host ? host.getColor() : null;
    }

    public void checkGridConnectionColor() {
        if (level == null || level.isClientSide() || colorForced) {
            return;
        }
        if (attachmentSide != null) {
            AEColor attached = neighborColor(attachmentSide);
            if (attached != null) {
                if (attached != color) {
                    setProviderColor(attached);
                }
                return;
            }
        }
        for (Direction side : Direction.values()) {
            AEColor neighbor = neighborColor(side);
            if (neighbor == null) {
                continue;
            }
            if (neighbor == color) {
                attachmentSide = side;
                saveChanges();
                return;
            }
            if (color == AEColor.TRANSPARENT) {
                attachmentSide = side;
                setProviderColor(neighbor);
                return;
            }
        }
        attachmentSide = null;
        setProviderColor(AEColor.TRANSPARENT);
    }

    private void setProviderColor(AEColor newColor) {
        if (color != newColor) {
            color = newColor;
            getMainNode().setGridColor(newColor);
            markForUpdate();
            saveChanges();
        }
    }

    @Override
    public boolean recolourBlock(Direction side, AEColor newColor, Player player) {
        colorForced = true;
        setProviderColor(newColor);
        return true;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeEnum(color);
        data.writeBoolean(isActive());
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        AEColor oldColor = color;
        boolean oldActive = active;
        color = data.readEnum(AEColor.class);
        active = data.readBoolean();
        return changed || oldColor != color || oldActive != active;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        colorForced = tag.getBoolean(FORCED_KEY);
        color = AEColor.values()[Math.floorMod(tag.getInt(COLOR_KEY), AEColor.values().length)];
        if (!tag.contains(COLOR_KEY)) {
            color = AEColor.TRANSPARENT;
        }
        getMainNode().setGridColor(color);
        attachmentSide = tag.contains(ATTACHMENT_KEY) ? Direction.from3DDataValue(tag.getInt(ATTACHMENT_KEY)) : null;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(COLOR_KEY, color.ordinal());
        if (attachmentSide != null) {
            tag.putInt(ATTACHMENT_KEY, attachmentSide.get3DDataValue());
        }
        tag.putBoolean(FORCED_KEY, colorForced);
    }
}
