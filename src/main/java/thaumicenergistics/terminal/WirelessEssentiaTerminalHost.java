package thaumicenergistics.terminal;

import appeng.api.config.ViewItems;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;

public final class WirelessEssentiaTerminalHost extends WirelessTerminalMenuHost<WirelessEssentiaTerminalItem> implements EssentiaTerminalHost {
    private final AppEngInternalInventory workInventory = new AppEngInternalInventory(2);
    private @Nullable Aspect selectedAspect;

    public WirelessEssentiaTerminalHost(WirelessEssentiaTerminalItem item, Player player, ItemMenuHostLocator locator, BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
    }

    @Override
    public @Nullable IGrid getEssentiaGrid() {
        if (!getLinkStatus().connected()) {
            return null;
        }
        IGridNode node = getActionableNode();
        return node == null ? null : node.getGrid();
    }

    @Override
    public InternalInventory getWorkInventory() {
        return workInventory;
    }

    @Override
    public @Nullable Aspect getSelectedAspect() {
        return selectedAspect;
    }

    @Override
    public void setSelectedAspect(@Nullable Aspect aspect) {
        selectedAspect = aspect;
    }

    private EssentiaTerminalModes modes() {
        return TerminalFeature.MODES.getOrDefault(getItemStack(), EssentiaTerminalModes.DEFAULT);
    }

    @Override
    public EssentiaSortMode getSortMode() {
        return modes().sort();
    }

    @Override
    public void setSortMode(EssentiaSortMode mode) {
        TerminalFeature.MODES.set(getItemStack(), modes().withSort(mode));
    }

    @Override
    public ViewItems getViewMode() {
        return modes().view();
    }

    @Override
    public void setViewMode(ViewItems mode) {
        TerminalFeature.MODES.set(getItemStack(), modes().withView(mode));
    }

    @Override
    public int lockedPlayerSlot() {
        Integer slot = getPlayerInventorySlot();
        return slot == null ? -1 : slot;
    }

    @Override
    public void onMenuClosed(Player player) {
        for (int slot = 0; slot < workInventory.size(); slot++) {
            ItemStack stack = workInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
                workInventory.setItemDirect(slot, ItemStack.EMPTY);
            }
        }
    }
}
