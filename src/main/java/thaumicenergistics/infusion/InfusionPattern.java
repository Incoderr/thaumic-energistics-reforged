package thaumicenergistics.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.AspectList;

public record InfusionPattern(ItemStack central, List<ItemStack> components, ItemStack result, AspectList aspects) {
    public static final Codec<InfusionPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("central").forGetter(InfusionPattern::central),
        ItemStack.CODEC.listOf().fieldOf("components").forGetter(InfusionPattern::components),
        ItemStack.CODEC.fieldOf("result").forGetter(InfusionPattern::result),
        AspectList.CODEC.fieldOf("aspects").forGetter(InfusionPattern::aspects)
    ).apply(instance, InfusionPattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionPattern> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, InfusionPattern::central,
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), InfusionPattern::components,
        ItemStack.STREAM_CODEC, InfusionPattern::result,
        AspectList.STREAM_CODEC.cast(), InfusionPattern::aspects,
        InfusionPattern::new
    );

    public InfusionPattern {
        central = central.copyWithCount(1);
        components = components.stream().map(stack -> stack.copyWithCount(1)).toList();
        result = result.copy();
        aspects = aspects.copy();
    }

    public boolean isValid() {
        return !central.isEmpty() && !result.isEmpty() && components.stream().noneMatch(ItemStack::isEmpty);
    }

    public boolean producesSame(ItemStack stack) {
        return ItemStack.matches(result, stack);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InfusionPattern pattern && ItemStack.matches(result, pattern.result) && ItemStack.matches(central, pattern.central)
            && ItemStack.listMatches(components, pattern.components) && aspects.equals(pattern.aspects);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(result);
    }
}
