package thaumicenergistics.act;

import appeng.api.inventories.InternalInventory;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumicenergistics.ThaumicEnergistics;

public final class ArcaneCraftingTerminalPart extends AbstractTerminalPart {
    public static final ResourceLocation INV_CRAFTING = ThaumicEnergistics.id("arcane_crafting_grid");
    public static final ResourceLocation INV_WAND = ThaumicEnergistics.id("arcane_crafting_wand");
    public static final ResourceLocation INV_ARMOR = ThaumicEnergistics.id("arcane_crafting_armor");
    public static final ResourceLocation MODEL_BASE = ThaumicEnergistics.id("part/arcane_crafting_terminal_base");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/arcane_crafting_terminal_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/arcane_crafting_terminal_on");
    public static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);
    private static final double IDLE_POWER_DRAIN = 0.5;

    private final AppEngInternalInventory craftingGrid = new AppEngInternalInventory(this, 9);
    private final AppEngInternalInventory wand = new AppEngInternalInventory(this, 1, 1, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return isValidWand(stack);
        }
    });
    private final AppEngInternalInventory armor = new AppEngInternalInventory(this, 4, 1, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return isArmorFor(stack, slot);
        }
    });

    public ArcaneCraftingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN);
    }

    public static boolean isValidWand(ItemStack stack) {
        return stack.getItem() instanceof WandItem && !Wands.isStaff(stack);
    }

    public static boolean isArmorFor(ItemStack stack, int slot) {
        Equipable equipable = Equipable.get(stack);
        return equipable != null && equipable.getEquipmentSlot() == ARMOR_SLOTS.get(slot);
    }

    public InternalInventory craftingGrid() {
        return craftingGrid;
    }

    public InternalInventory wandInventory() {
        return wand;
    }

    public InternalInventory armorInventory() {
        return armor;
    }

    public void swapStoredArmor(Player player) {
        for (int index = 0; index < ARMOR_SLOTS.size(); index++) {
            EquipmentSlot slot = ARMOR_SLOTS.get(index);
            ItemStack stored = armor.getStackInSlot(index);
            ItemStack worn = player.getItemBySlot(slot);
            player.setItemSlot(slot, stored);
            armor.setItemDirect(index, worn);
        }
        player.inventoryMenu.broadcastChanges();
        getHost().markForSave();
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return ActFeature.MENU.get();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    @Override
    public IPartModel getStaticModels() {
        return isActive() ? MODELS_ON : MODELS_OFF;
    }

    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(INV_CRAFTING)) {
            return craftingGrid;
        }
        if (id.equals(INV_WAND)) {
            return wand;
        }
        if (id.equals(INV_ARMOR)) {
            return armor;
        }
        return super.getSubInventory(id);
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inventory) {
        if (inventory == craftingGrid || inventory == wand || inventory == armor) {
            getHost().markForSave();
        } else {
            super.saveChangedInventory(inventory);
        }
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inventory, int slot) {
        if (inventory != craftingGrid && inventory != wand && inventory != armor) {
            super.onChangeInventory(inventory, slot);
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (InternalInventory inventory : List.of(craftingGrid, wand, armor)) {
            for (ItemStack stack : inventory) {
                drops.add(stack);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        craftingGrid.clear();
        wand.clear();
        armor.clear();
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        craftingGrid.readFromNBT(tag, "craftingGrid", registries);
        wand.readFromNBT(tag, "wand", registries);
        armor.readFromNBT(tag, "armor", registries);
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        craftingGrid.writeToNBT(tag, "craftingGrid", registries);
        wand.writeToNBT(tag, "wand", registries);
        armor.writeToNBT(tag, "armor", registries);
    }
}
