package thaumicenergistics.essentia;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import thaumcraft.aspect.Aspect;

public final class EssentiaNetwork {
    public static final double AE_PER_ESSENTIA = 0.3;

    private EssentiaNetwork() {
    }

    public static boolean hasPower(IEnergyService energy, long amount) {
        double request = AE_PER_ESSENTIA * amount;
        return energy.extractAEPower(request, Actionable.SIMULATE, PowerMultiplier.CONFIG) >= request;
    }

    public static void usePower(IEnergyService energy, long amount) {
        energy.extractAEPower(AE_PER_ESSENTIA * amount, Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    public static long insert(IGrid grid, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0) {
            return 0;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        EssentiaKey key = EssentiaKey.of(aspect);
        long accepted = storage.insert(key, amount, Actionable.SIMULATE, source);
        if (accepted <= 0 || !hasPower(grid.getEnergyService(), accepted)) {
            return 0;
        }
        if (mode == Actionable.SIMULATE) {
            return accepted;
        }
        long inserted = storage.insert(key, accepted, Actionable.MODULATE, source);
        usePower(grid.getEnergyService(), inserted);
        return inserted;
    }

    public static long extract(IGrid grid, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0 || !hasPower(grid.getEnergyService(), amount)) {
            return 0;
        }
        long extracted = grid.getStorageService().getInventory().extract(EssentiaKey.of(aspect), amount, mode, source);
        if (extracted > 0 && mode == Actionable.MODULATE) {
            usePower(grid.getEnergyService(), extracted);
        }
        return extracted;
    }
}
