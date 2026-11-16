package thaumicenergistics.platform;

import net.neoforged.neoforge.registries.DeferredRegister;
import thaumicenergistics.ThaumicEnergistics;

public final class TeRegistration {
    private TeRegistration() {
    }

    public static final class Items extends DeferredRegister.Items {
        public Items() {
            super(ThaumicEnergistics.MODID);
        }
    }

    public static final class Blocks extends DeferredRegister.Blocks {
        public Blocks() {
            super(ThaumicEnergistics.MODID);
        }
    }
}
