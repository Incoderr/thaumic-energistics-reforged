package thaumicenergistics.monitor;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.util.Platform;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaNetwork;

public final class EssentiaConversionMonitorPart extends EssentiaStorageMonitorPart {
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/essentia_conversion_monitor_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/essentia_conversion_monitor_on");
    private static final long DOUBLE_CLICK_TICKS = 2 * 20L;

    private @Nullable UUID depositedPlayer;
    private long depositedTick;
    private @Nullable Aspect depositedAspect;

    public EssentiaConversionMonitorPart(IPartItem<?> partItem) {
        super(partItem, MODEL_OFF, MODEL_ON);
    }

    @Override
    protected boolean onActivateWithAspectItem(Player player, InteractionHand hand, ItemStack heldItem) {
        if (getTrackedAspect() == null || !isLocked()) {
            return super.onActivateWithAspectItem(player, hand, heldItem);
        }
        return fillContainer(player, hand, heldItem);
    }

    private boolean fillContainer(Player player, InteractionHand hand, ItemStack heldItem) {
        Aspect tracked = getTrackedAspect();
        if (EssentiaContainers.isLabel(heldItem)) {
            player.setItemInHand(hand, EssentiaContainers.withLabel(heldItem, tracked));
            return true;
        }
        if (!EssentiaContainers.isContainer(heldItem)) {
            return false;
        }
        int stored = EssentiaContainers.storedAmount(heldItem);
        if (stored > 0 && EssentiaContainers.storedAspect(heldItem) != tracked) {
            return false;
        }
        Aspect label = EssentiaContainers.jarLabel(heldItem);
        if (label != null && label != tracked) {
            return false;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        int request = EssentiaContainers.capacity(heldItem) - stored;
        if (request <= 0) {
            return false;
        }
        IActionSource source = IActionSource.ofPlayer(player, this);
        long available = EssentiaNetwork.extract(grid, tracked, request, Actionable.SIMULATE, source);
        if (available <= 0) {
            return false;
        }
        EssentiaContainers.Result filled = EssentiaContainers.fill(heldItem.copyWithCount(1), tracked, (int) available);
        if (filled == null || filled.amount() <= 0) {
            return false;
        }
        long extracted = EssentiaNetwork.extract(grid, tracked, filled.amount(), Actionable.MODULATE, source);
        if (extracted < filled.amount()) {
            EssentiaNetwork.insert(grid, tracked, extracted, Actionable.MODULATE, source);
            return false;
        }
        heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, filled.stack());
        } else {
            give(player, filled.stack(), this::dropInFront);
        }
        player.containerMenu.broadcastChanges();
        return true;
    }

    private void dropInFront(ItemStack stack) {
        Platform.spawnDrops(getLevel(), getBlockEntity().getBlockPos().relative(getSide()), List.of(stack));
    }

    @Override
    protected boolean onShiftActivate(Player player, ItemStack heldItem) {
        if (!EssentiaContainers.isContainer(heldItem)) {
            return false;
        }
        if (wasDoubleClick(player)) {
            insertAll(player);
            return true;
        }
        boolean drained = drainContainer(player, player.getInventory().selected, null);
        if (drained) {
            depositedPlayer = player.getUUID();
            depositedTick = getLevel().getGameTime();
        }
        return drained;
    }

    private boolean wasDoubleClick(Player player) {
        if (depositedPlayer != null && depositedPlayer.equals(player.getUUID()) && getLevel().getGameTime() - depositedTick <= DOUBLE_CLICK_TICKS) {
            depositedPlayer = null;
            depositedTick = 0;
            return true;
        }
        return false;
    }

    private void insertAll(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            for (int guard = 0; guard < 64 && drainContainer(player, slot, depositedAspect); guard++) {
                if (inventory.getItem(slot).isEmpty()) {
                    break;
                }
            }
        }
    }

    private boolean drainContainer(Player player, int slot, @Nullable Aspect mustMatch) {
        Inventory inventory = player.getInventory();
        ItemStack container = inventory.getItem(slot);
        if (!EssentiaContainers.isContainer(container)) {
            return false;
        }
        Aspect aspect = EssentiaContainers.storedAspect(container);
        int amount = EssentiaContainers.storedAmount(container);
        if (aspect == null || amount <= 0 || mustMatch != null && aspect != mustMatch) {
            return false;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        IActionSource source = IActionSource.ofPlayer(player, this);
        long accepted = EssentiaNetwork.insert(grid, aspect, amount, Actionable.SIMULATE, source);
        if (accepted <= 0) {
            return false;
        }
        EssentiaContainers.Result drained = EssentiaContainers.drain(container.copyWithCount(1), (int) accepted);
        if (drained == null || drained.amount() <= 0) {
            return false;
        }
        long inserted = EssentiaNetwork.insert(grid, aspect, drained.amount(), Actionable.MODULATE, source);
        if (inserted < drained.amount()) {
            EssentiaNetwork.extract(grid, aspect, inserted, Actionable.MODULATE, source);
            return false;
        }
        if (container.getCount() == 1) {
            inventory.setItem(slot, drained.stack());
        } else {
            container.shrink(1);
            give(player, drained.stack(), stack -> player.drop(stack, false));
        }
        depositedAspect = aspect;
        return true;
    }
}
