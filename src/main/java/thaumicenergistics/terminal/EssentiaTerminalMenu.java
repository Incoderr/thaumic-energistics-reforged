package thaumicenergistics.terminal;

import appeng.api.config.Actionable;
import appeng.api.config.ViewItems;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.crafting.CraftAmountMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModSounds;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaKeyType;
import thaumicenergistics.platform.TeNetwork;

public final class EssentiaTerminalMenu extends AEBaseMenu {
    public static final int INPUT_X = 8;
    public static final int OUTPUT_X = 26;
    public static final int WORK_SLOT_Y = 92;
    private static final int PLAYER_INVENTORY_Y = 122;
    private static final int HOTBAR_Y = 180;
    private static final int WORK_TICK_RATE = 3;
    private static final int TRANSFER_PER_WORK_CYCLE = 64;
    private static final long MINIMUM_SOUND_WAIT = 900;
    private static final String ACTION_SELECT = "select";
    private static final String ACTION_SORT = "sort";
    private static final String ACTION_VIEW = "view";
    private static final String ACTION_INTERACT = "interact";
    private static final String ACTION_CRAFT = "craft";

    private final EssentiaTerminalHost host;
    private final Slot inputSlot;
    private final Slot outputSlot;

    @GuiSync(1)
    public String selectedAspect = "";
    @GuiSync(2)
    public EssentiaSortMode sortMode = EssentiaSortMode.ALPHABETIC;
    @GuiSync(3)
    public ViewItems viewMode = ViewItems.ALL;

    private @Nullable List<EssentiaListPayload.Entry> sentList;
    private List<EssentiaListPayload.Entry> clientList = List.of();
    private int tickCounter;
    private int workCounter;
    private long lastSoundTime;

    public EssentiaTerminalMenu(int containerId, Inventory inventory, EssentiaTerminalHost host) {
        super(TerminalFeature.MENU.get(), containerId, inventory, host);
        this.host = host;
        Container work = host.getWorkInventory().toContainer();
        this.inputSlot = addSlot(new Slot(work, 0, INPUT_X, WORK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EssentiaContainers.isContainer(stack) || EssentiaContainers.isLabel(stack);
            }
        }, SlotSemantics.MACHINE_INPUT);
        this.outputSlot = addSlot(new Slot(work, 1, OUTPUT_X, WORK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }, SlotSemantics.MACHINE_OUTPUT);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(playerSlot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INVENTORY_Y + row * 18), SlotSemantics.PLAYER_INVENTORY);
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(inventory, column, 8 + column * 18, HOTBAR_Y), SlotSemantics.PLAYER_HOTBAR);
        }
        registerClientAction(ACTION_SELECT, String.class, this::selectAspect);
        registerClientAction(ACTION_SORT, Boolean.class, this::cycleSortMode);
        registerClientAction(ACTION_VIEW, Boolean.class, this::cycleViewMode);
        registerClientAction(ACTION_INTERACT, String.class, this::interactWithHeldItem);
        registerClientAction(ACTION_CRAFT, String.class, this::requestCraft);
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        boolean locked = index == host.lockedPlayerSlot();
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

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && button == host.lockedPlayerSlot()) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (isServerSide()) {
            host.onMenuClosed(player);
        }
    }

    public EssentiaTerminalHost getHost() {
        return host;
    }

    public List<EssentiaListPayload.Entry> clientList() {
        return clientList;
    }

    void setClientList(List<EssentiaListPayload.Entry> entries) {
        clientList = List.copyOf(entries);
    }

    public @Nullable Aspect selected() {
        return selectedAspect.isEmpty() ? null : Aspect.getAspect(selectedAspect);
    }

    public void selectAspect(String tag) {
        if (isClientSide()) {
            sendClientAction(ACTION_SELECT, tag);
            return;
        }
        host.setSelectedAspect(tag.isEmpty() ? null : Aspect.getAspect(tag));
    }

    public void cycleSortMode(Boolean backwards) {
        if (isClientSide()) {
            sendClientAction(ACTION_SORT, backwards);
            return;
        }
        host.setSortMode(host.getSortMode().next(backwards));
    }

    public void cycleViewMode(Boolean backwards) {
        if (isClientSide()) {
            sendClientAction(ACTION_VIEW, backwards);
            return;
        }
        ViewItems[] values = ViewItems.values();
        host.setViewMode(values[(host.getViewMode().ordinal() + (backwards ? values.length - 1 : 1)) % values.length]);
    }

    public void requestCraft(String tag) {
        if (isClientSide()) {
            sendClientAction(ACTION_CRAFT, tag);
            return;
        }
        Aspect aspect = Aspect.getAspect(tag);
        IGrid grid = host.getEssentiaGrid();
        if (aspect == null || grid == null || !(getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        EssentiaKey key = EssentiaKey.of(aspect);
        if (grid.getCraftingService().isCraftable(key)) {
            CraftAmountMenu.open(player, getLocator(), key, 1);
        }
    }

    public void interactWithHeldItem(String tag) {
        if (isClientSide()) {
            sendClientAction(ACTION_INTERACT, tag);
            return;
        }
        Aspect aspect = tag.isEmpty() ? null : Aspect.getAspect(tag);
        Player player = getPlayer();
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }
        ItemStack single = carried.copyWithCount(1);
        ItemStack simulated = transfer(single, aspect, Actionable.SIMULATE);
        if (simulated == single) {
            return;
        }
        if (carried.getCount() > 1 && player.getInventory().getSlotWithRemainingSpace(simulated) < 0 && player.getInventory().getFreeSlot() < 0) {
            return;
        }
        ItemStack result = transfer(single, aspect, Actionable.MODULATE);
        if (result == single) {
            return;
        }
        if (carried.getCount() > 1) {
            carried.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        } else {
            setCarried(result);
        }
        broadcastChanges();
        playTransferSound(EssentiaContainers.isLabel(result));
    }

    private @Nullable MEStorage storage() {
        IGrid grid = host.getEssentiaGrid();
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    private ItemStack transfer(ItemStack stack, @Nullable Aspect aspect, Actionable mode) {
        if (EssentiaContainers.isLabel(stack)) {
            workCounter++;
            return EssentiaContainers.withLabel(stack, aspect);
        }
        MEStorage storage = storage();
        if (storage == null || !EssentiaContainers.isContainer(stack)) {
            return stack;
        }
        if (EssentiaContainers.storedAmount(stack) == 0) {
            if (aspect == null) {
                return stack;
            }
            AEKey key = EssentiaKey.of(aspect);
            long available = storage.extract(key, EssentiaContainers.capacity(stack), Actionable.SIMULATE, getActionSource());
            EssentiaContainers.Result result = EssentiaContainers.fill(stack, aspect, (int) available);
            if (result == null) {
                return stack;
            }
            if (mode == Actionable.MODULATE) {
                long extracted = storage.extract(key, result.amount(), Actionable.MODULATE, getActionSource());
                if (extracted < result.amount()) {
                    storage.insert(key, extracted, Actionable.MODULATE, getActionSource());
                    return stack;
                }
                workCounter += result.amount();
            }
            return result.stack();
        }
        Aspect stored = EssentiaContainers.storedAspect(stack);
        AEKey key = EssentiaKey.of(stored);
        long accepted = storage.insert(key, EssentiaContainers.storedAmount(stack), Actionable.SIMULATE, getActionSource());
        EssentiaContainers.Result result = EssentiaContainers.drain(stack, (int) accepted);
        if (result == null) {
            return stack;
        }
        if (mode == Actionable.MODULATE) {
            long inserted = storage.insert(key, result.amount(), Actionable.MODULATE, getActionSource());
            if (inserted < result.amount()) {
                storage.extract(key, inserted, Actionable.MODULATE, getActionSource());
                return stack;
            }
            workCounter += result.amount();
        }
        return result.stack();
    }

    private boolean canMergeWithOutput(ItemStack result) {
        ItemStack output = outputSlot.getItem();
        if (output.isEmpty()) {
            return true;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize() && ItemStack.isSameItemSameComponents(output, result);
    }

    public void transferFromWorkSlots() {
        ItemStack input = inputSlot.getItem();
        if (input.isEmpty()) {
            return;
        }
        ItemStack output = outputSlot.getItem();
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return;
        }
        workCounter = 0;
        Aspect aspect = host.getSelectedAspect();
        boolean moved = false;
        boolean label = false;
        do {
            ItemStack container = input.copyWithCount(1);
            ItemStack simulated = transfer(container, aspect, Actionable.SIMULATE);
            if (simulated == container || !canMergeWithOutput(simulated)) {
                break;
            }
            ItemStack result = transfer(container, aspect, Actionable.MODULATE);
            if (result == container) {
                break;
            }
            if (outputSlot.hasItem()) {
                outputSlot.getItem().grow(1);
                outputSlot.setChanged();
            } else {
                outputSlot.set(result);
            }
            moved = true;
            label = EssentiaContainers.isLabel(result);
            input.shrink(1);
            inputSlot.setChanged();
        } while (!input.isEmpty() && workCounter < TRANSFER_PER_WORK_CYCLE);
        if (moved) {
            playTransferSound(label);
        }
    }

    private void playTransferSound(boolean label) {
        if (!(getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSoundTime <= MINIMUM_SOUND_WAIT) {
            return;
        }
        lastSoundTime = now;
        SoundEvent sound = label ? ModSounds.PAGE.get() : SoundEvents.GENERIC_SWIM;
        player.playNotifySound(sound, SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            Aspect selected = host.getSelectedAspect();
            selectedAspect = selected == null ? "" : selected.getTag();
            sortMode = host.getSortMode();
            viewMode = host.getViewMode();
            if (++tickCounter > WORK_TICK_RATE) {
                tickCounter = 0;
                transferFromWorkSlots();
                sendList();
            } else if (sentList == null) {
                sendList();
            }
        }
        super.broadcastChanges();
    }

    private void sendList() {
        List<EssentiaListPayload.Entry> entries = collectEntries(host.getEssentiaGrid());
        if (!entries.equals(sentList) && getPlayer() instanceof ServerPlayer player) {
            sentList = entries;
            TeNetwork.sendToPlayer(player, new EssentiaListPayload(containerId, entries));
        }
    }

    public static List<EssentiaListPayload.Entry> collectEntries(@Nullable IGrid grid) {
        if (grid == null) {
            return List.of();
        }
        Map<Aspect, Long> amounts = new LinkedHashMap<>();
        KeyCounter cached = grid.getStorageService().getCachedInventory();
        for (var entry : cached) {
            if (entry.getKey() instanceof EssentiaKey key && entry.getLongValue() > 0) {
                amounts.put(key.getAspect(), entry.getLongValue());
            }
        }
        Set<AEKey> craftables = grid.getCraftingService().getCraftables(EssentiaKeyType.TYPE.filter());
        for (AEKey key : craftables) {
            if (key instanceof EssentiaKey essentia) {
                amounts.putIfAbsent(essentia.getAspect(), 0L);
            }
        }
        List<EssentiaListPayload.Entry> entries = new ArrayList<>();
        for (Map.Entry<Aspect, Long> entry : amounts.entrySet()) {
            entries.add(new EssentiaListPayload.Entry(entry.getKey(), entry.getValue(), craftables.contains(EssentiaKey.of(entry.getKey()))));
        }
        return entries;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (slot == inputSlot || slot == outputSlot) {
            moveItemStackTo(stack, 2, slots.size(), true);
        } else if (inputSlot.mayPlace(stack)) {
            moveItemStackTo(stack, 0, 1, false);
        } else if (index < 29) {
            moveItemStackTo(stack, 29, slots.size(), false);
        } else {
            moveItemStackTo(stack, 2, 29, false);
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }
}
