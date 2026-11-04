package thaumicenergistics.emitter;

import appeng.api.config.RedstoneMode;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import java.util.Set;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaItems;

public final class EssentiaLevelEmitterMenu extends AEBaseMenu {
    private static final int PLAYER_INVENTORY_Y = 102;
    private static final int HOTBAR_Y = 160;
    private static final Set<Long> ADJUSTMENTS = Set.of(1L, 10L, 100L, 1000L, 10000L, -1L, -10L, -100L, -1000L, -10000L);
    private static final String ACTION_ASPECT = "aspect";
    private static final String ACTION_THRESHOLD = "threshold";
    private static final String ACTION_ADJUST = "adjust";

    private final EssentiaLevelEmitterPart emitter;

    @GuiSync(10)
    public String trackedAspect = "";
    @GuiSync(11)
    public long threshold;
    @GuiSync(12)
    public RedstoneMode redstoneMode = RedstoneMode.HIGH_SIGNAL;

    public EssentiaLevelEmitterMenu(int containerId, Inventory inventory, EssentiaLevelEmitterPart emitter) {
        super(EmitterFeature.MENU.get(), containerId, inventory, emitter);
        this.emitter = emitter;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INVENTORY_Y + row * 18), SlotSemantics.PLAYER_INVENTORY);
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y), SlotSemantics.PLAYER_HOTBAR);
        }
        registerClientAction(ACTION_ASPECT, Boolean.class, this::clickAspect);
        registerClientAction(ACTION_THRESHOLD, Long.class, this::setThreshold);
        registerClientAction(ACTION_ADJUST, Long.class, this::adjustThreshold);
    }

    public EssentiaLevelEmitterPart getEmitter() {
        return emitter;
    }

    public @Nullable Aspect tracked() {
        return trackedAspect.isEmpty() ? null : Aspect.getAspect(trackedAspect);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            Aspect aspect = emitter.getTrackedAspect();
            trackedAspect = aspect == null ? "" : aspect.getTag();
            threshold = emitter.getThresholdLevel();
            redstoneMode = emitter.getRedstoneMode();
        }
        super.broadcastChanges();
    }

    public void clickAspect(boolean ignored) {
        if (isClientSide()) {
            sendClientAction(ACTION_ASPECT, true);
            return;
        }
        emitter.setTrackedAspect(EssentiaItems.filterAspect(getCarried()));
    }

    public void setThreshold(long value) {
        if (isClientSide()) {
            sendClientAction(ACTION_THRESHOLD, value);
            return;
        }
        emitter.setThresholdLevel(value);
    }

    public void adjustThreshold(long amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_ADJUST, amount);
            return;
        }
        if (ADJUSTMENTS.contains(amount)) {
            emitter.adjustThresholdLevel(amount);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        Aspect aspect = EssentiaItems.filterAspect(stack);
        if (aspect != null) {
            if (isServerSide()) {
                emitter.setTrackedAspect(aspect);
            }
            return ItemStack.EMPTY;
        }
        boolean inventory = index < 27;
        if (inventory ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false)) {
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }
}
