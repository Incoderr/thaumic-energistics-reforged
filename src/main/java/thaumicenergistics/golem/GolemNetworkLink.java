package thaumicenergistics.golem;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.entity.golem.GolemBase;
import thaumicenergistics.essentia.EssentiaNetwork;

public final class GolemNetworkLink {
    private static final double FLUID_POWER_DIVISOR = 100.0;

    private final GolemBase golem;
    private final GlobalPos link;
    private final int maxItemRate;
    private final int maxFluidRate;
    private final int maxEssentiaRate;
    private @Nullable IWirelessAccessPoint accessPoint;
    private boolean inRange;

    public GolemNetworkLink(GolemBase golem, GlobalPos link, int maxItems, int maxFluids, int maxEssentia) {
        this.golem = golem;
        this.link = link;
        this.maxItemRate = maxItems;
        this.maxFluidRate = maxFluids;
        this.maxEssentiaRate = maxEssentia;
    }

    private BlockPos golemPos() {
        return BlockPos.containing(golem.getX(), golem.getY(), golem.getZ());
    }

    private static boolean inRange(IWirelessAccessPoint accessPoint, Level level, BlockPos pos) {
        if (!accessPoint.getLocation().isInWorld(level)) {
            return false;
        }
        double range = accessPoint.getRange();
        return accessPoint.getLocation().getPos().distSqr(pos) <= range * range;
    }

    private @Nullable IGrid linkedGrid() {
        if (!(golem.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerLevel level = serverLevel.getServer().getLevel(link.dimension());
        if (level == null || !level.isLoaded(link.pos())) {
            return null;
        }
        return level.getBlockEntity(link.pos()) instanceof IWirelessAccessPoint linked ? linked.getGrid() : null;
    }

    private boolean linkWithNewAccessPoint() {
        IGrid grid = accessPoint != null && accessPoint.getGrid() != null ? accessPoint.getGrid() : linkedGrid();
        accessPoint = null;
        if (grid == null) {
            return false;
        }
        Level level = golem.level();
        BlockPos pos = golemPos();
        double closest = Double.MAX_VALUE;
        for (WirelessAccessPointBlockEntity candidate : grid.getMachines(WirelessAccessPointBlockEntity.class)) {
            if (!candidate.isActive() || !inRange(candidate, level, pos)) {
                continue;
            }
            double distance = candidate.getLocation().getPos().distSqr(pos);
            if (distance < closest) {
                closest = distance;
                accessPoint = candidate;
            }
        }
        return accessPoint != null;
    }

    public boolean isConnected() {
        if (accessPoint != null && accessPoint.isActive() && inRange(accessPoint, golem.level(), golemPos())) {
            inRange = true;
            return true;
        }
        inRange = linkWithNewAccessPoint();
        return inRange;
    }

    public boolean wasInRange() {
        return inRange;
    }

    private @Nullable IGrid grid() {
        return accessPoint != null && isConnected() ? accessPoint.getGrid() : null;
    }

    private IActionSource source() {
        return IActionSource.ofMachine(accessPoint);
    }

    public void depositStack(ItemStack stack) {
        IGrid grid = grid();
        if (grid == null || stack.isEmpty()) {
            return;
        }
        int depositSize = Math.min(stack.getCount(), maxItemRate);
        long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), AEItemKey.of(stack), depositSize, source());
        stack.shrink((int) inserted);
    }

    public ItemStack extractStack(ItemStack target, int amount) {
        IGrid grid = grid();
        AEItemKey key = AEItemKey.of(target);
        if (grid == null || key == null) {
            return ItemStack.EMPTY;
        }
        long extracted = StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(), key, Math.min(amount, maxItemRate), source());
        return extracted > 0 ? key.toStack((int) extracted) : ItemStack.EMPTY;
    }

    public FluidStack extractFluid(FluidStack target, int amount) {
        IGrid grid = grid();
        if (grid == null || target.isEmpty()) {
            return FluidStack.EMPTY;
        }
        IEnergyService energy = grid.getEnergyService();
        long requestSize = Math.min(amount, maxFluidRate);
        double powerRequired = requestSize / FLUID_POWER_DIVISOR;
        if (energy.extractAEPower(powerRequired, Actionable.SIMULATE, PowerMultiplier.CONFIG) < powerRequired) {
            return FluidStack.EMPTY;
        }
        AEFluidKey key = AEFluidKey.of(target);
        MEStorage storage = grid.getStorageService().getInventory();
        long extracted = storage.extract(key, requestSize, Actionable.MODULATE, source());
        if (extracted <= 0) {
            return FluidStack.EMPTY;
        }
        energy.extractAEPower(extracted / FLUID_POWER_DIVISOR, Actionable.MODULATE, PowerMultiplier.CONFIG);
        return key.toStack((int) extracted);
    }

    public long extractEssentia(Aspect aspect, int amount, Actionable mode) {
        IGrid grid = grid();
        return grid == null ? 0 : EssentiaNetwork.extract(grid, aspect, Math.min(amount, maxEssentiaRate), mode, source());
    }

    public long insertEssentia(Aspect aspect, int amount) {
        IGrid grid = grid();
        return grid == null ? 0 : EssentiaNetwork.insert(grid, aspect, Math.min(amount, maxEssentiaRate), Actionable.MODULATE, source());
    }
}
