package thaumicenergistics.workbench;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaItems;
import thaumicenergistics.platform.TeNetwork;

public final class EssentiaCellWorkbenchMenu extends AbstractContainerMenu {
    public static final int CELL_SLOT_X = 152;
    public static final int CELL_SLOT_Y = 8;
    private static final int PLAYER_INVENTORY_Y = 169;
    private static final int HOTBAR_Y = PLAYER_INVENTORY_Y + 58;

    private final Player player;
    private final Container container;
    private final @Nullable EssentiaCellWorkbenchBlockEntity workbench;
    private final Slot cellSlot;
    private @Nullable List<Aspect> sentPartition;
    private List<Aspect> clientPartition = List.of();

    public EssentiaCellWorkbenchMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(WorkbenchFeature.MENU.get(), containerId);
        this.player = inventory.player;
        this.workbench = inventory.player.level().getBlockEntity(pos) instanceof EssentiaCellWorkbenchBlockEntity found ? found : null;
        this.container = workbench != null ? workbench : new SimpleContainer(1);
        this.cellSlot = addSlot(new Slot(container, 0, CELL_SLOT_X, CELL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(0, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    public List<Aspect> clientPartition() {
        return clientPartition;
    }

    void setClientPartition(List<Aspect> partition) {
        clientPartition = List.copyOf(partition);
    }

    public List<Aspect> partition() {
        return workbench == null ? List.of() : workbench.getPartition();
    }

    @Override
    public boolean stillValid(Player player) {
        return workbench != null && workbench.stillValid(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (workbench != null && player instanceof ServerPlayer serverPlayer) {
            List<Aspect> partition = workbench.getPartition();
            if (!partition.equals(sentPartition)) {
                sentPartition = partition;
                TeNetwork.sendToPlayer(serverPlayer, new WorkbenchPartitionPayload(containerId, partition));
            }
        }
    }

    public void handleAction(int action, @Nullable Aspect slotAspect) {
        if (workbench == null) {
            return;
        }
        switch (action) {
            case WorkbenchActionPayload.CLICK_SLOT -> {
                Aspect itemAspect = EssentiaItems.filterAspect(getCarried());
                if (slotAspect == null && itemAspect != null) {
                    workbench.addToPartition(itemAspect);
                } else if (slotAspect != null && itemAspect == null) {
                    workbench.removeFromPartition(slotAspect);
                } else if (slotAspect != null) {
                    workbench.replaceInPartition(slotAspect, itemAspect);
                }
            }
            case WorkbenchActionPayload.CLEAR -> workbench.clearPartition();
            case WorkbenchActionPayload.PARTITION_TO_CONTENTS -> workbench.partitionToContents();
            default -> {
            }
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (slot == cellSlot) {
            moveItemStackTo(stack, 1, slots.size(), true);
        } else if (!cellSlot.hasItem() && cellSlot.mayPlace(stack)) {
            cellSlot.set(stack.split(1));
        } else {
            Aspect aspect = EssentiaItems.filterAspect(stack);
            if (aspect == null || workbench == null || !workbench.addToPartition(aspect)) {
                if (index < 28) {
                    moveItemStackTo(stack, 28, slots.size(), false);
                } else {
                    moveItemStackTo(stack, 1, 28, false);
                }
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }
}
