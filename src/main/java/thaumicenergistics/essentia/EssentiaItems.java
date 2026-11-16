package thaumicenergistics.essentia;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;

public final class EssentiaItems {
    private static final Set<Identifier> FILTER_ITEMS = Set.of(
        Identifier.parse("thaumcraft:jar_label"),
        Identifier.parse("thaumcraft:crystallized_essence"),
        Identifier.parse("thaumcraft:wisp_essence"),
        Identifier.parse("thaumcraft:essentia_phial"),
        Identifier.parse("thaumcraft:filled_jar"),
        Identifier.parse("thaumcraft:filled_void_jar")
    );

    private EssentiaItems() {
    }

    public static @Nullable Aspect filterAspect(ItemStack stack) {
        if (stack.isEmpty() || !FILTER_ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return null;
        }
        AspectList aspects = AspectKeys.ASPECTS.get(stack);
        if (aspects == null) {
            return null;
        }
        List<Aspect> list = aspects.getAspects();
        return list.isEmpty() ? null : list.get(0);
    }
}
