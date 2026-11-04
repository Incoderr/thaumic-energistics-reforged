package thaumicenergistics.buses;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.essentia.EssentiaReservoirBlockEntity;
import thaumcraft.essentia.VoidJarBlockEntity;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaNetwork;
import thaumicenergistics.essentia.EssentiaTileContainers;

final class EssentiaBusStorage implements MEStorage {
    private final EssentiaStorageBusPart bus;
    private @Nullable AspectContainer container;
    private @Nullable MEStorage external;
    private boolean busy;

    EssentiaBusStorage(EssentiaStorageBusPart bus) {
        this.bus = bus;
    }

    void setTarget(@Nullable AspectContainer container, @Nullable MEStorage external) {
        this.container = container;
        this.external = external;
    }

    @Nullable AspectContainer container() {
        return container;
    }

    @Nullable MEStorage external() {
        return external;
    }

    private boolean canTransfer(Aspect aspect) {
        return bus.allowAny() || bus.hasFilter(aspect) != bus.isInverted();
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null) {
            return false;
        }
        if (!bus.allowAny() && !bus.isInverted() && bus.hasFilter(aspect)) {
            return true;
        }
        return container != null && container.getAspects().getAmount(aspect) > 0;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null || amount <= 0 || busy || !canTransfer(aspect)) {
            return 0;
        }
        if (external != null) {
            busy = true;
            try {
                return external.insert(what, amount, mode, source);
            } finally {
                busy = false;
            }
        }
        AspectContainer target = container;
        IGrid grid = bus.getMainNode().getGrid();
        if (target == null || grid == null || !canAccept(target, aspect)) {
            return 0;
        }
        int request = (int) Math.min(amount, Integer.MAX_VALUE);
        boolean canVoid = bus.isVoidAllowed() && target instanceof VoidJarBlockEntity;
        int filled = canVoid ? request : EssentiaTileContainers.inject(target, request, aspect, false);
        if (filled <= 0 || !EssentiaNetwork.hasPower(grid.getEnergyService(), filled)) {
            return 0;
        }
        if (mode == Actionable.MODULATE) {
            int injected = EssentiaTileContainers.inject(target, request, aspect, true);
            EssentiaNetwork.usePower(grid.getEnergyService(), filled);
            return canVoid ? request : injected;
        }
        return filled;
    }

    private static boolean canAccept(AspectContainer target, Aspect aspect) {
        if (!EssentiaTileContainers.canInject(target)) {
            return false;
        }
        if (target instanceof EssentiaReservoirBlockEntity) {
            return true;
        }
        Aspect stored = EssentiaTileContainers.mainAspect(target);
        return stored == null || stored == aspect;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null || amount <= 0 || busy || !canTransfer(aspect)) {
            return 0;
        }
        if (external != null) {
            busy = true;
            try {
                return external.extract(what, amount, mode, source);
            } finally {
                busy = false;
            }
        }
        AspectContainer target = container;
        IGrid grid = bus.getMainNode().getGrid();
        if (target == null || grid == null) {
            return 0;
        }
        int request = (int) Math.min(amount, Integer.MAX_VALUE);
        int drained = EssentiaTileContainers.extract(target, request, aspect, false);
        if (drained <= 0 || !EssentiaNetwork.hasPower(grid.getEnergyService(), drained)) {
            return 0;
        }
        if (mode == Actionable.MODULATE) {
            drained = EssentiaTileContainers.extract(target, drained, aspect, true);
            EssentiaNetwork.usePower(grid.getEnergyService(), drained);
        }
        return drained;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (busy) {
            return;
        }
        if (external != null) {
            busy = true;
            try {
                KeyCounter stacks = new KeyCounter();
                external.getAvailableStacks(stacks);
                for (Object2LongMap.Entry<AEKey> entry : stacks) {
                    Aspect aspect = EssentiaKey.aspectOf(entry.getKey());
                    if (aspect != null && canTransfer(aspect)) {
                        out.add(entry.getKey(), entry.getLongValue());
                    }
                }
            } finally {
                busy = false;
            }
            return;
        }
        AspectContainer target = container;
        if (target == null || !EssentiaTileContainers.canExtract(target)) {
            return;
        }
        for (Map.Entry<Aspect, Integer> entry : EssentiaTileContainers.contents(target).entrySet()) {
            if (entry.getValue() > 0 && canTransfer(entry.getKey())) {
                out.add(EssentiaKey.of(entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public Component getDescription() {
        return bus.getPartItem().asItem().getDescription();
    }
}
