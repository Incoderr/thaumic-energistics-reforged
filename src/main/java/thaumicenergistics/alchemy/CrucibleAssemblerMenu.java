package thaumicenergistics.alchemy;

import appeng.api.upgrades.IUpgradeInventory;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class CrucibleAssemblerMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_Y = 115;
    private static final int HOTBAR_Y = PLAYER_INV_Y + 58;
    private static final int PATTERN_SLOT_X = 26;
    private static final int PATTERN_SLOT_Y = 25;
    private static final int PATTERN_COLS = 7;
    private static final int CORE_SLOT_X = 187;
    private static final int CORE_SLOT_Y = 8;
    private static final int UPGRADE_SLOT_X = 187;
    private static final int UPGRADE_SLOT_Y = 26;
    private static final int TARGET_SLOT_X = 14;
    private static final int TARGET_SLOT_Y = 87;

    private final Player player;
    private final @Nullable CrucibleAssemblerBlockEntity assembler;
    private final SimpleContainer patterns = new SimpleContainer(AlchemyCore.MAXIMUM_STORED_PATTERNS);
    private final SimpleContainer target = new SimpleContainer(1);
    private final Slot coreSlot;
    private final int firstUpgradeSlot;
    private final int playerStart;
    private final DataSlot progress = DataSlot.standalone();

    public CrucibleAssemblerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(AlchemyFeature.ASSEMBLER_MENU.get(), containerId);
        this.player = inventory.player;
        this.assembler = player.level().getBlockEntity(pos) instanceof CrucibleAssemblerBlockEntity found ? found : null;
        Container internal = assembler != null ? assembler.inventory() : new SimpleContainer(1);
        coreSlot = addSlot(new Slot(internal, CrucibleAssemblerBlockEntity.CORE_SLOT, CORE_SLOT_X, CORE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AlchemyCore.isCore(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int index = 0; index < AlchemyCore.MAXIMUM_STORED_PATTERNS; index++) {
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
        playerStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
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
        List<CruciblePattern> stored = AlchemyCore.patterns(coreSlot.getItem());
        for (int index = 0; index < patterns.getContainerSize(); index++) {
            ItemStack shown = index < stored.size() ? stored.get(index).result() : ItemStack.EMPTY;
            if (!ItemStack.matches(shown, patterns.getItem(index))) {
                patterns.setItem(index, shown.copy());
            }
        }
        if (!ItemStack.matches(assembler.target(), target.getItem(0))) {
            target.setItem(0, assembler.target().copy());
        }
        progress.set(Math.round(assembler.percentComplete() * 1000.0F));
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
            } else if (firstUpgradeSlot < playerStart - 1) {
                moved = moveItemStackTo(stack, firstUpgradeSlot, playerStart - 1, false);
            }
            if (!moved) {
                moved = index < playerStart + 27 ? moveItemStackTo(stack, playerStart + 27, slots.size(), false)
                    : moveItemStackTo(stack, playerStart, playerStart + 27, false);
            }
        } else if (slot == coreSlot || index >= firstUpgradeSlot && slot.container != target) {
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
