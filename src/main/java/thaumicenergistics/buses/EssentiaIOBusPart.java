package thaumicenergistics.buses;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManagerBuilder;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.UpgradeablePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumicenergistics.essentia.EssentiaTileContainers;

public abstract class EssentiaIOBusPart extends UpgradeablePart implements IGridTickable, EssentiaBusHost {
    private static final int BASE_TRANSFER_PER_SECOND = 4;
    private static final int ADDITIONAL_TRANSFER_PER_SECOND = 8;
    private static final int MINIMUM_TICKS_PER_OPERATION = 10;
    private static final int MAXIMUM_TICKS_PER_OPERATION = 40;
    private static final int MAXIMUM_TRANSFER_PER_SECOND = 64;
    private static final int MINIMUM_TRANSFER_PER_SECOND = 1;
    private static final int BASE_SLOT_INDEX = 4;
    private static final int[] TIER1_INDEXES = {1, 3, 5, 7};
    private static final int[] TIER2_INDEXES = {0, 2, 6, 8};
    private static final int UPGRADE_INVENTORY_SIZE = 4;
    private static final double IDLE_POWER_DRAIN = 0.7;
    private static final String FILTER_KEY = "AspectFilter#";

    protected final Aspect[] filters = new Aspect[FILTER_SIZE];
    protected final IActionSource source;
    private boolean lastRedstone;

    protected EssentiaIOBusPart(IPartItem<?> partItem) {
        super(partItem);
        source = new MachineSource(this);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN).addService(IGridTickable.class, this);
    }

    protected abstract boolean doWork(IGrid grid, int transferAmount);

    @Override
    protected void registerSettings(IConfigManagerBuilder builder) {
        super.registerSettings(builder);
        builder.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
    }

    @Override
    protected int getUpgradeSlots() {
        return UPGRADE_INVENTORY_SIZE;
    }

    @Override
    public RedstoneMode getRSMode() {
        return getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    public int filterSize() {
        return getInstalledUpgrades(AEItems.CAPACITY_CARD);
    }

    @Override
    public boolean isRedstoneControlled() {
        return isUpgradedWith(AEItems.REDSTONE_CARD);
    }

    public int transferAmountPerSecond() {
        return BASE_TRANSFER_PER_SECOND + getInstalledUpgrades(AEItems.SPEED_CARD) * ADDITIONAL_TRANSFER_PER_SECOND;
    }

    @Override
    public int[] availableFilterSlots() {
        int size = filterSize();
        int[] slots = new int[1 + Math.min(size, 2) * 4];
        slots[0] = BASE_SLOT_INDEX;
        if (size >= 1) {
            System.arraycopy(TIER1_INDEXES, 0, slots, 1, 4);
        }
        if (size >= 2) {
            System.arraycopy(TIER2_INDEXES, 0, slots, 5, 4);
        }
        return slots;
    }

    @Override
    public void upgradesChanged() {
        int size = filterSize();
        if (size < 2) {
            for (int index : TIER2_INDEXES) {
                filters[index] = null;
            }
            if (size < 1) {
                for (int index : TIER1_INDEXES) {
                    filters[index] = null;
                }
            }
        }
    }

    @Override
    public @Nullable Aspect getFilter(int index) {
        return filters[index];
    }

    @Override
    public void setFilter(int index, @Nullable Aspect aspect) {
        filters[index] = aspect;
        getHost().markForSave();
    }

    @Override
    public boolean supportsVoid() {
        return false;
    }

    @Override
    public boolean isVoidAllowed() {
        return false;
    }

    @Override
    public void setVoidAllowed(boolean allowed) {
    }

    @Override
    public boolean isStorageBus() {
        return false;
    }

    @Override
    public void openPriority(Player player) {
    }

    protected @Nullable AspectContainer facingContainer() {
        if (getLevel() == null) {
            return null;
        }
        BlockPos target = getBlockEntity().getBlockPos().relative(getSide());
        return getLevel().isLoaded(target) ? EssentiaTileContainers.container(getLevel().getBlockEntity(target)) : null;
    }

    private boolean canDoWork() {
        if (!isRedstoneControlled()) {
            return true;
        }
        return switch (getRSMode()) {
            case HIGH_SIGNAL -> getHost().hasRedstone();
            case LOW_SIGNAL -> !getHost().hasRedstone();
            case SIGNAL_PULSE -> false;
            case IGNORE -> true;
        };
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(MINIMUM_TICKS_PER_OPERATION, MAXIMUM_TICKS_PER_OPERATION, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!getMainNode().isActive() || !canDoWork()) {
            return TickRateModulation.IDLE;
        }
        int transferAmount = Mth.clamp((int) (transferAmountPerSecond() * (ticksSinceLastCall / 20.0F)), MINIMUM_TRANSFER_PER_SECOND, MAXIMUM_TRANSFER_PER_SECOND);
        return doWork(node.getGrid(), transferAmount) ? TickRateModulation.URGENT : TickRateModulation.IDLE;
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        if (isClientSide() || getRSMode() != RedstoneMode.SIGNAL_PULSE) {
            return;
        }
        boolean powered = getHost().hasRedstone();
        if (powered != lastRedstone) {
            lastRedstone = powered;
            IGrid grid = getMainNode().getGrid();
            if (grid != null && getMainNode().isActive()) {
                doWork(grid, transferAmountPerSecond());
            }
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        lastRedstone = getHost().hasRedstone();
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(BusesFeature.MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public int getLightLevel() {
        return isActive() ? 4 : 0;
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 5;
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        for (int index = 0; index < FILTER_SIZE; index++) {
            filters[index] = tag.contains(FILTER_KEY + index) ? Aspect.getAspect(tag.getString(FILTER_KEY + index)) : null;
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        for (int index = 0; index < FILTER_SIZE; index++) {
            if (filters[index] != null) {
                tag.putString(FILTER_KEY + index, filters[index].getTag());
            }
        }
    }
}
