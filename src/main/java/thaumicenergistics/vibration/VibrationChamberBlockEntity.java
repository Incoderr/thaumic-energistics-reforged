package thaumicenergistics.vibration;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.EssentiaTransport;

public final class VibrationChamberBlockEntity extends AENetworkedBlockEntity implements IGridTickable, EssentiaTransport, AspectContainer {
    public static final int MAX_ESSENTIA_STORED = 64;
    public static final double BASE_POWER_PER_TICK = 5.0D;
    public static final int PROCESS_SPEED_MAX = 200;
    public static final int PROCESS_SPEED_MIN = 20;
    private static final int TICKRATE_MIN = 10;
    private static final int TICKRATE_MAX = 40;
    private static final double DILATATION_DIVISOR = 100;
    private static final String STORED_ASPECT_KEY = "StoredAspect";
    private static final String STORED_AMOUNT_KEY = "StoredAmount";
    private static final String TIME_REMAINING_KEY = "TRemain";
    private static final String PROCESSING_SPEED_KEY = "ProcSpeed";
    private static final String POWER_PER_TICK_KEY = "PwrPerTick";
    private static final String PROCESSING_ASPECT_KEY = "ProcAspect";

    private static int coalBurnTime;

    private @Nullable Aspect storedAspect;
    private int storedAmount;
    private double dilation = 1.0D;
    private int processingTicksRemaining;
    private int processingSpeed;
    private double powerProducedPerProcessingTick;
    private @Nullable Aspect processingAspect;
    private int syncTotalProcessingTicks;
    private double syncPowerPerTick;

    public VibrationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(VibrationFeature.ENTITY.get(), pos, state);
        getMainNode().setIdlePowerUsage(0.0D).addService(IGridTickable.class, this);
    }

    public static boolean acceptsAspect(@Nullable Aspect aspect) {
        return aspect == Aspect.FIRE || aspect == Aspect.ENERGY;
    }

    private Direction forward() {
        return getBlockState().getValue(VibrationChamberBlock.FACING);
    }

    public boolean hasStoredEssentia() {
        return storedAspect != null && storedAmount > 0;
    }

    public @Nullable Aspect storedAspect() {
        return hasStoredEssentia() ? storedAspect : null;
    }

    public int storedAmount() {
        return hasStoredEssentia() ? storedAmount : 0;
    }

    public @Nullable Aspect processingAspect() {
        return processingAspect;
    }

    public double powerPerTick() {
        return syncPowerPerTick;
    }

    public double maxPowerPerTick() {
        return powerProducedPerProcessingTick * (PROCESS_SPEED_MAX / DILATATION_DIVISOR);
    }

    public int ticksRemaining() {
        clampProcessingSpeed();
        return (int) (processingTicksRemaining / dilation);
    }

    public int totalTicks() {
        return (int) (syncTotalProcessingTicks / dilation);
    }

    public void setStored(@Nullable Aspect aspect, int amount) {
        storedAspect = amount > 0 ? aspect : null;
        storedAmount = amount > 0 ? amount : 0;
        changed();
    }

    public boolean hasSaveDataForDismantle() {
        return hasStoredEssentia() || processingTicksRemaining > 0;
    }

    private void changed() {
        saveChanges();
        markForUpdate();
    }

    private int adjustProcessingValues() {
        if (coalBurnTime == 0) {
            coalBurnTime = new ItemStack(Items.COAL).getBurnTime(null);
        }
        int time = 0;
        if (storedAspect == Aspect.FIRE) {
            time = coalBurnTime / 2;
            powerProducedPerProcessingTick = BASE_POWER_PER_TICK * 1.0D;
        } else if (storedAspect == Aspect.ENERGY) {
            time = (int) (coalBurnTime / 1.6F);
            powerProducedPerProcessingTick = BASE_POWER_PER_TICK * 1.6D;
        }
        syncTotalProcessingTicks = time;
        return time;
    }

    private void clampProcessingSpeed() {
        processingSpeed = Math.max(PROCESS_SPEED_MIN, Math.min(PROCESS_SPEED_MAX, processingSpeed));
    }

    private void setProcessingAspect(@Nullable Aspect aspect) {
        processingAspect = aspect;
        if (level != null && !level.isClientSide() && !isRemoved()) {
            VibrationChamberBlock.Processing state = VibrationChamberBlock.Processing.of(aspect);
            BlockState blockState = getBlockState();
            if (blockState.getValue(VibrationChamberBlock.PROCESSING) != state) {
                level.setBlockAndUpdate(worldPosition, blockState.setValue(VibrationChamberBlock.PROCESSING, state));
            }
        }
    }

    private boolean consumeEssentia() {
        if (hasStoredEssentia()) {
            int time = adjustProcessingValues();
            if (time > 0) {
                setProcessingAspect(storedAspect);
                storedAmount--;
                if (storedAmount <= 0) {
                    storedAspect = null;
                    storedAmount = 0;
                }
                changed();
                processingTicksRemaining += time;
                return true;
            }
        }
        return false;
    }

    private TickRateModulation doProcessingTick(int ticksSinceLastCall) {
        if (processingTicksRemaining == 0) {
            return TickRateModulation.IDLE;
        }
        clampProcessingSpeed();
        dilation = processingSpeed / DILATATION_DIVISOR;
        double processingTicks = ticksSinceLastCall * dilation;
        processingTicksRemaining = (int) (processingTicksRemaining - processingTicks);
        syncPowerPerTick = processingTicks * powerProducedPerProcessingTick / ticksSinceLastCall;
        if (processingTicksRemaining <= 0) {
            processingTicks += processingTicksRemaining;
            processingTicksRemaining = 0;
            setProcessingAspect(null);
            changed();
        }
        double producedPower = processingTicks * powerProducedPerProcessingTick;
        TickRateModulation rate = TickRateModulation.SLOWER;
        IGrid grid = getMainNode().getGrid();
        if (grid != null) {
            double rejectedPower = grid.getEnergyService().injectPower(producedPower, Actionable.SIMULATE);
            if (rejectedPower > 0) {
                producedPower = Math.max(0, producedPower - rejectedPower);
                processingSpeed -= ticksSinceLastCall;
            } else {
                rate = TickRateModulation.FASTER;
                processingSpeed += ticksSinceLastCall;
            }
            if (producedPower > 0) {
                grid.getEnergyService().injectPower(producedPower, Actionable.MODULATE);
            }
        }
        return rate;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TICKRATE_MIN, TICKRATE_MAX, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        boolean replenish = false;
        TickRateModulation rate = TickRateModulation.IDLE;
        if (processingTicksRemaining > 0) {
            rate = doProcessingTick(ticksSinceLastCall);
        } else {
            syncPowerPerTick = 0;
            processingSpeed = PROCESS_SPEED_MIN;
        }
        if (hasStoredEssentia()) {
            if (processingTicksRemaining == 0 && consumeEssentia()) {
                rate = TickRateModulation.URGENT;
            }
            if (storedAmount < MAX_ESSENTIA_STORED) {
                replenish = true;
            }
        } else {
            replenish = true;
        }
        if (replenish) {
            takeEssentiaFromTransportNeighbors();
        }
        return rate;
    }

    private void takeEssentiaFromTransportNeighbors() {
        if (level == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (!canInputFrom(side)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (!(neighbor instanceof EssentiaTransport source)) {
                continue;
            }
            Direction sourceSide = side.getOpposite();
            if (!source.canOutputTo(sourceSide) || source.getEssentiaAmount(sourceSide) <= 0) {
                continue;
            }
            int suction = getSuctionAmount(side);
            if (suction < source.getMinimumSuction() || suction < source.getSuctionAmount(sourceSide)) {
                continue;
            }
            Aspect aspect = source.getEssentiaType(sourceSide);
            if (aspect != null && addEssentia(aspect, 1, false) == 1) {
                addEssentia(aspect, source.takeEssentia(aspect, 1, sourceSide), true);
            }
        }
    }

    public int addEssentia(Aspect aspect, int amount, boolean modulate) {
        if (hasStoredEssentia() && storedAspect != aspect) {
            return 0;
        }
        int added = Math.min(amount, MAX_ESSENTIA_STORED - storedAmount());
        if (added > 0 && modulate) {
            storedAspect = aspect;
            storedAmount = storedAmount() + added;
            changed();
        }
        return Math.max(added, 0);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return face != forward();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != forward();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(@Nullable Aspect aspect, int amount) {
    }

    @Override
    public @Nullable Aspect getSuctionType(Direction face) {
        if (hasStoredEssentia()) {
            return storedAspect;
        }
        int tick = level != null && level.getServer() != null ? level.getServer().getTickCount() : 0;
        return tick % 200 > 100 ? Aspect.ENERGY : Aspect.FIRE;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        float stored = storedAmount();
        if (stored == MAX_ESSENTIA_STORED) {
            return 0;
        }
        return (int) (128 * (1.0F - stored * 0.75F / MAX_ESSENTIA_STORED));
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return addEssentia(aspect, amount, true);
    }

    @Override
    public @Nullable Aspect getEssentiaType(Direction face) {
        return storedAspect();
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return storedAmount();
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public AspectList getAspects() {
        AspectList list = new AspectList();
        if (hasStoredEssentia()) {
            list.add(storedAspect, storedAmount);
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return hasStoredEssentia() ? aspect == storedAspect : acceptsAspect(aspect);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return amount - addEssentia(aspect, amount, true);
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return storedAspect() == aspect && storedAmount >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return storedAspect() == aspect ? storedAmount : 0;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        storedAspect = tag.contains(STORED_ASPECT_KEY) ? Aspect.getAspect(tag.getString(STORED_ASPECT_KEY)) : null;
        storedAmount = storedAspect == null ? 0 : tag.getInt(STORED_AMOUNT_KEY);
        processingTicksRemaining = tag.getInt(TIME_REMAINING_KEY);
        processingAspect = null;
        if (processingTicksRemaining > 0) {
            processingSpeed = tag.getInt(PROCESSING_SPEED_KEY);
            powerProducedPerProcessingTick = tag.getDouble(POWER_PER_TICK_KEY);
            if (tag.contains(PROCESSING_ASPECT_KEY)) {
                processingAspect = Aspect.getAspect(tag.getString(PROCESSING_ASPECT_KEY));
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (hasStoredEssentia()) {
            tag.putString(STORED_ASPECT_KEY, storedAspect.getTag());
            tag.putInt(STORED_AMOUNT_KEY, storedAmount);
        }
        if (processingTicksRemaining > 0) {
            tag.putInt(TIME_REMAINING_KEY, processingTicksRemaining);
            tag.putInt(PROCESSING_SPEED_KEY, processingSpeed);
            tag.putDouble(POWER_PER_TICK_KEY, powerProducedPerProcessingTick);
            if (processingAspect != null) {
                tag.putString(PROCESSING_ASPECT_KEY, processingAspect.getTag());
            }
        }
    }
}
