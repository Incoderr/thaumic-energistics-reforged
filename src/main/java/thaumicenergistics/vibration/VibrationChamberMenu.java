package thaumicenergistics.vibration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;

public final class VibrationChamberMenu extends AbstractContainerMenu {
    private final @Nullable VibrationChamberBlockEntity chamber;
    private final DataSlot storedAmount = DataSlot.standalone();
    private final DataSlot storedAspect = DataSlot.standalone();
    private final DataSlot processingAspect = DataSlot.standalone();
    private final DataSlot powerPerTick = DataSlot.standalone();
    private final DataSlot maxPowerPerTick = DataSlot.standalone();
    private final DataSlot ticksRemaining = DataSlot.standalone();
    private final DataSlot totalTicks = DataSlot.standalone();

    public VibrationChamberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(VibrationFeature.MENU.get(), containerId);
        this.chamber = inventory.player.level().getBlockEntity(pos) instanceof VibrationChamberBlockEntity found ? found : null;
        addDataSlot(storedAmount);
        addDataSlot(storedAspect);
        addDataSlot(processingAspect);
        addDataSlot(powerPerTick);
        addDataSlot(maxPowerPerTick);
        addDataSlot(ticksRemaining);
        addDataSlot(totalTicks);
    }

    private static int aspectId(@Nullable Aspect aspect) {
        return VibrationChamberBlock.Processing.of(aspect).ordinal();
    }

    private static @Nullable Aspect aspect(int id) {
        return id == 1 ? Aspect.FIRE : id == 2 ? Aspect.ENERGY : null;
    }

    @Override
    public void broadcastChanges() {
        if (chamber != null && !chamber.getLevel().isClientSide()) {
            storedAmount.set(chamber.storedAmount());
            storedAspect.set(aspectId(chamber.storedAspect()));
            processingAspect.set(aspectId(chamber.processingAspect()));
            powerPerTick.set((int) Math.round(chamber.powerPerTick() * 100.0D));
            maxPowerPerTick.set((int) Math.round(chamber.maxPowerPerTick() * 100.0D));
            ticksRemaining.set(chamber.ticksRemaining());
            totalTicks.set(chamber.totalTicks());
        }
        super.broadcastChanges();
    }

    public int storedAmount() {
        return storedAmount.get();
    }

    public @Nullable Aspect storedAspect() {
        return aspect(storedAspect.get());
    }

    public @Nullable Aspect processingAspect() {
        return aspect(processingAspect.get());
    }

    public float powerPerTick() {
        return powerPerTick.get() / 100.0F;
    }

    public float maxPowerPerTick() {
        return maxPowerPerTick.get() / 100.0F;
    }

    public int ticksRemaining() {
        return ticksRemaining.get();
    }

    public int totalTicks() {
        return totalTicks.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return chamber != null && !chamber.isRemoved()
            && player.distanceToSqr(chamber.getBlockPos().getX() + 0.5D, chamber.getBlockPos().getY() + 0.5D, chamber.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }
}
