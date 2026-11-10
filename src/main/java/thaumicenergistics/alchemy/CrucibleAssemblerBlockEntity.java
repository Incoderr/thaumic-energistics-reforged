package thaumicenergistics.alchemy;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.MachineSource;
import thaumicenergistics.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

public final class CrucibleAssemblerBlockEntity extends AENetworkedBlockEntity implements ICraftingProvider, IUpgradeableObject {
    public static final int MAX_SPEED_UPGRADES = 4;
    public static final int CORE_SLOT = 0;
    private static final String UPGRADES_KEY = "upgrades";
    private static final String CRAFTING_KEY = "is_crafting";
    private static final String PATTERN_KEY = "pattern";
    private static final String PROGRESS_KEY = "progress";
    private static final String ITEMS_KEY = "items";

    private final SimpleContainer inventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            coreChanged = true;
            saveChanges();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return AlchemyCore.isCore(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final IUpgradeInventory upgrades;
    private final IActionSource source = new MachineSource(this);
    private List<IPatternDetails> patterns = List.of();
    private @Nullable CrucibleAssemblerPattern currentPattern;
    private boolean crafting;
    private int craftTickCounter;
    private boolean coreChanged = true;
    private boolean wasEnabled = true;
    private boolean unloading;

    public CrucibleAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyFeature.ASSEMBLER_ENTITY.get(), pos, state);
        upgrades = UpgradeInventories.forMachine(AlchemyFeature.ASSEMBLER.get(), MAX_SPEED_UPGRADES, this::onUpgradesChanged);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(0.0D).addService(ICraftingProvider.class, this);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public boolean isCrafting() {
        return crafting;
    }

    public ItemStack target() {
        return currentPattern == null ? ItemStack.EMPTY : currentPattern.pattern().result();
    }

    public int ticksPerCraft() {
        return Config.ticksPerCraft(Config.CRUCIBLE_ASSEMBLER_TICKS, getInstalledUpgrades(AEItems.SPEED_CARD), MAX_SPEED_UPGRADES);
    }

    public float percentComplete() {
        return crafting ? Math.min((float) craftTickCounter / ticksPerCraft(), 1.0F) : 0.0F;
    }

    public boolean stillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private void onUpgradesChanged() {
        saveChanges();
        markForUpdate();
    }

    private static boolean enabled() {
        return Config.flag(Config.CRUCIBLE_ASSEMBLER_ENABLED);
    }

    private void updatePatterns() {
        coreChanged = false;
        wasEnabled = enabled();
        List<IPatternDetails> updated = new ArrayList<>();
        for (CruciblePattern pattern : AlchemyCore.patterns(inventory.getItem(CORE_SLOT))) {
            if (wasEnabled && pattern.isValid()) {
                updated.add(new CrucibleAssemblerPattern(pattern));
            }
        }
        if (!updated.equals(patterns)) {
            patterns = List.copyOf(updated);
            ICraftingProvider.requestUpdate(getMainNode());
        }
    }

    private void craftingTick(IGrid grid) {
        if (currentPattern == null) {
            crafting = false;
            return;
        }
        if (craftTickCounter >= ticksPerCraft()) {
            GenericStack output = currentPattern.getPrimaryOutput();
            var storage = grid.getStorageService().getInventory();
            if (storage.insert(output.what(), output.amount(), Actionable.SIMULATE, source) != output.amount()) {
                return;
            }
            storage.insert(output.what(), output.amount(), Actionable.MODULATE, source);
            crafting = false;
            currentPattern = null;
            craftTickCounter = 0;
            saveChanges();
            markForUpdate();
            return;
        }
        double power = Config.amount(Config.CRUCIBLE_ASSEMBLER_POWER);
        double required = power + power * getInstalledUpgrades(AEItems.SPEED_CARD) / 2.0D;
        if (grid.getEnergyService().extractAEPower(required, Actionable.MODULATE, PowerMultiplier.CONFIG) >= required - 0.01D) {
            craftTickCounter++;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleAssemblerBlockEntity assembler) {
        if (assembler.coreChanged || assembler.wasEnabled != enabled()) {
            assembler.updatePatterns();
        }
        IGrid grid = assembler.getMainNode().getGrid();
        if (assembler.crafting && assembler.getMainNode().isActive() && grid != null) {
            assembler.craftingTick(grid);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrucibleAssemblerBlockEntity assembler) {
        if (assembler.crafting) {
            assembler.craftTickCounter++;
        }
    }

    public int clientTicks() {
        return craftTickCounter;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (crafting || !enabled() || !(patternDetails instanceof CrucibleAssemblerPattern pattern) || !patterns.contains(pattern)) {
            return false;
        }
        crafting = true;
        craftTickCounter = 0;
        currentPattern = pattern;
        saveChanges();
        markForUpdate();
        return true;
    }

    @Override
    public boolean isBusy() {
        return crafting;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (!unloading && !isRemoved()) {
            markForUpdate();
        }
    }

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        super.onChunkUnloaded();
    }

    public List<ItemStack> drops() {
        List<ItemStack> drops = new ArrayList<>();
        if (!inventory.getItem(CORE_SLOT).isEmpty()) {
            drops.add(inventory.getItem(CORE_SLOT));
        }
        for (ItemStack upgrade : upgrades) {
            drops.add(upgrade);
        }
        return drops;
    }

    @Override
    protected boolean readFromStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean wasCrafting = crafting;
        crafting = data.readBoolean();
        if (crafting && !wasCrafting) {
            craftTickCounter = 0;
        }
        return changed || wasCrafting != crafting;
    }

    @Override
    protected void writeToStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(crafting);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        upgrades.readFromNBT(tag, UPGRADES_KEY, registries);
        inventory.clearContent();
        if (tag.contains(ITEMS_KEY)) {
            var items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound(ITEMS_KEY), items, registries);
            inventory.getItems().set(CORE_SLOT, items.get(CORE_SLOT));
        }
        crafting = tag.getBoolean(CRAFTING_KEY);
        craftTickCounter = tag.getInt(PROGRESS_KEY);
        currentPattern = null;
        if (tag.contains(PATTERN_KEY)) {
            CruciblePattern.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(PATTERN_KEY)).result()
                .filter(CruciblePattern::isValid)
                .ifPresent(pattern -> currentPattern = new CrucibleAssemblerPattern(pattern));
        }
        if (currentPattern == null) {
            crafting = false;
        }
        coreChanged = true;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        upgrades.writeToNBT(tag, UPGRADES_KEY, registries);
        CompoundTag items = new CompoundTag();
        ContainerHelper.saveAllItems(items, inventory.getItems(), registries);
        tag.put(ITEMS_KEY, items);
        tag.putBoolean(CRAFTING_KEY, crafting);
        tag.putInt(PROGRESS_KEY, craftTickCounter);
        if (currentPattern != null) {
            CruciblePattern.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), currentPattern.pattern()).result()
                .ifPresent(encoded -> tag.put(PATTERN_KEY, encoded));
        }
    }
}
