package thaumicenergistics.alchemy;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaKey;

public final class CrucibleAssemblerPattern implements IPatternDetails {
    private final CruciblePattern pattern;
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    public CrucibleAssemblerPattern(CruciblePattern pattern) {
        this.pattern = pattern;
        ItemStack encoded = new ItemStack(AlchemyFeature.CORE.get());
        AlchemyFeature.PATTERN.set(encoded, pattern);
        this.definition = AEItemKey.of(encoded);
        List<IInput> list = new ArrayList<>();
        list.add(new Input(new GenericStack(AEItemKey.of(pattern.catalyst()), 1)));
        for (Aspect aspect : pattern.aspects().getAspects()) {
            list.add(new Input(new GenericStack(EssentiaKey.of(aspect), pattern.aspects().getAmount(aspect))));
        }
        this.inputs = list.toArray(IInput[]::new);
        this.outputs = List.of(GenericStack.fromItemStack(pattern.result()));
    }

    public CruciblePattern pattern() {
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
        return other instanceof CrucibleAssemblerPattern that && definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    private record Input(GenericStack template) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[] {new GenericStack(template.what(), 1)};
        }

        @Override
        public long getMultiplier() {
            return template.amount();
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.equals(template.what());
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }

    public static final class Decoder implements IPatternDetailsDecoder {
        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return AlchemyFeature.PATTERN.has(stack);
        }

        @Override
        public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
            CruciblePattern stored = AlchemyFeature.PATTERN.get(what.toStack());
            return stored != null && stored.isValid() ? new CrucibleAssemblerPattern(stored) : null;
        }
    }
}
