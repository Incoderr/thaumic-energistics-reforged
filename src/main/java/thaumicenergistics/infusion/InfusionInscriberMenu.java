package thaumicenergistics.infusion;

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
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchManager;

public final class InfusionInscriberMenu extends AbstractContainerMenu {
    public static final int BUTTON_SAVE_DELETE = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int CORE_SLOT_X = 186;
    public static final int CORE_SLOT_Y = 8;
    public static final int CATALYST_SLOT_X = 44;
    public static final int CATALYST_SLOT_Y = 108;
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
    private final @Nullable InfusionInscriberBlockEntity inscriber;
    private final Slot coreSlot;
    private final Slot catalystSlot;
    private final SimpleContainer patterns = new SimpleContainer(InfusionCore.MAXIMUM_STORED_PATTERNS);
    private final SimpleContainer catalyst = new SimpleContainer(1);
    private final SimpleContainer result = new SimpleContainer(1);
    private final List<Slot> patternSlots = new ArrayList<>();
    private final DataSlot saveState = DataSlot.standalone();
    private final DataSlot recipeIndex = DataSlot.standalone();
    private List<InfusionPattern> shownPatterns = List.of();

    public InfusionInscriberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(InfusionFeature.INSCRIBER_MENU.get(), containerId);
        this.player = inventory.player;
        this.inscriber = player.level().getBlockEntity(pos) instanceof InfusionInscriberBlockEntity found ? found : null;
        Container container = inscriber != null ? inscriber : new SimpleContainer(1);
        coreSlot = addSlot(new Slot(container, 0, CORE_SLOT_X, CORE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return InfusionCore.isCore(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int index = 0; index < InfusionCore.MAXIMUM_STORED_PATTERNS; index++) {
            patternSlots.add(addSlot(new FakeSlot(patterns, index, PATTERN_SLOT_X + index % PATTERN_COLS * SLOT_SPACING, PATTERN_SLOT_Y + index / PATTERN_COLS * SLOT_SPACING)));
        }
        catalystSlot = addSlot(new FakeSlot(catalyst, 0, CATALYST_SLOT_X, CATALYST_SLOT_Y));
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
        addDataSlot(recipeIndex);
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

    public ItemStack catalyst() {
        return catalystSlot.getItem();
    }

    public List<InfusionRecipe> recipes() {
        if (catalyst().isEmpty()) {
            return List.of();
        }
        List<InfusionRecipe> found = new ArrayList<>();
        for (InfusionRecipe recipe : TcRecipes.all(InfusionRecipe.class)) {
            if (InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || !recipe.central().test(catalyst())) {
                continue;
            }
            if (recipe.research().isEmpty() || ResearchManager.isComplete(player, recipe.research())) {
                found.add(recipe);
            }
        }
        return found;
    }

    public @Nullable InfusionRecipe selected() {
        List<InfusionRecipe> recipes = recipes();
        return recipes.isEmpty() ? null : recipes.get(Math.floorMod(recipeIndex.get(), recipes.size()));
    }

    public @Nullable InfusionPattern selectedPattern() {
        InfusionRecipe recipe = selected();
        if (recipe == null) {
            return null;
        }
        List<ItemStack> components = new ArrayList<>();
        for (TcIngredient component : recipe.components()) {
            List<ItemStack> options = component.displayStacks();
            if (options.isEmpty()) {
                return null;
            }
            components.add(options.get(0));
        }
        ItemStack result = InfusionCrafting.result(player.level(), InfusionCrafting.TYPE_RECIPE, recipe.key(), catalyst());
        InfusionPattern pattern = new InfusionPattern(catalyst(), components, result, recipe.aspects());
        return pattern.isValid() ? pattern : null;
    }

    private ItemStack core() {
        return coreSlot.getItem();
    }

    public void setCatalyst(ItemStack stack) {
        catalyst.setItem(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        recipeIndex.set(0);
        onRecipeChanged();
    }

    private void onRecipeChanged() {
        if (player.level().isClientSide()) {
            return;
        }
        InfusionPattern pattern = selectedPattern();
        result.setItem(0, pattern == null ? ItemStack.EMPTY : pattern.result());
        updateSaveState();
    }

    private void updateSaveState() {
        InfusionPattern pattern = selectedPattern();
        SaveState state;
        if (!InfusionCore.isCore(core())) {
            state = SaveState.MISSING_CORE;
        } else if (pattern == null) {
            state = SaveState.INVALID_RECIPE;
        } else if (InfusionCore.patternFor(core(), pattern.result()) != null) {
            state = SaveState.DELETE;
        } else if (InfusionCore.hasRoom(core())) {
            state = SaveState.SAVE;
        } else {
            state = SaveState.CORE_FULL;
        }
        saveState.set(state.ordinal());
    }

    private void updatePatternSlots() {
        List<InfusionPattern> stored = InfusionCore.patterns(core());
        if (stored.equals(shownPatterns)) {
            return;
        }
        shownPatterns = stored;
        for (int index = 0; index < patterns.getContainerSize(); index++) {
            patterns.setItem(index, index < stored.size() ? stored.get(index).result() : ItemStack.EMPTY);
        }
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
            if (slot == catalystSlot) {
                if (!player.level().isClientSide()) {
                    setCatalyst(getCarried());
                }
                return;
            }
            if (patternSlots.contains(slot)) {
                int index = patternSlots.indexOf(slot);
                List<InfusionPattern> stored = InfusionCore.patterns(core());
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
    }

    private void loadPattern(InfusionPattern pattern) {
        catalyst.setItem(0, pattern.central().copy());
        List<InfusionRecipe> recipes = recipes();
        int found = 0;
        for (int index = 0; index < recipes.size(); index++) {
            if (pattern.producesSame(InfusionCrafting.result(player.level(), InfusionCrafting.TYPE_RECIPE, recipes.get(index).key(), catalyst()))) {
                found = index;
                break;
            }
        }
        recipeIndex.set(found);
        onRecipeChanged();
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == BUTTON_NEXT) {
            recipeIndex.set(recipeIndex.get() + 1);
            onRecipeChanged();
            return true;
        }
        if (id == BUTTON_SAVE_DELETE) {
            saveOrDelete();
            return true;
        }
        return false;
    }

    public void saveOrDelete() {
        InfusionPattern pattern = selectedPattern();
        updateSaveState();
        if (pattern == null) {
            return;
        }
        SaveState state = saveState();
        if (state == SaveState.SAVE) {
            if (InfusionCore.add(core(), pattern)) {
                coreSlot.setChanged();
            }
        } else if (state == SaveState.DELETE) {
            InfusionPattern stored = InfusionCore.patternFor(core(), pattern.result());
            if (stored != null && InfusionCore.remove(core(), stored)) {
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
            moved = InfusionCore.isCore(stack) && !coreSlot.hasItem() && moveItemStackTo(stack, coreSlot.index, coreSlot.index + 1, false);
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
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return inscriber != null && inscriber.stillValid(clicker);
    }
}
