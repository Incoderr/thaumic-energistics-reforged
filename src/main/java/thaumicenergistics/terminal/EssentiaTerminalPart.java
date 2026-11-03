package thaumicenergistics.terminal;

import appeng.api.config.ViewItems;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaContainers;

public final class EssentiaTerminalPart extends AbstractTerminalPart implements EssentiaTerminalHost {
    public static final ResourceLocation MODEL_TERMINAL_BASE = ThaumicEnergistics.id("part/essentia_terminal_base");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/essentia_terminal_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/essentia_terminal_on");
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_TERMINAL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_TERMINAL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_TERMINAL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private static final double IDLE_POWER_DRAIN = 0.5;
    private static final String SLOTS_KEY = "slots";
    private static final String SORT_KEY = "sortMode";
    private static final String VIEW_KEY = "ViewMode";
    private static final String SELECTED_KEY = "selectedAspect";

    private final AppEngInternalInventory workInventory = new AppEngInternalInventory(this, 2, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return EssentiaContainers.isContainer(stack) || EssentiaContainers.isLabel(stack);
        }
    });
    private EssentiaSortMode sortMode = EssentiaSortMode.ALPHABETIC;
    private ViewItems viewMode = ViewItems.ALL;
    private @Nullable Aspect selectedAspect;

    public EssentiaTerminalPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN);
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return TerminalFeature.MENU.get();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    @Override
    public @Nullable IGrid getEssentiaGrid() {
        return getMainNode().isActive() ? getMainNode().getGrid() : null;
    }

    @Override
    public InternalInventory getWorkInventory() {
        return workInventory;
    }

    @Override
    public @Nullable Aspect getSelectedAspect() {
        return selectedAspect;
    }

    @Override
    public void setSelectedAspect(@Nullable Aspect aspect) {
        selectedAspect = aspect;
        getHost().markForSave();
    }

    @Override
    public EssentiaSortMode getSortMode() {
        return sortMode;
    }

    @Override
    public void setSortMode(EssentiaSortMode mode) {
        sortMode = mode;
        getHost().markForSave();
    }

    @Override
    public ViewItems getViewMode() {
        return viewMode;
    }

    @Override
    public void setViewMode(ViewItems mode) {
        viewMode = mode;
        getHost().markForSave();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inventory) {
        if (inventory == workInventory) {
            getHost().markForSave();
        } else {
            super.saveChangedInventory(inventory);
        }
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inventory, int slot) {
        if (inventory != workInventory) {
            super.onChangeInventory(inventory, slot);
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (ItemStack stack : workInventory) {
            drops.add(stack);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        workInventory.clear();
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        workInventory.readFromNBT(tag, SLOTS_KEY, registries);
        if (tag.contains(SORT_KEY)) {
            sortMode = EssentiaSortMode.values()[Math.floorMod(tag.getInt(SORT_KEY), EssentiaSortMode.values().length)];
        }
        if (tag.contains(VIEW_KEY)) {
            viewMode = ViewItems.values()[Math.floorMod(tag.getInt(VIEW_KEY), ViewItems.values().length)];
        }
        selectedAspect = tag.contains(SELECTED_KEY) ? Aspect.getAspect(tag.getString(SELECTED_KEY)) : null;
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        workInventory.writeToNBT(tag, SLOTS_KEY, registries);
        tag.putInt(SORT_KEY, sortMode.ordinal());
        tag.putInt(VIEW_KEY, viewMode.ordinal());
        if (selectedAspect != null) {
            tag.putString(SELECTED_KEY, selectedAspect.getTag());
        }
    }
}
