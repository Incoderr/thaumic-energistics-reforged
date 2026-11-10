package thaumicenergistics.assembler;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.wand.VisDiscountGear;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeCore;
import thaumicenergistics.visrelay.DigiVisSource;
import thaumicenergistics.visrelay.VisSourceLink;

public final class ArcaneAssemblerBlockEntity extends AENetworkedBlockEntity implements ICraftingProvider, IUpgradeableObject {
    public static final Aspect[] PRIMALS = {Aspect.AIR, Aspect.WATER, Aspect.FIRE, Aspect.ORDER, Aspect.ENTROPY, Aspect.EARTH};
    public static final int CVIS_MULTIPLIER = 10;
    public static final int MAX_STORED_CVIS = 150 * CVIS_MULTIPLIER;
    public static final int BASE_TICKS_PER_CRAFT = 20;
    public static final int MAX_SPEED_UPGRADES = 4;
    public static final int KCORE_SLOT = 0;
    public static final int ARMOR_SLOT = 1;
    public static final int ARMOR_COUNT = 4;
    public static final double IDLE_POWER = 0.0D;
    public static final double ACTIVE_POWER = 1.5D;
    public static final double WARP_POWER_PERCENT = 0.15D;
    private static final String VIS_INTERFACE_KEY = "vis_interface";
    public static final String STORED_VIS_KEY = "stored_vis";
    private static final String UPGRADES_KEY = "upgrades";
    private static final String CRAFTING_KEY = "is_crafting";
    private static final String PATTERN_KEY = "pattern";
    private static final String PROGRESS_KEY = "progress";
    private static final String ITEMS_KEY = "items";
    private static final ItemStack SCEPTRE = Wands.create(WandRegistry.CAP_THAUMIUM, WandRegistry.ROD_SILVERWOOD, true);

    private final SimpleContainer inventory = new SimpleContainer(1 + ARMOR_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            onInventoryChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot != KCORE_SLOT || KnowledgeCore.isCore(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final IUpgradeInventory upgrades;
    private final IActionSource source = new MachineSource(this);
    private final VisSourceLink visSourceInfo = new VisSourceLink();
    private final Map<Aspect, Float> visDiscount = new HashMap<>();
    private AspectList storedVis = new AspectList();
    private List<IPatternDetails> patterns = List.of();
    private @Nullable AssemblerPattern currentPattern;
    private boolean crafting;
    private int craftTickCounter;
    private int visTickCounter;
    private int delayTickCounter;
    private boolean delayedUpdate;
    private boolean coreChanged = true;
    private boolean recalcVis = true;
    private float warpPowerMultiplier = 1.0F;
    private boolean unloading;

    public ArcaneAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(AssemblerFeature.ENTITY.get(), pos, state);
        upgrades = UpgradeInventories.forMachine(AssemblerFeature.BLOCK.get(), MAX_SPEED_UPGRADES, this::onUpgradesChanged);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(IDLE_POWER).addService(ICraftingProvider.class, this);
        for (Aspect primal : PRIMALS) {
            visDiscount.put(primal, sceptreModifier(primal));
        }
    }

    public static AspectList fullVis() {
        AspectList vis = new AspectList();
        for (Aspect primal : PRIMALS) {
            vis.add(primal, MAX_STORED_CVIS);
        }
        return vis;
    }

    private static float sceptreModifier(Aspect aspect) {
        return ((WandItem) SCEPTRE.getItem()).getConsumptionModifier(SCEPTRE, null, aspect, true);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public AspectList storedVis() {
        return storedVis;
    }

    public void setStoredVis(AspectList vis) {
        storedVis = vis.copy();
        setChanged();
    }

    public float visDiscount(Aspect aspect) {
        return visDiscount.getOrDefault(aspect, 1.0F);
    }

    public boolean isCrafting() {
        return crafting;
    }

    public int craftTickCounter() {
        return craftTickCounter;
    }

    public ItemStack target() {
        return currentPattern == null ? ItemStack.EMPTY : currentPattern.pattern().result();
    }

    public int upgradeCount() {
        return getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public int ticksPerCraft() {
        return BASE_TICKS_PER_CRAFT - 4 * upgradeCount();
    }

    public float percentComplete() {
        return crafting ? Math.min((float) craftTickCounter / ticksPerCraft(), 1.0F) : 0.0F;
    }

    public boolean stillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private void onInventoryChanged() {
        coreChanged = true;
        recalcVis = true;
        saveChanges();
    }

    private void onUpgradesChanged() {
        saveChanges();
        markForUpdate();
    }

    public VisSourceLink visSourceInfo() {
        return visSourceInfo;
    }

    public void linkChanged() {
        saveChanges();
    }

    private void calculateVisDiscounts() {
        recalcVis = false;
        for (Aspect primal : PRIMALS) {
            float discount = sceptreModifier(primal);
            for (int index = 0; index < ARMOR_COUNT; index++) {
                ItemStack armor = inventory.getItem(ARMOR_SLOT + index);
                if (armor.getItem() instanceof VisDiscountGear gear) {
                    try {
                        discount -= gear.getVisDiscount(armor, null, primal) / 100.0F;
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            visDiscount.put(primal, discount);
        }
        int warp = 0;
        for (int index = 0; index < ARMOR_COUNT; index++) {
            ItemStack armor = inventory.getItem(ARMOR_SLOT + index);
            if (armor.getItem() instanceof WarpingGear gear) {
                try {
                    warp += gear.getWarp(armor, null);
                } catch (RuntimeException ignored) {
                }
            }
        }
        warpPowerMultiplier = 1.0F + (float) (warp * WARP_POWER_PERCENT);
    }

    private void updatePatterns() {
        coreChanged = false;
        List<IPatternDetails> updated = new ArrayList<>();
        for (ArcanePattern pattern : KnowledgeCore.patterns(inventory.getItem(KCORE_SLOT))) {
            if (AssemblerPattern.isValid(pattern)) {
                updated.add(new AssemblerPattern(pattern));
            }
        }
        if (!updated.equals(patterns)) {
            patterns = List.copyOf(updated);
            ICraftingProvider.requestUpdate(getMainNode());
        }
    }

    private int requiredAmount(Aspect aspect) {
        return (int) Math.ceil(visDiscount(aspect) * (currentPattern.pattern().aspects().getAmount(aspect) * CVIS_MULTIPLIER));
    }

    private boolean hasEnoughVisForCraft() {
        if (currentPattern == null) {
            return false;
        }
        for (Aspect aspect : currentPattern.pattern().aspects().getAspects()) {
            if (storedVis.getAmount(aspect) < requiredAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    private void replenishVis() {
        IGrid grid = getMainNode().getGrid();
        DigiVisSource visSource = visSourceInfo.tryGetSource(grid);
        if (visSource == null) {
            return;
        }
        for (Aspect primal : PRIMALS) {
            int needed = MAX_STORED_CVIS - storedVis.getAmount(primal);
            if (needed > 0) {
                int drained = visSource.consumeVis(primal, needed);
                if (drained > 0) {
                    storedVis.add(primal, drained);
                    delayedUpdate = true;
                }
            }
        }
    }

    private void craftingTick(IGrid grid) {
        if (currentPattern == null) {
            crafting = false;
        }
        if (!hasEnoughVisForCraft()) {
            return;
        }
        if (craftTickCounter >= ticksPerCraft()) {
            List<GenericStack> results = currentPattern.allResults();
            var storage = grid.getStorageService().getInventory();
            for (GenericStack output : results) {
                if (storage.insert(output.what(), output.amount(), Actionable.SIMULATE, source) != output.amount()) {
                    return;
                }
            }
            for (GenericStack output : results) {
                storage.insert(output.what(), output.amount(), Actionable.MODULATE, source);
            }
            crafting = false;
            currentPattern = null;
            delayedUpdate = true;
            saveChanges();
            return;
        }
        double powerRequired = (ACTIVE_POWER + ACTIVE_POWER * upgradeCount() / 2.0D) * warpPowerMultiplier;
        double powerExtracted = grid.getEnergyService().extractAEPower(powerRequired, Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (powerExtracted - powerRequired <= 0.0D) {
            craftTickCounter++;
            if (craftTickCounter >= ticksPerCraft()) {
                for (Aspect aspect : currentPattern.pattern().aspects().getAspects()) {
                    storedVis.reduce(aspect, requiredAmount(aspect));
                }
                delayedUpdate = true;
                saveChanges();
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneAssemblerBlockEntity assembler) {
        assembler.serverTick();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArcaneAssemblerBlockEntity assembler) {
        if (assembler.crafting && assembler.craftTickCounter < BASE_TICKS_PER_CRAFT) {
            assembler.craftTickCounter++;
        }
    }

    private void serverTick() {
        if (recalcVis) {
            calculateVisDiscounts();
        }
        if (coreChanged) {
            updatePatterns();
        }
        IGrid grid = getMainNode().getGrid();
        if (!getMainNode().isActive() || grid == null) {
            return;
        }
        if (visSourceInfo.hasSourceData() && ++visTickCounter == 5) {
            visTickCounter = 0;
            replenishVis();
        }
        if (delayedUpdate && ++delayTickCounter >= 5) {
            markForUpdate();
            saveChanges();
            delayedUpdate = false;
            delayTickCounter = 0;
        }
        if (crafting) {
            craftingTick(grid);
        }
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (crafting || !(patternDetails instanceof AssemblerPattern pattern) || !patterns.contains(pattern)) {
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

    @Override
    public void onReady() {
        super.onReady();
        coreChanged = true;
        if (inventory.getItem(KCORE_SLOT).isEmpty() && currentPattern != null) {
            crafting = false;
            currentPattern = null;
        }
        calculateVisDiscounts();
    }

    public List<ItemStack> drops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            if (!inventory.getItem(index).isEmpty()) {
                drops.add(inventory.getItem(index));
            }
        }
        for (ItemStack upgrade : upgrades) {
            drops.add(upgrade);
        }
        return drops;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        visSourceInfo.readFromTag(tag, VIS_INTERFACE_KEY);
        storedVis = readVis(tag);
        upgrades.readFromNBT(tag, UPGRADES_KEY, registries);
        inventory.clearContent();
        if (tag.contains(ITEMS_KEY)) {
            var items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound(ITEMS_KEY), items, registries);
            for (int index = 0; index < items.size(); index++) {
                inventory.getItems().set(index, items.get(index));
            }
        }
        crafting = tag.getBoolean(CRAFTING_KEY);
        craftTickCounter = tag.getInt(PROGRESS_KEY);
        currentPattern = null;
        if (tag.contains(PATTERN_KEY)) {
            ArcanePattern.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(PATTERN_KEY)).result()
                .filter(AssemblerPattern::isValid)
                .ifPresent(pattern -> currentPattern = new AssemblerPattern(pattern));
        }
        if (currentPattern == null) {
            crafting = false;
        }
        coreChanged = true;
        recalcVis = true;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        visSourceInfo.writeToTag(tag, VIS_INTERFACE_KEY);
        writeVis(tag, storedVis);
        upgrades.writeToNBT(tag, UPGRADES_KEY, registries);
        CompoundTag items = new CompoundTag();
        ContainerHelper.saveAllItems(items, inventory.getItems(), registries);
        tag.put(ITEMS_KEY, items);
        tag.putBoolean(CRAFTING_KEY, crafting);
        tag.putInt(PROGRESS_KEY, craftTickCounter);
        if (currentPattern != null) {
            ArcanePattern.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), currentPattern.pattern()).result()
                .ifPresent(encoded -> tag.put(PATTERN_KEY, encoded));
        }
    }

    public static AspectList readVis(CompoundTag tag) {
        if (!tag.contains(STORED_VIS_KEY, Tag.TAG_COMPOUND)) {
            return new AspectList();
        }
        return AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get(STORED_VIS_KEY)).result().orElseGet(AspectList::new);
    }

    public static void writeVis(CompoundTag tag, AspectList vis) {
        if (vis.size() > 0) {
            AspectList.CODEC.encodeStart(NbtOps.INSTANCE, vis).result().ifPresent(encoded -> tag.put(STORED_VIS_KEY, encoded));
        }
    }

}
