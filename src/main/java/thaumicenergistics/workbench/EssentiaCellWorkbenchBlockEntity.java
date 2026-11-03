package thaumicenergistics.workbench;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.cells.EssentiaCellInventory;
import thaumicenergistics.cells.EssentiaCellItem;

public final class EssentiaCellWorkbenchBlockEntity extends BlockEntity implements Container {
    private static final String CELL_KEY = "EssentiaCell";

    private ItemStack cell = ItemStack.EMPTY;

    public EssentiaCellWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(WorkbenchFeature.BLOCK_ENTITY.get(), pos, state);
    }

    public boolean hasCell() {
        return !cell.isEmpty();
    }

    public List<Aspect> getPartition() {
        EssentiaCellInventory inventory = EssentiaCellInventory.of(cell, null);
        return inventory == null ? List.of() : inventory.getPartition();
    }

    public boolean addToPartition(Aspect aspect) {
        return modify(inventory -> inventory.addToPartition(aspect));
    }

    public boolean removeFromPartition(Aspect aspect) {
        return modify(inventory -> inventory.removeFromPartition(aspect));
    }

    public boolean replaceInPartition(Aspect original, Aspect replacement) {
        return modify(inventory -> inventory.replaceInPartition(original, replacement));
    }

    public void clearPartition() {
        apply(EssentiaCellInventory::clearPartition);
    }

    public void partitionToContents() {
        apply(EssentiaCellInventory::partitionToContents);
    }

    private boolean modify(Predicate<EssentiaCellInventory> action) {
        EssentiaCellInventory inventory = EssentiaCellInventory.of(cell, null);
        if (inventory == null || !action.test(inventory)) {
            return false;
        }
        setChanged();
        return true;
    }

    private void apply(Consumer<EssentiaCellInventory> action) {
        modify(inventory -> {
            action.accept(inventory);
            return true;
        });
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return cell.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return cell;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = cell;
        cell = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = cell;
        cell = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (canPlaceItem(slot, stack)) {
            cell = stack;
            setChanged();
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof EssentiaCellItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        cell = ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cell = tag.contains(CELL_KEY) ? ItemStack.parseOptional(registries, tag.getCompound(CELL_KEY)) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!cell.isEmpty()) {
            tag.put(CELL_KEY, cell.save(registries));
        }
    }
}
