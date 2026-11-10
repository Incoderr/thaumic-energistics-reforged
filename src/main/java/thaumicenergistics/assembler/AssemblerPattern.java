package thaumicenergistics.assembler;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeFeature;

public final class AssemblerPattern implements IPatternDetails {
    private static final ResourceLocation PRIMORDIAL_PEARL = ResourceLocation.parse("thaumcraft:primordial_pearl");

    private final ArcanePattern pattern;
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    public AssemblerPattern(ArcanePattern pattern) {
        this.pattern = pattern;
        ItemStack encoded = new ItemStack(KnowledgeFeature.CORE.get());
        AssemblerFeature.PATTERN.set(encoded, pattern);
        this.definition = AEItemKey.of(encoded);
        List<IInput> list = new ArrayList<>();
        for (ItemStack ingredient : pattern.grid()) {
            if (!ingredient.isEmpty()) {
                list.add(new Input(ingredient));
            }
        }
        this.inputs = list.toArray(IInput[]::new);
        this.outputs = List.of(GenericStack.fromItemStack(pattern.result()));
    }

    public static boolean isValid(ArcanePattern pattern) {
        return pattern.isValid() && pattern.aspects().size() > 0 && pattern.grid().stream().anyMatch(stack -> !stack.isEmpty());
    }

    public ArcanePattern pattern() {
        return pattern;
    }

    public List<GenericStack> allResults() {
        List<GenericStack> results = new ArrayList<>();
        for (ItemStack ingredient : pattern.grid()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack remainder = remainder(ingredient);
            if (!remainder.isEmpty()) {
                results.add(GenericStack.fromItemStack(remainder));
            }
        }
        results.add(GenericStack.fromItemStack(pattern.result()));
        return results;
    }

    static ItemStack remainder(ItemStack ingredient) {
        if (ingredient.hasCraftingRemainingItem()) {
            return ingredient.getCraftingRemainingItem();
        }
        if (ingredient.is(BuiltInRegistries.ITEM.get(PRIMORDIAL_PEARL))) {
            return ingredient.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    static boolean canSubstitute(ItemStack target, ItemStack input) {
        if (ItemStack.isSameItemSameComponents(target, input)) {
            return true;
        }
        if (!target.is(input.getItem())) {
            return false;
        }
        if (!target.isDamageableItem()) {
            return false;
        }
        ItemStack left = target.copy();
        ItemStack right = input.copy();
        left.remove(DataComponents.DAMAGE);
        right.remove(DataComponents.DAMAGE);
        return ItemStack.isSameItemSameComponents(left, right);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AssemblerPattern that && definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    private record Input(ItemStack template) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[] {new GenericStack(AEItemKey.of(template), 1)};
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input instanceof AEItemKey item && canSubstitute(template, item.toStack());
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            if (!(template instanceof AEItemKey item)) {
                return null;
            }
            ItemStack remainder = remainder(item.toStack());
            return remainder.isEmpty() ? null : AEItemKey.of(remainder);
        }
    }

    public static final class Decoder implements IPatternDetailsDecoder {
        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return AssemblerFeature.PATTERN.has(stack);
        }

        @Override
        public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
            ArcanePattern stored = AssemblerFeature.PATTERN.get(what.toStack());
            return stored != null && isValid(stored) ? new AssemblerPattern(stored) : null;
        }
    }
}
