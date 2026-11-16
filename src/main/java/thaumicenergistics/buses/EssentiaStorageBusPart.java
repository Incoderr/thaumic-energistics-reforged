package thaumicenergistics.buses;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import appeng.api.AECapabilities;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.core.definitions.AEItems;
import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.UpgradeablePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaTileContainers;

public final class EssentiaStorageBusPart extends UpgradeablePart implements IGridTickable, IStorageProvider, IPriorityHost, EssentiaBusHost {
    public static final Identifier MODEL_BASE = ThaumicEnergistics.id("part/essentia_storage_bus_base");
    public static final Identifier MODEL_OFF = ThaumicEnergistics.id("part/essentia_storage_bus_off");
    public static final Identifier MODEL_ON = ThaumicEnergistics.id("part/essentia_storage_bus_on");
    private static final double IDLE_POWER_DRAIN = 1.0;
    private static final int[] FILTER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final String PRIORITY_KEY = "Priority";
    private static final String FILTER_KEY = "FilterAspects#";
    private static final String VOID_KEY = "IsVoidAllowed";

    private final Aspect[] filters = new Aspect[FILTER_SIZE];
    private final EssentiaBusStorage storage = new EssentiaBusStorage(this);
    private int priority;
    private boolean voidAllowed;
    private boolean wasOnline;

    public EssentiaStorageBusPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN).addService(IStorageProvider.class, this).addService(IGridTickable.class, this);
    }

    @Override
    protected int getUpgradeSlots() {
        return 1;
    }

    public boolean isInverted() {
        return isUpgradedWith(AEItems.INVERTER_CARD);
    }

    public boolean allowAny() {
        for (Aspect filter : filters) {
            if (filter != null) {
                return false;
            }
        }
        return true;
    }

    MEStorage storage() {
        return storage;
    }

    public @Nullable AspectContainer connectedContainer() {
        return storage.container();
    }

    public @Nullable MEStorage connectedStorage() {
        return storage.external();
    }

    public void updateTarget() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos target = getBlockEntity().getBlockPos().relative(getSide());
        if (!level.isLoaded(target)) {
            storage.setTarget(null, null);
            return;
        }
        MEStorage external = level.getCapability(AECapabilities.ME_STORAGE, target, getSide().getOpposite());
        AspectContainer container = external == null ? EssentiaTileContainers.container(level.getBlockEntity(target)) : null;
        storage.setTarget(container, external);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        updateTarget();
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        if (!isClientSide() && pos.relative(getSide()).equals(neighbor)) {
            updateTarget();
        }
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        boolean online = getMainNode().isOnline();
        if (online != wasOnline) {
            wasOnline = online;
            getHost().markForUpdate();
            IStorageProvider.requestUpdate(getMainNode());
        }
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        if (getMainNode().isOnline()) {
            mounts.mount(storage, priority);
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        updateTarget();
        return TickRateModulation.SAME;
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
    public int[] availableFilterSlots() {
        return FILTER_SLOTS;
    }

    @Override
    public boolean supportsVoid() {
        return true;
    }

    @Override
    public boolean isVoidAllowed() {
        return voidAllowed;
    }

    @Override
    public void setVoidAllowed(boolean allowed) {
        voidAllowed = allowed;
        getHost().markForSave();
    }

    @Override
    public boolean isStorageBus() {
        return true;
    }

    @Override
    public boolean isRedstoneControlled() {
        return false;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
        getHost().markForSave();
        IStorageProvider.requestUpdate(getMainNode());
    }

    @Override
    public void openPriority(Player player) {
        MenuOpener.open(PriorityMenu.TYPE, player, MenuLocators.forPart(this));
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(BusesFeature.MENU.get(), player, MenuLocators.forPart(this));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(BusesFeature.MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(1, 1, 15, 15, 15, 16);
        helper.addBox(4, 4, 14, 12, 12, 15);
        helper.addBox(5, 5, 13, 11, 11, 14);
    }


    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 3;
    }

    @Override
    public void readFromNBT(ValueInput tag) {
        super.readFromNBT(tag);
        priority = tag.getIntOr(PRIORITY_KEY, 0);
        for (int index = 0; index < FILTER_SIZE; index++) {
            filters[index] = tag.getString(FILTER_KEY + index).map(Aspect::getAspect).orElse(null);
        }
        voidAllowed = tag.getBooleanOr(VOID_KEY, false);
    }

    @Override
    public void writeToNBT(ValueOutput tag) {
        super.writeToNBT(tag);
        if (priority != 0) {
            tag.putInt(PRIORITY_KEY, priority);
        }
        for (int index = 0; index < FILTER_SIZE; index++) {
            if (filters[index] != null) {
                tag.putString(FILTER_KEY + index, filters[index].getTag());
            }
        }
        if (voidAllowed) {
            tag.putBoolean(VOID_KEY, true);
        }
    }
}
