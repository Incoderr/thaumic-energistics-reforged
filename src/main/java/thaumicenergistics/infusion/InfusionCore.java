package thaumicenergistics.infusion;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class InfusionCore extends Item {
    public static final int MAXIMUM_STORED_PATTERNS = 21;

    public InfusionCore(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isCore(ItemStack stack) {
        return stack.getItem() instanceof InfusionCore;
    }

    public static List<InfusionPattern> patterns(ItemStack core) {
        if (!isCore(core)) {
            return List.of();
        }
        List<InfusionPattern> stored = InfusionFeature.PATTERNS.get(core);
        return stored == null ? List.of() : stored;
    }

    public static @Nullable InfusionPattern patternFor(ItemStack core, ItemStack result) {
        for (InfusionPattern pattern : patterns(core)) {
            if (pattern.producesSame(result)) {
                return pattern;
            }
        }
        return null;
    }

    public static boolean hasRoom(ItemStack core) {
        return patterns(core).size() < MAXIMUM_STORED_PATTERNS;
    }

    public static boolean add(ItemStack core, InfusionPattern pattern) {
        if (!isCore(core) || !pattern.isValid() || !hasRoom(core) || patternFor(core, pattern.result()) != null) {
            return false;
        }
        List<InfusionPattern> updated = new ArrayList<>(patterns(core));
        updated.add(pattern);
        InfusionFeature.PATTERNS.set(core, List.copyOf(updated));
        return true;
    }

    public static boolean remove(ItemStack core, InfusionPattern pattern) {
        List<InfusionPattern> updated = new ArrayList<>(patterns(core));
        if (!updated.remove(pattern)) {
            return false;
        }
        if (updated.isEmpty()) {
            InfusionFeature.PATTERNS.remove(core);
        } else {
            InfusionFeature.PATTERNS.set(core, List.copyOf(updated));
        }
        return true;
    }
}
