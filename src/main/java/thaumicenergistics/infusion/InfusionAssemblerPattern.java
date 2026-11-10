package thaumicenergistics.infusion;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaKey;

public final class InfusionAssemblerPattern implements IPatternDetails {
    private final InfusionPattern pattern;
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    public InfusionAssemblerPattern(InfusionPattern pattern) {
        this.pattern = pattern;
        ItemStack encoded = new ItemStack(InfusionFeature.CORE.get());
        InfusionFeature.PATTERN.set(encoded, pattern);
        this.definition = AEItemKey.of(encoded);
        Map<AEKey, Long> items = new LinkedHashMap<>();
        items.merge(AEItemKey.of(pattern.central()), 1L, Long::sum);
        for (ItemStack component : pattern.components()) {
            items.merge(AEItemKey.of(component), 1L, Long::sum);
        }
        List<IInput> list = new ArrayList<>();
        items.forEach((key, amount) -> list.add(new Input(key, amount)));
        for (Aspect aspect : pattern.aspects().getAspects()) {
            list.add(new Input(EssentiaKey.of(aspect), pattern.aspects().getAmount(aspect)));
        }
        this.inputs = list.toArray(IInput[]::new);
        this.outputs = List.of(GenericStack.fromItemStack(pattern.result()));
    }

    public InfusionPattern pattern() {
        return pattern;
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
        return other instanceof InfusionAssemblerPattern that && definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    private record Input(AEKey key, long amount) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[] {new GenericStack(key, 1)};
        }

        @Override
        public long getMultiplier() {
            return amount;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.equals(key);
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }

    public static final class Decoder implements IPatternDetailsDecoder {
        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return InfusionFeature.PATTERN.has(stack);
        }

        @Override
        public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
            InfusionPattern stored = InfusionFeature.PATTERN.get(what.toStack());
            return stored != null && stored.isValid() ? new InfusionAssemblerPattern(stored) : null;
        }
    }
}
