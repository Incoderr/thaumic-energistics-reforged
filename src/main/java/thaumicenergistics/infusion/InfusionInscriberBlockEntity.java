package thaumicenergistics.infusion;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InfusionInscriberBlockEntity extends BlockEntity implements Container {
    private static final String CORE_KEY = "core";

    private ItemStack core = ItemStack.EMPTY;

    public InfusionInscriberBlockEntity(BlockPos pos, BlockState state) {
        super(InfusionFeature.INSCRIBER_ENTITY.get(), pos, state);
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
        return stack.isEmpty() || InfusionCore.isCore(stack);
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
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        core = tag.read(CORE_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!core.isEmpty()) {
            tag.store(CORE_KEY, ItemStack.CODEC, core);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, this);
        }
    }
}
