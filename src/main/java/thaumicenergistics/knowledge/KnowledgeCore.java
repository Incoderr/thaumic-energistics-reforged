package thaumicenergistics.knowledge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class KnowledgeCore {
    public static final int MAXIMUM_STORED_PATTERNS = 21;

    private KnowledgeCore() {
    }

    public static boolean isCore(ItemStack stack) {
        return stack.getItem() instanceof KnowledgeCoreItem;
    }

    public static List<ArcanePattern> patterns(ItemStack core) {
        if (!isCore(core)) {
            return List.of();
        }
        List<ArcanePattern> stored = KnowledgeFeature.PATTERNS.get(core);
        return stored == null ? List.of() : stored;
    }

    public static @Nullable ArcanePattern patternFor(ItemStack core, ItemStack result) {
        for (ArcanePattern pattern : patterns(core)) {
            if (pattern.producesSame(result)) {
                return pattern;
            }
        }
        return null;
    }

    public static boolean hasRoom(ItemStack core) {
        return patterns(core).size() < MAXIMUM_STORED_PATTERNS;
    }

    public static boolean add(ItemStack core, ArcanePattern pattern) {
        if (!isCore(core) || !pattern.isValid() || !hasRoom(core) || patternFor(core, pattern.result()) != null) {
            return false;
        }
        List<ArcanePattern> updated = new ArrayList<>(patterns(core));
        updated.add(pattern);
        KnowledgeFeature.PATTERNS.set(core, List.copyOf(updated));
        return true;
    }

    public static boolean remove(ItemStack core, ArcanePattern pattern) {
        List<ArcanePattern> updated = new ArrayList<>(patterns(core));
        if (!updated.remove(pattern)) {
            return false;
        }
        if (updated.isEmpty()) {
            KnowledgeFeature.PATTERNS.remove(core);
        } else {
            KnowledgeFeature.PATTERNS.set(core, List.copyOf(updated));
        }
        return true;
    }
}
