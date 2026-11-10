package thaumicenergistics.assembler;

import appeng.api.upgrades.IUpgradeInventory;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.VisDiscountGear;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeCore;

public final class ArcaneAssemblerMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_Y = 115;
    private static final int HOTBAR_Y = PLAYER_INV_Y + 58;
    private static final int PATTERN_SLOT_X = 26;
    private static final int PATTERN_SLOT_Y = 25;
    private static final int PATTERN_COLS = 7;
    private static final int KCORE_SLOT_X = 187;
    private static final int KCORE_SLOT_Y = 8;
    private static final int UPGRADE_SLOT_X = 187;
    private static final int UPGRADE_SLOT_Y = 26;
    private static final int TARGET_SLOT_X = 14;
    private static final int TARGET_SLOT_Y = 87;
    private static final int ARMOR_SLOT_X = 210;
    private static final int ARMOR_SLOT_Y = 26;
    private static final EquipmentSlot[] ARMOR_TYPES = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player player;
    private final @Nullable ArcaneAssemblerBlockEntity assembler;
    private final SimpleContainer patterns = new SimpleContainer(KnowledgeCore.MAXIMUM_STORED_PATTERNS);
    private final SimpleContainer target = new SimpleContainer(1);
    private final Slot coreSlot;
    private final Slot[] armorSlots = new Slot[ArcaneAssemblerBlockEntity.ARMOR_COUNT];
    private final int firstUpgradeSlot;
    private final int playerStart;
    private final DataSlot[] vis = new DataSlot[ArcaneAssemblerBlockEntity.PRIMALS.length];
    private final DataSlot[] discount = new DataSlot[ArcaneAssemblerBlockEntity.PRIMALS.length];
    private final DataSlot progress = DataSlot.standalone();

    public ArcaneAssemblerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(AssemblerFeature.MENU.get(), containerId);
        this.player = inventory.player;
        this.assembler = player.level().getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity found ? found : null;
        Container internal = assembler != null ? assembler.inventory() : new SimpleContainer(1 + ArcaneAssemblerBlockEntity.ARMOR_COUNT);
        coreSlot = addSlot(new Slot(internal, ArcaneAssemblerBlockEntity.KCORE_SLOT, KCORE_SLOT_X, KCORE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KnowledgeCore.isCore(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int index = 0; index < KnowledgeCore.MAXIMUM_STORED_PATTERNS; index++) {
            addSlot(new DisplaySlot(patterns, index, PATTERN_SLOT_X + index % PATTERN_COLS * 18, PATTERN_SLOT_Y + index / PATTERN_COLS * 18));
        }
        firstUpgradeSlot = slots.size();
        if (assembler != null) {
            IUpgradeInventory upgrades = assembler.getUpgrades();
            Container upgradeContainer = upgrades.toContainer();
            for (int index = 0; index < upgrades.size(); index++) {
                int slot = index;
                addSlot(new Slot(upgradeContainer, index, UPGRADE_SLOT_X, UPGRADE_SLOT_Y + index * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return upgrades.isItemValid(slot, stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
        addSlot(new DisplaySlot(target, 0, TARGET_SLOT_X, TARGET_SLOT_Y));
        for (int index = 0; index < ArcaneAssemblerBlockEntity.ARMOR_COUNT; index++) {
            EquipmentSlot type = ARMOR_TYPES[index];
            armorSlots[index] = addSlot(new Slot(internal, ArcaneAssemblerBlockEntity.ARMOR_SLOT + index, ARMOR_SLOT_X, ARMOR_SLOT_Y + index * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return player.getEquipmentSlotForItem(stack) == type;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        playerStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        for (int index = 0; index < vis.length; index++) {
            vis[index] = addDataSlot(DataSlot.standalone());
            discount[index] = addDataSlot(DataSlot.standalone());
        }
        addDataSlot(progress);
        refresh();
    }

    private static final class DisplaySlot extends Slot {
        DisplaySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private void refresh() {
        if (assembler == null || player.level().isClientSide()) {
            return;
        }
        List<ArcanePattern> stored = KnowledgeCore.patterns(coreSlot.getItem());
        for (int index = 0; index < patterns.getContainerSize(); index++) {
            ItemStack shown = index < stored.size() ? stored.get(index).result() : ItemStack.EMPTY;
            if (!ItemStack.matches(shown, patterns.getItem(index))) {
                patterns.setItem(index, shown.copy());
            }
        }
        if (!ItemStack.matches(assembler.target(), target.getItem(0))) {
            target.setItem(0, assembler.target().copy());
        }
        for (int index = 0; index < vis.length; index++) {
            Aspect aspect = ArcaneAssemblerBlockEntity.PRIMALS[index];
            vis[index].set(assembler.storedVis().getAmount(aspect));
            discount[index].set(Math.round(assembler.visDiscount(aspect) * 1000.0F));
        }
        progress.set(Math.round(assembler.percentComplete() * 1000.0F));
    }

    public int storedVis(int primal) {
        return vis[primal].get();
    }

    public float visDiscount(int primal) {
        return discount[primal].get() / 1000.0F;
    }

    public float percentComplete() {
        return progress.get() / 1000.0F;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(clicker)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        boolean moved = false;
        if (index >= playerStart) {
            if (coreSlot.mayPlace(stack) && !coreSlot.hasItem()) {
                moved = moveItemStackTo(stack, coreSlot.index, coreSlot.index + 1, false);
            } else if (stack.getItem() instanceof VisDiscountGear) {
                for (Slot armor : armorSlots) {
                    if (armor.mayPlace(stack)) {
                        if (!armor.hasItem()) {
                            armor.setByPlayer(stack.split(1));
                            moved = true;
                        }
                        break;
                    }
                }
            }
            if (!moved) {
                moved = index < playerStart + 27 ? moveItemStackTo(stack, playerStart + 27, slots.size(), false)
                    : moveItemStackTo(stack, playerStart, playerStart + 27, false);
            }
        } else if (slot == coreSlot || List.of(armorSlots).contains(slot) || index >= firstUpgradeSlot && slot.container != target) {
            moved = moveItemStackTo(stack, playerStart + 27, slots.size(), false) || moveItemStackTo(stack, playerStart, playerStart + 27, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return assembler != null && assembler.stillValid(clicker);
    }
}
