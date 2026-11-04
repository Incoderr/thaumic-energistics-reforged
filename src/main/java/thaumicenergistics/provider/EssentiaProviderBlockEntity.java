package thaumicenergistics.provider;

import appeng.api.config.Actionable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaTransport;

public final class EssentiaProviderBlockEntity extends ProviderBlockEntity implements EssentiaTransport {
    private static final int TICK_RATE_IDLE = 15;
    private static final int TICK_RATE_URGENT = 5;

    private int tickCount;
    private int tickRate = TICK_RATE_IDLE;

    public EssentiaProviderBlockEntity(BlockPos pos, BlockState state) {
        super(ProviderFeature.ESSENTIA_PROVIDER_ENTITY.get(), pos, state, 3.0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssentiaProviderBlockEntity provider) {
        if (++provider.tickCount >= provider.tickRate) {
            provider.tickCount = 0;
            provider.tickRate = TICK_RATE_IDLE;
            provider.takeFromNeighbors();
        }
    }

    private void takeFromNeighbors() {
        for (Direction side : Direction.values()) {
            if (!(level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTransport source)) {
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
            if (aspect == null) {
                continue;
            }
            if (addEssentia(aspect, 1, Actionable.SIMULATE) == 1) {
                addEssentia(aspect, source.takeEssentia(aspect, 1, sourceSide), Actionable.MODULATE);
            }
        }
    }

    private int addEssentia(Aspect aspect, int amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = (int) insertIntoNetwork(aspect, amount, mode);
        if (mode == Actionable.MODULATE && accepted > 0) {
            tickRate = TICK_RATE_URGENT;
        }
        return accepted;
    }

    private @Nullable Aspect neighborWantedAspect(Direction side) {
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
        return neighbor instanceof EssentiaTransport transport ? transport.getSuctionType(side.getOpposite()) : null;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return addEssentia(aspect, amount, Actionable.MODULATE);
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return extractFromNetwork(aspect, amount, false);
    }

    @Override
    public @Nullable Aspect getEssentiaType(Direction face) {
        Aspect wanted = neighborWantedAspect(face);
        return wanted != null && networkAmount(wanted) > 0 ? wanted : null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        Aspect wanted = neighborWantedAspect(face);
        return wanted == null ? 0 : (int) Math.min(networkAmount(wanted), Integer.MAX_VALUE);
    }

    @Override
    public boolean isConnectable(Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return true;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return true;
    }

    @Override
    public void setSuction(@Nullable Aspect aspect, int amount) {
    }

    @Override
    public @Nullable Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return 8;
    }

    @Override
    public int getMinimumSuction() {
        return 1;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
