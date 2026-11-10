package thaumicenergistics.gearbox;

import appeng.api.implementations.blockentities.ICrankable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

public final class GearBoxBlockEntity extends BlockEntity {
    private static final int BASE_POWER = 6;
    private static final int REQUIRED_POWER = 18;
    private static final int SIDE_COUNT = Direction.values().length;
    private static final int MIN_TICKS_PER_SYNC = 6;
    private static final float FULL_ROTATION = (float) Math.PI;
    private static final String CRANKABLES_KEY = "crankables";
    private static final String ROTATION_KEY = "shaftrotation";

    private final int[] shafts = new int[SIDE_COUNT];
    private final ICrankable[] crankables = new ICrankable[SIDE_COUNT];
    private final boolean[] canTurn = new boolean[SIDE_COUNT];
    private final boolean[] sideIsFacingCrankable = new boolean[SIDE_COUNT];
    private int crankableCount = -1;
    private boolean hasNewCranks;
    private int syncTickCount;
    private float shaftRotation;

    public GearBoxBlockEntity(BlockPos pos, BlockState state) {
        super(GearBoxFeature.ENTITY.get(), pos, state);
    }

    public boolean isThaumium() {
        return getBlockState().is(GearBoxFeature.THAUMIUM_GEAR_BOX.get());
    }

    public boolean isSideFacingCrankable(Direction side) {
        return sideIsFacingCrankable[side.get3DDataValue()];
    }

    public float shaftRotation() {
        return shaftRotation;
    }

    private int calculateTransferPower() {
        int divisor = 0;
        for (int side = 0; side < SIDE_COUNT; side++) {
            ICrankable crankable = crankables[side];
            if (crankable != null && crankable.canTurn()) {
                canTurn[side] = true;
                divisor++;
            }
        }
        return divisor == 0 ? 0 : BASE_POWER / divisor;
    }

    private void updateShafts(int power) {
        for (int side = 0; side < SIDE_COUNT; side++) {
            if (canTurn[side] && crankables[side] != null) {
                shafts[side] += power;
                if (shafts[side] >= REQUIRED_POWER) {
                    shafts[side] = 0;
                    crankables[side].applyTurn();
                }
            } else {
                shafts[side] = 0;
            }
        }
    }

    public boolean crankGearbox() {
        if (level == null || level.isClientSide()) {
            return true;
        }
        if (crankableCount < 0) {
            updateCrankables();
        }
        if (crankableCount <= 0) {
            return false;
        }
        int power = calculateTransferPower();
        if (power == 0) {
            return false;
        }
        updateShafts(power);
        hasNewCranks = true;
        return true;
    }

    public void updateCrankables() {
        if (level == null) {
            return;
        }
        crankableCount = 0;
        for (Direction side : Direction.values()) {
            int index = side.get3DDataValue();
            crankables[index] = null;
            sideIsFacingCrankable[index] = false;
            ICrankable crankable = ICrankable.get(level, worldPosition.relative(side), side.getOpposite());
            if (crankable != null) {
                crankableCount++;
                crankables[index] = crankable;
                sideIsFacingCrankable[index] = true;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GearBoxBlockEntity gearbox) {
        if (gearbox.crankableCount < 0) {
            gearbox.updateCrankables();
            gearbox.sync();
        }
        gearbox.syncTickCount++;
        if (gearbox.syncTickCount >= MIN_TICKS_PER_SYNC) {
            gearbox.syncTickCount--;
            if (gearbox.hasNewCranks) {
                gearbox.sync();
                gearbox.syncTickCount = 0;
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GearBoxBlockEntity gearbox) {
        if (gearbox.shaftRotation != 0) {
            gearbox.shaftRotation -= 0.20F;
            if (gearbox.shaftRotation < 0.001F) {
                gearbox.shaftRotation = 0;
            }
        }
    }

    public void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        byte flags = 0;
        for (int side = 0; side < SIDE_COUNT; side++) {
            if (sideIsFacingCrankable[side]) {
                flags |= (byte) (1 << side);
            }
        }
        tag.putByte(CRANKABLES_KEY, flags);
        if (hasNewCranks) {
            tag.putBoolean(ROTATION_KEY, true);
            hasNewCranks = false;
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readSync(packet.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSync(tag);
    }

    private void readSync(CompoundTag tag) {
        if (tag.contains(CRANKABLES_KEY)) {
            byte flags = tag.getByte(CRANKABLES_KEY);
            for (int side = 0; side < SIDE_COUNT; side++) {
                sideIsFacingCrankable[side] = (flags & 1 << side) != 0;
            }
        }
        if (tag.contains(ROTATION_KEY) && shaftRotation < 1.0F) {
            shaftRotation += FULL_ROTATION * 2;
        }
    }
}
