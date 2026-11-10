package thaumicenergistics.distillation;

import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DistillationEncoderBlockEntity extends BlockEntity implements Container {
    public static final int SLOT_SOURCE_ITEM = 0;
    public static final int SLOT_BLANK_PATTERNS = 1;
    public static final int SLOT_ENCODED_PATTERN = 2;
    private static final String INVENTORY_KEY = "inventory";

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    public DistillationEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(DistillationFeature.ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (slot == SLOT_BLANK_PATTERNS) {
            return AEItems.BLANK_PATTERN.is(stack);
        }
        if (slot == SLOT_ENCODED_PATTERN) {
            return AEItems.PROCESSING_PATTERN.is(stack);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag.getCompound(INVENTORY_KEY), items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_KEY, ContainerHelper.saveAllItems(new CompoundTag(), items, registries));
    }
}
