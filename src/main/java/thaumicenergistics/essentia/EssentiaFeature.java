package thaumicenergistics.essentia;

import appeng.api.stacks.AEKeyType;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumicenergistics.registry.ModKeyTypes;

public final class EssentiaFeature {
    public static final DeferredHolder<AEKeyType, EssentiaKeyType> KEY_TYPE = ModKeyTypes.KEY_TYPES.register("essentia", () -> EssentiaKeyType.TYPE);

    private EssentiaFeature() {
    }

    public static void load() {
    }
}
