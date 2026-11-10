package thaumicenergistics.golem;

import appeng.api.config.Actionable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import javax.annotation.Nullable;
import thaumcraft.alchemy.AlembicBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.Marker;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.platform.GolemFluids;
import thaumcraft.platform.Transfer;

public abstract class GolemWifiGoal extends Goal {
    public static final int CORE_FILL = 0;
    public static final int CORE_GATHER = 2;
    public static final int CORE_LIQUID = 5;
    public static final int CORE_ESSENTIA = 6;
    private static final int UPGRADE_ORDER = 4;
    private static final int NETWORK_COOLDOWN = 20;
    private static final int[] FLUID_RATES = {100, 250, 500};
    private static final int[] ITEM_RATES = {8, 24, 32};
    private static final int[] ESS_RATES = {4, 12, 16};
    private static final int BUCKET = 1000;

    protected final GolemBase golem;
    protected final GolemNetworkLink network;
    private int actionTimer;

    protected GolemWifiGoal(GolemBase golem, GlobalPos link) {
        this.golem = golem;
        int rateMult = golem.isAdvanced() ? 2 : 1;
        int order = Math.clamp(golem.getUpgradeAmount(UPGRADE_ORDER), 0, 2);
        this.network = new GolemNetworkLink(golem, link, ITEM_RATES[order] * rateMult, FLUID_RATES[order] * rateMult, ESS_RATES[order] * rateMult);
    }

    public static boolean supportsCore(int core) {
        return core == CORE_FILL || core == CORE_GATHER || core == CORE_LIQUID || core == CORE_ESSENTIA;
    }

    public static int priority(int core) {
        return switch (core) {
            case CORE_GATHER -> 1;
            case CORE_FILL -> 3;
            default -> 2;
        };
    }

    public static @Nullable GolemWifiGoal create(GolemBase golem, GlobalPos link) {
        return switch (golem.getCore()) {
            case CORE_GATHER -> new Gather(golem, link);
            case CORE_FILL -> new Fill(golem, link);
            case CORE_LIQUID -> new Liquid(golem, link);
            case CORE_ESSENTIA -> new Essentia(golem, link);
            default -> null;
        };
    }

    public boolean isConnected() {
        return network.isConnected();
    }

    public boolean wasInRange() {
        return network.wasInRange();
    }

    protected abstract boolean needsNetworkNow();

    protected abstract void updateTask();

    @Override
    public final boolean canUse() {
        if (!golem.isWorking()) {
            return false;
        }
        if (actionTimer > 0) {
            actionTimer--;
            return false;
        }
        actionTimer = NETWORK_COOLDOWN;
        return network.isConnected() && needsNetworkNow();
    }

    @Override
    public final boolean canContinueToUse() {
        return false;
    }

    @Override
    public final void start() {
        updateTask();
    }

    protected static BlockPos homeContainer(GolemBase golem) {
        return golem.getRestrictCenter().relative(golem.getHomeFacing().getOpposite());
    }

    protected static List<ItemStack> filters(GolemBase golem) {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < golem.inventory().getContainerSize(); slot++) {
            ItemStack stack = golem.inventory().getItem(slot);
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
        }
        return result;
    }

    private static final class Gather extends GolemWifiGoal {
        private Gather(GolemBase golem, GlobalPos link) {
            super(golem, link);
        }

        @Override
        protected boolean needsNetworkNow() {
            return !golem.getCarried().isEmpty();
        }

        @Override
        protected void updateTask() {
            ItemStack held = golem.getCarried().copy();
            if (held.isEmpty()) {
                return;
            }
            network.depositStack(held);
            golem.setCarried(held);
            if (held.isEmpty()) {
                golem.startActionTimer();
            }
        }
    }

    private static final class Fill extends GolemWifiGoal {
        private ItemStack watched = ItemStack.EMPTY;
        private int watchedAmount;

        private Fill(GolemBase golem, GlobalPos link) {
            super(golem, link);
        }

        @Override
        protected boolean needsNetworkNow() {
            watched = ItemStack.EMPTY;
            if (!golem.getCarried().isEmpty()) {
                return false;
            }
            Level level = golem.level();
            BlockPos home = homeContainer(golem);
            Direction side = golem.getHomeFacing();
            boolean fillAll = golem.getToggles()[0];
            if (!fillAll && !Transfer.hasItems(level, home, side)) {
                return false;
            }
            List<ItemStack> filters = filters(golem);
            for (ItemStack filter : filters) {
                int missing = fillAll ? golem.getCarrySpace() : required(filters, filter) - Transfer.count(level, home, side, stack -> golem.matchesFilter(filter, stack));
                if (missing > 0) {
                    watched = filter;
                    watchedAmount = Math.min(missing, golem.getCarrySpace());
                    return true;
                }
            }
            return false;
        }

        private int required(List<ItemStack> filters, ItemStack stack) {
            int total = 0;
            for (ItemStack filter : filters) {
                if (ItemStack.isSameItemSameComponents(filter, stack)) {
                    total += filter.getCount();
                }
            }
            return total;
        }

        @Override
        protected void updateTask() {
            if (watched.isEmpty()) {
                return;
            }
            ItemStack extracted = network.extractStack(watched, watchedAmount);
            if (!extracted.isEmpty()) {
                golem.setCarried(extracted);
                watched = ItemStack.EMPTY;
            }
        }
    }

    private static final class Liquid extends GolemWifiGoal {
        private FluidStack wantedFluid = FluidStack.EMPTY;

        private Liquid(GolemBase golem, GlobalPos link) {
            super(golem, link);
        }

        private List<FluidStack> missingLiquids() {
            List<FluidStack> result = new ArrayList<>();
            BlockPos home = homeContainer(golem);
            for (ItemStack filter : filters(golem)) {
                FluidStack contained = GolemFluids.contained(filter);
                if (contained.isEmpty() || result.stream().anyMatch(existing -> FluidStack.isSameFluidSameComponents(existing, contained))) {
                    continue;
                }
                FluidStack candidate = contained.copyWithAmount(BUCKET);
                if (Transfer.insertFluid(golem.level(), home, golem.getHomeFacing(), candidate, true).getAmount() < candidate.getAmount()) {
                    result.add(candidate);
                }
            }
            return result;
        }

        @Override
        protected boolean needsNetworkNow() {
            List<FluidStack> fluids = missingLiquids();
            if (fluids.size() == 1) {
                wantedFluid = fluids.getFirst().copy();
                FluidStack carried = golem.getFluidCarried();
                if (carried.isEmpty() || FluidStack.isSameFluidSameComponents(carried, wantedFluid)) {
                    return true;
                }
                wantedFluid = FluidStack.EMPTY;
            }
            return false;
        }

        @Override
        protected void updateTask() {
            if (wantedFluid.isEmpty()) {
                return;
            }
            FluidStack carried = golem.getFluidCarried();
            int existing = carried.isEmpty() ? 0 : carried.getAmount();
            FluidStack extracted = network.extractFluid(wantedFluid, BUCKET - existing);
            if (extracted.isEmpty()) {
                return;
            }
            golem.setFluidCarried(existing > 0 ? carried.copyWithAmount(existing + extracted.getAmount()) : extracted);
            wantedFluid = FluidStack.EMPTY;
        }
    }

    private static final class Essentia extends GolemWifiGoal {
        private static final int MAX_CHECKED = 200;
        private static final int ALEMBIC_HEIGHT = 6;

        private final Set<JarBlockEntity> checkedJars = new HashSet<>();
        private @Nullable JarBlockEntity targetJar;
        private double targetDistance;
        private boolean targetVoid;
        private double golemRange;
        private boolean modeFill;

        private Essentia(GolemBase golem, GlobalPos link) {
            super(golem, link);
        }

        private static @Nullable Aspect wanted(JarBlockEntity jar) {
            return jar.getAspectFilter() != null ? jar.getAspectFilter() : jar.getAspect();
        }

        private void checkConnectedJars(Level level, BlockPos home, BlockPos jarPos) {
            if (checkedJars.size() >= MAX_CHECKED) {
                return;
            }
            for (Direction side : Direction.values()) {
                if (level.getBlockEntity(jarPos.relative(side)) instanceof JarBlockEntity jar && setTargetIfValid(jar, home) > 2) {
                    checkConnectedJars(level, home, jar.getBlockPos());
                }
            }
        }

        private void findClosestValidJar() {
            if (targetJar != null && golem.level().getBlockEntity(targetJar.getBlockPos()) == targetJar) {
                Aspect aspect = wanted(targetJar);
                if (aspect != null && (targetVoid || targetJar.getAmount() < targetJar.getMaxAmount())
                    && network.extractEssentia(aspect, 1, Actionable.SIMULATE) > 0) {
                    return;
                }
            }
            targetJar = null;
            targetDistance = Double.MAX_VALUE;
            targetVoid = false;
            Level level = golem.level();
            golemRange = golem.getRange() * golem.getRange();
            BlockPos home = golem.getRestrictCenter();
            for (Marker marker : golem.getMarkers()) {
                if (!marker.inLevel(level) || !(level.getBlockEntity(marker.pos()) instanceof JarBlockEntity jar)) {
                    continue;
                }
                setTargetIfValid(jar, home);
                checkConnectedJars(level, home, marker.pos());
            }
            checkedJars.clear();
        }

        private int setTargetIfValid(JarBlockEntity jar, BlockPos home) {
            if (!checkedJars.add(jar)) {
                return 2;
            }
            double distance = jar.getBlockPos().distToCenterSqr(home.getX(), home.getY(), home.getZ());
            if (distance > golemRange) {
                return 1;
            }
            boolean isVoid = jar.isVoid();
            if (!(targetVoid && !isVoid) && distance > targetDistance) {
                return 1;
            }
            if (jar.getAspect() == null && jar.getAspectFilter() == null) {
                return 3;
            }
            if (!isVoid && jar.getAmount() >= jar.getMaxAmount()) {
                return 3;
            }
            targetJar = jar;
            targetDistance = distance;
            targetVoid = isVoid;
            return 4;
        }

        @Override
        protected boolean needsNetworkNow() {
            Level level = golem.level();
            BlockPos source = homeContainer(golem);
            BlockEntity entity = level.getBlockEntity(source);
            modeFill = !(entity instanceof EssentiaTransport);
            for (int offset = 1; offset < ALEMBIC_HEIGHT && modeFill; offset++) {
                modeFill = !(level.getBlockEntity(source.above(offset)) instanceof AlembicBlockEntity);
            }
            if (modeFill) {
                if (golem.getEssentiaAmount() > 0) {
                    return false;
                }
                findClosestValidJar();
                return targetJar != null;
            }
            return golem.getNavigation().isDone() && golem.getEssentiaAmount() > 0;
        }

        @Override
        protected void updateTask() {
            if (modeFill) {
                if (targetJar == null) {
                    return;
                }
                Aspect aspect = wanted(targetJar);
                if (aspect == null) {
                    targetJar = null;
                    return;
                }
                int amount = targetJar.getMaxAmount();
                if (!targetVoid) {
                    amount -= targetJar.getAmount();
                    if (amount < 1) {
                        targetJar = null;
                        return;
                    }
                }
                int extracted = (int) network.extractEssentia(aspect, amount, Actionable.MODULATE);
                if (extracted > 0) {
                    golem.setEssentia(aspect, extracted);
                }
                return;
            }
            Aspect carried = golem.getEssentia();
            if (carried == null || golem.getEssentiaAmount() <= 0 || !golem.getNavigation().isDone()) {
                return;
            }
            int injected = (int) network.insertEssentia(carried, golem.getEssentiaAmount());
            if (injected > 0) {
                golem.setEssentia(carried, golem.getEssentiaAmount() - injected);
            }
        }
    }
}
