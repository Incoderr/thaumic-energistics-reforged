package thaumicenergistics.act;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import appeng.api.inventories.InternalInventory;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.parts.reporting.AbstractTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.visrelay.DigiVisSource;
import thaumicenergistics.visrelay.VisRelayFeature;
import thaumicenergistics.visrelay.VisSourceLink;

public final class ArcaneCraftingTerminalPart extends AbstractTerminalPart implements IGridTickable {
    public static final Identifier INV_CRAFTING = ThaumicEnergistics.id("arcane_crafting_grid");
    public static final Identifier INV_WAND = ThaumicEnergistics.id("arcane_crafting_wand");
    public static final Identifier INV_ARMOR = ThaumicEnergistics.id("arcane_crafting_armor");
    public static final Identifier MODEL_BASE = ThaumicEnergistics.id("part/arcane_crafting_terminal_base");
    public static final Identifier MODEL_OFF = ThaumicEnergistics.id("part/arcane_crafting_terminal_off");
    public static final Identifier MODEL_ON = ThaumicEnergistics.id("part/arcane_crafting_terminal_on");
    public static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final double IDLE_POWER_DRAIN = 0.5;
    private static final String NBT_KEY_VIS_INTERFACE = "VisInterface";
    private static final TickingRequest TICKING = new TickingRequest(2, 20, false);

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
    private final VisSourceLink visSourceInfo = new VisSourceLink();

    public ArcaneCraftingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN);
        getMainNode().addService(IGridTickable.class, this);
    }

    public static boolean isValidWand(ItemStack stack) {
        return stack.getItem() instanceof WandItem && !Wands.isStaff(stack);
    }

    public static boolean isArmorFor(ItemStack stack, int slot) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == ARMOR_SLOTS.get(slot);
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
    public boolean onUseItemOn(ItemStack held, Player player, InteractionHand hand, Vec3 position) {
        if (isClientSide()) {
            return true;
        }
        if (held.getItem() instanceof IMemoryCard card) {
            CompoundTag data = VisRelayFeature.LINK.get(held);
            if (data != null) {
                visSourceInfo.readFromTag(data);
                if (visSourceInfo.hasSourceData()) {
                    card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                }
                getHost().markForSave();
            } else if (held.get(AEComponents.EXPORTED_SETTINGS_SOURCE) == null) {
                visSourceInfo.clearData();
                card.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
                getHost().markForSave();
            }
            return true;
        }
        return super.onUseItemOn(held, player, hand, position);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return TICKING;
    }

    /**
     * Charges the wand from the linked vis source.
     */
    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!visSourceInfo.hasSourceData()) {
            return TickRateModulation.IDLE;
        }
        ItemStack stack = wand.getStackInSlot(0);
        if (stack.isEmpty()) {
            return TickRateModulation.IDLE;
        }
        AspectList neededVis = Wands.getAspectsWithRoom(stack);
        if (neededVis.isEmpty()) {
            return TickRateModulation.IDLE;
        }
        DigiVisSource visSource = visSourceInfo.tryGetSource(getMainNode().getGrid());
        if (visSource == null) {
            return TickRateModulation.IDLE;
        }
        boolean charged = false;
        for (Aspect vis : neededVis.getAspects()) {
            int amountToDrain = Wands.getMaxVis(stack) - Wands.getVis(stack, vis);
            int amountDrained = visSource.consumeVis(vis, amountToDrain);
            if (amountDrained > 0) {
                Wands.addRealVis(stack, vis, amountDrained, true);
                charged = true;
            }
        }
        if (charged) {
            getHost().markForSave();
        }
        return TickRateModulation.URGENT;
    }


    @Override
    public InternalInventory getSubInventory(Identifier id) {
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
    public void readFromNBT(ValueInput tag) {
        super.readFromNBT(tag);
        craftingGrid.readFromNBT(tag, "craftingGrid");
        wand.readFromNBT(tag, "wand");
        armor.readFromNBT(tag, "armor");
        visSourceInfo.readFromTag(tag, NBT_KEY_VIS_INTERFACE);
    }

    @Override
    public void writeToNBT(ValueOutput tag) {
        super.writeToNBT(tag);
        craftingGrid.writeToNBT(tag, "craftingGrid");
        wand.writeToNBT(tag, "wand");
        armor.writeToNBT(tag, "armor");
        visSourceInfo.writeToTag(tag, NBT_KEY_VIS_INTERFACE);
    }
}
