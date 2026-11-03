package thaumicenergistics.essentia;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.EssentiaKeys;

public final class EssentiaContainers {
    public static final int PHIAL_CAPACITY = 8;
    public static final int JAR_CAPACITY = 64;

    private static final ResourceLocation GLASS_PHIAL = ResourceLocation.parse("thaumcraft:glass_phial");
    private static final ResourceLocation ESSENTIA_PHIAL = ResourceLocation.parse("thaumcraft:essentia_phial");
    private static final ResourceLocation FILLED_JAR = ResourceLocation.parse("thaumcraft:filled_jar");
    private static final ResourceLocation FILLED_VOID_JAR = ResourceLocation.parse("thaumcraft:filled_void_jar");
    private static final ResourceLocation JAR_LABEL = ResourceLocation.parse("thaumcraft:jar_label");
    private static final Map<ResourceLocation, ResourceLocation> EMPTY_JARS = Map.of(
        FILLED_JAR, ResourceLocation.parse("thaumcraft:warded_jar"),
        FILLED_VOID_JAR, ResourceLocation.parse("thaumcraft:void_jar")
    );

    public record Result(int amount, ItemStack stack) {
    }

    private EssentiaContainers() {
    }

    private static ResourceLocation id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static ItemStack create(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    private static boolean isPhial(ItemStack stack) {
        ResourceLocation id = id(stack);
        return id.equals(GLASS_PHIAL) || id.equals(ESSENTIA_PHIAL);
    }

    private static boolean isJar(ItemStack stack) {
        return EMPTY_JARS.containsKey(id(stack));
    }

    public static boolean isLabel(ItemStack stack) {
        return !stack.isEmpty() && id(stack).equals(JAR_LABEL);
    }

    public static boolean isContainer(ItemStack stack) {
        return capacity(stack) > 0;
    }

    public static int capacity(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (isPhial(stack)) {
            return PHIAL_CAPACITY;
        }
        return isJar(stack) ? JAR_CAPACITY : 0;
    }

    private static boolean holdsPartial(ItemStack stack) {
        return isJar(stack);
    }

    public static @Nullable Aspect storedAspect(ItemStack stack) {
        if (!isContainer(stack)) {
            return null;
        }
        AspectList aspects = AspectKeys.ASPECTS.get(stack);
        return aspects == null || aspects.isEmpty() ? null : aspects.getAspects().get(0);
    }

    public static int storedAmount(ItemStack stack) {
        Aspect aspect = storedAspect(stack);
        return aspect == null ? 0 : AspectKeys.ASPECTS.get(stack).getAmount(aspect);
    }

    public static @Nullable Aspect jarLabel(ItemStack stack) {
        return isJar(stack) ? EssentiaKeys.FILTER.get(stack) : null;
    }

    public static ItemStack withLabel(ItemStack label, @Nullable Aspect aspect) {
        ItemStack result = label.copyWithCount(1);
        if (aspect == null) {
            AspectKeys.ASPECTS.remove(result);
        } else {
            AspectKeys.ASPECTS.set(result, new AspectList().add(aspect, 1));
        }
        return result;
    }

    public static @Nullable Result drain(ItemStack container, int amount) {
        Aspect aspect = storedAspect(container);
        int stored = storedAmount(container);
        int capacity = capacity(container);
        if (aspect == null || stored <= 0) {
            return null;
        }
        int drained = amount;
        if (!holdsPartial(container)) {
            if (drained < capacity) {
                return null;
            }
            drained = capacity;
        } else {
            drained = Math.min(drained, stored);
        }
        if (drained <= 0) {
            return null;
        }
        ItemStack result;
        if (drained == stored || drained == capacity) {
            if (isPhial(container)) {
                result = create(GLASS_PHIAL);
            } else if (jarLabel(container) != null) {
                result = create(id(container));
                EssentiaKeys.FILTER.set(result, jarLabel(container));
            } else {
                result = create(EMPTY_JARS.get(id(container)));
            }
        } else {
            result = container.copyWithCount(1);
            AspectKeys.ASPECTS.set(result, new AspectList().add(aspect, stored - drained));
        }
        return new Result(drained, result);
    }

    public static @Nullable Result fill(ItemStack container, Aspect aspect, int amount) {
        int capacity = capacity(container);
        if (capacity == 0) {
            return null;
        }
        Aspect label = jarLabel(container);
        if (label != null && label != aspect) {
            return null;
        }
        Aspect stored = storedAspect(container);
        if (stored != null && stored != aspect) {
            return null;
        }
        int storedAmount = storedAmount(container);
        int remaining = capacity - storedAmount;
        if (remaining <= 0) {
            return null;
        }
        int filled;
        if (!holdsPartial(container)) {
            if (amount < capacity) {
                return null;
            }
            filled = capacity;
        } else {
            filled = Math.min(amount, remaining);
        }
        if (filled <= 0) {
            return null;
        }
        ItemStack result = isPhial(container) ? create(ESSENTIA_PHIAL) : container.copyWithCount(1);
        AspectKeys.ASPECTS.set(result, new AspectList().add(aspect, storedAmount + filled));
        return new Result(filled, result);
    }
}
