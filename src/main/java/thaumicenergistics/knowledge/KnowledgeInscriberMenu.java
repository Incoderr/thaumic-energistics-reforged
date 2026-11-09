package thaumicenergistics.knowledge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.crafting.ArcaneCrafting;

public final class KnowledgeInscriberMenu extends AbstractContainerMenu {
    public static final int BUTTON_SAVE_DELETE = 0;
    public static final int BUTTON_CLEAR = 1;
    public static final int KCORE_SLOT_X = 186;
    public static final int KCORE_SLOT_Y = 8;
    public static final int CRAFTING_SLOT_X = 26;
    public static final int CRAFTING_SLOT_Y = 90;
    public static final int SLOT_SPACING = 18;
    private static final int PATTERN_SLOT_X = 26;
    private static final int PATTERN_SLOT_Y = 18;
    private static final int PATTERN_COLS = 7;
    private static final int RESULT_SLOT_X = 116;
    private static final int RESULT_SLOT_Y = 108;
    private static final int PLAYER_INVENTORY_Y = 162;
    private static final int HOTBAR_Y = PLAYER_INVENTORY_Y + 58;

    public enum SaveState {
        INVALID_RECIPE, CORE_FULL, MISSING_CORE, SAVE, DELETE
    }

    private final Player player;
    private final @Nullable KnowledgeInscriberBlockEntity inscriber;
    private final Slot coreSlot;
    private final SimpleContainer patterns = new SimpleContainer(KnowledgeCore.MAXIMUM_STORED_PATTERNS);
    private final SimpleContainer grid = new SimpleContainer(ArcanePattern.GRID_SIZE);
    private final SimpleContainer result = new SimpleContainer(1);
    private final List<Slot> patternSlots = new ArrayList<>();
    private final List<Slot> gridSlots = new ArrayList<>();
    private final DataSlot saveState = DataSlot.standalone();
    private final DataSlot saveCounter = DataSlot.standalone();
    private @Nullable ArcaneCrafting.Match match;
    private List<ArcanePattern> shownPatterns = List.of();

    public KnowledgeInscriberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(KnowledgeFeature.MENU.get(), containerId);
        this.player = inventory.player;
        this.inscriber = player.level().getBlockEntity(pos) instanceof KnowledgeInscriberBlockEntity found ? found : null;
        Container container = inscriber != null ? inscriber : new SimpleContainer(1);
        coreSlot = addSlot(new Slot(container, 0, KCORE_SLOT_X, KCORE_SLOT_Y) {
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
            patternSlots.add(addSlot(new FakeSlot(patterns, index, PATTERN_SLOT_X + index % PATTERN_COLS * SLOT_SPACING, PATTERN_SLOT_Y + index / PATTERN_COLS * SLOT_SPACING)));
        }
        for (int index = 0; index < ArcanePattern.GRID_SIZE; index++) {
            gridSlots.add(addSlot(new FakeSlot(grid, index, CRAFTING_SLOT_X + index % 3 * SLOT_SPACING, CRAFTING_SLOT_Y + index / 3 * SLOT_SPACING)));
        }
        addSlot(new FakeSlot(result, 0, RESULT_SLOT_X, RESULT_SLOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlot(saveState);
        addDataSlot(saveCounter);
        saveState.set(SaveState.MISSING_CORE.ordinal());
    }

    private static final class FakeSlot extends Slot {
        FakeSlot(Container container, int index, int x, int y) {
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

    public SaveState saveState() {
        return SaveState.values()[Math.floorMod(saveState.get(), SaveState.values().length)];
    }

    public int saveCounter() {
        return saveCounter.get();
    }

    private ItemStack core() {
        return coreSlot.getItem();
    }

    private List<ItemStack> gridStacks() {
        List<ItemStack> stacks = new ArrayList<>(ArcanePattern.GRID_SIZE);
        for (int index = 0; index < ArcanePattern.GRID_SIZE; index++) {
            stacks.add(grid.getItem(index));
        }
        return stacks;
    }

    private void onGridChanged() {
        if (player.level().isClientSide()) {
            return;
        }
        match = ArcaneCrafting.find(gridStacks(), player);
        result.setItem(0, match == null ? ItemStack.EMPTY : match.result());
        updateSaveState();
    }

    private void updateSaveState() {
        SaveState state;
        if (!KnowledgeCore.isCore(core())) {
            state = SaveState.MISSING_CORE;
        } else if (match == null || match.result().isEmpty()) {
            state = SaveState.INVALID_RECIPE;
        } else if (KnowledgeCore.patternFor(core(), match.result()) != null) {
            state = SaveState.DELETE;
        } else if (KnowledgeCore.hasRoom(core())) {
            state = SaveState.SAVE;
        } else {
            state = SaveState.CORE_FULL;
        }
        saveState.set(state.ordinal());
    }

    private void updatePatternSlots() {
        List<ArcanePattern> stored = KnowledgeCore.patterns(core());
        if (stored.equals(shownPatterns)) {
            return;
        }
        shownPatterns = stored;
        for (int index = 0; index < patterns.getContainerSize(); index++) {
            patterns.setItem(index, index < stored.size() ? stored.get(index).result() : ItemStack.EMPTY);
        }
        updateSaveState();
    }

    private void loadPattern(ArcanePattern pattern) {
        for (int index = 0; index < ArcanePattern.GRID_SIZE; index++) {
            grid.setItem(index, pattern.grid().get(index).copy());
        }
        onGridChanged();
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            updatePatternSlots();
            updateSaveState();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player clicker) {
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            Slot slot = slots.get(slotIndex);
            if (gridSlots.contains(slot)) {
                ItemStack carried = getCarried();
                slot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                onGridChanged();
                return;
            }
            if (patternSlots.contains(slot)) {
                int index = patternSlots.indexOf(slot);
                List<ArcanePattern> stored = KnowledgeCore.patterns(core());
                if (!player.level().isClientSide() && index < stored.size()) {
                    loadPattern(stored.get(index));
                }
                return;
            }
            if (slot.container == result) {
                return;
            }
        }
        super.clicked(slotIndex, button, clickType, clicker);
        if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex) == coreSlot && !player.level().isClientSide()) {
            updatePatternSlots();
            updateSaveState();
        }
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == BUTTON_CLEAR) {
            for (int index = 0; index < ArcanePattern.GRID_SIZE; index++) {
                grid.setItem(index, ItemStack.EMPTY);
            }
            onGridChanged();
            return true;
        }
        if (id == BUTTON_SAVE_DELETE) {
            saveOrDelete();
            return true;
        }
        return false;
    }

    public void saveOrDelete() {
        onGridChanged();
        ItemStack core = core();
        if (match == null) {
            return;
        }
        SaveState state = saveState();
        if (state == SaveState.SAVE) {
            ArcanePattern pattern = new ArcanePattern(gridStacks(), match.result(), match.aspects());
            if (KnowledgeCore.add(core, pattern)) {
                saveCounter.set(saveCounter.get() + 1);
                coreSlot.setChanged();
            }
        } else if (state == SaveState.DELETE) {
            ArcanePattern pattern = KnowledgeCore.patternFor(core, match.result());
            if (pattern != null && KnowledgeCore.remove(core, pattern)) {
                coreSlot.setChanged();
            }
        }
        updatePatternSlots();
        updateSaveState();
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
        int playerStart = slots.size() - 36;
        boolean moved;
        if (slot == coreSlot) {
            moved = moveItemStackTo(stack, playerStart, slots.size(), true);
        } else if (index >= playerStart) {
            moved = KnowledgeCore.isCore(stack) && !coreSlot.hasItem() && moveItemStackTo(stack, coreSlot.index, coreSlot.index + 1, false);
            if (!moved) {
                moved = index < playerStart + 27 ? moveItemStackTo(stack, playerStart + 27, slots.size(), false) : moveItemStackTo(stack, playerStart, playerStart + 27, false);
            }
        } else {
            moved = false;
        }
        if (moved) {
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            slot.setChanged();
            if (slot == coreSlot || !coreSlot.hasItem() || stack == coreSlot.getItem()) {
                updatePatternSlots();
                updateSaveState();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return inscriber != null && inscriber.stillValid(clicker);
    }
}
