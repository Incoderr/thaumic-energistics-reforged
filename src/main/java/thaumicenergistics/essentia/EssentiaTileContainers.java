package thaumicenergistics.essentia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nullable;
import thaumcraft.alchemy.AdvancedAlchemicalFurnaceNozzleBlockEntity;
import thaumcraft.alchemy.AlembicBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.CentrifugeBlockEntity;
import thaumcraft.essentia.EssentiaBufferBlockEntity;
import thaumcraft.essentia.EssentiaReservoirBlockEntity;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.essentia.VoidJarBlockEntity;

public final class EssentiaTileContainers {
    private static final Map<Class<?>, Permission> PERMISSIONS = new HashMap<>();

    private record Permission(int capacity, boolean extract, boolean inject) {
    }

    static {
        extractOnly(AlembicBlockEntity.class, 32);
        extractOnly(CentrifugeBlockEntity.class, 0);
        both(JarBlockEntity.class, 64);
        both(VoidJarBlockEntity.class, 64);
        extractOnly(EssentiaBufferBlockEntity.class, 8);
        both(EssentiaReservoirBlockEntity.class, 256);
        extractOnly(AdvancedAlchemicalFurnaceNozzleBlockEntity.class, 0);
    }

    private EssentiaTileContainers() {
    }

    public static void extractOnly(Class<? extends AspectContainer> type, int capacity) {
        register(type, capacity, true, false);
    }

    public static void injectOnly(Class<? extends AspectContainer> type, int capacity) {
        register(type, capacity, false, true);
    }

    public static void both(Class<? extends AspectContainer> type, int capacity) {
        register(type, capacity, true, true);
    }

    private static synchronized void register(Class<? extends AspectContainer> type, int capacity, boolean extract, boolean inject) {
        Permission old = PERMISSIONS.get(type);
        PERMISSIONS.put(type, new Permission(capacity, extract || old != null && old.extract(), inject || old != null && old.inject()));
    }

    public static @Nullable AspectContainer container(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof AspectContainer container ? container : null;
    }

    public static boolean canExtract(AspectContainer container) {
        Permission permission = PERMISSIONS.get(container.getClass());
        return permission != null && permission.extract();
    }

    public static boolean canInject(AspectContainer container) {
        Permission permission = PERMISSIONS.get(container.getClass());
        return permission != null && permission.inject();
    }

    public static int capacity(AspectContainer container) {
        Permission permission = PERMISSIONS.get(container.getClass());
        return permission == null ? -1 : permission.capacity();
    }

    public static @Nullable Aspect mainAspect(@Nullable AspectContainer container) {
        if (container == null) {
            return null;
        }
        AspectList aspects = container.getAspects();
        if (aspects == null) {
            return null;
        }
        List<Aspect> sorted = aspects.getAspectsSortedAmount();
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public static Map<Aspect, Integer> contents(@Nullable AspectContainer container) {
        Map<Aspect, Integer> result = new HashMap<>();
        if (container == null || container.getAspects() == null) {
            return result;
        }
        AspectList aspects = container.getAspects();
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (amount != 0) {
                result.put(aspect, amount);
            }
        }
        return result;
    }

    public static int storedAmount(AspectContainer container) {
        int stored = 0;
        for (int amount : contents(container).values()) {
            stored += amount;
        }
        return stored;
    }

    public static int extract(AspectContainer container, int amount, Aspect aspect, boolean modulate) {
        if (amount == 0 || !canExtract(container)) {
            return 0;
        }
        int stored = container.getAspects().getAmount(aspect);
        if (stored == 0) {
            return 0;
        }
        int drained = Math.min(amount, stored);
        if (modulate) {
            container.takeFromContainer(aspect, drained);
        }
        return drained;
    }

    public static int inject(AspectContainer container, int amount, Aspect aspect, boolean modulate) {
        if (!canInject(container)) {
            return 0;
        }
        Aspect stored = mainAspect(container);
        if (stored != null && container instanceof JarBlockEntity) {
            if (aspect != stored) {
                return 0;
            }
        } else if (!container.doesContainerAccept(aspect)) {
            return 0;
        }
        int filled = Math.max(0, Math.min(amount, capacity(container) - storedAmount(container)));
        if (modulate && filled > 0) {
            filled -= container.addToContainer(aspect, filled);
        }
        return filled;
    }
}
