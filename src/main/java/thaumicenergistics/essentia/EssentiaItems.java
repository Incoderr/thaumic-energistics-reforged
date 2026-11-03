package thaumicenergistics.essentia;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;

public final class EssentiaItems {
    private static final Set<ResourceLocation> FILTER_ITEMS = Set.of(
        ResourceLocation.parse("thaumcraft:jar_label"),
        ResourceLocation.parse("thaumcraft:crystallized_essence"),
        ResourceLocation.parse("thaumcraft:wisp_essence"),
        ResourceLocation.parse("thaumcraft:essentia_phial"),
        ResourceLocation.parse("thaumcraft:filled_jar"),
        ResourceLocation.parse("thaumcraft:filled_void_jar")
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
