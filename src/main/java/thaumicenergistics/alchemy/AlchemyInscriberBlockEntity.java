package thaumicenergistics.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AlchemyInscriberBlockEntity extends BlockEntity implements Container {
    private static final String CORE_KEY = "core";

    private ItemStack core = ItemStack.EMPTY;

    public AlchemyInscriberBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyFeature.INSCRIBER_ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return core.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return core;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = core;
        core = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = core;
        core = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (canPlaceItem(slot, stack)) {
            core = stack;
            setChanged();
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.isEmpty() || AlchemyCore.isCore(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        core = ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core = tag.contains(CORE_KEY) ? ItemStack.parseOptional(registries, tag.getCompound(CORE_KEY)) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!core.isEmpty()) {
            tag.put(CORE_KEY, core.save(registries));
        }
    }
}
