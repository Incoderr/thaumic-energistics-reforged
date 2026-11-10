package thaumicenergistics.alchemy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class AlchemyCore extends Item {
    public static final int MAXIMUM_STORED_PATTERNS = 21;

    public AlchemyCore(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isCore(ItemStack stack) {
        return stack.getItem() instanceof AlchemyCore;
    }

    public static List<CruciblePattern> patterns(ItemStack core) {
        if (!isCore(core)) {
            return List.of();
        }
        List<CruciblePattern> stored = AlchemyFeature.PATTERNS.get(core);
        return stored == null ? List.of() : stored;
    }

    public static @Nullable CruciblePattern patternFor(ItemStack core, ItemStack result) {
        for (CruciblePattern pattern : patterns(core)) {
            if (pattern.producesSame(result)) {
                return pattern;
            }
        }
        return null;
    }

    public static boolean hasRoom(ItemStack core) {
        return patterns(core).size() < MAXIMUM_STORED_PATTERNS;
    }

    public static boolean add(ItemStack core, CruciblePattern pattern) {
        if (!isCore(core) || !pattern.isValid() || !hasRoom(core) || patternFor(core, pattern.result()) != null) {
            return false;
        }
        List<CruciblePattern> updated = new ArrayList<>(patterns(core));
        updated.add(pattern);
        AlchemyFeature.PATTERNS.set(core, List.copyOf(updated));
        return true;
    }

    public static boolean remove(ItemStack core, CruciblePattern pattern) {
        List<CruciblePattern> updated = new ArrayList<>(patterns(core));
        if (!updated.remove(pattern)) {
            return false;
        }
        if (updated.isEmpty()) {
            AlchemyFeature.PATTERNS.remove(core);
        } else {
            AlchemyFeature.PATTERNS.set(core, List.copyOf(updated));
        }
        return true;
    }
}
