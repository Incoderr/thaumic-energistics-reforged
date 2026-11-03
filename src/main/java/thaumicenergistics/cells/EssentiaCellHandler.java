package thaumicenergistics.cells;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class EssentiaCellHandler implements ICellHandler {
    public static final EssentiaCellHandler INSTANCE = new EssentiaCellHandler();

    private EssentiaCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return stack.getItem() instanceof EssentiaCellItem;
    }

    @Override
    public @Nullable StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider saveProvider) {
        return EssentiaCellInventory.of(stack, saveProvider);
    }
}
