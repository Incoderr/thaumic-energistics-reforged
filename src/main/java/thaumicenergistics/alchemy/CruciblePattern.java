package thaumicenergistics.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.AspectList;

public record CruciblePattern(ItemStack catalyst, ItemStack result, AspectList aspects) {
    public static final Codec<CruciblePattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("catalyst").forGetter(CruciblePattern::catalyst),
        ItemStack.CODEC.fieldOf("result").forGetter(CruciblePattern::result),
        AspectList.CODEC.fieldOf("aspects").forGetter(CruciblePattern::aspects)
    ).apply(instance, CruciblePattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CruciblePattern> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, CruciblePattern::catalyst,
        ItemStack.STREAM_CODEC, CruciblePattern::result,
        AspectList.STREAM_CODEC.cast(), CruciblePattern::aspects,
        CruciblePattern::new
    );

    public CruciblePattern {
        catalyst = catalyst.copyWithCount(1);
        result = result.copy();
        aspects = aspects.copy();
    }

    public boolean isValid() {
        return !catalyst.isEmpty() && !result.isEmpty() && aspects.size() > 0;
    }

    public boolean producesSame(ItemStack stack) {
        return ItemStack.matches(result, stack);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CruciblePattern pattern && ItemStack.matches(result, pattern.result) && ItemStack.matches(catalyst, pattern.catalyst)
            && aspects.equals(pattern.aspects);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(result);
    }
}
