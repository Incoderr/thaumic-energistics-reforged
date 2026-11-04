package thaumicenergistics.act;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneCrafting;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;

public final class ArcaneCraftingTermMenu extends MEStorageMenu {
    public static final SlotSemantic WAND = SlotSemantics.register("THAUMICENERGISTICS_WAND", false);
    public static final SlotSemantic ARMOR = SlotSemantics.register("THAUMICENERGISTICS_ARMOR", false);
    private static final String ACTION_CLEAR = "clearGrid";
    private static final String ACTION_SWAP_ARMOR = "swapArmor";
    private static final int MAX_SHIFT_CRAFT = 64;
    private static final int COST_REFRESH_TICKS = 20;

    public record Cost(Aspect aspect, float vis, boolean enough) {
    }

    private final ArcaneCraftingTerminalPart terminal;
    private final InternalInventory grid;
    private final InternalInventory wand;
    private final List<Slot> gridSlots = new ArrayList<>();
    private final ResultSlot resultSlot;
    private @Nullable AspectList requiredAspects;
    private int refreshCounter;

    @GuiSync(40)
    public String costs = "";

    public ArcaneCraftingTermMenu(int id, Inventory inventory, ArcaneCraftingTerminalPart terminal) {
        super(ActFeature.MENU.get(), id, inventory, terminal);
        this.terminal = terminal;
        this.grid = terminal.craftingGrid();
        this.wand = terminal.wandInventory();
        for (int index = 0; index < grid.size(); index++) {
            gridSlots.add(addSlot(new GridSlot(grid, index), SlotSemantics.CRAFTING_GRID));
        }
        resultSlot = new ResultSlot();
        addSlot(resultSlot, SlotSemantics.CRAFTING_RESULT);
        addSlot(new GridSlot(wand, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ArcaneCraftingTerminalPart.isValidWand(stack);
            }
        }, WAND);
        InternalInventory armor = terminal.armorInventory();
        for (int index = 0; index < armor.size(); index++) {
            int slot = index;
            addSlot(new AppEngSlot(armor, index) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ArcaneCraftingTerminalPart.isArmorFor(stack, slot);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            }, ARMOR);
        }
        registerClientAction(ACTION_CLEAR, this::clearCraftingGrid);
        registerClientAction(ACTION_SWAP_ARMOR, this::swapArmor);
        updateResult();
    }

    public ArcaneCraftingTerminalPart getTerminal() {
        return terminal;
    }

    private static class GridSlot extends AppEngSlot {
        GridSlot(InternalInventory inventory, int index) {
            super(inventory, index);
        }
    }

    private final class ResultSlot extends Slot {
        ResultSlot() {
            super(new SimpleContainer(1), 0, 0, 0);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (isServerSide()) {
                onCrafted(player, stack);
            }
        }
    }

    public List<Cost> clientCosts() {
        List<Cost> result = new ArrayList<>();
        if (costs.isEmpty()) {
            return result;
        }
        for (String entry : costs.split(";")) {
            String[] parts = entry.split(":");
            Aspect aspect = parts.length == 3 ? Aspect.getAspect(parts[0]) : null;
            if (aspect != null) {
                result.add(new Cost(aspect, Float.parseFloat(parts[1]), parts[2].equals("1")));
            }
        }
        return result;
    }

    private List<ItemStack> gridStacks() {
        List<ItemStack> stacks = new ArrayList<>(grid.size());
        for (int index = 0; index < grid.size(); index++) {
            stacks.add(grid.getStackInSlot(index));
        }
        return stacks;
    }

    private Optional<RecipeHolder<CraftingRecipe>> vanillaRecipe(CraftingInput input) {
        if (!(getPlayer().level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    private void updateResult() {
        if (!isServerSide()) {
            return;
        }
        requiredAspects = null;
        List<Cost> costList = new ArrayList<>();
        List<ItemStack> stacks = gridStacks();
        CraftingInput input = CraftingInput.ofPositioned(ArcaneCrafting.GRID_SIZE, ArcaneCrafting.GRID_SIZE, stacks).input();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> vanilla = vanillaRecipe(input);
        if (vanilla.isPresent()) {
            result = vanilla.get().value().assemble(input, getPlayer().level().registryAccess());
        } else {
            ArcaneCrafting.Match match = ArcaneCrafting.find(stacks, getPlayer());
            if (match != null && match.aspects().size() > 0) {
                requiredAspects = match.aspects();
                ItemStack wandStack = wand.getStackInSlot(0);
                WandItem wandItem = wandStack.getItem() instanceof WandItem item ? item : null;
                boolean hasAll = true;
                for (Aspect aspect : requiredAspects.getAspects()) {
                    int required = requiredAspects.getAmount(aspect) * 100;
                    boolean enough = false;
                    if (wandItem != null) {
                        required = (int) (required * wandItem.getConsumptionModifier(wandStack, getPlayer(), aspect, true));
                        enough = Wands.getVis(wandStack, aspect) >= required;
                    }
                    hasAll &= enough;
                    costList.add(new Cost(aspect, Math.round(required / 100.0F * 10.0F) / 10.0F, enough));
                }
                if (hasAll) {
                    result = match.result();
                }
            }
        }
        resultSlot.set(result);
        StringBuilder builder = new StringBuilder();
        for (Cost cost : costList) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(cost.aspect().getTag()).append(':').append(cost.vis()).append(':').append(cost.enough() ? '1' : '0');
        }
        costs = builder.toString();
    }

    private void onCrafted(Player player, ItemStack crafted) {
        List<ItemStack> stacks = gridStacks();
        crafted.onCraftedBy(player.level(), player, crafted.getCount());
        EventHooks.firePlayerCraftingEvent(player, crafted, grid.toContainer());
        if (requiredAspects != null && wand.getStackInSlot(0).getItem() instanceof WandItem wandItem) {
            ItemStack wandStack = wand.getStackInSlot(0).copy();
            wandItem.consumeAllVisCrafting(wandStack, player, requiredAspects, true);
            wand.setItemDirect(0, wandStack);
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(ArcaneCrafting.GRID_SIZE, ArcaneCrafting.GRID_SIZE, stacks);
        CraftingInput input = positioned.input();
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int index = 0; index < input.size(); index++) {
            remaining.set(index, input.getItem(index).getCraftingRemainingItem());
        }
        Optional<RecipeHolder<CraftingRecipe>> vanilla = vanillaRecipe(input);
        if (vanilla.isPresent()) {
            CommonHooks.setCraftingPlayer(player);
            try {
                remaining = vanilla.get().value().getRemainingItems(input);
            } finally {
                CommonHooks.setCraftingPlayer(null);
            }
        }
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + positioned.left() + (y + positioned.top()) * ArcaneCrafting.GRID_SIZE;
                ItemStack current = grid.getStackInSlot(slot);
                ItemStack replacement = remaining.get(x + y * input.width());
                if (!current.isEmpty()) {
                    if (replacement.isEmpty() && current.getCount() == 1) {
                        ItemStack refill = replenish(player, current);
                        grid.setItemDirect(slot, refill);
                    } else {
                        ItemStack reduced = current.copy();
                        reduced.shrink(1);
                        grid.setItemDirect(slot, reduced);
                    }
                    current = grid.getStackInSlot(slot);
                }
                if (!replacement.isEmpty()) {
                    if (current.isEmpty()) {
                        grid.setItemDirect(slot, replacement);
                    } else if (ItemStack.isSameItemSameComponents(current, replacement)) {
                        replacement.grow(current.getCount());
                        grid.setItemDirect(slot, replacement);
                    } else if (!player.getInventory().add(replacement)) {
                        player.drop(replacement, false);
                    }
                }
            }
        }
        updateResult();
    }

    private ItemStack replenish(Player player, ItemStack wanted) {
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.items.size(); index++) {
            if (isPlayerInventorySlotLocked(index)) {
                continue;
            }
            ItemStack stack = inventory.items.get(index);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted)) {
                return stack.split(1);
            }
        }
        if (!canInteractWithGrid()) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = AEItemKey.of(wanted);
        if (StorageHelper.poweredExtraction(energySource, storage, key, 1, getActionSource()) == 1) {
            return key.toStack();
        }
        for (var entry : storage.getAvailableStacks()) {
            AEKey candidate = entry.getKey();
            if (candidate instanceof AEItemKey itemKey && itemKey.getItem() == wanted.getItem()
                && StorageHelper.poweredExtraction(energySource, storage, itemKey, 1, getActionSource()) == 1) {
                return itemKey.toStack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide() && ++refreshCounter >= COST_REFRESH_TICKS) {
            refreshCounter = 0;
            updateResult();
        }
        super.broadcastChanges();
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        if (isServerSide() && (gridSlots.contains(slot) || getSlots(WAND).contains(slot) || getSlots(ARMOR).contains(slot))) {
            updateResult();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 0 && index < slots.size() && slots.get(index) == resultSlot) {
            if (isServerSide()) {
                shiftCraft(player);
            }
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    private void shiftCraft(Player player) {
        ItemStack original = resultSlot.getItem().copy();
        if (original.isEmpty()) {
            return;
        }
        for (int crafted = original.getCount(); crafted <= MAX_SHIFT_CRAFT; crafted += original.getCount()) {
            ItemStack result = resultSlot.getItem();
            if (result.isEmpty() || !ItemStack.isSameItemSameComponents(result, original)) {
                break;
            }
            ItemStack moving = result.copy();
            if (!canFit(player.getInventory(), moving)) {
                break;
            }
            player.getInventory().add(moving);
            onCrafted(player, result.copy());
        }
    }

    private static boolean canFit(Inventory inventory, ItemStack stack) {
        int room = 0;
        for (ItemStack existing : inventory.items) {
            if (existing.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                room += existing.getMaxStackSize() - existing.getCount();
            }
            if (room >= stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    public void clearCraftingGrid() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }
        if (!canInteractWithGrid()) {
            return;
        }
        for (int index = 0; index < grid.size(); index++) {
            ItemStack stack = grid.getStackInSlot(index);
            if (stack.isEmpty()) {
                continue;
            }
            long inserted = StorageHelper.poweredInsert(energySource, storage, AEItemKey.of(stack), stack.getCount(), getActionSource(), Actionable.MODULATE);
            if (inserted > 0) {
                ItemStack rest = stack.copy();
                rest.shrink((int) inserted);
                grid.setItemDirect(index, rest);
            }
        }
        updateResult();
    }

    public void swapArmor() {
        if (isClientSide()) {
            sendClientAction(ACTION_SWAP_ARMOR);
            return;
        }
        terminal.swapStoredArmor(getPlayer());
        updateResult();
    }
}
