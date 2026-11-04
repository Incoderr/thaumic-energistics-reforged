package thaumicenergistics.buses;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.Upgrades;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaItems;

public final class EssentiaBusMenu extends AEBaseMenu {
    public static final int UPGRADE_X = 187;
    public static final int UPGRADE_Y = 8;
    public static final int TOOL_X = 187;
    public static final int TOOL_Y = 102;
    private static final int PLAYER_INVENTORY_Y = 102;
    private static final int HOTBAR_Y = 160;
    private static final int SLOT_SIZE = 18;
    private static final String ACTION_FILTER = "filter";
    private static final String ACTION_VOID = "void";
    private static final String ACTION_PRIORITY = "priority";

    private final EssentiaBusHost host;
    private final @Nullable NetworkToolMenuHost<?> toolbox;
    private final int lockedSlot;
    private final List<Slot> upgradeSlots = new ArrayList<>();
    private final List<Slot> toolSlots = new ArrayList<>();
    private final List<Slot> playerSlots = new ArrayList<>();

    @GuiSync(10)
    public String filters = "";
    @GuiSync(11)
    public int availableMask;
    @GuiSync(12)
    public boolean redstoneControlled;
    @GuiSync(13)
    public RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    @GuiSync(14)
    public boolean voidAllowed;

    public EssentiaBusMenu(int containerId, Inventory inventory, EssentiaBusHost host) {
        super(BusesFeature.MENU.get(), containerId, inventory, host);
        this.host = host;
        this.toolbox = NetworkToolItem.findNetworkToolInv(inventory.player);
        Integer toolSlot = toolbox == null ? null : toolbox.getPlayerInventorySlot();
        this.lockedSlot = toolSlot == null ? -1 : toolSlot;
        InternalInventory upgrades = host.getUpgrades();
        Container upgradeContainer = upgrades.toContainer();
        for (int index = 0; index < upgrades.size(); index++) {
            int slot = index;
            upgradeSlots.add(addSlot(new Slot(upgradeContainer, index, UPGRADE_X, UPGRADE_Y + index * SLOT_SIZE) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return upgrades.isItemValid(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            }, SlotSemantics.UPGRADE));
        }
        if (toolbox != null) {
            Container tool = toolbox.getInventory().toContainer();
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    toolSlots.add(addSlot(new Slot(tool, column + row * 3, TOOL_X + column * SLOT_SIZE, TOOL_Y + row * SLOT_SIZE) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return Upgrades.isUpgradeCardItem(stack);
                        }
                    }, SlotSemantics.TOOLBOX));
                }
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                playerSlots.add(addSlot(playerSlot(inventory, column + row * 9 + 9, 8 + column * SLOT_SIZE, PLAYER_INVENTORY_Y + row * SLOT_SIZE), SlotSemantics.PLAYER_INVENTORY));
            }
        }
        for (int column = 0; column < 9; column++) {
            playerSlots.add(addSlot(playerSlot(inventory, column, 8 + column * SLOT_SIZE, HOTBAR_Y), SlotSemantics.PLAYER_HOTBAR));
        }
        registerClientAction(ACTION_FILTER, Integer.class, this::clickFilter);
        registerClientAction(ACTION_VOID, Boolean.class, this::toggleVoid);
        registerClientAction(ACTION_PRIORITY, Boolean.class, this::openPriority);
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        boolean locked = index == lockedSlot;
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return !locked;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !locked;
            }
        };
    }

    public EssentiaBusHost getHost() {
        return host;
    }

    public boolean hasToolbox() {
        return toolbox != null;
    }

    public @Nullable Aspect filter(int index) {
        String[] tags = filters.split(",", -1);
        if (index >= tags.length || tags[index].isEmpty()) {
            return null;
        }
        return Aspect.getAspect(tags[index]);
    }

    public boolean isFilterAvailable(int index) {
        return (availableMask & 1 << index) != 0;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            if (toolbox != null) {
                if (!toolbox.isValid()) {
                    setValidMenu(false);
                    return;
                }
                toolbox.tick();
            }
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < EssentiaBusHost.FILTER_SIZE; index++) {
                if (index > 0) {
                    builder.append(',');
                }
                Aspect aspect = host.getFilter(index);
                if (aspect != null) {
                    builder.append(aspect.getTag());
                }
            }
            filters = builder.toString();
            int mask = 0;
            for (int index : host.availableFilterSlots()) {
                mask |= 1 << index;
            }
            availableMask = mask;
            redstoneControlled = host.isRedstoneControlled();
            if (host.getConfigManager().hasSetting(Settings.REDSTONE_CONTROLLED)) {
                redstoneMode = host.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
            }
            voidAllowed = host.isVoidAllowed();
        }
        super.broadcastChanges();
    }

    public void clickFilter(int index) {
        if (isClientSide()) {
            sendClientAction(ACTION_FILTER, index);
            return;
        }
        if (index < 0 || index >= EssentiaBusHost.FILTER_SIZE || !host.isFilterSlotAvailable(index)) {
            return;
        }
        Aspect aspect = EssentiaItems.filterAspect(getCarried());
        if (aspect != null && host.hasFilter(aspect)) {
            return;
        }
        host.setFilter(index, aspect);
    }

    public void toggleVoid(boolean ignored) {
        if (isClientSide()) {
            sendClientAction(ACTION_VOID, true);
            return;
        }
        if (host.supportsVoid()) {
            host.setVoidAllowed(!host.isVoidAllowed());
        }
    }

    public void openPriority(boolean ignored) {
        if (isClientSide()) {
            sendClientAction(ACTION_PRIORITY, true);
            return;
        }
        if (host.isStorageBus()) {
            host.openPriority(getPlayer());
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && button == lockedSlot) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (EssentiaItems.filterAspect(stack) != null && (isClientSide() || host.addFilterFromItem(stack))) {
            return ItemStack.EMPTY;
        }
        boolean moved;
        if (playerSlots.contains(slot)) {
            moved = mergeInto(stack, upgradeSlots) || mergeInto(stack, toolSlots) || swapInventoryHotbar(slot, stack);
        } else if (upgradeSlots.contains(slot)) {
            moved = mergeInto(stack, toolSlots) || mergeInto(stack, playerSlots);
        } else if (toolSlots.contains(slot)) {
            moved = mergeInto(stack, upgradeSlots) || mergeInto(stack, playerSlots);
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

    private boolean swapInventoryHotbar(Slot slot, ItemStack stack) {
        int position = playerSlots.indexOf(slot);
        return position < 27 ? mergeInto(stack, playerSlots.subList(27, 36)) : mergeInto(stack, playerSlots.subList(0, 27));
    }

    private boolean mergeInto(ItemStack stack, List<Slot> targets) {
        boolean moved = false;
        for (Slot target : targets) {
            if (stack.isEmpty()) {
                break;
            }
            if (!target.mayPlace(stack)) {
                continue;
            }
            ItemStack existing = target.getItem();
            int limit = Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize());
            if (existing.isEmpty()) {
                int count = Math.min(limit, stack.getCount());
                target.set(stack.split(count));
                moved = true;
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < limit) {
                int count = Math.min(limit - existing.getCount(), stack.getCount());
                existing.grow(count);
                stack.shrink(count);
                target.setChanged();
                moved = true;
            }
        }
        return moved;
    }
}
